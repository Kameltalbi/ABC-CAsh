package com.abccash.app.treasury.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abccash.app.R
import com.abccash.app.treasury.datastore.AppSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsIncomeCategoriesScreen(
    entrepriseId: String,
    appSettings: AppSettings,
    onBack: () -> Unit
) {
    CustomCategoriesScreen(
        title = stringResource(R.string.settings_income_categories),
        entrepriseId = entrepriseId,
        onBack = onBack,
        observeCustom = { id -> appSettings.customIncomeCategories(id) },
        onAdd = { id, label -> appSettings.addCustomIncomeCategory(id, label) },
        onRename = { id, old, new -> appSettings.renameCustomIncomeCategory(id, old, new) },
        onDelete = { id, label -> appSettings.removeCustomIncomeCategory(id, label) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsExpenseCategoriesScreen(
    entrepriseId: String,
    appSettings: AppSettings,
    onBack: () -> Unit
) {
    CustomCategoriesScreen(
        title = stringResource(R.string.settings_expense_categories),
        entrepriseId = entrepriseId,
        onBack = onBack,
        observeCustom = { id -> appSettings.customExpenseCategories(id) },
        onAdd = { id, label -> appSettings.addCustomExpenseCategory(id, label) },
        onRename = { id, old, new -> appSettings.renameCustomExpenseCategory(id, old, new) },
        onDelete = { id, label -> appSettings.removeCustomExpenseCategory(id, label) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomCategoriesScreen(
    title: String,
    entrepriseId: String,
    onBack: () -> Unit,
    observeCustom: (String) -> kotlinx.coroutines.flow.Flow<List<String>>,
    onAdd: suspend (String, String) -> Unit,
    onRename: suspend (String, String, String) -> String?,
    onDelete: suspend (String, String) -> Unit
) {
    val customCategories by observeCustom(entrepriseId).collectAsStateWithLifecycle(initialValue = emptyList())
    var newLabel by remember { mutableStateOf("") }
    var categoryToEdit by remember { mutableStateOf<String?>(null) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val labelRequiredError = stringResource(R.string.label_required)
    val categoryExistsError = stringResource(R.string.category_exists)

    fun resolveError(code: String?): String? = when (code) {
        "label_required" -> labelRequiredError
        "category_exists" -> categoryExistsError
        else -> code
    }

    categoryToEdit?.let { original ->
        EditCategoryDialog(
            initialLabel = original,
            error = formError,
            onDismiss = {
                categoryToEdit = null
                formError = null
            },
            onConfirm = { updated ->
                if (entrepriseId.isBlank()) return@EditCategoryDialog
                scope.launch {
                    val error = onRename(entrepriseId, original, updated)
                    if (error == null) {
                        categoryToEdit = null
                        formError = null
                    } else {
                        formError = resolveError(error)
                    }
                }
            }
        )
    }

    categoryToDelete?.let { label ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.delete_category_confirm, label)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (entrepriseId.isNotBlank()) {
                            scope.launch {
                                onDelete(entrepriseId, label)
                                categoryToDelete = null
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    SettingsDetailScaffold(title = title, onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (entrepriseId.isBlank()) {
                item {
                    Text(
                        text = stringResource(R.string.session_expired),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.custom_categories),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            if (customCategories.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_custom_categories),
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            items(customCategories, key = { it }) { label ->
                CategoryRow(
                    label = label,
                    onEdit = if (entrepriseId.isNotBlank()) {
                        { categoryToEdit = label }
                    } else {
                        null
                    },
                    onDelete = if (entrepriseId.isNotBlank()) {
                        { categoryToDelete = label }
                    } else {
                        null
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
                        label = { Text(stringResource(R.string.new_category)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = entrepriseId.isNotBlank()
                    )
                    IconButton(
                        onClick = {
                            val label = newLabel.trim()
                            if (label.isBlank() || entrepriseId.isBlank()) return@IconButton
                            scope.launch {
                                onAdd(entrepriseId, label)
                                newLabel = ""
                            }
                        },
                        enabled = entrepriseId.isNotBlank() && newLabel.trim().isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
                    }
                }
            }
        }
    }
}

@Composable
private fun EditCategoryDialog(
    initialLabel: String,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var label by remember(initialLabel) { mutableStateOf(initialLabel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_category)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.new_category)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(label.trim()) },
                enabled = label.trim().isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun CategoryRow(
    label: String,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            if (onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = Color(0xFFF44336)
                    )
                }
            }
        }
    }
}
