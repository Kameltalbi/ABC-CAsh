package com.abccash.app.treasury.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.treasury.data.Expense
import com.abccash.app.treasury.data.ExpenseRecurrence
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import com.abccash.app.treasury.data.forMonth
import com.abccash.app.treasury.data.hasPermission
import com.abccash.app.treasury.data.occurrenceDateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesManagementScreen(
    userRole: UserRole,
    permissions: Set<UserPermission>,
    expenses: List<Expense>,
    selectedMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onNavigateToAddExpense: () -> Unit,
    onUpdateExpense: (String, String, Double, LocalDate, Boolean, ExpenseRecurrence?, LocalDate?, Boolean) -> Unit,
    onStopRecurrence: (String, LocalDate) -> Unit,
    onDeleteExpense: (String) -> Unit,
    onDeleteExpenses: (Collection<String>) -> Unit = {}
) {
    if (!hasPermission(userRole, permissions, UserPermission.MANAGE_EXPENSES)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(32.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔐",
                        fontSize = 64.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Accès refusé",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Vous n'avez pas la permission de gérer les dépenses",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        return
    }
    
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    var selectedExpenseIds by remember { mutableStateOf(setOf<String>()) }
    val isAdmin = userRole == UserRole.ADMIN
    val canManage = hasPermission(userRole, permissions, UserPermission.MANAGE_EXPENSES)
    
    val monthExpenses = remember(expenses, selectedMonth) {
        expenses.forMonth(selectedMonth)
    }

    LaunchedEffect(selectedMonth) {
        selectedExpenseIds = emptySet()
    }

    val monthLabel = remember(selectedMonth) {
        selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH))
            .replaceFirstChar { it.uppercase() }
    }

    Scaffold(
        floatingActionButton = {
            if (canManage) {
                FloatingActionButton(
                    onClick = onNavigateToAddExpense,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter une dépense", tint = Color.White)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            MonthSelectorRow(
                selectedMonth = selectedMonth,
                onMonthChange = onMonthChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Dépenses — $monthLabel",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isAdmin && monthExpenses.isNotEmpty()) {
                AdminBulkSelectionBar(
                    totalCount = monthExpenses.size,
                    selectedCount = selectedExpenseIds.size,
                    onToggleSelectAll = {
                        selectedExpenseIds = if (selectedExpenseIds.size == monthExpenses.size) {
                            emptySet()
                        } else {
                            monthExpenses.map { it.id }.toSet()
                        }
                    },
                    onDeleteSelected = { showBulkDeleteConfirm = true }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (monthExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune dépense pour ce mois",
                        color = Color.Gray,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(monthExpenses, key = { it.id }) { expense ->
                        ExpenseItem(
                            expense = expense,
                            displayMonth = selectedMonth,
                            showSelection = isAdmin,
                            isSelected = expense.id in selectedExpenseIds,
                            onSelectionChange = { selected ->
                                selectedExpenseIds = if (selected) {
                                    selectedExpenseIds + expense.id
                                } else {
                                    selectedExpenseIds - expense.id
                                }
                            },
                            canManage = true,
                            onEdit = { expenseToEdit = expense },
                            onDelete = { expenseToDelete = expense }
                        )
                    }
                }
            }
        }
    }

    expenseToEdit?.let { expense ->
        ExpenseFormDialog(
            initialExpense = expense,
            selectedMonth = selectedMonth,
            onDismiss = { expenseToEdit = null },
            onConfirm = { label, amount, date, recurring, recurrence, endDate, paid ->
                onUpdateExpense(expense.id, label, amount, date, recurring, recurrence, endDate, paid)
                expenseToEdit = null
            },
            onStopRecurrence = { endDate ->
                onStopRecurrence(expense.id, endDate)
                expenseToEdit = null
            }
        )
    }

    expenseToDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Supprimer la dépense ?") },
            text = { Text("Supprimer « ${expense.label} » (${formatMoney(expense.amount)}) ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteExpense(expense.id)
                        expenseToDelete = null
                    }
                ) {
                    Text("Supprimer", color = Color(0xFFF44336))
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    if (showBulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text("Supprimer la sélection ?") },
            text = { Text("Supprimer ${selectedExpenseIds.size} dépense(s) ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteExpenses(selectedExpenseIds)
                        selectedExpenseIds = emptySet()
                        showBulkDeleteConfirm = false
                    }
                ) {
                    Text("Supprimer", color = Color(0xFFF44336))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpenseFormDialog(
    initialExpense: Expense,
    selectedMonth: YearMonth,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, LocalDate, Boolean, ExpenseRecurrence?, LocalDate?, Boolean) -> Unit,
    onStopRecurrence: (LocalDate) -> Unit
) {
    var expenseLabel by remember(initialExpense) { mutableStateOf(initialExpense.label) }
    var expenseAmount by remember(initialExpense) { mutableStateOf(initialExpense.amount.toString()) }
    var expenseDate by remember(initialExpense) { mutableStateOf(initialExpense.date) }
    var isRecurring by remember(initialExpense) { mutableStateOf(initialExpense.isRecurring) }
    var selectedRecurrence by remember(initialExpense) {
        mutableStateOf(initialExpense.recurrence ?: ExpenseRecurrence.MONTHLY)
    }
    var showRecurrenceMenu by remember { mutableStateOf(false) }
    var hasRecurrenceEnd by remember(initialExpense) {
        mutableStateOf(initialExpense.recurrenceEndDate != null)
    }
    var recurrenceEndDate by remember(initialExpense, selectedMonth) {
        mutableStateOf(
            initialExpense.recurrenceEndDate ?: selectedMonth.atEndOfMonth()
        )
    }
    var isPaid by remember(initialExpense) { mutableStateOf(initialExpense.isPaid) }
    val parsedAmount = expenseAmount.replace(",", ".").toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier la dépense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = expenseLabel,
                    onValueChange = { expenseLabel = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Libellé") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = expenseAmount,
                    onValueChange = { expenseAmount = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Montant") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    suffix = { CurrencySuffix() }
                )
                TreasuryDateField(
                    label = "Date",
                    date = expenseDate,
                    onDateChange = { expenseDate = it }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it }
                    )
                    Text("Dépense récurrente", fontSize = 14.sp)
                }
                if (isRecurring) {
                    ExposedDropdownMenuBox(
                        expanded = showRecurrenceMenu,
                        onExpandedChange = { showRecurrenceMenu = it }
                    ) {
                        OutlinedTextField(
                            value = selectedRecurrence.label,
                            onValueChange = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            label = { Text("Fréquence") },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showRecurrenceMenu) }
                        )
                        ExposedDropdownMenu(
                            expanded = showRecurrenceMenu,
                            onDismissRequest = { showRecurrenceMenu = false }
                        ) {
                            ExpenseRecurrence.entries.forEach { recurrence ->
                                DropdownMenuItem(
                                    text = { Text(recurrence.label) },
                                    onClick = {
                                        selectedRecurrence = recurrence
                                        showRecurrenceMenu = false
                                    }
                                )
                            }
                        }
                    }

                    if (initialExpense.recurrenceEndDate == null) {
                        TextButton(
                            onClick = {
                                val endDate = initialExpense.occurrenceDateIn(selectedMonth)
                                    ?: selectedMonth.atEndOfMonth()
                                onStopRecurrence(endDate)
                            }
                        ) {
                            Text("Arrêter après ce mois", color = Color(0xFFF44336))
                        }
                    } else {
                        Text(
                            text = "Récurrence arrêtée le ${initialExpense.recurrenceEndDate!!.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = hasRecurrenceEnd,
                            onCheckedChange = { hasRecurrenceEnd = it }
                        )
                        Text("Date de fin", fontSize = 14.sp)
                    }

                    if (hasRecurrenceEnd) {
                        TreasuryDateField(
                            label = "Fin de récurrence",
                            date = recurrenceEndDate,
                            onDateChange = { recurrenceEndDate = it }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPaid) "Déjà payée" else "À venir",
                        fontSize = 14.sp
                    )
                    Switch(checked = isPaid, onCheckedChange = { isPaid = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = parsedAmount ?: return@Button
                    if (isRecurring && hasRecurrenceEnd && recurrenceEndDate.isBefore(expenseDate)) return@Button
                    onConfirm(
                        expenseLabel,
                        amount,
                        expenseDate,
                        isRecurring,
                        selectedRecurrence,
                        if (isRecurring && hasRecurrenceEnd) recurrenceEndDate else null,
                        isPaid
                    )
                },
                enabled = expenseLabel.isNotBlank() &&
                    (parsedAmount?.let { it > 0 } == true) &&
                    !(isRecurring && hasRecurrenceEnd && recurrenceEndDate.isBefore(expenseDate))
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun ExpenseItem(
    expense: Expense,
    displayMonth: YearMonth? = null,
    showSelection: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChange: (Boolean) -> Unit = {},
    canManage: Boolean = true,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val formatAmount = rememberFormatMoney()
    val displayDate = displayMonth?.let { expense.occurrenceDateIn(it) } ?: expense.date

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showSelection) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectionChange
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A),
                    maxLines = 1
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayDate.format(DateTimeFormatter.ofPattern("dd/MM/yy")),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    if (expense.isRecurring) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF2196F3).copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = expense.recurrence?.label ?: "Récurrent",
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                fontSize = 9.sp,
                                color = Color(0xFF2196F3),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (expense.isRecurring && expense.recurrenceEndDate != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF9E9E9E).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Fin ${expense.recurrenceEndDate.format(DateTimeFormatter.ofPattern("dd/MM/yy"))}",
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                fontSize = 9.sp,
                                color = Color(0xFF757575),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (!expense.isPaid) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFF9800).copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "À venir",
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                fontSize = 9.sp,
                                color = Color(0xFFFF9800),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = formatAmount(expense.amount),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF44336)
                )
                if (canManage) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Modifier",
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFBDBDBD)
                        )
                    }
                }
            }
        }
    }
}
