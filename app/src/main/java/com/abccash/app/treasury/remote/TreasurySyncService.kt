package com.abccash.app.treasury.remote

import com.abccash.app.treasury.data.User
import com.abccash.app.treasury.datastore.UserPreferences
import com.abccash.app.treasury.repository.TreasuryRepository
import com.abccash.app.treasury.security.PasswordHasher
import kotlinx.coroutines.flow.first
import java.time.Instant

class TreasurySyncService(
    private val apiClient: TreasuryApiClient,
    private val repository: TreasuryRepository,
    private val userPreferences: UserPreferences
) {
    suspend fun isEnabled(): Boolean = userPreferences.isSyncEnabled.first()

    suspend fun getApiBaseUrl(): String = userPreferences.getApiBaseUrl()

    suspend fun setApiBaseUrl(url: String) = userPreferences.setApiBaseUrl(url)

    suspend fun setSyncEnabled(enabled: Boolean) = userPreferences.setSyncEnabled(enabled)

    suspend fun getLastSyncAt(): String? = userPreferences.getLastSyncAt()

    suspend fun hasCloudSession(): Boolean = !userPreferences.getAuthToken().isNullOrBlank()

    suspend fun isServerReachable(): Boolean = runCatching {
        apiClient.updateBaseUrl(getApiBaseUrl())
        apiClient.health()
        true
    }.getOrDefault(false)

    /** @deprecated Use [isServerReachable]; never expose server details to the UI. */
    suspend fun testConnection(): Result<String> = runCatching {
        apiClient.updateBaseUrl(getApiBaseUrl())
        val health = apiClient.health()
        "${health.service} ${health.version} — ${health.status}"
    }

    suspend fun login(email: String, password: String): Result<User> = runCatching {
        if (!isEnabled()) error("sync_disabled")
        apiClient.updateBaseUrl(getApiBaseUrl())
        val auth = apiClient.login(LoginRequest(email.trim(), password))
        userPreferences.saveAuthToken(auth.token)
        mergePull(apiClient.pull(auth.token), auth.user.id, password)
        repository.getUserById(auth.user.id) ?: error("user_not_found")
    }

    suspend fun register(
        entrepriseNom: String,
        nom: String,
        email: String,
        telephone: String,
        password: String
    ): Result<User> = runCatching {
        if (!isEnabled()) error("sync_disabled")
        apiClient.updateBaseUrl(getApiBaseUrl())
        val auth = apiClient.register(
            RegisterRequest(
                entrepriseNom = entrepriseNom.trim(),
                nom = nom.trim(),
                email = email.trim(),
                telephone = telephone.replace("\\s".toRegex(), ""),
                password = password
            )
        )
        userPreferences.saveAuthToken(auth.token)
        mergePull(apiClient.pull(auth.token), auth.user.id, password)
        repository.getUserById(auth.user.id) ?: error("user_not_found")
    }

    suspend fun syncNow(entrepriseId: String): String? {
        if (!isEnabled()) return null
        val token = userPreferences.getAuthToken()
            ?: return "Session cloud expirée — reconnectez-vous"
        return runCatching {
            apiClient.updateBaseUrl(getApiBaseUrl())
            mergePull(apiClient.pull(token), null, null)
            val pushRequest = repository.buildSyncPushRequest(entrepriseId).copy(
                deletedUserIds = userPreferences.getPendingDeletedUserIds()
            )
            apiClient.push(token, pushRequest)
            userPreferences.clearPendingDeletedUserIds()
            userPreferences.setLastSyncAt(Instant.now().toString())
            null
        }.getOrElse { it.message ?: "Erreur de synchronisation" }
    }

    suspend fun trackUserDeletion(userId: String) {
        userPreferences.addPendingDeletedUserId(userId)
    }

    private suspend fun mergePull(pull: SyncPullResponse, passwordUserId: String?, plainPassword: String?) {
        repository.applySyncPull(pull) { userId, existingHash ->
            when {
                plainPassword != null && userId == passwordUserId -> PasswordHasher.hash(plainPassword)
                existingHash.isNotBlank() -> existingHash
                else -> existingHash
            }
        }
    }
}
