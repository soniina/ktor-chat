package learn.ktor.services

import learn.ktor.connection.OnlineUserProvider
import learn.ktor.model.ChatEvent
import learn.ktor.repositories.MessageRepository
import learn.ktor.repositories.UserRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CommandHandler(private val messageRepository: MessageRepository, private val userRepository: UserRepository, private val onlineUserProvider: OnlineUserProvider) {
    suspend fun handle(user: String, text: String): ChatEvent {
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
                "Online users: ${onlineUserProvider.getOnlineUsers().joinToString()}"
            )
            "/history" -> {
                if (argument == null) {
                    ChatEvent.ErrorMessage("Usage: /history <username>")
                } else {

                    val userId = userRepository.getIdByUsername(user)
                        ?: return ChatEvent.ErrorMessage("Internal error: user not found")

                    val targetUserId = userRepository.getIdByUsername(argument)
                        ?: return ChatEvent.ErrorMessage("Unknown user: $argument")

                    val history = messageRepository.getMessagesBetween(userId, targetUserId)

                    if (history.isEmpty()) {
                        ChatEvent.CommandResult("history", "No messages with $argument")
                    } else {
                        val formatted = history.joinToString("\n") { msg ->
                            val senderName = if (msg.senderId == targetUserId) argument else user
                            "[${formatTimestamp(msg.timestamp)}] $senderName: ${msg.content}"
                        }
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