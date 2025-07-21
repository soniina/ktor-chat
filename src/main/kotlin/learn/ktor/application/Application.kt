package learn.ktor.application

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import learn.ktor.config.DatabaseFactory
import learn.ktor.config.getJwtProperties
import learn.ktor.connection.ConnectionManager
import learn.ktor.repositories.MessageRepository
import learn.ktor.repositories.UserRepository
import learn.ktor.routes.*
import learn.ktor.routes.configureAuthRouting
import learn.ktor.services.TokenService
import learn.ktor.services.ChatService
import learn.ktor.services.UserService

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val config = environment.config
    val jwtProperties = config.getJwtProperties()

    val connectionManager = ConnectionManager()
    val userRepository = UserRepository()
    val messageRepository = MessageRepository()
    val userService = UserService(userRepository)
    val tokenService = TokenService(jwtProperties)
    val chatService = ChatService(connectionManager, messageRepository)


    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            classDiscriminator = "type"
        })
    }
    configureRouting()
    configureWebSockets(tokenService, chatService)
    configureAuthRouting(userService, tokenService)

    DatabaseFactory.connect(DatabaseFactory.postgresConfig(config))
}
