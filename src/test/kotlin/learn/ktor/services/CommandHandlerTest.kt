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
import learn.ktor.repositories.UserRepository
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.*

@ExtendWith(MockKExtension::class)
class CommandHandlerTest {

    @MockK
    lateinit var messageRepository: MessageRepository

    @MockK
    lateinit var userRepository: UserRepository

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
        val userId = 1
        val targetUserId = 2

        coEvery { userRepository.getIdByUsername("alice") } returns userId
        coEvery { userRepository.getIdByUsername("bob") } returns targetUserId
        coEvery { messageRepository.getMessagesBetween(userId, targetUserId) } returns emptyList()

        val event = handler.handle("alice", "/history bob")

        assertIs<ChatEvent.CommandResult>(event)
        assertEquals("history", event.command)
        assertEquals(event.result, "No messages with bob")
    }

    @Test
    fun `should return messages between users in chronological order`() = runTest {
        val aliceId = 1
        val bobId = 2

        val now = System.currentTimeMillis()
        val messages = listOf(
            Message(1, aliceId, bobId, "First", now - 10000),
            Message(2, bobId, aliceId, "Second", now - 5000),
            Message(3, aliceId, bobId, "Third", now)
        )


        coEvery { userRepository.getIdByUsername("alice") } returns aliceId
        coEvery { userRepository.getIdByUsername("bob") } returns bobId
        coEvery { messageRepository.getMessagesBetween(aliceId, bobId) } returns messages

        val event = handler.handle("alice", "/history bob")

        assertIs<ChatEvent.CommandResult>(event)

        val resultLines = event.result.split("\n")

        assertEquals(3, resultLines.size)
        assertTrue(resultLines[0].contains("First"))
        assertTrue(resultLines[2].contains("Third"))
    }

    @Test
    fun `should return internal error when invoking user not found in repository`() = runTest {
        coEvery { userRepository.getIdByUsername("alice") } returns null

        val event = handler.handle("alice", "/history bob")

        assertIs<ChatEvent.ErrorMessage>(event)
        assertEquals(event.reason, "Internal error: user not found")
    }

    @Test
    fun `should return error when history target user not found`() = runTest {
        val userId = 1

        coEvery { userRepository.getIdByUsername("alice") } returns userId
        coEvery { userRepository.getIdByUsername("bob") } returns null

        val event = handler.handle("alice", "/history bob")

        assertIs<ChatEvent.ErrorMessage>(event)
        assertEquals(event.reason, "Unknown user: bob")
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