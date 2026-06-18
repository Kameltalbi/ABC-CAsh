package com.abccash.app.treasury.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import kotlinx.coroutines.flow.Flow
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
        }
    }
    
    suspend fun setOnboardingAdminVu(vu: Boolean = true) {
        context.userDataStore.edit { preferences ->
            preferences[UserPreferencesKeys.ONBOARDING_ADMIN_VU] = vu
        }
    }
    
    suspend fun clearUserSession() {
        context.userDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
