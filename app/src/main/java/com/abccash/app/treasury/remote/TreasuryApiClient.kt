package com.abccash.app.treasury.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class TreasuryApiClient(baseUrl: String) {
    private var baseUrl: String = baseUrl.trimEnd('/')

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
    }

    fun updateBaseUrl(url: String) {
        baseUrl = url.trimEnd('/')
    }

    private fun endpoint(path: String): String = "$baseUrl${path.ensureLeadingSlash()}"

    suspend fun health(): HealthResponse =
        client.get(endpoint("/health")).body()

    suspend fun register(request: RegisterRequest): AuthResponse =
        client.post(endpoint("/v1/auth/register")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun login(request: LoginRequest): AuthResponse =
        client.post(endpoint("/v1/auth/login")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun pull(token: String): SyncPullResponse =
        client.get(endpoint("/v1/sync")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()

    suspend fun push(token: String, request: SyncPushRequest): SyncPushResponse =
        client.post(endpoint("/v1/sync")) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(request)
        }.body()
}

private fun String.ensureLeadingSlash(): String =
    if (startsWith('/')) this else "/$this"
