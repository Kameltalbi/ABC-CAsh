package com.abccash.app.treasury.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abccash.app.R
import com.abccash.app.locale.AppLocale
import com.abccash.app.treasury.remote.TreasurySyncService
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsSyncScreen(
    syncService: TreasurySyncService,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var lastSync by remember { mutableStateOf<String?>(null) }
    var serverOnline by remember { mutableStateOf<Boolean?>(null) }
    var cloudLinked by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }

    suspend fun refreshStatus() {
        isChecking = true
        cloudLinked = syncService.hasCloudSession()
        lastSync = syncService.getLastSyncAt()
        serverOnline = syncService.isServerReachable()
        isChecking = false
    }

    LaunchedEffect(Unit) {
        refreshStatus()
    }

    SettingsDetailScaffold(title = stringResource(R.string.settings_sync), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_sync_auto_info),
                style = MaterialTheme.typography.bodyLarge
            )

            SyncStatusLine(
                label = when (serverOnline) {
                    true -> stringResource(R.string.settings_sync_server_online)
                    false -> stringResource(R.string.settings_sync_server_offline)
                    null -> stringResource(R.string.settings_sync_checking)
                },
                color = when (serverOnline) {
                    true -> Color(0xFF22C55E)
                    false -> MaterialTheme.colorScheme.error
                    null -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            SyncStatusLine(
                label = if (cloudLinked) {
                    stringResource(R.string.settings_sync_account_linked)
                } else {
                    stringResource(R.string.settings_sync_account_local)
                },
                color = if (cloudLinked) Color(0xFF22C55E) else Color(0xFFF59E0B)
            )

            SyncStatusLine(
                label = lastSync?.let { formatSyncInstant(it) }?.let { formatted ->
                    stringResource(R.string.settings_sync_last, formatted)
                } ?: stringResource(R.string.settings_sync_never),
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedButton(
                onClick = { scope.launch { refreshStatus() } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isChecking
            ) {
                Text(
                    if (isChecking) {
                        stringResource(R.string.settings_sync_checking)
                    } else {
                        stringResource(R.string.settings_sync_test)
                    }
                )
            }
        }
    }
}

@Composable
private fun SyncStatusLine(label: String, color: Color) {
    Text(text = label, color = color, style = MaterialTheme.typography.bodyMedium)
}

private fun formatSyncInstant(isoInstant: String): String? = runCatching {
    val instant = Instant.parse(isoInstant)
    val zoned = instant.atZone(ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", AppLocale.current())
    zoned.format(formatter)
}.getOrNull() ?: isoInstant
