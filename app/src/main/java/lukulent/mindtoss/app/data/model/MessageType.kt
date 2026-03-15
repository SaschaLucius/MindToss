package lukulent.mindtoss.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class MessageType {
    NOTE, TASK
}
