package integration

import io.ktor.client.plugins.websocket.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import learn.ktor.application.module
import learn.ktor.connection.ConnectionManager
import learn.ktor.util.JsonFormat
import model.ChatEvent
import kotlin.test.*


class WebSocketIntegrationTest {

    @BeforeTest
    fun setup() = runTest {
        ConnectionManager.getOnlineUsers().forEach {
            ConnectionManager.unregister(it)
        }
    }

    @Test
    fun `should connect and exchange messages`() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(WebSockets)
        }

        val aliceGreeting = "Hello Alice!"
        client.webSocket("/chat?user=alice") {
            val welcome = (incoming.receive() as? Frame.Text)?.readText()
            assertTrue(welcome!!.contains("Welcome"))

            val bobSession = client.webSocketSession("/chat?user=bob")
            (bobSession.incoming.receive() as? Frame.Text)?.readText() // welcome bob
            bobSession.send("@alice $aliceGreeting")

            withTimeout(5000) {
                val message = (incoming.receive() as? Frame.Text)?.readText()
                assertEquals(message!!, JsonFormat.encodeToString(ChatEvent.serializer(), ChatEvent.UserMessage("bob", aliceGreeting)))
            }
        }
    }
}