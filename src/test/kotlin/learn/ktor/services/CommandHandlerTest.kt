package learn.ktor.services

import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import learn.ktor.connection.OnlineUserProvider
import learn.ktor.model.ChatEvent
import learn.ktor.model.Message
import learn.ktor.repositories.MessageRepository
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.*

@ExtendWith(MockKExtension::class)
class CommandHandlerTest {

    @MockK
    lateinit var messageRepository: MessageRepository

    @MockK
    lateinit var onlineUserProvider: OnlineUserProvider

    @InjectMockKs
    lateinit var handler: CommandHandler

    @Test
    fun `should return all available commands`() = runTest {
        val event = handler.handle("alice", "/help")
        assertIs<ChatEvent.CommandResult>(event)
        assertTrue(event.result.contains("Available commands"))
    }

    @Test
    fun `should return online users`() = runTest {
        every { onlineUserProvider.getOnlineUsers() } returns listOf("alice", "bob")

        val event = handler.handle("alice", "/users")
        assertIs<ChatEvent.CommandResult>(event)
        assertTrue("alice" in event.result)
        assertTrue("bob" in event.result)
    }

    @Test
    fun `should require username for history command`() = runTest {
        val event = handler.handle("alice", "/history")

        assertIs<ChatEvent.ErrorMessage>(event)
        assertEquals(event.reason, "Usage: /history <username>")
    }

    @Test
    fun `should handle empty history`() = runTest {
        coEvery { messageRepository.getMessagesBetween("alice", any()) } returns emptyList()

        val event = handler.handle("alice", "/history bob")

        assertIs<ChatEvent.CommandResult>(event)
        assertEquals("history", event.command)
        assertEquals(event.result, "No messages with bob")
    }

    @Test
    fun `should return messages between users in chronological order`() = runTest {
        val now = System.currentTimeMillis()
        val messages = listOf(
            Message(1, "alice", "bob", "First", now - 10000),
            Message(2, "bob", "alice", "Second", now - 5000),
            Message(3, "alice", "bob", "Third", now)
        )

        coEvery { messageRepository.getMessagesBetween("alice", "bob") } returns messages

        val event = handler.handle("alice", "/history bob")

        assertIs<ChatEvent.CommandResult>(event)

        val resultLines = event.result.split("\n")

        assertEquals(3, resultLines.size)
        assertTrue(resultLines[0].contains("First"))
        assertTrue(resultLines[2].contains("Third"))
    }

    @Test
    fun `should return goodbye`() = runTest {
        val event = handler.handle("alice", "/bye")

        assertIs<ChatEvent.CloseConnection>(event)
        assertEquals(event.text, "Goodbye!")
    }

    @Test
    fun `should return error for unknown command`() = runTest {
        val event = handler.handle("alice", "/unknown_command")

        assertIs<ChatEvent.ErrorMessage>(event)
        assertEquals(event.reason, "Unknown command: /unknown_command")
    }

}