package lukulent.mindtoss.app.ui.main

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lukulent.mindtoss.app.data.HistoryRepository
import lukulent.mindtoss.app.data.SettingsRepository
import lukulent.mindtoss.app.data.model.HistoryEntry
import lukulent.mindtoss.app.data.model.MessageType
import lukulent.mindtoss.app.data.model.SendStatus
import lukulent.mindtoss.app.network.ResendApi
import lukulent.mindtoss.app.worker.SendMailWorker
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val historyRepo = HistoryRepository(application)

    private val _draftText = MutableStateFlow("")
    val draftText: StateFlow<String> = _draftText.asStateFlow()

    init {
        viewModelScope.launch {
            _draftText.value = settingsRepo.draft.first()
        }
        // Pick up external draft changes (e.g. share intent)
        viewModelScope.launch {
            settingsRepo.draft.collect { stored ->
                if (stored != _draftText.value) {
                    _draftText.value = stored
                }
            }
        }
    }

    val taskRecipient = settingsRepo.taskRecipient
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _sendSuccess = MutableSharedFlow<Unit>()
    val sendSuccess: SharedFlow<Unit> = _sendSuccess.asSharedFlow()

    private val _queued = MutableSharedFlow<Unit>()
    val queued: SharedFlow<Unit> = _queued.asSharedFlow()

    // Pending work count for queue indicator
    val hasPendingWork = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val workManager = androidx.work.WorkManager.getInstance(application)
            workManager.getWorkInfosByTagFlow("send_mail").collect { workInfos ->
                hasPendingWork.value = workInfos.any { !it.state.isFinished }
            }
        }
    }

    fun updateDraft(text: String) {
        _draftText.value = text
        viewModelScope.launch {
            settingsRepo.setDraft(text)
        }
    }

    fun appendToDraft(text: String) {
        val current = _draftText.value
        val newText = if (current.isBlank()) text else "$current\n$text"
        _draftText.value = newText
        viewModelScope.launch {
            settingsRepo.setDraft(newText)
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun send(type: MessageType) {
        viewModelScope.launch {
            val text = draftText.value.trim()
            if (text.isBlank()) return@launch

            val apiKey = settingsRepo.apiKey.first()
            val senderEmail = settingsRepo.effectiveSenderEmail.first()
            val noteRecipient = settingsRepo.noteRecipient.first()
            val taskRecip = settingsRepo.taskRecipient.first()
            val smartSplit = settingsRepo.smartTaskSplit.first()

            if (apiKey.isBlank() || noteRecipient.isBlank()) {
                val errorMsg = "Bitte konfiguriere zuerst die Resend-Einstellungen"
                _error.value = errorMsg
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
                val errorMsg = "Kein Empfänger konfiguriert"
                _error.value = errorMsg
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

            // Split into chunks if smart task split is enabled and sending as task
            val chunks = if (smartSplit && type == MessageType.TASK) {
                text.split(Regex("\n\\s*\n")).map { it.trim() }.filter { it.isNotBlank() }
            } else {
                listOf(text)
            }

            _isSending.value = true
            _error.value = null
            val errors = mutableListOf<String>()
            var allQueued = false

            for (chunk in chunks) {
                val lines = chunk.lines()
                val subject = lines.first()
                val body = chunk

                if (isOnline()) {
                    val result = ResendApi.sendEmail(
                        apiKey = apiKey,
                        from = senderEmail,
                        to = recipient,
                        subject = subject,
                        body = body,
                    )
                    if (!result.isSuccess) {
                        errors.add(result.exceptionOrNull()?.message ?: "Unbekannter Fehler")
                    }
                } else {
                    allQueued = true
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
                        subject = subject,
                        body = body,
                        content = chunk,
                        messageType = type,
                        historyId = historyId,
                    )
                }
            }

            _isSending.value = false

            if (allQueued) {
                settingsRepo.setDraft("")
                _draftText.value = ""
                _queued.emit(Unit)
            } else if (errors.isEmpty()) {
                historyRepo.addEntry(
                    HistoryEntry(
                        id = UUID.randomUUID().toString(),
                        content = text,
                        timestamp = System.currentTimeMillis(),
                        type = type,
                        status = SendStatus.SUCCESS,
                    )
                )
                settingsRepo.setDraft("")
                _draftText.value = ""
                _sendSuccess.emit(Unit)
            } else {
                val errorMsg = errors.joinToString("\n")
                _error.value = errorMsg
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
            }
        }
    }

    private fun isOnline(): Boolean {
        val cm = getApplication<Application>().getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
