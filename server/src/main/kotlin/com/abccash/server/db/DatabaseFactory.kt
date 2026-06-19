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

        val hikari = HikariConfig().apply {
            jdbcUrl = url
            username = user
            this.password = password
            driverClassName = driver
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
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
