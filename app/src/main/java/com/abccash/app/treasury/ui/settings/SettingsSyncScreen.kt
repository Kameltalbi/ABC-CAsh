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
    onSyncNow: (onResult: (String?) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var apiUrl by remember { mutableStateOf("") }
    var syncEnabled by remember { mutableStateOf(true) }
    var lastSync by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    val okMessage = stringResource(R.string.settings_sync_ok)

    LaunchedEffect(Unit) {
        apiUrl = syncService.getApiBaseUrl()
        syncEnabled = syncService.isEnabled()
        lastSync = syncService.getLastSyncAt()
    }

    SettingsDetailScaffold(title = stringResource(R.string.settings_sync), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.settings_sync_enabled))
                Switch(
                    checked = syncEnabled,
                    onCheckedChange = { enabled ->
                        syncEnabled = enabled
                        scope.launch { syncService.setSyncEnabled(enabled) }
                    }
                )
            }

            OutlinedTextField(
                value = apiUrl,
                onValueChange = { apiUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_sync_url)) },
                singleLine = true
            )

            lastSync?.let {
                Text(stringResource(R.string.settings_sync_last, it))
            }

            feedback?.let {
                Text(
                    text = it,
                    color = if (it == okMessage) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        isBusy = true
                        syncService.setApiBaseUrl(apiUrl)
                        feedback = syncService.testConnection().fold(
                            onSuccess = { it },
                            onFailure = { it.message ?: "Error" }
                        )
                        isBusy = false
                    }
                },
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_sync_test))
            }

            Button(
                onClick = {
                    scope.launch { syncService.setApiBaseUrl(apiUrl) }
                    isBusy = true
                    feedback = null
                    onSyncNow { error ->
                        feedback = error ?: okMessage
                        isBusy = false
                        scope.launch { lastSync = syncService.getLastSyncAt() }
                    }
                },
                enabled = !isBusy && syncEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.settings_sync_now))
                }
            }
        }
    }
}
