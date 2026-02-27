package com.abccash.app.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.abccash.app.data.local.entity.CategoryEntity
import com.abccash.app.data.local.entity.RecurrenceRule
import com.abccash.app.data.local.entity.TransactionStatus
import com.abccash.app.data.local.entity.TransactionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header avec icône et titre
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = "Nouvelle Transaction",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Fermer")
                    }
                }

                Divider()

                // Type de transaction (Revenu/Dépense)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Type",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = type == TransactionType.INCOME,
                            onClick = { onTypeChange(TransactionType.INCOME) },
                            label = { Text("💰 Revenu") },
                            leadingIcon = if (type == TransactionType.INCOME) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF10B981),
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = type == TransactionType.EXPENSE,
                            onClick = { onTypeChange(TransactionType.EXPENSE) },
                            label = { Text("💸 Dépense") },
                            leadingIcon = if (type == TransactionType.EXPENSE) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFEF4444),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Montant
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Montant",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = onAmountChange,
                        placeholder = { Text("0.00") },
                        leadingIcon = {
                            Icon(Icons.Filled.AttachMoney, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Catégorie
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Catégorie",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = categoryText,
                        onValueChange = onCategoryChange,
                        placeholder = { Text("Sélectionner une catégorie") },
                        leadingIcon = {
                            Icon(Icons.Filled.Category, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    
                    // Catégories rapides
                    if (filteredCategories.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredCategories.take(8)) { category ->
                                AssistChip(
                                    onClick = { onCategoryChange(category.name) },
                                    label = { Text(category.name, fontSize = 12.sp) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (categoryText == category.name)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                // Statut
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Statut",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = status == TransactionStatus.PLANIFIEE,
                            onClick = { onStatusChange(TransactionStatus.PLANIFIEE) },
                            label = { Text("📅 Planifiée", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = status == TransactionStatus.EN_RETARD,
                            onClick = { onStatusChange(TransactionStatus.EN_RETARD) },
                            label = { Text("⏰ En retard", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = status == TransactionStatus.VALIDEE,
                            onClick = { onStatusChange(TransactionStatus.VALIDEE) },
                            label = { Text("✅ Validée", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Récurrence
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Récurrence",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedCard(
                        onClick = { onRecurrenceChange(nextRecurrenceRule(recurrence)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Repeat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = recurrenceLabel(recurrence),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Divider()

                // Boutons d'action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Annuler")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ajouter")
                    }
                }
            }
        }
    }
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
