package lukulent.mindtoss.app.ui.main

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lukulent.mindtoss.app.data.HistoryRepository
import lukulent.mindtoss.app.data.SettingsRepository
import lukulent.mindtoss.app.data.ShareInbox
import lukulent.mindtoss.app.data.model.HistoryEntry
import lukulent.mindtoss.app.data.model.MessageType
import lukulent.mindtoss.app.data.model.SendStatus
import lukulent.mindtoss.app.network.MailFormat
import lukulent.mindtoss.app.network.ResendApi
import lukulent.mindtoss.app.network.ResendErrorMapper
import lukulent.mindtoss.app.network.UserFacingError
import lukulent.mindtoss.app.worker.SendMailWorker
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val sendInFlight = AtomicBoolean(false)

    private val settingsRepo = SettingsRepository(application)
    private val historyRepo = HistoryRepository(application)

    private val _draftText = MutableStateFlow("")
    val draftText: StateFlow<String> = _draftText.asStateFlow()

    private val draftToSave = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private var localDraftGeneration = 0L
    private var savedDraftGeneration = 0L

    init {
        viewModelScope.launch {
            _draftText.value = settingsRepo.draft.first()
            drainShareInbox()
            ShareInbox.signal.collect { drainShareInbox() }
        }
        viewModelScope.launch {
            draftToSave.collectLatest { text ->
                val generation = localDraftGeneration
                settingsRepo.setDraft(text)
                savedDraftGeneration = generation
            }
        }
        // Pick up external draft replaces (e.g. "edit" from history) when not mid-typing.
        viewModelScope.launch {
            settingsRepo.draft.collect { stored ->
                if (localDraftGeneration != savedDraftGeneration) return@collect
                if (stored != _draftText.value) {
                    _draftText.value = stored
                }
            }
        }
        viewModelScope.launch {
            val workManager = androidx.work.WorkManager.getInstance(application)
            workManager.getWorkInfosByTagFlow("send_mail").collect { workInfos ->
                hasPendingWork.value = workInfos.any { !it.state.isFinished }
            }
        }
    }

    val taskRecipient = settingsRepo.taskRecipient
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val isConfigured = combine(settingsRepo.apiKey, settingsRepo.noteRecipient) { key, recipient ->
        key.isNotBlank() && recipient.isNotBlank()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _error = MutableStateFlow<UserFacingError?>(null)
    val error: StateFlow<UserFacingError?> = _error.asStateFlow()

    private val _sendSuccess = MutableSharedFlow<Unit>()
    val sendSuccess: SharedFlow<Unit> = _sendSuccess.asSharedFlow()

    private val _queued = MutableSharedFlow<Unit>()
    val queued: SharedFlow<Unit> = _queued.asSharedFlow()

    // Pending work count for queue indicator
    val hasPendingWork = MutableStateFlow(false)

    fun updateDraft(text: String) {
        _draftText.value = text
        localDraftGeneration++
        draftToSave.tryEmit(text)
    }

    fun appendToDraft(text: String) {
        val current = _draftText.value
        val newText = if (current.isBlank()) text else "$current\n$text"
        updateDraft(newText)
    }

    private fun drainShareInbox() {
        val addition = ShareInbox.poll() ?: return
        val current = _draftText.value
        val newText = if (current.isBlank()) addition else "$current\n\n$addition"
        updateDraft(newText)
    }

    fun clearError() {
        _error.value = null
    }

    fun send(type: MessageType) {
        if (!sendInFlight.compareAndSet(false, true)) return
        viewModelScope.launch {
            _isSending.value = true
            _error.value = null
            try {
                val text = draftText.value.trim()
                if (text.isBlank()) return@launch

                val apiKey = settingsRepo.apiKey.first()
                val senderEmail = settingsRepo.effectiveSenderEmail.first()
                val noteRecipient = settingsRepo.noteRecipient.first()
                val taskRecip = settingsRepo.taskRecipient.first()
                val smartSplit = settingsRepo.smartTaskSplit.first()

                if (apiKey.isBlank() || noteRecipient.isBlank()) {
                    val errorMsg = "Bitte konfiguriere zuerst die Resend-Einstellungen"
                    _error.value = UserFacingError(errorMsg, errorMsg)
                    historyRepo.addEntry(
                        HistoryEntry(
                            id = UUID.randomUUID().toString(),
                            content = text,
                            timestamp = System.currentTimeMillis(),
                            type = type,
                            status = SendStatus.FAILED,
                            errorMessage = errorMsg,
                        )
                    )
                    return@launch
                }

                val recipient = when (type) {
                    MessageType.NOTE -> noteRecipient
                    MessageType.TASK -> taskRecip
                }

                if (recipient.isBlank()) {
                    val errorMsg = "Kein Empfänger für Aufgaben konfiguriert"
                    _error.value = UserFacingError(errorMsg, errorMsg)
                    historyRepo.addEntry(
                        HistoryEntry(
                            id = UUID.randomUUID().toString(),
                            content = text,
                            timestamp = System.currentTimeMillis(),
                            type = type,
                            status = SendStatus.FAILED,
                            errorMessage = errorMsg,
                        )
                    )
                    return@launch
                }

                val chunks = MailFormat.taskChunks(text, smartSplit && type == MessageType.TASK)

                var queuedAny = false
                var sentAny = false
                var failedAt: Int? = null
                var failRaw: String? = null

                for ((index, chunk) in chunks.withIndex()) {
                    val parts = MailFormat.subjectAndBody(chunk)

                    if (isOnline()) {
                        val result = ResendApi.sendEmail(
                            apiKey = apiKey,
                            from = senderEmail,
                            to = recipient,
                            subject = parts.subject,
                            body = parts.body,
                        )
                        if (result.isSuccess) {
                            sentAny = true
                            historyRepo.addEntry(
                                HistoryEntry(
                                    id = UUID.randomUUID().toString(),
                                    content = chunk,
                                    timestamp = System.currentTimeMillis(),
                                    type = type,
                                    status = SendStatus.SUCCESS,
                                    resendId = result.getOrNull(),
                                )
                            )
                        } else {
                            failedAt = index
                            failRaw = result.exceptionOrNull()?.message ?: "Unbekannter Fehler"
                            val mapped = ResendErrorMapper.map(failRaw)
                            historyRepo.addEntry(
                                HistoryEntry(
                                    id = UUID.randomUUID().toString(),
                                    content = chunk,
                                    timestamp = System.currentTimeMillis(),
                                    type = type,
                                    status = SendStatus.FAILED,
                                    errorMessage = mapped.message,
                                )
                            )
                            break
                        }
                    } else {
                        queuedAny = true
                        val historyId = UUID.randomUUID().toString()
                        historyRepo.addEntry(
                            HistoryEntry(
                                id = historyId,
                                content = chunk,
                                timestamp = System.currentTimeMillis(),
                                type = type,
                                status = SendStatus.QUEUED,
                            )
                        )
                        SendMailWorker.enqueue(
                            context = getApplication(),
                            to = recipient,
                            subject = parts.subject,
                            body = parts.body,
                            content = chunk,
                            messageType = type,
                            historyId = historyId,
                        )
                    }
                }

                if (failedAt != null) {
                    updateDraft(chunks.drop(failedAt).joinToString("\n\n"))
                    _error.value = ResendErrorMapper.map(failRaw)
                } else {
                    updateDraft("")
                    if (queuedAny) {
                        _queued.emit(Unit)
                    } else if (sentAny) {
                        _sendSuccess.emit(Unit)
                    }
                }
            } finally {
                _isSending.value = false
                sendInFlight.set(false)
            }
        }
    }

    private fun isOnline(): Boolean {
        val cm = getApplication<Application>().getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
