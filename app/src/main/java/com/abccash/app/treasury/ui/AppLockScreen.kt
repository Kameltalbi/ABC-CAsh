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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
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
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

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
            .setTitle("Déverrouiller ABC Cash")
            .setSubtitle("Utilisez votre empreinte digitale")
            .setNegativeButtonText("Utiliser le PIN")
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
            Text("ABC Cash", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Application verrouillée", textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))

            if (settings.pinEnabled && settings.hasPin) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 8 && it.all { c -> c.isDigit() }) pin = it
                    },
                    label = { Text("Code PIN") },
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
                                error = "Code PIN incorrect"
                                pin = ""
                            }
                        }
                    },
                    enabled = pin.length >= 4,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Déverrouiller")
                }
            }

            if (settings.biometricEnabled && biometricAvailable) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showBiometricPrompt() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Empreinte digitale")
                }
            }

            error?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
        }
    }
}
