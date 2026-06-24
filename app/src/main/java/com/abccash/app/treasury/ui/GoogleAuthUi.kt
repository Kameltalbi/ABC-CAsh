package com.abccash.app.treasury.ui

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R
import com.abccash.app.treasury.backup.GoogleBackupManager
import com.abccash.app.treasury.data.User
import com.abccash.app.treasury.data.TreasuryMessage
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException

fun googleSignInErrorMessage(context: Context, throwable: Throwable): String {
    val apiException = throwable as? ApiException
    return when (apiException?.statusCode) {
        GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> context.getString(R.string.google_sign_in_cancelled)
        GoogleSignInStatusCodes.SIGN_IN_FAILED,
        10 -> context.getString(R.string.google_sign_in_config_error)
        else -> {
            val codeSuffix = apiException?.statusCode?.let { " ($it)" }.orEmpty()
            context.getString(R.string.google_sign_in_failed) + codeSuffix
        }
    }
}

@Composable
fun AuthGoogleSection(
    googleBackupManager: GoogleBackupManager,
    connectedEmail: String?,
    onRestoreFromGoogle: ((User?, String?) -> Unit) -> Unit,
    onRestoreSuccess: (User) -> Unit,
    onGoogleConnected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var googleError by remember { mutableStateOf<String?>(null) }
    var isSigningIn by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    val restoreFailedMessage = stringResource(R.string.google_restore_failed)
    val connectedMessage = stringResource(R.string.google_account_connected, connectedEmail.orEmpty())

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isSigningIn = false
        if (result.resultCode != Activity.RESULT_OK) {
            if (result.resultCode != Activity.RESULT_CANCELED) {
                googleError = context.getString(R.string.google_sign_in_failed)
            }
            return@rememberLauncherForActivityResult
        }
        googleBackupManager.handleSignInResult(result.data)
            .onSuccess { account ->
                googleError = null
                onGoogleConnected(account.email)
                isRestoring = true
                onRestoreFromGoogle { user, error ->
                    isRestoring = false
                    when {
                        user != null -> onRestoreSuccess(user)
                        error == TreasuryMessage.GOOGLE_NO_BACKUP -> Unit
                        error != null -> googleError = context.resolveTreasuryMessage(error) ?: error
                        else -> googleError = restoreFailedMessage
                    }
                }
            }
            .onFailure { error ->
                googleError = googleSignInErrorMessage(context, error)
                isRestoring = false
            }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = BorderStroke(1.dp, Color(0xFFE8E4DD))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.google_backup_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A1A)
            )
            Text(
                text = stringResource(R.string.google_restore_existing_hint),
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                lineHeight = 18.sp
            )

            if (!connectedEmail.isNullOrBlank()) {
                Text(
                    text = connectedMessage,
                    fontSize = 13.sp,
                    color = Color(0xFF15803D),
                    fontWeight = FontWeight.Medium
                )
            }

            googleError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            OutlinedButton(
                onClick = {
                    googleError = null
                    isSigningIn = true
                    googleSignInLauncher.launch(googleBackupManager.getSignInIntent())
                },
                enabled = !isSigningIn && !isRestoring,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE8E4DD)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1A1A1A)
                )
            ) {
                if (isSigningIn || isRestoring) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(
                            if (isRestoring) R.string.google_restore else R.string.google_sign_in
                        ),
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "G  ${stringResource(R.string.google_sign_in)}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            Text(
                text = stringResource(R.string.google_sign_in_or_restore_hint),
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun AuthSectionDivider(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE8E4DD))
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Medium
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE8E4DD))
    }
}
