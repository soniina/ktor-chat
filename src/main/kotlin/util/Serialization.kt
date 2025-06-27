package learn.ktor.util

import kotlinx.serialization.json.Json

val JsonFormat = Json {
    prettyPrint = true
    classDiscriminator = "type"
}