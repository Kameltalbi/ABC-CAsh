package com.abccash.app.treasury.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.abccash.app.treasury.data.Entreprise
import com.abccash.app.treasury.data.User
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsUserProfileScreen(
    currentUser: User?,
    onBack: () -> Unit,
    onSave: (String, String, String, (String?) -> Unit) -> Unit,
    onSessionUpdated: (String, String) -> Unit
) {
    var nom by remember(currentUser) { mutableStateOf(currentUser?.nom.orEmpty()) }
    var email by remember(currentUser) { mutableStateOf(currentUser?.email.orEmpty()) }
    var telephone by remember(currentUser) { mutableStateOf(currentUser?.telephone.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    SettingsDetailScaffold(title = "Mon profil", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = nom,
                onValueChange = { nom = it },
                label = { Text("Nom complet") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = telephone,
                onValueChange = { telephone = it },
                label = { Text("Téléphone") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (success) {
                Text("Profil enregistré", color = Color(0xFF4CAF50))
            }
            Button(
                onClick = {
                    onSave(nom, email, telephone) { err ->
                        error = err
                        if (err == null) {
                            success = true
                            scope.launch { onSessionUpdated(nom, email) }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enregistrer")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsCompanyProfileScreen(
    entreprise: Entreprise?,
    canEdit: Boolean,
    onBack: () -> Unit,
    onSave: (String, String, String, String, (String?) -> Unit) -> Unit
) {
    var nom by remember(entreprise) { mutableStateOf(entreprise?.nom.orEmpty()) }
    var email by remember(entreprise) { mutableStateOf(entreprise?.email.orEmpty()) }
    var telephone by remember(entreprise) { mutableStateOf(entreprise?.telephone.orEmpty()) }
    var adresse by remember(entreprise) { mutableStateOf(entreprise?.adresse.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }

    SettingsDetailScaffold(title = "Entreprise", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = nom,
                onValueChange = { if (canEdit) nom = it },
                label = { Text("Nom de l'entreprise") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = !canEdit,
                singleLine = true
            )
            OutlinedTextField(
                value = email,
                onValueChange = { if (canEdit) email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = !canEdit,
                singleLine = true
            )
            OutlinedTextField(
                value = telephone,
                onValueChange = { if (canEdit) telephone = it },
                label = { Text("Téléphone") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = !canEdit,
                singleLine = true
            )
            OutlinedTextField(
                value = adresse,
                onValueChange = { if (canEdit) adresse = it },
                label = { Text("Adresse") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = !canEdit,
                minLines = 2
            )
            if (!canEdit) {
                Text(
                    "Seul l'administrateur peut modifier les informations de l'entreprise.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (success) {
                Text("Entreprise enregistrée", color = Color(0xFF4CAF50))
            }
            if (canEdit) {
                Button(
                    onClick = {
                        onSave(nom, email, telephone, adresse) { err ->
                            error = err
                            if (err == null) success = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enregistrer")
                }
            }
        }
    }
}
