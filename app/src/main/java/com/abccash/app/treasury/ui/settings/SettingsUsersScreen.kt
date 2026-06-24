package com.abccash.app.treasury.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import com.abccash.app.R
import com.abccash.app.treasury.ui.resolveTreasuryMessage
import com.abccash.app.treasury.ui.AbcCashFab
import com.abccash.app.treasury.data.User
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import com.abccash.app.treasury.data.hasPermission
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsUsersScreen(
    userRole: UserRole,
    permissions: Set<UserPermission>,
    currentUserId: String?,
    users: List<User>,
    onBack: () -> Unit,
    onNavigateToAddUser: () -> Unit,
    onDeleteUser: (String) -> Unit,
    onChangePassword: (String, String, String, (String?) -> Unit) -> Unit,
    onResetPassword: (String, String, (String?) -> Unit) -> Unit,
    onExportBackup: ((String?) -> Unit) -> Unit,
    onRestoreBackup: (String, (String?) -> Unit) -> Unit,
    backupFeedback: String? = null,
    onClearBackupFeedback: () -> Unit = {}
) {
    if (!hasPermission(userRole, permissions, UserPermission.MANAGE_USERS)) {
        SettingsDetailScaffold(title = stringResource(R.string.settings_users), onBack = onBack) {
            Box(Modifier.fillMaxSize().padding(it), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.access_denied), color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var userToReset by remember { mutableStateOf<User?>(null) }
    var passwordMessage by remember { mutableStateOf<String?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var backupError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var pendingBackupJson by remember { mutableStateOf<String?>(null) }

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
            backupError = "Fichier vide ou illisible"
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
            text = {
                Text(
                    "Les données existantes seront fusionnées avec le fichier importé. " +
                        "Les entrées avec le même identifiant seront remplacées."
                )
            },
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

    if (showChangePasswordDialog && currentUserId != null) {
        ChangePasswordDialog(
            onDismiss = {
                showChangePasswordDialog = false
                passwordMessage = null
            },
            onConfirm = { current, newPassword ->
                onChangePassword(currentUserId, current, newPassword) { error ->
                    if (error == null) {
                        showChangePasswordDialog = false
                        passwordMessage = "Mot de passe modifié"
                    } else {
                        passwordMessage = error
                    }
                }
            },
            errorMessage = passwordMessage
        )
    }

    userToReset?.let { user ->
        ResetPasswordDialog(
            userName = user.nom,
            onDismiss = {
                userToReset = null
                passwordMessage = null
            },
            onConfirm = { newPassword ->
                onResetPassword(user.id, newPassword) { error ->
                    if (error == null) {
                        userToReset = null
                        passwordMessage = "Mot de passe réinitialisé pour ${user.nom}"
                    } else {
                        passwordMessage = error
                    }
                }
            },
            errorMessage = passwordMessage
        )
    }

    SettingsDetailScaffold(title = stringResource(R.string.settings_users), onBack = onBack) { padding ->
        Scaffold(
            modifier = Modifier.padding(padding),
            floatingActionButton = {
                if (userRole == UserRole.ADMIN) {
                    AbcCashFab(
                        onClick = onNavigateToAddUser,
                        contentDescription = stringResource(R.string.add_user)
                    )
                }
            }
        ) { fabPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(fabPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F8F1))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.my_account), fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.change_your_password), fontSize = 12.sp, color = Color.Gray)
                                passwordMessage?.let {
                                    Text(it, fontSize = 12.sp, color = Color(0xFF4CAF50))
                                }
                            }
                            OutlinedButton(onClick = { showChangePasswordDialog = true }) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.edit))
                            }
                        }
                    }
                }

                if (userRole == UserRole.ADMIN) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F6FF))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(stringResource(R.string.backup_restore), fontWeight = FontWeight.Bold)
                                Text(
                                    "Exportez toutes les données de l'entreprise en JSON, ou restaurez depuis un fichier.",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                backupFeedback?.let {
                                    Text(resolveTreasuryMessage(it) ?: it, fontSize = 12.sp, color = Color(0xFF4CAF50))
                                }
                                backupError?.let {
                                    Text(resolveTreasuryMessage(it) ?: it, fontSize = 12.sp, color = Color(0xFFF44336))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            onExportBackup { json ->
                                                if (json == null) {
                                                    backupError = "Impossible d'exporter la sauvegarde"
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

                item {
                    Text(
                        text = "${stringResource(R.string.settings_users)} (${users.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (users.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.no_users_added),
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    items(users) { user ->
                        UserAdminCard(
                            user = user,
                            canManage = userRole == UserRole.ADMIN,
                            onDelete = { onDeleteUser(user.id) },
                            onResetPassword = { userToReset = user }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    errorMessage: String?
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val passwordsMatch = newPassword == confirmPassword

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.change_password)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text(stringResource(R.string.current_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.new_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.confirm_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = confirmPassword.isNotBlank() && !passwordsMatch,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(currentPassword, newPassword) },
                enabled = currentPassword.isNotBlank() && newPassword.length >= 6 && passwordsMatch
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun ResetPasswordDialog(
    userName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    errorMessage: String?
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val passwordsMatch = newPassword == confirmPassword

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reset_password)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nouveau mot de passe pour $userName")
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.new_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.confirm_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = confirmPassword.isNotBlank() && !passwordsMatch,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newPassword) },
                enabled = newPassword.length >= 6 && passwordsMatch
            ) {
                Text(stringResource(R.string.reset_password))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun UserAdminCard(
    user: User,
    canManage: Boolean,
    onDelete: () -> Unit,
    onResetPassword: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(user.nom, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(user.email, fontSize = 12.sp, color = Color.Gray)
                Text(user.telephone, fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (user.role == UserRole.ADMIN) {
                        Color(0xFF2563EB).copy(alpha = 0.12f)
                    } else {
                        Color(0xFF4CAF50).copy(alpha = 0.12f)
                    }
                ) {
                    Text(
                        text = user.role.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        color = if (user.role == UserRole.ADMIN) Color(0xFF2563EB) else Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (canManage && user.role != UserRole.ADMIN) {
                TextButton(onClick = onResetPassword) {
                    Text(stringResource(R.string.pwd_short), fontSize = 12.sp)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color.Gray)
                }
            }
        }
    }
}
