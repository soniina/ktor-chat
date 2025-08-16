package learn.ktor.model

data class Message (
    val id: Int,
    val senderId: Int,
    val recipientId: Int,
    val content: String,
    val timestamp: Long,
    val delivered: Boolean = false
)
