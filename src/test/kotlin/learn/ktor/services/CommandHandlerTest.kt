package learn.ktor.services

import kotlinx.coroutines.test.runTest
import learn.ktor.testutil.FakeWebSocketSession
import learn.ktor.connection.ConnectionManager
import learn.ktor.model.ChatEvent
import kotlin.test.*

class CommandHandlerTest {

    val handler = CommandHandler()

    @BeforeTest
    fun setup() = runTest {
        ConnectionManager.getOnlineUsers().forEach {
            ConnectionManager.unregister(it)
        }
    }

    @Test
    fun `should return all available commands`() = runTest {
        val event = handler.handle("alice", "/help")
        assertIs<ChatEvent.CommandResult>(event)
        assertTrue(event.result.contains("Available commands"))
    }

    @Test
    fun `should return online users`() = runTest {
        val alice = FakeWebSocketSession()
        val bob = FakeWebSocketSession()
        ConnectionManager.register("alice", alice)
        ConnectionManager.register("bob", bob)

        val event = handler.handle("alice", "/users")
        assertIs<ChatEvent.CommandResult>(event)
        assertTrue("alice" in event.result)
        assertTrue("bob" in event.result)
    }

    @Test
    fun `should return goodbye`() = runTest {
        val event = handler.handle("alice", "/bye")
        assertIs<ChatEvent.CloseConnection>(event)
    }

    @Test
    fun `should return error for unknown command`() = runTest {
        val event = handler.handle("alice", "/unknown_command")
        assertIs<ChatEvent.ErrorMessage>(event)
        assertEquals(event.reason, "Unknown command: /unknown_command")
    }

}