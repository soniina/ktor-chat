package learn.ktor.application

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json
import learn.ktor.config.DatabaseFactory
import learn.ktor.config.getJwtProperties
import learn.ktor.connection.ConnectionManager
import learn.ktor.repositories.MessageRepository
import learn.ktor.repositories.Messages
import learn.ktor.repositories.UserRepository
import learn.ktor.repositories.Users
import learn.ktor.routes.*
import learn.ktor.services.TokenService
import learn.ktor.services.ChatService
import learn.ktor.services.CommandHandler
import learn.ktor.services.UserService

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val config = environment.config

    DatabaseFactory.connect(config)
    DatabaseFactory.init(listOf(Users, Messages))

    val connectionManager = ConnectionManager()
    val userRepository = UserRepository()
    val messageRepository = MessageRepository()
    val userService = UserService(userRepository)
    val tokenService = TokenService(config.getJwtProperties())
    val commandHandler = CommandHandler(messageRepository, userRepository, connectionManager)
    val chatService = ChatService(connectionManager, messageRepository, commandHandler, userRepository)

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
}
