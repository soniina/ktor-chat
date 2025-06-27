package model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ChatEvent {
    @Serializable
    @SerialName("user_message")
    data class UserMessage(val sender: String, val text: String) : ChatEvent()

    @Serializable
    @SerialName("system_message")
    data class SystemMessage(val text: String) : ChatEvent()

    @Serializable
    @SerialName("error_message")
    data class ErrorMessage(val reason: String) : ChatEvent()

    @Serializable
    @SerialName("command_result")
    data class CommandResult(val command: String, val result: String) : ChatEvent()

    @Serializable
    @SerialName("close_connection")
    data class CloseConnection(val text: String) : ChatEvent()
}