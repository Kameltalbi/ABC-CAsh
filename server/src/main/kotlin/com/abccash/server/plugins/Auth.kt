package com.abccash.server.plugins

import com.abccash.server.config.AppConfig
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureAuth() {
    val config = environment.config
    val secret = AppConfig.jwtSecret(config)
    val issuer = AppConfig.jwtIssuer(config)
    val audience = AppConfig.jwtAudience(config)
    val realm = AppConfig.jwtRealm(config)

    install(Authentication) {
        jwt("auth-jwt") {
            this.realm = realm
            verifier(
                JWT.require(Algorithm.HMAC256(secret))
                    .withIssuer(issuer)
                    .withAudience(audience)
                    .build()
            )
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString()
                val entrepriseId = credential.payload.getClaim("entrepriseId").asString()
                if (userId.isNullOrBlank() || entrepriseId.isNullOrBlank()) null
                else JWTPrincipal(credential.payload)
            }
        }
    }
}

fun ApplicationCall.entrepriseId(): String? =
    principal<JWTPrincipal>()?.payload?.getClaim("entrepriseId")?.asString()

fun ApplicationCall.userId(): String? =
    principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()

fun ApplicationCall.userRole(): String? =
    principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
