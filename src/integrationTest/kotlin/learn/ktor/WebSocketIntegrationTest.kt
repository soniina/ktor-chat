package learn.ktor

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import learn.ktor.application.module
import learn.ktor.config.DatabaseFactory
import learn.ktor.config.JsonFormat
import learn.ktor.model.ChatEvent
import learn.ktor.model.auth.AuthRequest
import learn.ktor.repositories.Messages
import learn.ktor.repositories.Users
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import kotlin.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebSocketIntegrationTest {

    private val configFileName = "application-test.yaml"

    private var isDbConnected = false

    @BeforeAll
    fun setupAll() {
        if (!isDbConnected) {
            DatabaseFactory.connect(ApplicationConfig(configFileName))
            DatabaseFactory.init(listOf(Users, Messages))
            isDbConnected = true
        }
    }

    @BeforeEach
    fun cleanupDb() {
        transaction {
            Messages.deleteAll()
            Users.deleteAll()
        }
    }

    private suspend fun registerAndGetToken(client: HttpClient, username: String, password: String): String? {
        val response = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(JsonFormat.encodeToString(AuthRequest(username, password)))
        }
        val body = response.bodyAsText()
        return JsonFormat.decodeFromString<Map<String, String>>(body)["token"]
    }

    private suspend fun WebSocketSession.receiveEvents(count: Int = 1): List<ChatEvent> {
        return (1..count).mapNotNull {
            val frame = incoming.receive() as? Frame.Text
            frame?.readText()?.let { JsonFormat.decodeFromString(it) }
        }
    }

    @Test
    fun `should connect and exchange messages`() = testApplication {
        environment {
            config = ApplicationConfig(configFileName)
        }

        application {
            module()
        }

        val client = createClient {
            install(WebSockets)
            install(ContentNegotiation) {
                json(JsonFormat)
            }
        }

        val aliceToken = registerAndGetToken(client, "alice", "password")
        assertNotNull(aliceToken)

        val bobToken = registerAndGetToken(client, "bob", "password")
        assertNotNull(bobToken)

        coroutineScope {
            val bobConnected = CompletableDeferred<Unit>()
            val aliceSentMessage = CompletableDeferred<Unit>()
            val bobSentMessage = CompletableDeferred<Unit>()

            val aliceJob = launch {
                client.webSocket("/chat?token=$aliceToken") {
                    val welcome = receiveEvents().first()
                    assertIs<ChatEvent.SystemMessage>(welcome)
                    assertTrue(welcome.text.contains("Welcome, alice!"))

                    bobConnected.await()

                    send("@bob Hello Bob!")
                    val result = receiveEvents().first()
                    assertIs<ChatEvent.CommandResult>(result)
                    assertTrue(result.command == "sent")
                    aliceSentMessage.complete(Unit)

                    bobSentMessage.await()

                    val message = receiveEvents().first()
                    assertIs<ChatEvent.UserMessage>(message, "1")
                    assertEquals("bob", message.sender)
                    assertEquals("Hi Alice!", message.text)

                }
            }

            val bobJob = launch {
                client.webSocket("/chat?token=$bobToken") {
                    val welcome = receiveEvents().first()
                    assertIs<ChatEvent.SystemMessage>(welcome)
                    assertTrue(welcome.text.contains("Welcome, bob!"))

                    bobConnected.complete(Unit)
                    aliceSentMessage.await()

                    val message = receiveEvents().first()
                    assertIs<ChatEvent.UserMessage>(message, "1")
                    assertEquals("alice", message.sender)
                    assertEquals("Hello Bob!", message.text)

                    send("@alice Hi Alice!")
                    val result = receiveEvents().first()
                    assertIs<ChatEvent.CommandResult>(result)
                    assertTrue(result.command == "sent")

                    bobSentMessage.complete(Unit)
                }
            }

            aliceJob.join()
            bobJob.join()
        }
    }

    @Test
    fun `should queue message for offline user and deliver on reconnect`() = testApplication {
        environment {
            config = ApplicationConfig(configFileName)
        }

        application {
            module()
        }

        val client = createClient {
            install(WebSockets)
            install(ContentNegotiation) {
                json(JsonFormat)
            }
        }

        val aliceToken = registerAndGetToken(client, "alice", "password")
        assertNotNull(aliceToken)

        val bobToken = registerAndGetToken(client, "bob", "password")
        assertNotNull(bobToken)

        client.webSocket("/chat?token=$aliceToken") {
            val welcome = receiveEvents().first()
            assertIs<ChatEvent.SystemMessage>(welcome)
            assertTrue(welcome.text.contains("Welcome, alice!"))

            send("@bob Are you there?")

            val response = receiveEvents().first()
            assertIs<ChatEvent.CommandResult>(response)
            assertEquals("queued", response.command)
            assertContains("to bob (offline — will be delivered later)", response.result)
        }

        client.webSocket("/chat?token=$bobToken") {
            val welcome = receiveEvents().first()
            assertIs<ChatEvent.SystemMessage>(welcome)
            assertTrue(welcome.text.contains("Welcome, bob!"))

            val undeliveredMessages = receiveEvents()
            assertEquals(1, undeliveredMessages.size)
            val undeliveredMessage = undeliveredMessages.first()
            assertIs<ChatEvent.UserMessage>(undeliveredMessage)
            assertEquals("Are you there?", undeliveredMessage.text)
        }

    }

    @Test
    fun `should handle commands`() = testApplication {
        environment {
            config = ApplicationConfig(configFileName)
        }

        application {
            module()
        }

        val client = createClient {
            install(WebSockets)
            install(ContentNegotiation) {
                json(JsonFormat)
            }
        }

        val aliceToken = registerAndGetToken(client, "alice", "password")
        assertNotNull(aliceToken)

        client.webSocket("/chat?token=$aliceToken") {
            val welcome = receiveEvents().first()
            assertIs<ChatEvent.SystemMessage>(welcome)
            assertTrue(welcome.text.contains("Welcome, alice!"))

            send("/help")

            val response = receiveEvents().first()
            assertIs<ChatEvent.CommandResult>(response)
            assertEquals("help", response.command)
            assertContains(response.result, "Available commands")
        }
    }

    @Test
    fun `should close connection to missing token`() = testApplication {
        environment {
            config = ApplicationConfig(configFileName)
        }

        application {
            module()
        }

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/chat") {
            val closeReason = closeReason.await()
            assertNotNull(closeReason)
            assertEquals(CloseReason.Codes.VIOLATED_POLICY.code, closeReason.code)
            assertEquals("Missing token", closeReason.message)
        }
    }

    @Test
    fun `should close connection to invalid token`() = testApplication {
        environment {
            config = ApplicationConfig("application-test.yaml")
        }

        application {
            module()
        }

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/chat?token=invalid_token") {
            val closeReason = closeReason.await()
            assertNotNull(closeReason)
            assertEquals(CloseReason.Codes.VIOLATED_POLICY.code, closeReason.code)
            assertTrue(closeReason.message.contains("Invalid token"))
        }
    }

    @Test
    fun `should retrieve message history`() = testApplication {
        environment {
            config = ApplicationConfig(configFileName)
        }

        application {
            module()
        }

        val client = createClient {
            install(WebSockets)
            install(ContentNegotiation) {
                json(JsonFormat)
            }
        }

        val aliceToken = registerAndGetToken(client, "alice", "password")
        assertNotNull(aliceToken)

        val bobToken = registerAndGetToken(client, "bob", "password")
        assertNotNull(bobToken)

        client.webSocket("/chat?token=$bobToken") {
            client.webSocket("/chat?token=$aliceToken") {
                val welcome = receiveEvents().first()
                assertIs<ChatEvent.SystemMessage>(welcome)
                assertTrue(welcome.text.contains("Welcome, alice!"))

                send("@bob first message")
                send("@bob second message")
                send("@bob third message")

                val response = receiveEvents(3).first()
                assertIs<ChatEvent.CommandResult>(response)
                assertEquals("sent", response.command)

                send("/history bob")
                val history = receiveEvents().first()
                assertIs<ChatEvent.CommandResult>(history)
                assertEquals("history", history.command)

                val messages = history.result.split("\n")
                assertEquals(3, messages.size)
                assertContains(messages.first(), "first message")
                assertContains(messages.last(), "third message")
            }
        }

    }

    @Test
    fun `should list online users`() = testApplication {
        environment {
            config = ApplicationConfig(configFileName)
        }

        application {
            module()
        }

        val client = createClient {
            install(WebSockets)
            install(ContentNegotiation) {
                json(JsonFormat)
            }
        }

        val aliceToken = registerAndGetToken(client, "alice", "password")
        assertNotNull(aliceToken)

        val bobToken = registerAndGetToken(client, "bob", "password")
        assertNotNull(bobToken)

        client.webSocket("/chat?token=$aliceToken") {
            receiveEvents() // welcome

            client.webSocket("/chat?token=$bobToken") {
                receiveEvents() // welcome, Bob
            }

            send("/users")
            val response = receiveEvents().first()
            assertIs<ChatEvent.CommandResult>(response)
            assertEquals("users", response.command)
            assertContains(response.result, "alice")
            assertFalse(response.result.contains("bob"))
        }
    }

    @Test
    fun `should close connection on bye command`() = testApplication {
        environment {
            config = ApplicationConfig(configFileName)
        }

        application {
            module()
        }

        val client = createClient {
            install(WebSockets)
            install(ContentNegotiation) {
                json(JsonFormat)
            }
        }

        val aliceToken = registerAndGetToken(client, "alice", "password")
        assertNotNull(aliceToken)

        client.webSocket("/chat?token=$aliceToken") {
            receiveEvents() // welcome

            send("/bye")

            val closeReason = closeReason.await()
            assertNotNull(closeReason)
            assertEquals(CloseReason.Codes.NORMAL.code, closeReason.code)
            assertEquals("User left", closeReason.message)
        }
    }

}