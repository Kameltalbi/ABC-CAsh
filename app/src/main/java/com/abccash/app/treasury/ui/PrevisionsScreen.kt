package com.abccash.app.treasury.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R
import com.abccash.app.locale.AppLocale
import com.abccash.app.treasury.data.EcheanceForecast
import com.abccash.app.treasury.data.EcheanceItem
import com.abccash.app.treasury.data.EcheanceType
import com.abccash.app.treasury.data.Expense
import com.abccash.app.treasury.data.ExpenseRecurrence
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.data.PaymentMethod
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import com.abccash.app.treasury.data.hasPermission
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrevisionsScreen(
    userRole: UserRole,
    permissions: Set<UserPermission>,
    invoices: List<Invoice>,
    expenses: List<Expense>,
    selectedMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onNavigateToSettings: () -> Unit,
    onUpdateInvoice: (String, String, String, Double, LocalDate, (String?) -> Unit) -> Unit,
    onRecordPayment: (String, Double, LocalDate, PaymentMethod, (String?) -> Unit) -> Unit,
    onDeleteInvoice: (String) -> Unit,
    onUpdateExpense: (
        String, String, Double, LocalDate, Boolean,
        ExpenseRecurrence?, LocalDate?, Boolean
    ) -> Unit,
    onDeleteExpense: (String) -> Unit,
    onValidateForecastExpense: (String, LocalDate, PaymentMethod, (String?) -> Unit) -> Unit,
    onNavigateToAddIncome: () -> Unit,
    onNavigateToAddExpense: () -> Unit,
    onForecastValidated: (YearMonth) -> Unit
) {
    val canViewIncome = hasPermission(userRole, permissions, UserPermission.VIEW_INVOICES) ||
        hasPermission(userRole, permissions, UserPermission.VIEW_TREASURY)
    val canViewExpenses = hasPermission(userRole, permissions, UserPermission.MANAGE_EXPENSES) ||
        hasPermission(userRole, permissions, UserPermission.VIEW_TREASURY)
    val canManageExpense = hasPermission(userRole, permissions, UserPermission.MANAGE_EXPENSES)
    val canMarkPaidIncome = userRole == UserRole.ADMIN ||
        hasPermission(userRole, permissions, UserPermission.ADD_PAYMENTS)
    val isAdmin = userRole == UserRole.ADMIN
    val canAddIncome = isAdmin && canViewIncome
    val canAddExpense = canManageExpense
    var showTypeSheet by remember { mutableStateOf(false) }

    if (!hasPermission(userRole, permissions, UserPermission.VIEW_TREASURY)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.access_denied), color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
        }
        return
    }

    val formatAmount = rememberFormatMoney()
    val dateFormatter = remember { AppLocale.shortDayMonthYearFormatter() }
    val today = remember { LocalDate.now() }

    val monthItems = remember(invoices, expenses, selectedMonth, canViewIncome, canViewExpenses) {
        val filteredInvoices = if (canViewIncome) invoices else emptyList()
        val filteredExpenses = if (canViewExpenses) expenses else emptyList()
        EcheanceForecast.buildItemsForMonth(
            month = selectedMonth,
            invoices = filteredInvoices,
            expenses = filteredExpenses
        )
    }

    val forecastIncome = remember(monthItems) {
        monthItems.filter { it.type == EcheanceType.INCOME }.sumOf { it.amount }
    }
    val forecastExpenses = remember(monthItems) {
        monthItems.filter { it.type == EcheanceType.EXPENSE }.sumOf { it.amount }
    }

    var invoiceToEdit by remember { mutableStateOf<Invoice?>(null) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var itemToDelete by remember { mutableStateOf<EcheanceItem?>(null) }
    var invoiceToMarkPaid by remember { mutableStateOf<Invoice?>(null) }
    var expenseToMarkPaid by remember { mutableStateOf<Pair<Expense, LocalDate>?>(null) }
    var editError by remember { mutableStateOf<String?>(null) }

    invoiceToEdit?.let { invoice ->
        InvoiceFormDialog(
            title = "Modifier l'encaissement",
            initialInvoice = invoice,
            selectedMonth = YearMonth.from(invoice.dueDate),
            onDismiss = {
                invoiceToEdit = null
                editError = null
            },
            onConfirm = { invoiceNumber, clientName, totalAmount, dueDate, _, _ ->
                onUpdateInvoice(invoice.id, invoiceNumber, clientName, totalAmount, dueDate) { error ->
                    if (error == null) {
                        invoiceToEdit = null
                        editError = null
                    } else {
                        editError = error
                    }
                }
            },
            errorMessage = editError
        )
    }

    expenseToEdit?.let { expense ->
        ExpenseFormDialog(
            initialExpense = expense,
            selectedMonth = YearMonth.from(expense.date),
            onDismiss = {
                expenseToEdit = null
                editError = null
            },
            onConfirm = { label, amount, date, recurring, recurrence, endDate, paid ->
                onUpdateExpense(
                    expense.id, label, amount, date, recurring, recurrence, endDate, paid
                )
                expenseToEdit = null
            },
            onStopRecurrence = { _ -> expenseToEdit = null }
        )
    }

    invoiceToMarkPaid?.let { invoice ->
        FullPaymentConfirmDialog(
            invoice = invoice,
            onDismiss = { invoiceToMarkPaid = null },
            onConfirm = { date, method ->
                onRecordPayment(invoice.id, invoice.remainingAmount, date, method) { error ->
                    if (error == null) {
                        invoiceToMarkPaid = null
                        onForecastValidated(YearMonth.from(date))
                    }
                }
            }
        )
    }

    expenseToMarkPaid?.let { (expense, dueDate) ->
        ExpensePaymentConfirmDialog(
            expense = expense,
            dueDate = dueDate,
            onDismiss = { expenseToMarkPaid = null },
            onConfirm = { paymentDate, method ->
                onValidateForecastExpense(expense.id, paymentDate, method) { error ->
                    if (error == null) {
                        expenseToMarkPaid = null
                        onForecastValidated(YearMonth.from(paymentDate))
                    }
                }
            }
        )
    }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(stringResource(R.string.delete_forecast)) },
            text = { Text(stringResource(R.string.delete_forecast_confirm, item.label)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (item.type) {
                            EcheanceType.INCOME -> item.invoiceId?.let(onDeleteInvoice)
                            EcheanceType.EXPENSE -> item.expenseId?.let(onDeleteExpense)
                        }
                        itemToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete), color = Color(0xFFF44336))
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showTypeSheet) {
        TransactionTypeChoiceSheet(
            canAddIncome = canAddIncome,
            canAddExpense = canAddExpense,
            forecastMode = true,
            onDismiss = { showTypeSheet = false },
            onSelectIncome = {
                showTypeSheet = false
                onNavigateToAddIncome()
            },
            onSelectExpense = {
                showTypeSheet = false
                onNavigateToAddExpense()
            }
        )
    }

    Scaffold(
        containerColor = Color(0xFFFAF9F6),
        floatingActionButton = {
            if (canAddIncome || canAddExpense) {
                FloatingActionButton(
                    onClick = { showTypeSheet = true },
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFFAF9F6)),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.forecasts),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = Color(0xFF64748B)
                        )
                    }
                }
            }

            item {
                MonthSelectorRow(
                    selectedMonth = selectedMonth,
                    onMonthChange = onMonthChange
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ForecastSummaryCard(
                        label = stringResource(R.string.expected_income),
                        amount = formatAmount(forecastIncome),
                        color = Color(0xFF16A34A),
                        modifier = Modifier.weight(1f)
                    )
                    ForecastSummaryCard(
                        label = stringResource(R.string.expected_expenses),
                        amount = formatAmount(forecastExpenses),
                        color = Color(0xFFDC2626),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (monthItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.no_forecasts),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                AppLocale.monthYear(selectedMonth),
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            } else {
                items(monthItems, key = { it.id }) { item ->
                    EcheanceRow(
                        item = item,
                        formattedAmount = formatAmount(item.amount),
                        formattedDate = item.dueDate.format(dateFormatter),
                        isOverdue = item.dueDate.isBefore(today),
                        canMarkPaid = when (item.type) {
                            EcheanceType.INCOME -> canMarkPaidIncome
                            EcheanceType.EXPENSE -> canManageExpense
                        },
                        canEdit = when (item.type) {
                            EcheanceType.INCOME -> userRole == UserRole.ADMIN
                            EcheanceType.EXPENSE -> canManageExpense
                        },
                        onMarkPaid = {
                            when (item.type) {
                                EcheanceType.INCOME -> item.invoiceId?.let { id ->
                                    invoices.find { it.id == id }?.let { invoiceToMarkPaid = it }
                                }
                                EcheanceType.EXPENSE -> item.expenseId?.let { id ->
                                    expenses.find { it.id == id }?.let { expense ->
                                        expenseToMarkPaid = expense to item.dueDate
                                    }
                                }
                            }
                        },
                        onEdit = {
                            when (item.type) {
                                EcheanceType.INCOME -> {
                                    invoiceToEdit = item.invoiceId?.let { id ->
                                        invoices.find { it.id == id }
                                    }
                                }
                                EcheanceType.EXPENSE -> {
                                    expenseToEdit = item.expenseId?.let { id ->
                                        expenses.find { it.id == id }
                                    }
                                }
                            }
                        },
                        onDelete = { itemToDelete = item }
                    )
                }
            }
        }
    }
}

@Composable
private fun ForecastSummaryCard(
    label: String,
    amount: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, fontSize = 12.sp, color = Color(0xFF64748B))
            Text(amount, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun EcheanceRow(
    item: EcheanceItem,
    formattedAmount: String,
    formattedDate: String,
    isOverdue: Boolean,
    canMarkPaid: Boolean,
    canEdit: Boolean,
    onMarkPaid: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isIncome = item.type == EcheanceType.INCOME
    val accent = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
    val iconBg = accent.copy(alpha = 0.12f)

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
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A),
                    maxLines = 1
                )
                Text(
                    text = if (isOverdue) "$formattedDate · En retard" else formattedDate,
                    fontSize = 12.sp,
                    color = if (isOverdue) Color(0xFFF44336) else Color(0xFF9E9E9E)
                )
            }
            Text(
                text = formattedAmount,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color(0xFF9E9E9E))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    if (canMarkPaid) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.mark_paid)) },
                            onClick = {
                                menuExpanded = false
                                onMarkPaid()
                            }
                        )
                    }
                    if (canEdit) {
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
