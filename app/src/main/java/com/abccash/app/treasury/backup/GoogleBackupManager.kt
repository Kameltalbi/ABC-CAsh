package com.abccash.app.treasury.backup

import android.content.Context
import android.content.Intent
import com.abccash.app.BuildConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

class GoogleBackupManager(private val context: Context) {

    companion object {
        private const val BACKUP_FILE_NAME = "abc-cash-backup.json"
        private const val MIME_TYPE = "application/json"
    }

    fun createSignInClient(): GoogleSignInClient {
        return GoogleSignIn.getClient(context, buildSignInOptions())
    }

    private fun buildSignInOptions(): GoogleSignInOptions {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        if (webClientId.isNotBlank()) {
            builder.requestIdToken(webClientId)
        }
        return builder.build()
    }

    fun handleSignInResult(data: Intent?): Result<GoogleSignInAccount> = runCatching {
        GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
    }

    fun getSignInIntent(): Intent = createSignInClient().signInIntent

    fun getSignedInAccount(): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)

    fun isSignedIn(): Boolean = getSignedInAccount() != null

    fun getSignedInEmail(): String? = getSignedInAccount()?.email

    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            Tasks.await(createSignInClient().signOut())
        }
    }

    suspend fun uploadBackup(json: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val drive = buildDriveService()
                ?: error("Connectez-vous avec votre compte Google")

            val existing = findBackupFile(drive)
            val content = ByteArrayContent.fromString(MIME_TYPE, json)
            if (existing == null) {
                val metadata = File().apply {
                    name = BACKUP_FILE_NAME
                    parents = listOf("appDataFolder")
                }
                drive.files().create(metadata, content).setFields("id").execute()
            } else {
                drive.files().update(existing.id, null, content).execute()
            }
            Unit
        }
    }

    suspend fun downloadBackup(): Result<String?> = withContext(Dispatchers.IO) {
        runCatching {
            val drive = buildDriveService()
                ?: error("Connectez-vous avec votre compte Google")

            val existing = findBackupFile(drive) ?: return@runCatching null
            drive.files().get(existing.id).executeMediaAsInputStream().bufferedReader().use {
                it.readText()
            }
        }
    }

    suspend fun deleteBackup(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val drive = buildDriveService() ?: return@runCatching Unit
            val existing = findBackupFile(drive) ?: return@runCatching Unit
            drive.files().delete(existing.id).execute()
            Unit
        }
    }

    private fun buildDriveService(): Drive? {
        val account = getSignedInAccount()?.account ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            Collections.singleton(DriveScopes.DRIVE_APPDATA)
        ).apply {
            selectedAccount = account
        }
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName(context.getString(com.abccash.app.R.string.app_name))
            .build()
    }

    private fun findBackupFile(drive: Drive): File? {
        val result = drive.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '$BACKUP_FILE_NAME' and trashed = false")
            .setFields("files(id, name)")
            .execute()
        return result.files?.firstOrNull()
    }
}
