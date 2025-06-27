package ktor.learn.application.chat

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import ktor.learn.testutil.FakeWebSocketSession
import learn.ktor.application.chat.MessageService
import learn.ktor.connection.ConnectionManager
import learn.ktor.util.JsonFormat
import model.ChatEvent
import kotlin.test.*

class MessageServiceTest {

    private val service = MessageService()

    @BeforeTest
    fun setup() {
        runBlocking {
            ConnectionManager.getOnlineUsers().forEach {
                ConnectionManager.unregister(it)
            }
        }
    }

    @Test
    fun `should send message to recipient`() = runTest {
        val alice = FakeWebSocketSession()
        val bob = FakeWebSocketSession()
        ConnectionManager.register("alice", alice)
        ConnectionManager.register("bob", bob)

        val result = service.sendMessage("alice", "bob", "Hello Bob!")

        assertTrue(result)
        assertTrue(bob.sent.any { it.contains("Hello Bob!") })
    }

    @Test
    fun `should notify sender if recipient is offline`() = runTest {
        val alice = FakeWebSocketSession()
        ConnectionManager.register("alice", alice)

        val result = service.sendMessage("alice", "bob", "Hey!")

        assertFalse(result)
    }

    @Test
    fun `should notify user`() = runTest {
        val bob = FakeWebSocketSession()
        ConnectionManager.register("bob", bob)

        val event = ChatEvent.SystemMessage("Welcome!")
        service.notifyUser("bob", event)

        val encoded = JsonFormat.encodeToString(ChatEvent.serializer(), event)
        println(bob.sent)
        println(encoded)
        assertTrue(bob.sent.contains(encoded))
    }

}