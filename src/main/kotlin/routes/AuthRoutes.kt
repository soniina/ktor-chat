package routes

import kotlinx.serialization.Serializable
import application.security.JwtConfig
import application.services.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


@Serializable
data class AuthRequest(val username: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val message: String? = null)

@Serializable
data class ErrorResponse(val error: String)

fun Application.configureAuthRouting() {

    val userService = UserService()

    routing {
        post("/register") {
            val request = call.receive<AuthRequest>()
            val username = request.username
            val password = request.password
            println("$username:$password")
            if (username.isBlank() || password.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Username and password required"))
                return@post
            }
            if (!userService.register(username, password)) {
                call.respond(HttpStatusCode.Conflict, ErrorResponse("Username already exists"))
                return@post
            }
            println("User registered $username")
            call.respond(HttpStatusCode.Created, AuthResponse(JwtConfig.generateToken(username)))
        }

        post("/login") {
            val request = call.receive<AuthRequest>()
            val username = request.username
            val password = request.password

            if (username.isBlank() || password.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Username and password required"))
                return@post
            }
            if (!userService.authenticate(username, password)) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid username or password"))
                return@post
            }
            call.respond(HttpStatusCode.OK, AuthResponse(JwtConfig.generateToken(username)))
        }
    }

}