package com.abccash.app.treasury.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.treasury.data.ExpenseCategory
import com.abccash.app.treasury.data.RevenueCategory
import com.abccash.app.treasury.datastore.AppSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsIncomeCategoriesScreen(
    entrepriseId: String,
    appSettings: AppSettings,
    onBack: () -> Unit
) {
    val customCategories by appSettings.customIncomeCategories(entrepriseId)
        .collectAsState(initial = emptyList())
    var newLabel by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    SettingsDetailScaffold(title = "Catégories d'encaissement", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Catégories intégrées", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            items(RevenueCategory.entries.toList()) { category ->
                CategoryRow(label = category.label, isBuiltIn = true, onDelete = null)
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Catégories personnalisées", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            items(customCategories) { label ->
                CategoryRow(
                    label = label,
                    isBuiltIn = false,
                    onDelete = {
                        scope.launch { appSettings.removeCustomIncomeCategory(entrepriseId, label) }
                    }
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newLabel,
                        onValueChange = { newLabel = it },
                        label = { Text("Nouvelle catégorie") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            scope.launch {
                                appSettings.addCustomIncomeCategory(entrepriseId, newLabel)
                                newLabel = ""
                            }
                        },
                        enabled = newLabel.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Ajouter")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsExpenseCategoriesScreen(
    entrepriseId: String,
    appSettings: AppSettings,
    onBack: () -> Unit
) {
    val customCategories by appSettings.customExpenseCategories(entrepriseId)
        .collectAsState(initial = emptyList())
    var newLabel by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    SettingsDetailScaffold(title = "Catégories de dépense", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Catégories intégrées", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            items(ExpenseCategory.entries.toList()) { category ->
                CategoryRow(label = category.label, isBuiltIn = true, onDelete = null)
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Catégories personnalisées", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            items(customCategories) { label ->
                CategoryRow(
                    label = label,
                    isBuiltIn = false,
                    onDelete = {
                        scope.launch { appSettings.removeCustomExpenseCategory(entrepriseId, label) }
                    }
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newLabel,
                        onValueChange = { newLabel = it },
                        label = { Text("Nouvelle catégorie") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            scope.launch {
                                appSettings.addCustomExpenseCategory(entrepriseId, newLabel)
                                newLabel = ""
                            }
                        },
                        enabled = newLabel.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Ajouter")
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    label: String,
    isBuiltIn: Boolean,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isBuiltIn) Color(0xFFF5F5F5) else Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(label, fontWeight = FontWeight.Medium)
                Text(
                    if (isBuiltIn) "Intégrée" else "Personnalisée",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color.Gray)
                }
            }
        }
    }
}
