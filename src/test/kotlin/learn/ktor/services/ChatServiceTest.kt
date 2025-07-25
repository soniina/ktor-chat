package learn.ktor.services

import io.ktor.websocket.*
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import learn.ktor.connection.ConnectionManager
import learn.ktor.model.ChatEvent
import learn.ktor.model.Message
import learn.ktor.repositories.MessageRepository
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test

@ExtendWith(MockKExtension::class)
class ChatServiceTest {

    @MockK
    lateinit var connectionManager: ConnectionManager

    @MockK
    lateinit var messageRepository: MessageRepository

    @MockK
    lateinit var commandHandler: CommandHandler

    @InjectMockKs
    lateinit var chatService: ChatService

    @Test
    fun `should register user, send welcome message and receive undelivered messages`() = runTest {
        val user = "user"
        val sender = "sender"
        val session = mockk<DefaultWebSocketSession>()

        coEvery { connectionManager.register(user, session) } just Runs
        coEvery { connectionManager.sendTo(user, any()) } just Runs
        coEvery { connectionManager.isOnline(user) } returns true
        coEvery { messageRepository.getUndeliveredMessagesFor(user) } returns listOf(Message(1, sender, user, "Hi!", System.currentTimeMillis()))
        coEvery { messageRepository.markAsDelivered(any()) } just Runs

        chatService.handleConnection(user, session)

        coVerify {
            connectionManager.register(user, session)
            connectionManager.sendTo(user, any<ChatEvent.SystemMessage>())
            messageRepository.getUndeliveredMessagesFor(user)
            connectionManager.isOnline(user)
            connectionManager.sendTo(user, any<ChatEvent.UserMessage>())
            messageRepository.markAsDelivered(1)
        }
    }

    @Test
    fun `should handle command when message starts with slash`() = runTest {
        val user = "user"
        val command = "/help"
        val event = ChatEvent.CommandResult(
            "help",
            "Available commands: /users, /bye, /history <user>"
        )

        coEvery { commandHandler.handle(user, command) } returns event
        coEvery { connectionManager.sendTo(user, any()) } just Runs

        chatService.handleMessage(user, command)

        coVerify {
            commandHandler.handle(user, command)
            connectionManager.sendTo(user, event)
        }
    }

    @Test
    fun `should handle direct message`() = runTest {
        val sender = "sender"
        val recipient = "recipient"
        val message = "Hello there!"

        coEvery { messageRepository.saveMessage(sender, recipient, message) } returns
                Message(1, sender, recipient, message, System.currentTimeMillis())
        coEvery { connectionManager.isOnline(recipient) } returns true
        coEvery { connectionManager.sendTo(sender, any()) } just Runs
        coEvery { connectionManager.sendTo(recipient, any()) } just Runs

        chatService.handleMessage(sender, "@$recipient $message")

        coVerifyOrder {
            messageRepository.saveMessage("sender", "recipient", message)
            connectionManager.isOnline(recipient)
            connectionManager.sendTo(recipient, ChatEvent.UserMessage(sender, message))
            connectionManager.sendTo(sender, ChatEvent.CommandResult("sent", "to $recipient"))
        }
    }

    @Test
    fun `should return error for unrecognized input`() = runTest {
        val user = "user"
        val message = "invalid message"

        coEvery { connectionManager.sendTo(user, any()) } just Runs

        chatService.handleMessage(user, message)

        coVerify {
            connectionManager.sendTo(user, ChatEvent.ErrorMessage("Unrecognized input"))
        }
    }

    @Test
    fun `should return error for malformed direct message`() = runTest {
        val user = "user"
        val invalidMessages = listOf(
            "@username",
            "@  ",
            "@"
        )

        coEvery { connectionManager.sendTo(user, any()) } just Runs

        invalidMessages.forEach { message ->
            chatService.handleMessage(user, message)
            coVerify(atLeast = 1) {
                connectionManager.sendTo(user, match {
                    it is ChatEvent.ErrorMessage
                })
            }
        }
    }

    @Test
    fun `should close session on bye command`() = runTest {
        val user = "user"
        val session = mockk<DefaultWebSocketSession>()
        val byeEvent = ChatEvent.CloseConnection("Goodbye!")

        coEvery { commandHandler.handle(user, "/bye") } returns byeEvent
        coEvery { connectionManager.getSession(user) } returns session
        coEvery { connectionManager.sendTo(user, byeEvent) } just Runs
        coEvery { session.close(CloseReason(CloseReason.Codes.NORMAL, "User left")) } just Runs

        chatService.handleMessage(user, "/bye")

        coVerify {
            commandHandler.handle(user, "/bye")
            connectionManager.sendTo(user, byeEvent)
            connectionManager.getSession(user)
        }
    }

    @Test
    fun `should unregister user`() = runTest {
        val user = "user"

        coEvery { connectionManager.unregister(user) } just Runs

        chatService.cleanUp(user)

        coVerify(exactly = 1) { connectionManager.unregister(user) }
    }

    @Test
    fun `should store message and notify sender when recipient is offline`() = runTest {
        val sender = "sender"
        val recipient = "offlineUser"
        val content = "Hello!"

        coEvery { messageRepository.saveMessage(sender, recipient, content) } returns
                Message(1, sender, recipient, content, System.currentTimeMillis())
        coEvery { connectionManager.isOnline(recipient) } returns false
        coEvery { connectionManager.sendTo(sender, any()) } just Runs

        chatService.handleMessage(sender, "@$recipient $content")

        coVerifyOrder {
            messageRepository.saveMessage(sender, recipient, content)
            connectionManager.isOnline(recipient)
            connectionManager.sendTo(sender, ChatEvent.CommandResult("queued", "to $recipient (offline — will be delivered later)"))
        }
    }
}