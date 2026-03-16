package lukulent.mindtoss.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val API_KEY = stringPreferencesKey("api_key")
        val SENDER_EMAIL = stringPreferencesKey("sender_email")
        val NOTE_RECIPIENT = stringPreferencesKey("note_recipient")
        val TASK_RECIPIENT = stringPreferencesKey("task_recipient")
        val FETCH_TITLE = booleanPreferencesKey("fetch_title")
        val SMART_TASK_SPLIT = booleanPreferencesKey("smart_task_split")
        val THEME = stringPreferencesKey("theme")
        val DRAFT = stringPreferencesKey("draft")
        const val DEFAULT_SENDER = "onboarding@resend.dev"
    }

    val apiKey: Flow<String> = context.dataStore.data.map { it[API_KEY] ?: "" }
    val senderEmail: Flow<String> = context.dataStore.data.map { it[SENDER_EMAIL] ?: "" }
    val effectiveSenderEmail: Flow<String> = context.dataStore.data.map {
        val value = it[SENDER_EMAIL] ?: ""
        value.ifBlank { DEFAULT_SENDER }
    }
    val noteRecipient: Flow<String> = context.dataStore.data.map { it[NOTE_RECIPIENT] ?: "" }
    val taskRecipient: Flow<String> = context.dataStore.data.map { it[TASK_RECIPIENT] ?: "" }
    val fetchTitle: Flow<Boolean> = context.dataStore.data.map { it[FETCH_TITLE] ?: true }
    val smartTaskSplit: Flow<Boolean> = context.dataStore.data.map { it[SMART_TASK_SPLIT] ?: false }
    val theme: Flow<String> = context.dataStore.data.map { it[THEME] ?: "system" }
    val draft: Flow<String> = context.dataStore.data.map { it[DRAFT] ?: "" }

    suspend fun setApiKey(value: String) { context.dataStore.edit { it[API_KEY] = value } }
    suspend fun setSenderEmail(value: String) { context.dataStore.edit { it[SENDER_EMAIL] = value } }
    suspend fun setNoteRecipient(value: String) { context.dataStore.edit { it[NOTE_RECIPIENT] = value } }
    suspend fun setTaskRecipient(value: String) { context.dataStore.edit { it[TASK_RECIPIENT] = value } }
    suspend fun setFetchTitle(value: Boolean) { context.dataStore.edit { it[FETCH_TITLE] = value } }
    suspend fun setSmartTaskSplit(value: Boolean) { context.dataStore.edit { it[SMART_TASK_SPLIT] = value } }
    suspend fun setTheme(value: String) { context.dataStore.edit { it[THEME] = value } }
    suspend fun setDraft(value: String) { context.dataStore.edit { it[DRAFT] = value } }
}
