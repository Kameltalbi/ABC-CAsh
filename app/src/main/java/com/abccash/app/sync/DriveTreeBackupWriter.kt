package com.abccash.app.sync

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedReader
import java.io.InputStreamReader

private const val BACKUP_FILE_NAME = "abc_cash_backup.json"
private const val BACKUP_MIME_TYPE = "application/json"

fun writeBackupToTree(
    context: Context,
    treeUriString: String,
    json: String
): Result<Unit> = runCatching {
    val treeUri = Uri.parse(treeUriString)
    val root = DocumentFile.fromTreeUri(context, treeUri)
        ?: error("Dossier Drive invalide")

    val backupFile = root.findFile(BACKUP_FILE_NAME)
        ?: root.createFile(BACKUP_MIME_TYPE, BACKUP_FILE_NAME)
        ?: error("Impossible de creer le fichier de sauvegarde")

    context.contentResolver.openOutputStream(backupFile.uri, "wt")?.use { stream ->
        stream.write(json.toByteArray())
    } ?: error("Impossible d'ecrire la sauvegarde")
}

fun readBackupFromTree(
    context: Context,
    treeUriString: String
): Result<String> = runCatching {
    val treeUri = Uri.parse(treeUriString)
    val root = DocumentFile.fromTreeUri(context, treeUri)
        ?: error("Dossier Drive invalide")
    val backupFile = root.findFile(BACKUP_FILE_NAME)
        ?: error("Aucune sauvegarde Drive trouvee")

    context.contentResolver.openInputStream(backupFile.uri)?.use { input ->
        BufferedReader(InputStreamReader(input)).readText()
    } ?: error("Impossible de lire la sauvegarde Drive")
}
