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
import lukulent.mindtoss.app.network.ResendApi
import lukulent.mindtoss.app.worker.SendMailWorker
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val historyRepo = HistoryRepository(application)

    val draftText = settingsRepo.draft
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

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
        viewModelScope.launch {
            settingsRepo.setDraft(text)
        }
    }

    fun appendToDraft(text: String) {
        viewModelScope.launch {
            val current = draftText.value
            val newText = if (current.isBlank()) text else "$current\n$text"
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

            if (apiKey.isBlank() || noteRecipient.isBlank()) {
                _error.value = "Bitte konfiguriere zuerst die Resend-Einstellungen"
                return@launch
            }

            val recipient = when (type) {
                MessageType.NOTE -> noteRecipient
                MessageType.TASK -> taskRecip
            }

            if (recipient.isBlank()) {
                _error.value = "Kein Empfänger konfiguriert"
                return@launch
            }

            val lines = text.lines()
            val subject = lines.first()
            val body = if (lines.size > 1) lines.drop(1).joinToString("\n").trim() else subject

            if (isOnline()) {
                _isSending.value = true
                _error.value = null

                val result = ResendApi.sendEmail(
                    apiKey = apiKey,
                    from = senderEmail,
                    to = recipient,
                    subject = subject,
                    body = body,
                )

                _isSending.value = false

                if (result.isSuccess) {
                    historyRepo.addEntry(
                        HistoryEntry(
                            id = UUID.randomUUID().toString(),
                            content = text,
                            timestamp = System.currentTimeMillis(),
                            type = type,
                        )
                    )
                    settingsRepo.setDraft("")
                    _sendSuccess.emit(Unit)
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Unbekannter Fehler"
                }
            } else {
                SendMailWorker.enqueue(
                    context = getApplication(),
                    to = recipient,
                    subject = subject,
                    body = body,
                    content = text,
                    messageType = type,
                )
                settingsRepo.setDraft("")
                _queued.emit(Unit)
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
