package learn.ktor.config

import kotlinx.serialization.json.Json

val JsonFormat = Json {
    prettyPrint = true
    classDiscriminator = "type"
}