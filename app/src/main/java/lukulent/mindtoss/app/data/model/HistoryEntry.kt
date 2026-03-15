package lukulent.mindtoss.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class HistoryEntry(
    val id: String,
    val content: String,
    val timestamp: Long,
    val type: MessageType,
)
