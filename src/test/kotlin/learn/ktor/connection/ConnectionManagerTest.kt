package learn.ktor.connection

import io.ktor.websocket.*
import io.mockk.*
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import learn.ktor.config.JsonFormat
import learn.ktor.model.ChatEvent
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.*

@ExtendWith(MockKExtension::class)
class ConnectionManagerTest {

    private lateinit var connectionManager: ConnectionManager

    @BeforeTest
    fun setup() = runTest {
        connectionManager = ConnectionManager()
    }

    @Test
    fun `should register and unregister user`() = runTest {
        mockkStatic("kotlinx.coroutines.CoroutineScopeKt")

        val session = mockk<WebSocketSession>()

        coEvery { session.isActive }  returns true
        coEvery { session.close() } returns Unit

        connectionManager.register("user", session)
        assertTrue(connectionManager.isOnline("user"))

        connectionManager.unregister("user")
        assertFalse(connectionManager.isOnline("user"))
    }

    @Test
    fun `should return correct online users`() = runTest {
        mockkStatic("kotlinx.coroutines.CoroutineScopeKt")

        val alice = mockk<WebSocketSession>()
        val bob = mockk<WebSocketSession>()

        coEvery { alice.isActive }  returns true
        coEvery { bob.isActive }  returns true

        connectionManager.register("alice", alice)
        connectionManager.register("bob", bob)

        val users = connectionManager.getOnlineUsers()
        assertEquals(setOf("alice", "bob"), users.toSet())
    }

    @Test
    fun `should replace existing session`() = runTest {
        mockkStatic("kotlinx.coroutines.CoroutineScopeKt")

        val session1 = mockk<WebSocketSession>()
        val session2 = mockk<WebSocketSession>()

        coEvery { session1.isActive }  returns true
        coEvery { session2.isActive  } returns true

        connectionManager.register("alice", session1)
        connectionManager.register("alice", session2)

        assertTrue(connectionManager.isOnline("alice"))
        assertEquals(session2, connectionManager.getSession("alice"))
    }

    @Test
    fun `should send message to online user`() = runTest {
        mockkStatic("kotlinx.coroutines.CoroutineScopeKt")

        val event = ChatEvent.SystemMessage("Hello")

        val session = mockk<WebSocketSession>()

        coEvery { session.isActive } returns true
        coEvery { session.send(any()) } just Runs

        connectionManager.register("alice", session)
        connectionManager.sendTo("alice", event)

        coVerify(exactly = 1) {
            session.send(any())
        }
    }

    @Test
    fun `should not send to offline user`() = runTest {
        mockkStatic("kotlinx.coroutines.CoroutineScopeKt")

        val event = ChatEvent.SystemMessage("Hello")

        val session = mockk<WebSocketSession>()

        coEvery { session.isActive   } returns true

        connectionManager.register("alice", session)

        connectionManager.unregister("alice")

        connectionManager.sendTo("user", ChatEvent.SystemMessage("Hello!"))

        coVerify(exactly = 0) {
            session.send(JsonFormat.encodeToString(event))
        }
    }
}
