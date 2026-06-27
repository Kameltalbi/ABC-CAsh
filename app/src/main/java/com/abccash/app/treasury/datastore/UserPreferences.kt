package com.abccash.app.treasury.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.abccash.app.treasury.data.DocumentPdfTemplate
import com.abccash.app.treasury.data.InvoiceSettings
import com.abccash.app.treasury.data.OtherTaxMode
import com.abccash.app.treasury.data.SubscriptionPlan
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
    val GOOGLE_ACCOUNT_EMAIL = stringPreferencesKey("google_account_email")
    val GOOGLE_LAST_BACKUP_AT = stringPreferencesKey("google_last_backup_at")
    val SUBSCRIPTION_PLAN = stringPreferencesKey("subscription_plan")
    fun TREASURY_INITIALIZED(entrepriseId: String) = booleanPreferencesKey("treasury_initialized_$entrepriseId")
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

    val googleAccountEmail: Flow<String?> = context.userDataStore.data
        .map { preferences -> preferences[UserPreferencesKeys.GOOGLE_ACCOUNT_EMAIL] }

    val googleLastBackupAt: Flow<String?> = context.userDataStore.data
        .map { preferences -> preferences[UserPreferencesKeys.GOOGLE_LAST_BACKUP_AT] }

    suspend fun saveGoogleAccount(email: String?) {
        context.userDataStore.edit { preferences ->
            if (email.isNullOrBlank()) {
                preferences.remove(UserPreferencesKeys.GOOGLE_ACCOUNT_EMAIL)
            } else {
                preferences[UserPreferencesKeys.GOOGLE_ACCOUNT_EMAIL] = email
            }
        }
    }

    suspend fun setGoogleLastBackupAt(isoInstant: String) {
        context.userDataStore.edit { preferences ->
            preferences[UserPreferencesKeys.GOOGLE_LAST_BACKUP_AT] = isoInstant
        }
    }

    suspend fun clearGoogleAccount() {
        context.userDataStore.edit { preferences ->
            preferences.remove(UserPreferencesKeys.GOOGLE_ACCOUNT_EMAIL)
            preferences.remove(UserPreferencesKeys.GOOGLE_LAST_BACKUP_AT)
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
            preferences[UserPreferencesKeys.IS_LOGGED_IN] = false
            preferences[UserPreferencesKeys.ONBOARDING_ADMIN_VU] = onboardingVu
        }
    }

    suspend fun readLoggedIn(): Boolean =
        context.userDataStore.data.first()[UserPreferencesKeys.IS_LOGGED_IN] ?: false

    suspend fun readSessionUserId(): String? =
        context.userDataStore.data.first()[UserPreferencesKeys.USER_ID]

    suspend fun readEntrepriseId(): String? =
        context.userDataStore.data.first()[UserPreferencesKeys.USER_ENTREPRISE_ID]

    suspend fun readSubscriptionPlan(): SubscriptionPlan {
        val id = context.userDataStore.data.first()[UserPreferencesKeys.SUBSCRIPTION_PLAN]
        return SubscriptionPlan.fromId(id)
    }

    suspend fun saveSubscriptionPlan(plan: SubscriptionPlan) {
        context.userDataStore.edit { preferences ->
            preferences[UserPreferencesKeys.SUBSCRIPTION_PLAN] = plan.id
        }
    }

    suspend fun clearSubscriptionPlan() {
        context.userDataStore.edit { preferences ->
            preferences.remove(UserPreferencesKeys.SUBSCRIPTION_PLAN)
        }
    }

    fun observeTreasuryInitialized(entrepriseId: String): Flow<Boolean> =
        context.userDataStore.data.map { preferences ->
            preferences[UserPreferencesKeys.TREASURY_INITIALIZED(entrepriseId)] ?: false
        }

    suspend fun readTreasuryInitialized(entrepriseId: String): Boolean =
        context.userDataStore.data.first()[UserPreferencesKeys.TREASURY_INITIALIZED(entrepriseId)] ?: false

    suspend fun setTreasuryInitialized(entrepriseId: String, initialized: Boolean = true) {
        context.userDataStore.edit { preferences ->
            preferences[UserPreferencesKeys.TREASURY_INITIALIZED(entrepriseId)] = initialized
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

    fun observeInvoiceSettings(entrepriseId: String): Flow<InvoiceSettings> =
        context.userDataStore.data.map { preferences ->
            InvoiceSettings(
                prefix = preferences[invoicePrefixKey(entrepriseId)] ?: "FAC-",
                quotePrefix = preferences[quotePrefixKey(entrepriseId)] ?: "DEV-",
                tvaRate = preferences[invoiceTvaKey(entrepriseId)]?.toDoubleOrNull() ?: 19.0,
                otherTaxRate = preferences[invoiceOtherTaxKey(entrepriseId)]?.toDoubleOrNull() ?: 0.0,
                otherTaxMode = OtherTaxMode.fromName(preferences[invoiceOtherTaxModeKey(entrepriseId)]),
                otherTaxLabel = preferences[invoiceOtherTaxLabelKey(entrepriseId)] ?: "",
                pdfTemplate = DocumentPdfTemplate.fromName(preferences[invoicePdfTemplateKey(entrepriseId)])
            )
        }

    suspend fun saveInvoiceSettings(entrepriseId: String, settings: InvoiceSettings) {
        context.userDataStore.edit { preferences ->
            preferences[invoicePrefixKey(entrepriseId)] = settings.prefix.trim().ifBlank { "FAC-" }
            preferences[quotePrefixKey(entrepriseId)] = settings.quotePrefix.trim().ifBlank { "DEV-" }
            preferences[invoiceTvaKey(entrepriseId)] = settings.tvaRate.toString()
            preferences[invoiceOtherTaxKey(entrepriseId)] = settings.otherTaxRate.toString()
            preferences[invoiceOtherTaxModeKey(entrepriseId)] = settings.otherTaxMode.name
            preferences[invoiceOtherTaxLabelKey(entrepriseId)] = settings.otherTaxLabel.trim()
            preferences[invoicePdfTemplateKey(entrepriseId)] = settings.pdfTemplate.name
        }
    }

}

private fun bankBalanceKey(entrepriseId: String, year: Int) =
    stringPreferencesKey("bank_balance_${entrepriseId}_$year")

private fun invoicePrefixKey(entrepriseId: String) =
    stringPreferencesKey("invoice_prefix_$entrepriseId")

private fun quotePrefixKey(entrepriseId: String) =
    stringPreferencesKey("quote_prefix_$entrepriseId")

private fun invoiceTvaKey(entrepriseId: String) =
    stringPreferencesKey("invoice_tva_$entrepriseId")

private fun invoiceOtherTaxKey(entrepriseId: String) =
    stringPreferencesKey("invoice_other_tax_$entrepriseId")

private fun invoiceOtherTaxModeKey(entrepriseId: String) =
    stringPreferencesKey("invoice_other_tax_mode_$entrepriseId")

private fun invoiceOtherTaxLabelKey(entrepriseId: String) =
    stringPreferencesKey("invoice_other_tax_label_$entrepriseId")

private fun invoicePdfTemplateKey(entrepriseId: String) =
    stringPreferencesKey("invoice_pdf_template_$entrepriseId")

