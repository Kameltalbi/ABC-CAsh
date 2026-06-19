package com.abccash.app.treasury.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.abccash.app.R
import com.abccash.app.treasury.datastore.AppSettings
import com.abccash.app.treasury.datastore.AppSettingsState
import kotlinx.coroutines.launch

@Composable
fun AppLockGate(
    appSettings: AppSettings,
    content: @Composable () -> Unit
) {
    val settings by appSettings.settingsFlow.collectAsState(initial = AppSettingsState())
    var unlocked by remember { mutableStateOf(false) }

    LaunchedEffect(settings.requiresLock()) {
        unlocked = !settings.requiresLock()
    }

    if (!settings.requiresLock() || unlocked) {
        content()
    } else {
        AppLockScreen(
            appSettings = appSettings,
            settings = settings,
            onUnlocked = { unlocked = true }
        )
    }
}

@Composable
private fun AppLockScreen(
    appSettings: AppSettings,
    settings: AppSettingsState,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pinIncorrectError = stringResource(R.string.pin_incorrect)
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val unlockTitle = stringResource(R.string.unlock)
    val usePinLabel = stringResource(R.string.use_pin)
    val fingerprintLabel = stringResource(R.string.fingerprint)

    val activity = context as? FragmentActivity
    val biometricAvailable = remember {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showBiometricPrompt() {
        val host = activity ?: return
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            host,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    error = errString.toString()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(unlockTitle)
            .setSubtitle(fingerprintLabel)
            .setNegativeButtonText(usePinLabel)
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(settings.biometricEnabled, biometricAvailable) {
        if (settings.biometricEnabled && biometricAvailable) {
            showBiometricPrompt()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(stringResource(R.string.app_name), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.app_locked), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))

            if (settings.pinEnabled && settings.hasPin) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 8 && it.all { c -> c.isDigit() }) pin = it
                    },
                    label = { Text(stringResource(R.string.pin_code)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        scope.launch {
                            if (appSettings.verifyPin(pin)) {
                                onUnlocked()
                            } else {
                                error = pinIncorrectError
                                pin = ""
                            }
                        }
                    },
                    enabled = pin.length >= 4,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.unlock))
                }
            }

            if (settings.biometricEnabled && biometricAvailable) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showBiometricPrompt() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.fingerprint))
                }
            }

            error?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
        }
    }
}
