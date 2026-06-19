package com.abccash.server.plugins

import com.abccash.server.model.ApiError
import com.abccash.server.model.HealthResponse
import com.abccash.server.model.LoginRequest
import com.abccash.server.model.RegisterRequest
import com.abccash.server.model.SyncPushRequest
import com.abccash.server.model.SyncPushResponse
import com.abccash.server.service.AuthService
import com.abccash.server.service.SyncService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val authService = AuthService(environment.config)
    val syncService = SyncService()

    routing {
        get("/health") {
            call.respond(HealthResponse(status = "ok"))
        }

        route("/v1") {
            post("/auth/register") {
                val body = call.receive<RegisterRequest>()
                authService.register(body)
                    .onSuccess { call.respond(HttpStatusCode.Created, it) }
                    .onFailure { throw IllegalArgumentException(it.message) }
            }

            post("/auth/login") {
                val body = call.receive<LoginRequest>()
                authService.login(body)
                    .onSuccess { call.respond(it) }
                    .onFailure { throw IllegalArgumentException(it.message) }
            }

            authenticate("auth-jwt") {
                get("/sync") {
                    val entrepriseId = call.entrepriseId()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiError("Unauthorized"))
                    val data = syncService.pull(entrepriseId)
                        ?: return@get call.respond(HttpStatusCode.NotFound, ApiError("Company not found"))
                    call.respond(data)
                }

                post("/sync") {
                    val entrepriseId = call.entrepriseId()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiError("Unauthorized"))
                    val body = call.receive<SyncPushRequest>()
                    if (body.users.isNotEmpty() || body.deletedUserIds.isNotEmpty()) {
                        if (call.userRole() != "ADMIN") {
                            return@post call.respond(
                                HttpStatusCode.Forbidden,
                                ApiError("Only admin can sync users")
                            )
                        }
                    }
                    val error = syncService.push(entrepriseId, body)
                    if (error != null) {
                        call.respond(HttpStatusCode.BadRequest, ApiError(error))
                    } else {
                        call.respond(SyncPushResponse(ok = true, message = "Synced"))
                    }
                }
            }
        }
    }
}
