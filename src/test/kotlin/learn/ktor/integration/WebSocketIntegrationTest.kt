package learn.ktor.integration

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
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import learn.ktor.application.module
import learn.ktor.config.DatabaseFactory
import learn.ktor.config.JsonFormat
import learn.ktor.connection.ConnectionManager
import learn.ktor.model.ChatEvent
import learn.ktor.model.auth.AuthRequest
import learn.ktor.repository.Users
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.*


class WebSocketIntegrationTest {

    @BeforeTest
    fun setup() = runTest {
        ConnectionManager.getOnlineUsers().forEach {
            ConnectionManager.unregister(it)
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

    @Test
    fun `should connect and exchange messages`() = testApplication {
        environment {
            config = ApplicationConfig("application-test.yaml")
        }

        application {
            val config = environment.config
            DatabaseFactory.connect(DatabaseFactory.postgresConfig(config))
            transaction {
                Users.deleteAll()
            }
            module()
        }

        val client = createClient {
            install(WebSockets)
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                    classDiscriminator = "type"
                })
            }
        }

        val aliceToken = registerAndGetToken(client, "alice", "password")
        requireNotNull(aliceToken) { "Alice token should not be null" }

        val bobToken = registerAndGetToken(client, "bob", "password")
        requireNotNull(bobToken) { "Bob token should not be null" }

        val aliceGreeting = "Hello Alice!"
        client.webSocket("/chat?token=$aliceToken") {
            val welcome = (incoming.receive() as? Frame.Text)?.readText()
            assertTrue(welcome!!.contains("Welcome"))

            val bobSession = client.webSocketSession("/chat?token=$bobToken")
            (bobSession.incoming.receive() as? Frame.Text)?.readText() // welcome bob
            bobSession.send("@alice $aliceGreeting")

            withTimeout(5000) {
                val message = (incoming.receive() as? Frame.Text)?.readText()
                assertEquals(message!!, JsonFormat.encodeToString(ChatEvent.serializer(), ChatEvent.UserMessage("bob", aliceGreeting)))
            }
        }
    }
}