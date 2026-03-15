package lukulent.mindtoss.app.ui.settings

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lukulent.mindtoss.app.data.HistoryRepository
import lukulent.mindtoss.app.data.SettingsRepository
import lukulent.mindtoss.app.data.model.HistoryEntry
import lukulent.mindtoss.app.data.model.MessageType
import lukulent.mindtoss.app.network.ResendApi

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val historyRepo = HistoryRepository(application)

    val apiKey = settingsRepo.apiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val senderEmail = settingsRepo.senderEmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val noteRecipient = settingsRepo.noteRecipient
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val taskRecipient = settingsRepo.taskRecipient
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val fetchTitle = settingsRepo.fetchTitle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val theme = settingsRepo.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")
    val history = historyRepo.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message.asSharedFlow()

    fun updateApiKey(value: String) = viewModelScope.launch { settingsRepo.setApiKey(value) }
    fun updateSenderEmail(value: String) = viewModelScope.launch { settingsRepo.setSenderEmail(value) }
    fun updateNoteRecipient(value: String) = viewModelScope.launch { settingsRepo.setNoteRecipient(value) }
    fun updateTaskRecipient(value: String) = viewModelScope.launch { settingsRepo.setTaskRecipient(value) }
    fun updateFetchTitle(value: Boolean) = viewModelScope.launch { settingsRepo.setFetchTitle(value) }
    fun updateTheme(value: String) = viewModelScope.launch { settingsRepo.setTheme(value) }

    fun deleteHistoryEntry(id: String) = viewModelScope.launch {
        historyRepo.removeEntry(id)
    }

    fun copyHistoryEntry(entry: HistoryEntry) {
        val clipboard = getApplication<Application>()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("MindToss", entry.content))
        viewModelScope.launch { _message.emit("Kopiert") }
    }

    fun resendHistoryEntry(entry: HistoryEntry) {
        viewModelScope.launch {
            val apiKeyVal = apiKey.first()
            val senderVal = senderEmail.first()
            val recipient = when (entry.type) {
                MessageType.NOTE -> noteRecipient.first()
                MessageType.TASK -> taskRecipient.first()
            }

            if (apiKeyVal.isBlank() || senderVal.isBlank() || recipient.isBlank()) {
                _message.emit("Fehlende Konfiguration")
                return@launch
            }

            val lines = entry.content.lines()
            val subject = lines.first()
            val body = if (lines.size > 1) lines.drop(1).joinToString("\n") else ""

            val result = ResendApi.sendEmail(
                apiKey = apiKeyVal,
                from = senderVal,
                to = recipient,
                subject = subject,
                body = body,
            )

            if (result.isSuccess) {
                _message.emit("Erneut gesendet")
            } else {
                _message.emit("Fehler: ${result.exceptionOrNull()?.message}")
            }
        }
    }
}
