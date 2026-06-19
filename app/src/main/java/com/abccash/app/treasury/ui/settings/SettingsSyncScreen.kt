package com.abccash.app.treasury.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abccash.app.R
import com.abccash.app.treasury.remote.TreasurySyncService
import kotlinx.coroutines.launch

@Composable
fun SettingsSyncScreen(
    syncService: TreasurySyncService,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var apiUrl by remember { mutableStateOf("") }
    var lastSync by remember { mutableStateOf<String?>(null) }
    var connectionStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        apiUrl = syncService.getApiBaseUrl()
        lastSync = syncService.getLastSyncAt()
        connectionStatus = syncService.testConnection().getOrNull()
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

            connectionStatus?.let { status ->
                Text(
                    text = stringResource(R.string.settings_sync_status, status),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            lastSync?.let {
                Text(stringResource(R.string.settings_sync_last, it))
            } ?: Text(stringResource(R.string.settings_sync_never))

            OutlinedTextField(
                value = apiUrl,
                onValueChange = { apiUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_sync_url)) },
                singleLine = true,
                supportingText = { Text(stringResource(R.string.settings_sync_url_hint)) }
            )

            Button(
                onClick = {
                    scope.launch {
                        syncService.setApiBaseUrl(apiUrl)
                        connectionStatus = syncService.testConnection().fold(
                            onSuccess = { it },
                            onFailure = { it.message ?: "Error" }
                        )
                        lastSync = syncService.getLastSyncAt()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
