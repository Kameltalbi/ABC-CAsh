package com.abccash.server.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    private lateinit var dataSource: HikariDataSource

    fun init(config: ApplicationConfig) {
        val dbConfig = config.config("database")
        val url = System.getenv("DATABASE_URL") ?: dbConfig.property("url").getString()
        val user = System.getenv("DATABASE_USER") ?: dbConfig.property("user").getString()
        val password = System.getenv("DATABASE_PASSWORD") ?: dbConfig.property("password").getString()
        val driver = dbConfig.property("driver").getString()

        var lastError: Exception? = null
        repeat(15) { attempt ->
            try {
                connect(url, user, password, driver)
                return
            } catch (e: Exception) {
                lastError = e
                if (attempt < 14) Thread.sleep(2000)
            }
        }
        throw lastError ?: IllegalStateException("Database connection failed")
    }

    private fun connect(url: String, user: String, password: String, driver: String) {
        val hikari = HikariConfig().apply {
            jdbcUrl = url
            username = user
            this.password = password
            driverClassName = driver
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            connectionTimeout = 5000
            validate()
        }
        dataSource = HikariDataSource(hikari)
        Database.connect(dataSource)

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Entreprises,
                Users,
                Invoices,
                Payments,
                Expenses
            )
        }
    }
}
