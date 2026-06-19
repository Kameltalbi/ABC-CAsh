package com.abccash.server.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object DatabaseFactory {
    private val log = LoggerFactory.getLogger(DatabaseFactory::class.java)
    private lateinit var dataSource: HikariDataSource

    fun init(config: ApplicationConfig) {
        val dbConfig = config.config("database")
        val url = envOrConfig("DATABASE_URL", dbConfig, "url")
        val user = envOrConfig("DATABASE_USER", dbConfig, "user")
        val password = envOrConfig("DATABASE_PASSWORD", dbConfig, "password")
        // Ktor fusionne DATABASE_* depuis l'environnement sans inclure driver → défaut PostgreSQL
        val driver = envOrConfig("DATABASE_DRIVER", dbConfig, "driver")
            .ifBlank { "org.postgresql.Driver" }

        require(url.isNotBlank()) { "DATABASE_URL manquant (fichier .env ou variable d'environnement)" }
        require(user.isNotBlank()) { "DATABASE_USER manquant" }
        require(password.isNotBlank()) { "DATABASE_PASSWORD manquant" }

        log.info("Connexion PostgreSQL → {} (user={})", maskJdbcUrl(url), user)

        var lastError: Exception? = null
        repeat(15) { attempt ->
            try {
                connect(url, user, password, driver)
                log.info("Base de données connectée")
                return
            } catch (e: Exception) {
                lastError = e
                val msg = e.message.orEmpty()
                log.warn("Tentative DB {}/15 échouée: {}", attempt + 1, msg)
                if (msg.contains("password authentication failed", ignoreCase = true)) {
                    throw IllegalStateException(
                        "Mot de passe PostgreSQL refusé. Si deploy/.env a été régénéré, " +
                            "le volume Docker conserve l'ancien mot de passe. " +
                            "Lancez: cd deploy && docker-compose down -v && docker-compose up -d",
                        e
                    )
                }
                if (attempt < 14) Thread.sleep(2000)
            }
        }
        throw IllegalStateException(
            "Impossible de se connecter à PostgreSQL après 15 tentatives. " +
                "Vérifiez deploy/.env (DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD). " +
                "Cause: ${lastError?.message}",
            lastError
        )
    }

    private fun envOrConfig(envKey: String, config: ApplicationConfig, configKey: String): String =
        System.getenv(envKey)?.takeIf { it.isNotBlank() }
            ?: config.propertyOrNull(configKey)?.getString().orEmpty()

    private fun maskJdbcUrl(url: String): String =
        url.replace(Regex("password=[^&]*"), "password=***")

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
