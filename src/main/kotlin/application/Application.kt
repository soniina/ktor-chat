package learn.ktor.application

import io.ktor.server.application.*
import learn.ktor.routes.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSockets()
    configureRouting()
}
