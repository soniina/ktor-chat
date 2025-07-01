package learn.ktor.services

import learn.ktor.connection.ConnectionManager
import learn.ktor.model.ChatEvent

class CommandHandler {
    suspend fun handle(user: String, command: String): ChatEvent {
        return when (command) {
            "/help" -> ChatEvent.CommandResult(
                "help",
                "Available commands: /users, /bye"
            )
            "/users" -> ChatEvent.CommandResult(
                "users",
                "Online users: ${ConnectionManager.getOnlineUsers().joinToString()}"
            )
            "/bye" -> ChatEvent.CloseConnection("Goodbye!")
            else -> ChatEvent.ErrorMessage("Unknown command: $command")
        }
    }
}