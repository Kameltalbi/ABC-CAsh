package com.abccash.server.config

import io.ktor.server.config.ApplicationConfig

object AppConfig {
    fun jwtSecret(config: ApplicationConfig): String {
        val value = envOrAbc("JWT_SECRET", config, "jwtSecret")
        require(value.isNotBlank()) { "JWT_SECRET manquant (deploy/.env ou variable d'environnement)" }
        return value
    }

    fun jwtIssuer(config: ApplicationConfig): String =
        envOrAbc("JWT_ISSUER", config, "jwtIssuer").ifBlank { "abc-cash" }

    fun jwtAudience(config: ApplicationConfig): String =
        envOrAbc("JWT_AUDIENCE", config, "jwtAudience").ifBlank { "abc-cash-app" }

    fun jwtRealm(config: ApplicationConfig): String =
        envOrAbc("JWT_REALM", config, "jwtRealm").ifBlank { "ABC Cash API" }

    private fun envOrAbc(envKey: String, config: ApplicationConfig, abcKey: String): String =
        System.getenv(envKey)?.takeIf { it.isNotBlank() }
            ?: config.config("abc").propertyOrNull(abcKey)?.getString().orEmpty()
}
