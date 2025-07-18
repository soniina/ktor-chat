package learn.ktor.model

import kotlinx.datetime.LocalDateTime

data class Message (
    val id: Int,
    val sender: String,
    val recipient: String,
    val content: String,
    val timestamp: Long
)