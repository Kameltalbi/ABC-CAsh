package com.abccash.app.treasury.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import com.abccash.app.R
import com.abccash.app.treasury.backup.GoogleBackupManager
import com.abccash.app.treasury.ui.googleSignInErrorMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsBackupScreen(
    googleBackupManager: GoogleBackupManager,
    googleAccountEmail: String?,
    onBack: () -> Unit,
    onGoogleSignedIn: (String?) -> Unit,
    onGoogleSignedOut: () -> Unit,
    onBackupToGoogle: (onResult: (String?) -> Unit) -> Unit,
    onRestoreFromGoogle: (onResult: (String?) -> Unit) -> Unit,
    onExportBackup: ((String?) -> Unit) -> Unit,
    onRestoreBackup: (String, (String?) -> Unit) -> Unit,
    backupFeedback: String? = null,
    onClearBackupFeedback: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var backupError by remember { mutableStateOf<String?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var pendingBackupJson by remember { mutableStateOf<String?>(null) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var signedInEmail by remember(googleAccountEmail) {
        mutableStateOf(googleAccountEmail ?: googleBackupManager.getSignedInEmail())
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isGoogleLoading = false
        if (result.resultCode != android.app.Activity.RESULT_OK) {
            if (result.resultCode != android.app.Activity.RESULT_CANCELED) {
                backupError = context.getString(R.string.google_sign_in_failed)
            }
            return@rememberLauncherForActivityResult
        }
        googleBackupManager.handleSignInResult(result.data)
            .onSuccess { account ->
                signedInEmail = account.email
                onGoogleSignedIn(account.email)
                backupError = null
            }
            .onFailure { error ->
                backupError = googleSignInErrorMessage(context, error)
            }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val json = pendingBackupJson
        if (uri != null && json != null) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
            }
            backupError = null
        }
        pendingBackupJson = null
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val json = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().readText()
        }
        if (json.isNullOrBlank()) {
            backupError = context.getString(R.string.backup_file_unreadable)
        } else {
            pendingRestoreJson = json
            showRestoreConfirm = true
        }
    }

    LaunchedEffect(backupFeedback) {
        if (backupFeedback != null) {
            delay(4000)
            onClearBackupFeedback()
        }
    }

    if (showRestoreConfirm && pendingRestoreJson != null) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirm = false
                pendingRestoreJson = null
            },
            title = { Text(stringResource(R.string.restore_backup_question)) },
            text = { Text(stringResource(R.string.restore_backup_merge_hint)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val json = pendingRestoreJson ?: return@TextButton
                        onRestoreBackup(json) { error ->
                            backupError = error
                            showRestoreConfirm = false
                            pendingRestoreJson = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.restore_action), color = Color(0xFFF44336))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    pendingRestoreJson = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    SettingsDetailScaffold(title = stringResource(R.string.backup_restore), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (signedInEmail != null) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (signedInEmail != null) Color(0xFF2E7D32) else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.google_backup_title), fontWeight = FontWeight.Bold)
                    }
                    Text(
                        stringResource(R.string.google_backup_sub),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    signedInEmail?.let { email ->
                        Text(
                            stringResource(R.string.google_account_connected, email),
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                    if (signedInEmail == null) {
                        Button(
                            onClick = {
                                isGoogleLoading = true
                                googleSignInLauncher.launch(googleBackupManager.getSignInIntent())
                            },
                            enabled = !isGoogleLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isGoogleLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text(stringResource(R.string.google_sign_in))
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    isGoogleLoading = true
                                    onBackupToGoogle { error ->
                                        isGoogleLoading = false
                                        backupError = error
                                    }
                                },
                                enabled = !isGoogleLoading,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.google_backup_now), fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    isGoogleLoading = true
                                    onRestoreFromGoogle { error ->
                                        isGoogleLoading = false
                                        backupError = error
                                    }
                                },
                                enabled = !isGoogleLoading,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.google_restore), fontSize = 12.sp)
                            }
                        }
                        TextButton(
                            onClick = {
                                scope.launch {
                                    googleBackupManager.signOut()
                                    signedInEmail = null
                                    onGoogleSignedOut()
                                }
                            }
                        ) {
                            Text(stringResource(R.string.google_sign_out), color = Color(0xFFF44336))
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F6FF))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(stringResource(R.string.backup_manual_title), fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.backup_manual_sub),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    backupFeedback?.let {
                        Text(it, fontSize = 12.sp, color = Color(0xFF4CAF50))
                    }
                    backupError?.let {
                        Text(it, fontSize = 12.sp, color = Color(0xFFF44336))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                onExportBackup { json ->
                                    if (json == null) {
                                        backupError = context.getString(R.string.backup_export_failed)
                                    } else {
                                        pendingBackupJson = json
                                        exportLauncher.launch("abc-cash-backup.json")
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.export_action), fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.restore_action), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
