package com.abccash.app.treasury.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R
import com.abccash.app.treasury.data.AppCurrency
import com.abccash.app.treasury.datastore.AppSettings
import com.abccash.app.treasury.datastore.AppSettingsState
import com.abccash.app.treasury.notifications.OverdueNotificationHelper
import com.abccash.app.treasury.notifications.OverdueNotificationScheduler
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsCurrencyScreen(
    appSettings: AppSettings,
    onBack: () -> Unit
) {
    val settings by appSettings.settingsFlow.collectAsState(initial = AppSettingsState())
    val config = settings.currencyConfig
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var addError by remember { mutableStateOf<String?>(null) }

    if (showAddDialog) {
        AddCustomCurrencyDialog(
            onDismiss = {
                showAddDialog = false
                addError = null
            },
            onConfirm = { label, symbol, decimals ->
                scope.launch {
                    val error = appSettings.addCustomCurrency(label, symbol, decimals)
                    if (error == null) {
                        showAddDialog = false
                        addError = null
                    } else {
                        addError = error
                    }
                }
            },
            errorMessage = addError
        )
    }

    SettingsDetailScaffold(title = stringResource(R.string.settings_currency), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.settings_currency),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            config.allCurrencies.forEach { currency ->
                CurrencySelectionRow(
                    currency = currency,
                    selected = config.selectedCurrencyId == currency.id,
                    onSelect = {
                        scope.launch { appSettings.setSelectedCurrency(currency.id) }
                    },
                    onDelete = if (currency.isCustom) {
                        { scope.launch { appSettings.removeCustomCurrency(currency.id) } }
                    } else {
                        null
                    }
                )
            }

            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.add_custom_currency))
            }
        }
    }
}

@Composable
private fun CurrencySelectionRow(
    currency: AppCurrency,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFE8F5E9) else Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Column(modifier = Modifier.weight(1f)) {
                Text(currency.displayName(), fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                Text(
                    "${currency.decimalPlaces} · ${if (currency.isCustom) stringResource(R.string.category_custom_tag) else stringResource(R.string.category_builtin_tag)}",
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    color = Color.Gray
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun AddCustomCurrencyDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int) -> Unit,
    errorMessage: String?
) {
    var label by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("") }
    var decimalsText by remember { mutableStateOf("2") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_currency)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.label)) },
                    placeholder = { Text(stringResource(R.string.currency_label_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text(stringResource(R.string.symbol)) },
                    placeholder = { Text(stringResource(R.string.currency_symbol_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = decimalsText,
                    onValueChange = {
                        if (it.length <= 1 && (it.isEmpty() || it.all { c -> c.isDigit() })) {
                            decimalsText = it
                        }
                    },
                    label = { Text(stringResource(R.string.decimal_places)) },
                    placeholder = { Text(stringResource(R.string.decimal_places_placeholder)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(label, symbol, decimalsText.toIntOrNull() ?: 2)
                },
                enabled = label.isNotBlank() && symbol.isNotBlank() && decimalsText.isNotBlank()
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsNotificationsScreen(
    appSettings: AppSettings,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by appSettings.settingsFlow.collectAsState(initial = AppSettingsState())
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            OverdueNotificationScheduler.schedule(context)
        }
    }

    fun applyNotificationsEnabled(enabled: Boolean) {
        scope.launch {
            appSettings.setNotificationsEnabled(enabled)
            if (enabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !OverdueNotificationHelper.canPostNotifications(context)
                ) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                OverdueNotificationScheduler.schedule(context)
            } else {
                OverdueNotificationScheduler.cancel(context)
            }
        }
    }

    SettingsDetailScaffold(title = stringResource(R.string.settings_notifications), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.enable_notifications))
                    Text(
                        stringResource(R.string.settings_notifications_sub),
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = settings.notificationsEnabled,
                    onCheckedChange = { applyNotificationsEnabled(it) }
                )
            }
            Text(
                text = stringResource(R.string.settings_notifications_overdue_hint),
                fontSize = 13.sp,
                color = Color.Gray,
                lineHeight = 18.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSecurityScreen(
    appSettings: AppSettings,
    onBack: () -> Unit
) {
    val settings by appSettings.settingsFlow.collectAsState(initial = AppSettingsState())
    val scope = rememberCoroutineScope()
    var showPinDialog by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }

    if (showPinDialog) {
        SetPinDialog(
            onDismiss = {
                showPinDialog = false
                pinError = null
            },
            onConfirm = { pin ->
                scope.launch {
                    appSettings.setPin(pin)
                    showPinDialog = false
                    pinError = null
                }
            },
            errorMessage = pinError
        )
    }

    SettingsDetailScaffold(title = stringResource(R.string.settings_security), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.fingerprint_lock))
                    Text(
                        stringResource(R.string.settings_security_sub),
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = settings.biometricEnabled,
                    onCheckedChange = {
                        scope.launch { appSettings.setBiometricEnabled(it) }
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.pin_lock))
                    Text(
                        if (settings.hasPin) stringResource(R.string.pin_code)
                        else stringResource(R.string.set_pin),
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = settings.pinEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            showPinDialog = true
                        } else {
                            scope.launch { appSettings.setPinEnabled(false) }
                        }
                    }
                )
            }
            if (settings.hasPin) {
                OutlinedButton(
                    onClick = { showPinDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.change_pin))
                }
            }
        }
    }
}

@Composable
private fun SetPinDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    errorMessage: String?
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_pin)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text(stringResource(R.string.pin_code)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) confirm = it },
                    label = { Text(stringResource(R.string.confirm_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pin) },
                enabled = pin.length >= 4 && pin == confirm
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
