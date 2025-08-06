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
import learn.ktor.model.User
import learn.ktor.repositories.MessageRepository
import learn.ktor.repositories.UserRepository
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test

@ExtendWith(MockKExtension::class)
class ChatServiceTest {

    @MockK
    lateinit var connectionManager: ConnectionManager

    @MockK
    lateinit var messageRepository: MessageRepository

    @MockK
    lateinit var userRepository: UserRepository

    @MockK
    lateinit var commandHandler: CommandHandler

    @InjectMockKs
    lateinit var chatService: ChatService

    @Test
    fun `should register user, send welcome message and receive undelivered messages`() = runTest {
        val user = User(1, "user", "password")
        val sender = User(2, "sender", "password")
        val session = mockk<DefaultWebSocketSession>()

        coEvery { connectionManager.register(user.username, session) } just Runs
        coEvery { userRepository.getIdByUsername(user.username) } returns user.id
        coEvery { connectionManager.sendTo(user.username, any()) } just Runs
        coEvery { connectionManager.isOnline(user.username) } returns true
        coEvery { messageRepository.getUndeliveredMessagesFor(user.id) } returns listOf(Message(1, sender.id, user.id, "Hi!", System.currentTimeMillis()))
        coEvery { userRepository.getUsernameById(sender.id) } returns sender.username
        coEvery { messageRepository.markAsDelivered(any()) } just Runs

        chatService.handleConnection(user.username, session)

        coVerify {
            connectionManager.register(user.username, session)
            userRepository.getIdByUsername(user.username)
            connectionManager.sendTo(user.username, any<ChatEvent.SystemMessage>())
            messageRepository.getUndeliveredMessagesFor(user.id)
            connectionManager.isOnline(user.username)
            connectionManager.sendTo(user.username, any<ChatEvent.UserMessage>())
            messageRepository.markAsDelivered(1)
        }
    }

    @Test
    fun `should disconnect unfounded in repository user`() = runTest {
        val username = "user"
        val session = mockk<DefaultWebSocketSession>()

        coEvery { connectionManager.register(username, session) } just Runs
        coEvery { userRepository.getIdByUsername(username) } returns null
        coEvery { connectionManager.sendTo(username, any()) } just Runs
        coEvery { connectionManager.unregister(username) } just Runs

        chatService.handleConnection(username, session)

        coVerify {
            connectionManager.register(username, session)
            userRepository.getIdByUsername(username)
            connectionManager.sendTo(username, ChatEvent.ErrorMessage("Internal error: user not found. Please try to reconnect."))
            connectionManager.unregister(username)
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
        val sender = User(1, "sender", "password")
        val recipient = User(2, "recipient", "password")
        val message = "Hello there!"

        coEvery { userRepository.getIdByUsername(sender.username) } returns sender.id
        coEvery { userRepository.getIdByUsername(recipient.username) } returns recipient.id
        coEvery { messageRepository.saveMessage(sender.id, recipient.id, message) } returns
                Message(1, sender.id, recipient.id, message, System.currentTimeMillis())
        coEvery { connectionManager.isOnline(recipient.username) } returns true
        coEvery { connectionManager.sendTo(sender.username, any()) } just Runs
        coEvery { connectionManager.sendTo(recipient.username, any()) } just Runs

        chatService.handleMessage(sender.username, "@${recipient.username} $message")

        coVerifyOrder {
            userRepository.getIdByUsername(sender.username)
            userRepository.getIdByUsername(recipient.username)
            messageRepository.saveMessage(sender.id, recipient.id, message)
            connectionManager.isOnline(recipient.username)
            connectionManager.sendTo(recipient.username, ChatEvent.UserMessage(sender.username, message))
            connectionManager.sendTo(sender.username, ChatEvent.CommandResult("sent", "to ${recipient.username}"))
        }
    }

    @Test
    fun `should store message and notify sender when recipient is offline`() = runTest {
        val sender = User(1, "sender", "password")
        val recipient = User(2, "offline_recipient", "password")
        val message = "Hello!"

        coEvery { userRepository.getIdByUsername(sender.username) } returns sender.id
        coEvery { userRepository.getIdByUsername(recipient.username) } returns recipient.id
        coEvery { messageRepository.saveMessage(sender.id, recipient.id, message) } returns
                Message(1, sender.id, recipient.id, message, System.currentTimeMillis())
        coEvery { connectionManager.isOnline(recipient.username) } returns false
        coEvery { connectionManager.sendTo(sender.username, any()) } just Runs

        chatService.handleMessage(sender.username, "@${recipient.username} $message")

        coVerifyOrder {
            userRepository.getIdByUsername(sender.username)
            userRepository.getIdByUsername(recipient.username)
            messageRepository.saveMessage(sender.id, recipient.id, message)
            connectionManager.isOnline(recipient.username)
            connectionManager.sendTo(sender.username, ChatEvent.CommandResult("queued", "to ${recipient.username} (offline — will be delivered later)"))
        }
    }

    @Test
    fun `should return internal error when invoking user not found in repository`() = runTest {
        val username = "user"
        val message = "Hi!"

        coEvery { userRepository.getIdByUsername(username) } returns null
        coEvery { connectionManager.sendTo(username, any()) } just Runs

        chatService.handleMessage(username, "@someone $message")

        coVerifyOrder {
            userRepository.getIdByUsername(username)
            connectionManager.sendTo(username, ChatEvent.ErrorMessage("Internal error: user not found"))
        }

    }

    @Test
    fun `should return error when recipient user not found`() = runTest {
        val sender = User(1, "sender", "password")
        val recipient = "recipient"
        val message = "Hello!"

        coEvery { userRepository.getIdByUsername(sender.username) } returns sender.id
        coEvery { userRepository.getIdByUsername(recipient) } returns null
        coEvery { connectionManager.sendTo(sender.username, any()) } just Runs

        chatService.handleMessage(sender.username, "@$recipient $message")

        coVerifyOrder {
            userRepository.getIdByUsername(sender.username)
            userRepository.getIdByUsername(recipient)
            connectionManager.sendTo(sender.username, ChatEvent.ErrorMessage("Unknown user: $recipient"))
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
    fun `should return error for unrecognized input`() = runTest {
        val user = "user"
        val message = "invalid message"

        coEvery { connectionManager.sendTo(user, any()) } just Runs

        chatService.handleMessage(user, message)

        coVerify {
            connectionManager.sendTo(user, ChatEvent.ErrorMessage("Unrecognized input"))
        }
    }
}