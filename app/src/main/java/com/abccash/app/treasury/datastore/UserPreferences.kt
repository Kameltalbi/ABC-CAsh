package com.abccash.app.treasury.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.abccash.app.BuildConfig
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

object UserPreferencesKeys {
    val USER_ID = stringPreferencesKey("user_id")
    val USER_EMAIL = stringPreferencesKey("user_email")
    val USER_NOM = stringPreferencesKey("user_nom")
    val USER_ROLE = stringPreferencesKey("user_role")
    val USER_ENTREPRISE_ID = stringPreferencesKey("user_entreprise_id")
    val USER_PERMISSIONS = stringPreferencesKey("user_permissions")
    val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    val ONBOARDING_ADMIN_VU = booleanPreferencesKey("onboarding_admin_vu")
    val AUTH_TOKEN = stringPreferencesKey("auth_token")
    val API_BASE_URL = stringPreferencesKey("api_base_url")
    val LAST_SYNC_AT = stringPreferencesKey("last_sync_at")
    val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
}

class UserPreferences(private val context: Context) {
    
    val isLoggedIn: Flow<Boolean> = context.userDataStore.data
        .map { preferences ->
            preferences[UserPreferencesKeys.IS_LOGGED_IN] ?: false
        }
    
    val isAdmin: Flow<Boolean> = context.userDataStore.data
        .map { preferences ->
            val role = preferences[UserPreferencesKeys.USER_ROLE]
            role == UserRole.ADMIN.name
        }
    
    val onboardingAdminVu: Flow<Boolean> = context.userDataStore.data
        .map { preferences ->
            preferences[UserPreferencesKeys.ONBOARDING_ADMIN_VU] ?: false
        }
    
    val currentUserId: Flow<String?> = context.userDataStore.data
        .map { preferences ->
            preferences[UserPreferencesKeys.USER_ID]
        }

    val currentEntrepriseId: Flow<String?> = context.userDataStore.data
        .map { preferences ->
            preferences[UserPreferencesKeys.USER_ENTREPRISE_ID]
        }
    
    val currentPermissions: Flow<Set<UserPermission>> = context.userDataStore.data
        .map { preferences ->
            preferences[UserPreferencesKeys.USER_PERMISSIONS]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.mapNotNull { runCatching { UserPermission.valueOf(it) }.getOrNull() }
                ?.toSet()
                ?: emptySet()
        }

    val isSyncEnabled: Flow<Boolean> = context.userDataStore.data
        .map { preferences -> preferences[UserPreferencesKeys.SYNC_ENABLED] ?: true }

    suspend fun getApiBaseUrl(): String = BuildConfig.API_BASE_URL

    suspend fun setApiBaseUrl(@Suppress("UNUSED_PARAMETER") url: String) {
        // Server URL is fixed in the app build — not user-configurable.
    }

    suspend fun setSyncEnabled(enabled: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[UserPreferencesKeys.SYNC_ENABLED] = enabled
        }
    }

    suspend fun saveAuthToken(token: String) {
        context.userDataStore.edit { preferences ->
            preferences[UserPreferencesKeys.AUTH_TOKEN] = token
        }
    }

    suspend fun getAuthToken(): String? =
        context.userDataStore.data.first()[UserPreferencesKeys.AUTH_TOKEN]

    suspend fun getLastSyncAt(): String? =
        context.userDataStore.data.first()[UserPreferencesKeys.LAST_SYNC_AT]

    suspend fun setLastSyncAt(isoInstant: String) {
        context.userDataStore.edit { preferences ->
            preferences[UserPreferencesKeys.LAST_SYNC_AT] = isoInstant
        }
    }

    suspend fun saveUserSession(
        userId: String,
        email: String,
        nom: String,
        role: UserRole,
        entrepriseId: String,
        permissions: Set<UserPermission>
    ) {
        context.userDataStore.edit { preferences ->
            preferences[UserPreferencesKeys.USER_ID] = userId
            preferences[UserPreferencesKeys.USER_EMAIL] = email
            preferences[UserPreferencesKeys.USER_NOM] = nom
            preferences[UserPreferencesKeys.USER_ROLE] = role.name
            preferences[UserPreferencesKeys.USER_ENTREPRISE_ID] = entrepriseId
            preferences[UserPreferencesKeys.USER_PERMISSIONS] = permissions.joinToString(",") { it.name }
            preferences[UserPreferencesKeys.IS_LOGGED_IN] = true
            if (role == UserRole.ADMIN) {
                preferences[UserPreferencesKeys.ONBOARDING_ADMIN_VU] = true
            }
        }
    }
    
    suspend fun setOnboardingAdminVu(vu: Boolean = true) {
        context.userDataStore.edit { preferences ->
            preferences[UserPreferencesKeys.ONBOARDING_ADMIN_VU] = vu
        }
    }
    
    suspend fun updateProfileSession(nom: String, email: String) {
        context.userDataStore.edit { preferences ->
            preferences[UserPreferencesKeys.USER_NOM] = nom
            preferences[UserPreferencesKeys.USER_EMAIL] = email
        }
    }

    suspend fun clearUserSession() {
        context.userDataStore.edit { preferences ->
            val onboardingVu = preferences[UserPreferencesKeys.ONBOARDING_ADMIN_VU] ?: false
            preferences.remove(UserPreferencesKeys.USER_ID)
            preferences.remove(UserPreferencesKeys.USER_EMAIL)
            preferences.remove(UserPreferencesKeys.USER_NOM)
            preferences.remove(UserPreferencesKeys.USER_ROLE)
            preferences.remove(UserPreferencesKeys.USER_ENTREPRISE_ID)
            preferences.remove(UserPreferencesKeys.USER_PERMISSIONS)
            preferences.remove(UserPreferencesKeys.AUTH_TOKEN)
            preferences[UserPreferencesKeys.IS_LOGGED_IN] = false
            preferences[UserPreferencesKeys.ONBOARDING_ADMIN_VU] = onboardingVu
        }
    }

    fun observeBankBalance(entrepriseId: String, year: Int): Flow<Double?> =
        context.userDataStore.data.map { preferences ->
            preferences[bankBalanceKey(entrepriseId, year)]?.toDoubleOrNull()
        }

    suspend fun saveBankBalance(entrepriseId: String, year: Int, amount: Double?) {
        context.userDataStore.edit { preferences ->
            val key = bankBalanceKey(entrepriseId, year)
            if (amount == null) {
                preferences.remove(key)
            } else {
                preferences[key] = amount.toString()
            }
        }
    }
}

private fun bankBalanceKey(entrepriseId: String, year: Int) =
    stringPreferencesKey("bank_balance_${entrepriseId}_$year")
