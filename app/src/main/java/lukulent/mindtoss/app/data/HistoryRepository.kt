package lukulent.mindtoss.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import lukulent.mindtoss.app.data.model.HistoryEntry

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "history")

class HistoryRepository(private val context: Context) {

    companion object {
        private val HISTORY_JSON = stringPreferencesKey("history_json")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val history: Flow<List<HistoryEntry>> = context.historyDataStore.data.map { prefs ->
        val raw = prefs[HISTORY_JSON] ?: "[]"
        try {
            json.decodeFromString<List<HistoryEntry>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun addEntry(entry: HistoryEntry) {
        context.historyDataStore.edit { prefs ->
            val current = try {
                json.decodeFromString<List<HistoryEntry>>(prefs[HISTORY_JSON] ?: "[]")
            } catch (_: Exception) {
                emptyList()
            }
            prefs[HISTORY_JSON] = json.encodeToString(current + entry)
        }
    }

    suspend fun removeEntry(id: String) {
        context.historyDataStore.edit { prefs ->
            val current = try {
                json.decodeFromString<List<HistoryEntry>>(prefs[HISTORY_JSON] ?: "[]")
            } catch (_: Exception) {
                emptyList()
            }
            prefs[HISTORY_JSON] = json.encodeToString(current.filter { it.id != id })
        }
    }

    suspend fun updateEntry(id: String, transform: (HistoryEntry) -> HistoryEntry) {
        context.historyDataStore.edit { prefs ->
            val current = try {
                json.decodeFromString<List<HistoryEntry>>(prefs[HISTORY_JSON] ?: "[]")
            } catch (_: Exception) {
                emptyList()
            }
            prefs[HISTORY_JSON] = json.encodeToString(current.map { if (it.id == id) transform(it) else it })
        }
    }

    suspend fun clearAll() {
        context.historyDataStore.edit { prefs ->
            prefs[HISTORY_JSON] = "[]"
        }
    }
}
