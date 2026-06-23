package com.abccash.app.treasury.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.abccash.app.R
import com.abccash.app.locale.AppLocale
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
import com.abccash.app.treasury.data.PaymentMethod
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import com.abccash.app.treasury.data.forMonth
import com.abccash.app.treasury.data.hasPermission
import com.abccash.app.treasury.data.occurrenceDateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesManagementScreen(
    userRole: UserRole,
    permissions: Set<UserPermission>,
    expenses: List<Expense>,
    selectedMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onNavigateToAddExpense: () -> Unit,
    onUpdateExpense: (String, String, Double, LocalDate, Boolean, ExpenseRecurrence?, LocalDate?, Boolean, PaymentMethod?) -> Unit,
    onStopRecurrence: (String, LocalDate) -> Unit,
    onDeleteExpense: (String) -> Unit,
    onDeleteExpenses: (Collection<String>) -> Unit = {}
) {
    if (!hasPermission(userRole, permissions, UserPermission.MANAGE_EXPENSES)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
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
                        text = stringResource(R.string.access_denied),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.no_expense_permission),
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

    val monthLabel = remember(selectedMonth) { AppLocale.monthYear(selectedMonth) }

    Scaffold(
        containerColor = Color.White,
        floatingActionButton = {
            if (canManage) {
                AbcCashFab(
                    onClick = onNavigateToAddExpense,
                    contentDescription = stringResource(R.string.add_expense)
                )
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
                text = stringResource(R.string.expenses_month_title, monthLabel),
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
                        text = stringResource(R.string.no_expenses_month),
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
                            isAdmin = userRole == UserRole.ADMIN,
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
            onConfirm = { label, amount, date, recurring, recurrence, endDate, paid, paymentMethod ->
                onUpdateExpense(expense.id, label, amount, date, recurring, recurrence, endDate, paid, paymentMethod)
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
            title = { Text(stringResource(R.string.delete_expense_question)) },
            text = {
                Text(
                    stringResource(
                        R.string.delete_expense_item_confirm,
                        expense.label,
                        formatMoney(expense.amount)
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteExpense(expense.id)
                        expenseToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete), color = Color(0xFFF44336))
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showBulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_selection_question)) },
            text = { Text(stringResource(R.string.delete_count, selectedExpenseIds.size)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteExpenses(selectedExpenseIds)
                        selectedExpenseIds = emptySet()
                        showBulkDeleteConfirm = false
                    }
                ) {
                    Text(stringResource(R.string.delete), color = Color(0xFFF44336))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
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
    onConfirm: (String, Double, LocalDate, Boolean, ExpenseRecurrence?, LocalDate?, Boolean, PaymentMethod?) -> Unit,
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
    var selectedPaymentMethod by remember(initialExpense) {
        mutableStateOf(initialExpense.paymentMethod ?: PaymentMethod.CREDIT_CARD)
    }
    val parsedAmount = expenseAmount.replace(",", ".").toDoubleOrNull()
    val dateLabel = stringResource(R.string.date)
    val recurrenceEndLabel = stringResource(R.string.recurrence_end)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_expense)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = expenseLabel,
                    onValueChange = { expenseLabel = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.label)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = expenseAmount,
                    onValueChange = { expenseAmount = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    suffix = { CurrencySuffix() }
                )
                TreasuryDateField(
                    label = dateLabel,
                    date = expenseDate,
                    onDateChange = { expenseDate = it }
                )
                TreasuryPaymentMethodField(
                    selectedMethod = selectedPaymentMethod,
                    onMethodChange = { selectedPaymentMethod = it }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it }
                    )
                    Text(stringResource(R.string.recurring_expense), fontSize = 14.sp)
                }
                if (isRecurring) {
                    ExposedDropdownMenuBox(
                        expanded = showRecurrenceMenu,
                        onExpandedChange = { showRecurrenceMenu = it }
                    ) {
                        OutlinedTextField(
                            value = selectedRecurrence.localizedLabel(),
                            onValueChange = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            label = { Text(stringResource(R.string.frequency)) },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showRecurrenceMenu) }
                        )
                        ExposedDropdownMenu(
                            expanded = showRecurrenceMenu,
                            onDismissRequest = { showRecurrenceMenu = false }
                        ) {
                            ExpenseRecurrence.entries.forEach { recurrence ->
                                DropdownMenuItem(
                                    text = { Text(recurrence.localizedLabel()) },
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
                            Text(stringResource(R.string.stop_after_month), color = Color(0xFFF44336))
                        }
                    } else {
                        Text(
                            text = stringResource(
                                R.string.stop_recurrence_on,
                                initialExpense.recurrenceEndDate!!.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            ),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = hasRecurrenceEnd,
                            onCheckedChange = { hasRecurrenceEnd = it }
                        )
                        Text(stringResource(R.string.end_date), fontSize = 14.sp)
                    }

                    if (hasRecurrenceEnd) {
                        TreasuryDateField(
                            label = recurrenceEndLabel,
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
                        text = if (isPaid) {
                            stringResource(R.string.already_paid_expense)
                        } else {
                            stringResource(R.string.upcoming_badge)
                        },
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
                        isPaid,
                        selectedPaymentMethod
                    )
                },
                enabled = expenseLabel.isNotBlank() &&
                    (parsedAmount?.let { it > 0 } == true) &&
                    !(isRecurring && hasRecurrenceEnd && recurrenceEndDate.isBefore(expenseDate))
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
fun ExpenseItem(
    expense: Expense,
    displayMonth: YearMonth? = null,
    showSelection: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChange: (Boolean) -> Unit = {},
    isAdmin: Boolean = false,
    canManage: Boolean = true,
    onEdit: () -> Unit = {},
    onValidate: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val formatAmount = rememberFormatMoney()
    val recurringBadge = stringResource(R.string.recurring_badge)
    val upcomingBadge = stringResource(R.string.upcoming_badge)
    val displayDate = displayMonth?.let { expense.occurrenceDateIn(it) } ?: expense.date
    var menuExpanded by remember { mutableStateOf(false) }
    val accent = Color(0xFFEF4444)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showSelection) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectionChange
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.label,
                    fontSize = 15.sp,
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
                        fontSize = 12.sp,
                        color = Color(0xFF9E9E9E)
                    )
                    if (expense.isRecurring) {
                        ExpenseMetaBadge(
                            text = expense.recurrence?.localizedLabel() ?: recurringBadge,
                            color = Color(0xFF2196F3)
                        )
                    }
                    if (expense.isRecurring && expense.recurrenceEndDate != null) {
                        ExpenseMetaBadge(
                            text = stringResource(
                                R.string.recurrence_end_short,
                                expense.recurrenceEndDate.format(DateTimeFormatter.ofPattern("dd/MM/yy"))
                            ),
                            color = Color(0xFF757575)
                        )
                    }
                    if (!expense.isPaid) {
                        ExpenseMetaBadge(text = upcomingBadge, color = Color(0xFFFF9800))
                    }
                }
            }
            Text(
                text = formatAmount(expense.amount),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            if (isAdmin) {
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = Color(0xFF9E9E9E)
                        )
                    }
                    AbcDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        if (!expense.isPaid) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_validate)) },
                                onClick = {
                                    menuExpanded = false
                                    onValidate()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete), color = Color(0xFFF44336)) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            } else if (canManage) {
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = Color(0xFF9E9E9E)
                        )
                    }
                    AbcDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete), color = Color(0xFFF44336)) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseMetaBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp,
            color = color,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
