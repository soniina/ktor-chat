package learn.ktor.routes

import learn.ktor.config.JwtConfig
import learn.ktor.services.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import learn.ktor.model.auth.*

fun Application.configureAuthRouting(userService: UserService) {

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