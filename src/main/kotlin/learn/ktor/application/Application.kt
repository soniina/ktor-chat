package learn.ktor.application

import learn.ktor.config.JwtConfig
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import learn.ktor.config.DatabaseFactory
import learn.ktor.repository.UserRepository
import learn.ktor.routes.*
import learn.ktor.routes.configureAuthRouting
import learn.ktor.services.UserService

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val config = environment.config
    val userRepository = UserRepository()
    val userService = UserService(userRepository)

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            classDiscriminator = "type"
        })
    }
    configureRouting()
    configureWebSockets()
    configureAuthRouting(userService)
    JwtConfig.configure(this)

    DatabaseFactory.connect(DatabaseFactory.postgresConfig(config))
}
