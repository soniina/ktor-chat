package learn.ktor.services

import learn.ktor.connection.ConnectionManager
import learn.ktor.model.ChatEvent
import learn.ktor.repository.MessageRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CommandHandler(private val messageRepository: MessageRepository) {
    fun handle(user: String, text: String): ChatEvent {
        val parts = text.trim().split("\\s+".toRegex(), limit = 2)
        val command = parts[0]
        val argument = parts.getOrNull(1)

        return when (command) {
            "/help" -> ChatEvent.CommandResult(
                "help",
                "Available commands: /users, /bye, /history <user>"
            )
            "/users" -> ChatEvent.CommandResult(
                "users",
                "Online users: ${ConnectionManager.getOnlineUsers().joinToString()}"
            )
            "/history" -> {
                if (argument == null) {
                    ChatEvent.ErrorMessage("Usage: /history <username>")
                } else {
                    val history = messageRepository.getMessagesBetween(user, argument)
                    if (history.isEmpty()) {
                        ChatEvent.CommandResult("history", "No messages with $argument")
                    } else {
                        val formatted = history.joinToString("\n") { "[${formatTimestamp(it.timestamp)}] " +
                                "${it.sender}: ${it.content}" }
                        ChatEvent.CommandResult("history", formatted)
                    }
                }
            }
            "/bye" -> ChatEvent.CloseConnection("Goodbye!")
            else -> ChatEvent.ErrorMessage("Unknown command: $command")
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(timestamp))
    }
}