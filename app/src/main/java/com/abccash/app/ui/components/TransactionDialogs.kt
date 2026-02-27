package com.abccash.app.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.abccash.app.data.local.entity.CategoryEntity
import com.abccash.app.data.local.entity.RecurrenceRule
import com.abccash.app.data.local.entity.TransactionStatus
import com.abccash.app.data.local.entity.TransactionType
import java.time.LocalDate

@Composable
fun AddTransactionDialog(
    selectedDate: LocalDate,
    amountText: String,
    categoryText: String,
    type: TransactionType,
    status: TransactionStatus,
    recurrence: RecurrenceRule,
    filteredCategories: List<CategoryEntity>,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onStatusChange: (TransactionStatus) -> Unit,
    onRecurrenceChange: (RecurrenceRule) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter transaction ($selectedDate)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = onAmountChange,
                    label = { Text("Montant") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = categoryText,
                    onValueChange = onCategoryChange,
                    label = { Text("Categorie") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(
                        onClick = { onTypeChange(TransactionType.INCOME) },
                        label = { Text("Recette") },
                        modifier = Modifier.alpha(if (type == TransactionType.INCOME) 1f else 0.65f)
                    )
                    AssistChip(
                        onClick = { onTypeChange(TransactionType.EXPENSE) },
                        label = { Text("Depense") },
                        modifier = Modifier.alpha(if (type == TransactionType.EXPENSE) 1f else 0.65f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TransactionStatus.entries.forEach { statusOption ->
                        AssistChip(
                            onClick = { onStatusChange(statusOption) },
                            label = { Text(statusOption.name) },
                            modifier = Modifier.alpha(if (status == statusOption) 1f else 0.65f)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    filteredCategories.take(6).forEach { category ->
                        AssistChip(
                            onClick = { onCategoryChange(category.name) },
                            label = { Text(category.name) }
                        )
                    }
                }
                AssistChip(
                    onClick = { onRecurrenceChange(nextRecurrenceRule(recurrence)) },
                    label = { Text("Recurrence: ${recurrenceLabel(recurrence)}") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Ajouter") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun EditTransactionDialog(
    amountText: String,
    dateText: String,
    isRecurring: Boolean,
    applySeries: Boolean,
    onAmountChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onApplySeriesChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = onAmountChange,
                    label = { Text("Montant") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = onDateChange,
                    label = { Text("Date (YYYY-MM-DD)") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Button(onClick = {
                            val current = runCatching { LocalDate.parse(dateText) }.getOrElse { LocalDate.now() }
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    onDateChange(LocalDate.of(year, month + 1, day).toString())
                                },
                                current.year,
                                current.monthValue - 1,
                                current.dayOfMonth
                            ).show()
                        }) {
                            Text("Choisir")
                        }
                    }
                )
                if (isRecurring) {
                    Text("Appliquer les modifications a:")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = { onApplySeriesChange(false) },
                            label = { Text("Cette transaction") },
                            modifier = Modifier.alpha(if (!applySeries) 1f else 0.65f)
                        )
                        AssistChip(
                            onClick = { onApplySeriesChange(true) },
                            label = { Text("Toute la serie") },
                            modifier = Modifier.alpha(if (applySeries) 1f else 0.65f)
                        )
                    }
                } else {
                    Text("Modification sur cette transaction uniquement")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun DeleteTransactionDialog(
    isRecurring: Boolean,
    applySeries: Boolean,
    onApplySeriesChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Supprimer transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isRecurring) {
                    Text("Choisissez la portee:")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = { onApplySeriesChange(false) },
                            label = { Text("Cette transaction") },
                            modifier = Modifier.alpha(if (!applySeries) 1f else 0.65f)
                        )
                        AssistChip(
                            onClick = { onApplySeriesChange(true) },
                            label = { Text("Toute la serie") },
                            modifier = Modifier.alpha(if (applySeries) 1f else 0.65f)
                        )
                    }
                } else {
                    Text("Confirmer la suppression de cette transaction ?")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Supprimer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

private fun nextRecurrenceRule(current: RecurrenceRule): RecurrenceRule {
    return when (current) {
        RecurrenceRule.NONE -> RecurrenceRule.WEEKLY
        RecurrenceRule.WEEKLY -> RecurrenceRule.MONTHLY
        RecurrenceRule.MONTHLY -> RecurrenceRule.QUARTERLY
        RecurrenceRule.QUARTERLY -> RecurrenceRule.FOUR_MONTHLY
        RecurrenceRule.FOUR_MONTHLY -> RecurrenceRule.NONE
    }
}

private fun recurrenceLabel(rule: RecurrenceRule): String {
    return when (rule) {
        RecurrenceRule.NONE -> "Aucune"
        RecurrenceRule.WEEKLY -> "Hebdomadaire"
        RecurrenceRule.MONTHLY -> "Mensuelle"
        RecurrenceRule.QUARTERLY -> "Trimestrielle"
        RecurrenceRule.FOUR_MONTHLY -> "Quadrimestrielle"
    }
}
