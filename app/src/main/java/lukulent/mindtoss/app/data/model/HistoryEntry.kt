package lukulent.mindtoss.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class SendStatus {
    SUCCESS, FAILED, QUEUED
}

@Serializable
data class HistoryEntry(
    val id: String,
    val content: String,
    val timestamp: Long,
    val type: MessageType,
    val status: SendStatus = SendStatus.SUCCESS,
    val errorMessage: String? = null,
)
