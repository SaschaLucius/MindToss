package lukulent.mindtoss.app.ui.settings

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val historyRepo = HistoryRepository(application)

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()
    private val _senderEmail = MutableStateFlow("")
    val senderEmail: StateFlow<String> = _senderEmail.asStateFlow()
    private val _noteRecipient = MutableStateFlow("")
    val noteRecipient: StateFlow<String> = _noteRecipient.asStateFlow()
    private val _taskRecipient = MutableStateFlow("")
    val taskRecipient: StateFlow<String> = _taskRecipient.asStateFlow()

    val fetchTitle = settingsRepo.fetchTitle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val theme = settingsRepo.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")
    val history = historyRepo.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _apiKey.value = settingsRepo.apiKey.first()
            _senderEmail.value = settingsRepo.senderEmail.first()
            _noteRecipient.value = settingsRepo.noteRecipient.first()
            _taskRecipient.value = settingsRepo.taskRecipient.first()
        }
    }

    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message.asSharedFlow()

    fun updateApiKey(value: String) {
        _apiKey.value = value
        viewModelScope.launch { settingsRepo.setApiKey(value) }
    }
    fun updateSenderEmail(value: String) {
        _senderEmail.value = value
        viewModelScope.launch { settingsRepo.setSenderEmail(value) }
    }
    fun updateNoteRecipient(value: String) {
        _noteRecipient.value = value
        viewModelScope.launch { settingsRepo.setNoteRecipient(value) }
    }
    fun updateTaskRecipient(value: String) {
        _taskRecipient.value = value
        viewModelScope.launch { settingsRepo.setTaskRecipient(value) }
    }
    fun updateFetchTitle(value: Boolean) = viewModelScope.launch { settingsRepo.setFetchTitle(value) }
    fun updateTheme(value: String) = viewModelScope.launch { settingsRepo.setTheme(value) }

    fun deleteHistoryEntry(id: String) = viewModelScope.launch {
        historyRepo.removeEntry(id)
    }

    fun editHistoryEntry(entry: HistoryEntry) {
        viewModelScope.launch {
            settingsRepo.setDraft(entry.content)
        }
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
            val senderVal = settingsRepo.effectiveSenderEmail.first()
            val recipient = when (entry.type) {
                MessageType.NOTE -> noteRecipient.first()
                MessageType.TASK -> taskRecipient.first()
            }

            if (apiKeyVal.isBlank() || recipient.isBlank()) {
                _message.emit("Fehlende Konfiguration")
                return@launch
            }

            val lines = entry.content.lines()
            val subject = lines.first()
            val body = if (lines.size > 1) lines.drop(1).joinToString("\n").trim() else subject

            val result = ResendApi.sendEmail(
                apiKey = apiKeyVal,
                from = senderVal,
                to = recipient,
                subject = subject,
                body = body,
            )

            if (result.isSuccess) {
                historyRepo.updateEntry(entry.id) { it.copy(status = SendStatus.SUCCESS, errorMessage = null) }
                _message.emit("Erneut gesendet")
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unbekannter Fehler"
                historyRepo.updateEntry(entry.id) { it.copy(status = SendStatus.FAILED, errorMessage = errorMsg) }
                _message.emit("Fehler: $errorMsg")
            }
        }
    }
}
