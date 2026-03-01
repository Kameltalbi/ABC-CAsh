package com.abccash.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abccash.app.data.local.entity.CurrencyCode
import com.abccash.app.ui.DateFormatOption
import com.abccash.app.ui.theme.AppPalette
import com.abccash.app.ui.theme.palettePreviewColor

@Composable
fun SettingsScreen(
    currency: CurrencyCode,
    darkMode: Boolean,
    dateFormat: DateFormatOption,
    palette: AppPalette,
    onDarkModeChange: (Boolean) -> Unit,
    onDateFormatChange: (DateFormatOption) -> Unit,
    onPaletteChange: (AppPalette) -> Unit,
    onDefaultCurrencyChange: (CurrencyCode) -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onImportCsv: () -> Unit,
    driveSyncConfigured: Boolean,
    lastDriveSyncStatus: String,
    onConnectDriveFolder: () -> Unit,
    onSyncNow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Parametres", fontWeight = FontWeight.Bold)
                
                CurrencySection(
                    currency = currency,
                    onCurrencyChange = onDefaultCurrencyChange
                )
                
                DateFormatSection(
                    dateFormat = dateFormat,
                    onDateFormatChange = onDateFormatChange
                )
                
                PaletteSection(
                    palette = palette,
                    onPaletteChange = onPaletteChange
                )
                
                DarkModeSection(
                    darkMode = darkMode,
                    onDarkModeChange = onDarkModeChange
                )
                
                ImportCsvSection(
                    onImportCsv = onImportCsv
                )
                
                BackupSection(
                    onBackup = onBackup,
                    onRestore = onRestore
                )
                
                DriveSyncSection(
                    driveSyncConfigured = driveSyncConfigured,
                    lastDriveSyncStatus = lastDriveSyncStatus,
                    onConnectDriveFolder = onConnectDriveFolder,
                    onSyncNow = onSyncNow
                )
                
                Text("Version: MVP")
            }
        }
        Text("Preferences enregistrees localement.")
    }
}

@Composable
private fun CurrencySection(
    currency: CurrencyCode,
    onCurrencyChange: (CurrencyCode) -> Unit
) {
    Text("Devise active: ${currency.name}")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CurrencyCode.entries.forEach { code ->
            AssistChip(
                onClick = { onCurrencyChange(code) },
                label = { Text(code.name) },
                modifier = Modifier.alpha(if (currency == code) 1f else 0.65f)
            )
        }
    }
}

@Composable
private fun DateFormatSection(
    dateFormat: DateFormatOption,
    onDateFormatChange: (DateFormatOption) -> Unit
) {
    Text("Format date")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DateFormatOption.entries.forEach { option ->
            AssistChip(
                onClick = { onDateFormatChange(option) },
                label = { Text(option.label) },
                modifier = Modifier.alpha(if (dateFormat == option) 1f else 0.65f)
            )
        }
    }
}

@Composable
private fun PaletteSection(
    palette: AppPalette,
    onPaletteChange: (AppPalette) -> Unit
) {
    Text("Palette couleur")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AppPalette.entries.forEach { option ->
            PaletteCircleButton(
                color = palettePreviewColor(option),
                selected = palette == option,
                onClick = { onPaletteChange(option) }
            )
        }
    }
}

@Composable
private fun DarkModeSection(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Mode sombre")
        Switch(checked = darkMode, onCheckedChange = onDarkModeChange)
    }
}

@Composable
private fun BackupSection(
    onBackup: () -> Unit,
    onRestore: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onBackup, modifier = Modifier.weight(1f)) {
            Text("Sauvegarder")
        }
        Button(onClick = onRestore, modifier = Modifier.weight(1f)) {
            Text("Restaurer")
        }
    }
}

@Composable
private fun ImportCsvSection(
    onImportCsv: () -> Unit
) {
    Button(
        onClick = onImportCsv,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("📊 Importer CSV Bancaire")
    }
    Text("Importez vos transactions depuis un fichier CSV", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun DriveSyncSection(
    driveSyncConfigured: Boolean,
    lastDriveSyncStatus: String,
    onConnectDriveFolder: () -> Unit,
    onSyncNow: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onConnectDriveFolder, modifier = Modifier.weight(1f)) {
            Text(if (driveSyncConfigured) "Changer dossier Drive" else "Connecter Drive")
        }
        Button(
            onClick = onSyncNow,
            modifier = Modifier.weight(1f),
            enabled = driveSyncConfigured
        ) {
            Text("Sync maintenant")
        }
    }
    Text("Sync auto: active apres chaque modification")
    Text(lastDriveSyncStatus)
}

@Composable
private fun PaletteCircleButton(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    size: Dp = 36.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable { onClick() }
    ) {}
}
