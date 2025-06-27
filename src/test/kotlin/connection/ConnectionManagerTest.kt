package ktor.learn.connection

import kotlinx.coroutines.test.runTest
import ktor.learn.testutil.FakeWebSocketSession
import learn.ktor.connection.ConnectionManager
import kotlin.test.*

class ConnectionManagerTest {

    @BeforeTest
    fun setup() = runTest {
        ConnectionManager.getOnlineUsers().forEach {
            ConnectionManager.unregister(it)
        }
    }

    @Test
    fun `should register and unregister user`() = runTest {
        val session = FakeWebSocketSession()
        ConnectionManager.register("user", session)
        assertTrue(ConnectionManager.isOnline("user"))

        ConnectionManager.unregister("user")
        assertFalse(ConnectionManager.isOnline("user"))
    }

    @Test
    fun `should return correct online users`() = runTest {
        val alice = FakeWebSocketSession()
        val bob = FakeWebSocketSession()
        ConnectionManager.register("alice", alice)
        ConnectionManager.register("bob", bob)

        val users = ConnectionManager.getOnlineUsers()
        assertEquals(setOf("alice", "bob"), users.toSet())
    }

}