package com.abccash.server

import com.abccash.server.db.DatabaseFactory
import com.abccash.server.plugins.configureAuth
import com.abccash.server.plugins.configureRouting
import com.abccash.server.plugins.configureSerialization
import com.abccash.server.plugins.configureStatusPages
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8081
    val host = System.getenv("HOST") ?: "0.0.0.0"
    embeddedServer(Netty, host = host, port = port, module = Application::module).start(wait = true)
}

fun Application.module() {
    DatabaseFactory.init(environment.config)
    configureSerialization()
    configureAuth()
    configureStatusPages()
    configureRouting()
}
