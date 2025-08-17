package learn.ktor

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import learn.ktor.application.module
import learn.ktor.config.DatabaseFactory
import learn.ktor.config.JsonFormat
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
class AuthIntegrationTest {

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

    @Test
    fun `should register and login user`() = testApplication {
        environment {
            config = ApplicationConfig(configFileName)
        }

        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(JsonFormat)
            }
        }

        val request = AuthRequest("alice", "password")


        val registerResponse = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(JsonFormat.encodeToString(request))
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)

        val tokenFromRegister = JsonFormat.decodeFromString<Map<String, String>>(registerResponse.bodyAsText())["token"]

        assertNotNull(tokenFromRegister)


        val loginResponse = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(JsonFormat.encodeToString(request))
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status)

        val tokenFromLogin = JsonFormat.decodeFromString<Map<String, String>>(loginResponse.bodyAsText())["token"]
        assertNotNull(tokenFromLogin)
    }

    @Test
    fun `should not register blank username`() = testApplication {
        environment {
            config = ApplicationConfig(configFileName)
        }

        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(JsonFormat)
            }
        }

        val request = AuthRequest("", "password")

        val response = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(JsonFormat.encodeToString(request))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)

        val error = response.bodyAsText()
        assertTrue("Username and password required" in error)
    }

    @Test
    fun `should not login blank password`() = testApplication {
        environment {
            config = ApplicationConfig(configFileName)
        }

        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(JsonFormat)
            }
        }

        val request = AuthRequest("alice", "  ")

        val response = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(JsonFormat.encodeToString(request))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)

        val error = response.bodyAsText()
        assertTrue("Username and password required" in error)
    }


    @Test
    fun `should not register username exists`() = testApplication {
        environment {
            config = ApplicationConfig(configFileName)
        }

        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(JsonFormat)
            }
        }

        client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(JsonFormat.encodeToString(AuthRequest("alice", "password")))
        }

        val request = AuthRequest("alice", "another")

        val response = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(JsonFormat.encodeToString(request))
        }
        assertEquals(HttpStatusCode.Conflict, response.status)

        val error = response.bodyAsText()
        assertTrue("Username already exists" in error)
    }

    @Test
    fun `should not login invalid password`() = testApplication {
        environment {
            config = ApplicationConfig(configFileName)
        }

        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(JsonFormat)
            }
        }

        client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(JsonFormat.encodeToString(AuthRequest("alice", "password")))
        }

        val request = AuthRequest("alice", "wrong")

        val response = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(JsonFormat.encodeToString(request))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)

        val error = response.bodyAsText()
        assertTrue("Invalid username or password" in error)
    }

}