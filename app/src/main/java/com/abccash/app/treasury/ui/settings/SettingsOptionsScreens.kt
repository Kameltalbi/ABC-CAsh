package com.abccash.app.treasury.ui.settings

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.abccash.app.treasury.data.AppCurrency
import com.abccash.app.treasury.datastore.AppSettings
import com.abccash.app.treasury.datastore.AppSettingsState
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

    SettingsDetailScaffold(title = "Devise par défaut", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Devise active",
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
                Text("Ajouter une devise personnalisée")
            }

            Text(
                "La devise sélectionnée s'applique à l'affichage de tous les montants dans l'application.",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
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
                    "${currency.decimalPlaces} décimale(s) · ${if (currency.isCustom) "Personnalisée" else "Intégrée"}",
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    color = Color.Gray
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Supprimer",
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
        title = { Text("Nouvelle devise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Libellé") },
                    placeholder = { Text("Ex : Dinar algérien") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text("Symbole") },
                    placeholder = { Text("Ex : DZD") },
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
                    label = { Text("Nombre de décimales") },
                    placeholder = { Text("0 à 4") },
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
                Text("Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsNotificationsScreen(
    appSettings: AppSettings,
    onBack: () -> Unit
) {
    val settings by appSettings.settingsFlow.collectAsState(initial = AppSettingsState())
    val scope = rememberCoroutineScope()

    SettingsDetailScaffold(title = "Notifications", onBack = onBack) { padding ->
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
                    Text("Activer les notifications")
                    Text(
                        "Rappels d'échéances et alertes de trésorerie",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = settings.notificationsEnabled,
                    onCheckedChange = {
                        scope.launch { appSettings.setNotificationsEnabled(it) }
                    }
                )
            }
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

    SettingsDetailScaffold(title = "Sécurité", onBack = onBack) { padding ->
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
                    Text("Verrouillage par empreinte")
                    Text(
                        "Demander l'empreinte digitale à l'ouverture",
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
                    Text("Verrouillage par code PIN")
                    Text(
                        if (settings.hasPin) "PIN configuré (4 chiffres minimum)"
                        else "Aucun PIN configuré",
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
                    Text("Modifier le code PIN")
                }
            }
            Text(
                "Au moins une méthode de verrouillage peut être activée pour protéger l'accès à l'application.",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
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
        title = { Text("Définir un code PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text("Code PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) confirm = it },
                    label = { Text("Confirmer") },
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
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
