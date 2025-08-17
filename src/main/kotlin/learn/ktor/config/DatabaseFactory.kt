package learn.ktor.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    fun connect(appConfig: ApplicationConfig): Database {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = appConfig.property("ktor.db.url").getString()
            driverClassName = appConfig.property("ktor.db.driver").getString()
            username = appConfig.propertyOrNull("ktor.db.user")?.getString()
            password = appConfig.propertyOrNull("ktor.db.password")?.getString()
            maximumPoolSize = appConfig.property("ktor.db.maxPoolSize").getString().toInt()
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }
        return Database.connect(HikariDataSource(hikariConfig))
    }

    fun connect(url: String, driver: String, dbUser: String? = null, dbPassword: String? = null): Database {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = url
            driverClassName = driver
            username = dbUser
            password = dbPassword
            maximumPoolSize = 5
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }
        return Database.connect(HikariDataSource(hikariConfig))
    }

    fun init(tables: List<Table>) {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(*tables.toTypedArray())
        }
    }


}
