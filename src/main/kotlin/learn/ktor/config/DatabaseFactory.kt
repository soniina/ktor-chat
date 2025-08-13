package learn.ktor.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import learn.ktor.repositories.Messages
import learn.ktor.repositories.Users
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    fun connect(config: HikariConfig) {
        Database.connect(HikariDataSource(config))
        transaction {
            SchemaUtils.create(Users, Messages)
        }
    }

    fun postgresConfig(appConfig: ApplicationConfig): HikariConfig = HikariConfig().apply {
        jdbcUrl = appConfig.property("ktor.db.url").getString()
        driverClassName = "org.postgresql.Driver"
        username = appConfig.property("ktor.db.user").getString()
        password = appConfig.property("ktor.db.password").getString()
        maximumPoolSize = appConfig.property("ktor.db.maxPoolSize").getString().toInt()
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    }

    fun h2TestConfig(): HikariConfig = HikariConfig().apply {
        jdbcUrl = "jdbc:h2:mem:test-${System.nanoTime()};DB_CLOSE_DELAY=-1;"
        driverClassName = "org.h2.Driver"
        maximumPoolSize = 5
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    }
}
