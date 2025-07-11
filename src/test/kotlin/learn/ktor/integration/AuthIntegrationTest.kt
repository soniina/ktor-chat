package learn.ktor.integration

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import learn.ktor.application.module
import learn.ktor.config.JsonFormat
import learn.ktor.model.auth.AuthRequest
import learn.ktor.services.UserService
import kotlin.test.*

class AuthIntegrationTest {

    @BeforeTest
    fun setup() = runTest {
        UserService.clear()
    }

    @Test
    fun `should register and login user`() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                    classDiscriminator = "type"
                })
            }
        }

        val request = AuthRequest("alice", "password")


        val registerResponse = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(JsonFormat.encodeToString(request))
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)

        val tokenFromRegister = JsonFormat.decodeFromString<Map<String, String>>(registerResponse.bodyAsText())["token"]

        println("Register token: $tokenFromRegister")
        assertNotNull(tokenFromRegister)


        val loginResponse = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status)

        val tokenFromLogin = JsonFormat.decodeFromString<Map<String, String>>(loginResponse.bodyAsText())["token"]
        println("Login token: $tokenFromLogin")
        assertNotNull(tokenFromLogin)
    }

    @Test
    fun `should not register blank username`() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                    classDiscriminator = "type"
                })
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
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                    classDiscriminator = "type"
                })
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
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                    classDiscriminator = "type"
                })
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
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                    classDiscriminator = "type"
                })
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