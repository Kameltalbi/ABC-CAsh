package com.abccash.app.treasury.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R
import com.abccash.app.locale.AppLocale
import com.abccash.app.treasury.data.Expense
import com.abccash.app.treasury.data.ExpenseRecurrence
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.data.InvoiceStatus
import com.abccash.app.treasury.data.PaymentMethod
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import com.abccash.app.treasury.data.appearsInTransactions
import com.abccash.app.treasury.data.displayTransactionDate
import com.abccash.app.treasury.data.hasPermission
import com.abccash.app.treasury.data.occurrenceDateIn
import com.abccash.app.treasury.data.transactionDateIn
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private object TransactionsTheme {
    val Background = Color(0xFFF5F7FA)
    val Primary = Color(0xFF1A1A1A)
    val Income = Color(0xFF22C55E)
    val Expense = Color(0xFFEF4444)
    val Muted = Color(0xFF94A3B8)
    val Divider = Color(0xFFEEEEEE)
    val ChipInactive = Color(0xFF64748B)
}

private data class TransactionRow(
    val date: LocalDate,
    val invoice: Invoice? = null,
    val expense: Expense? = null
) {
    val isIncome: Boolean get() = invoice != null

    fun amountValue(): Double = invoice?.totalAmount ?: expense?.amount ?: 0.0
}

private enum class TransactionTypeFilter(@StringRes val labelRes: Int) {
    ALL(R.string.filter_all),
    INCOME(R.string.income_title),
    EXPENSE(R.string.expense_title)
}

private enum class TransactionStatusFilter(@StringRes val labelRes: Int) {
    ALL(R.string.filter_all),
    DUE(R.string.filter_due),
    PARTIAL(R.string.filter_partial),
    PAID(R.string.filter_paid)
}

private enum class TransactionSort(@StringRes val labelRes: Int) {
    DATE(R.string.transactions_sort_date),
    AMOUNT_DESC(R.string.transactions_sort_amount_desc),
    AMOUNT_ASC(R.string.transactions_sort_amount_asc)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    userRole: UserRole,
    permissions: Set<UserPermission>,
    invoices: List<Invoice>,
    expenses: List<Expense>,
    selectedMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    importFeedback: String? = null,
    onClearImportFeedback: () -> Unit = {},
    onNavigateToImport: () -> Unit,
    onNavigateToAddIncome: () -> Unit,
    onNavigateToAddExpense: () -> Unit,
    onUpdateInvoice: (String, String, String, Double, LocalDate, (String?) -> Unit) -> Unit,
    onRecordPayment: (String, Double, LocalDate, PaymentMethod, (String?) -> Unit) -> Unit,
    onDeleteInvoice: (String) -> Unit,
    onUpdateExpense: (String, String, Double, LocalDate, Boolean, ExpenseRecurrence?, LocalDate?, Boolean) -> Unit,
    onStopRecurrence: (String, LocalDate) -> Unit,
    onDeleteExpense: (String) -> Unit,
    onValidateExpense: (String, LocalDate, PaymentMethod, (String?) -> Unit) -> Unit = { _, _, _, onResult -> onResult(null) }
) {
    val canViewIncome = hasPermission(userRole, permissions, UserPermission.VIEW_INVOICES)
    val canManageExpense = hasPermission(userRole, permissions, UserPermission.MANAGE_EXPENSES)
    val isAdmin = userRole == UserRole.ADMIN
    val canAddPayment = hasPermission(userRole, permissions, UserPermission.ADD_PAYMENTS)
    val canAdd = isAdmin || canManageExpense

    if (!canViewIncome && !canManageExpense) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.access_denied), color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
        }
        return
    }

    var showTypeSheet by remember { mutableStateOf(false) }
    var typeFilter by remember { mutableStateOf(TransactionTypeFilter.ALL) }
    var statusFilter by remember { mutableStateOf(TransactionStatusFilter.ALL) }
    var selectedClient by remember { mutableStateOf<String?>(null) }
    var sortMode by remember { mutableStateOf(TransactionSort.DATE) }
    var showClientMenu by remember { mutableStateOf(false) }

    var invoiceToEdit by remember { mutableStateOf<Invoice?>(null) }
    var invoiceToDelete by remember { mutableStateOf<Invoice?>(null) }
    var invoiceForPartialPayment by remember { mutableStateOf<Invoice?>(null) }
    var invoiceToMarkPaid by remember { mutableStateOf<Invoice?>(null) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var expenseToValidate by remember { mutableStateOf<Expense?>(null) }
    var paymentError by remember { mutableStateOf<String?>(null) }
    var editError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(importFeedback) {
        if (importFeedback != null) {
            delay(4000)
            onClearImportFeedback()
        }
    }

    LaunchedEffect(selectedMonth) {
        selectedClient = null
        statusFilter = TransactionStatusFilter.ALL
    }

    val monthLabel = remember(selectedMonth) { AppLocale.monthYear(selectedMonth) }
    val datePattern = remember { DateTimeFormatter.ofPattern("dd/MM/yy") }

    val allRows = remember(invoices, expenses, selectedMonth, canViewIncome, canManageExpense) {
        val incomeRows = if (canViewIncome) {
            invoices
                .filter { it.transactionDateIn(selectedMonth) }
                .map { invoice ->
                    TransactionRow(date = invoice.displayTransactionDate(), invoice = invoice)
                }
        } else emptyList()
        val expenseRows = if (canManageExpense) {
            expenses
                .filter { it.appearsInTransactions(selectedMonth) }
                .map { expense ->
                    TransactionRow(
                        date = expense.occurrenceDateIn(selectedMonth) ?: expense.date,
                        expense = expense
                    )
                }
        } else emptyList()
        (incomeRows + expenseRows).sortedByDescending { it.date }
    }

    val availableClients = remember(allRows) {
        allRows.mapNotNull { it.invoice?.clientName }
            .distinct()
            .sortedBy { it.lowercase() }
    }

    val typeFilters = remember(canViewIncome, canManageExpense) {
        buildList {
            add(TransactionTypeFilter.ALL)
            if (canViewIncome) add(TransactionTypeFilter.INCOME)
            if (canManageExpense) add(TransactionTypeFilter.EXPENSE)
        }
    }

    val filteredRows = remember(allRows, typeFilter, statusFilter, selectedClient, sortMode) {
        var result = allRows

        result = when (typeFilter) {
            TransactionTypeFilter.ALL -> result
            TransactionTypeFilter.INCOME -> result.filter { it.isIncome }
            TransactionTypeFilter.EXPENSE -> result.filter { !it.isIncome }
        }

        selectedClient?.let { client ->
            result = result.filter { it.invoice?.clientName == client }
        }

        if (statusFilter != TransactionStatusFilter.ALL) {
            result = result.filter { row ->
                when {
                    row.invoice != null -> when (statusFilter) {
                        TransactionStatusFilter.DUE -> row.invoice.status == InvoiceStatus.DUE
                        TransactionStatusFilter.PARTIAL -> row.invoice.status == InvoiceStatus.PARTIAL
                        TransactionStatusFilter.PAID -> row.invoice.status == InvoiceStatus.PAID
                        TransactionStatusFilter.ALL -> true
                    }
                    row.expense != null -> when (statusFilter) {
                        TransactionStatusFilter.PAID -> row.expense.isPaid
                        TransactionStatusFilter.DUE -> !row.expense.isPaid
                        TransactionStatusFilter.PARTIAL -> false
                        TransactionStatusFilter.ALL -> true
                    }
                    else -> true
                }
            }
        }

        when (sortMode) {
            TransactionSort.DATE -> result.sortedByDescending { it.date }
            TransactionSort.AMOUNT_DESC -> result.sortedByDescending { it.amountValue() }
            TransactionSort.AMOUNT_ASC -> result.sortedBy { it.amountValue() }
        }
    }

    Scaffold(
        containerColor = TransactionsTheme.Background,
        floatingActionButton = {
            if (canAdd) {
                FloatingActionButton(
                    onClick = { showTypeSheet = true },
                    containerColor = TransactionsTheme.Primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_transaction))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.transactions),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = TransactionsTheme.Primary
                    )
                    Text(monthLabel, fontSize = 12.sp, color = TransactionsTheme.Muted)
                }
                if (isAdmin) {
                    IconButton(onClick = onNavigateToImport) {
                        Icon(
                            Icons.Default.FileUpload,
                            contentDescription = stringResource(R.string.import_action),
                            tint = TransactionsTheme.ChipInactive
                        )
                    }
                }
            }

            MonthSelectorRow(selectedMonth = selectedMonth, onMonthChange = onMonthChange)

            importFeedback?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    color = TransactionsTheme.Income
                )
            }

            TransactionTypeTabs(
                filters = typeFilters,
                selected = typeFilter,
                onSelect = { typeFilter = it }
            )

            TransactionSecondaryFilters(
                statusFilter = statusFilter,
                onStatusFilterChange = { statusFilter = it },
                selectedClient = selectedClient,
                showClientMenu = showClientMenu,
                onShowClientMenu = { showClientMenu = it },
                availableClients = availableClients,
                onClientSelected = {
                    selectedClient = it
                    showClientMenu = false
                },
                sortMode = sortMode,
                onSortClick = {
                    sortMode = when (sortMode) {
                        TransactionSort.DATE -> TransactionSort.AMOUNT_DESC
                        TransactionSort.AMOUNT_DESC -> TransactionSort.AMOUNT_ASC
                        TransactionSort.AMOUNT_ASC -> TransactionSort.DATE
                    }
                }
            )

            if (filteredRows.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.no_transactions_month),
                        color = TransactionsTheme.Muted,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(filteredRows, key = {
                        when {
                            it.invoice != null -> "inv-${it.invoice.id}"
                            it.expense != null -> "exp-${it.expense.id}"
                            else -> it.date.toString()
                        }
                    }) { row ->
                        when {
                            row.invoice != null -> TransactionIncomeLine(
                                invoice = row.invoice,
                                displayDate = row.date,
                                datePattern = datePattern,
                                isAdmin = isAdmin,
                                canAddPayment = canAddPayment,
                                onMarkPaid = { invoiceToMarkPaid = row.invoice },
                                onPartialPayment = {
                                    paymentError = null
                                    invoiceForPartialPayment = row.invoice
                                },
                                onEdit = { invoiceToEdit = row.invoice },
                                onDelete = { invoiceToDelete = row.invoice }
                            )
                            row.expense != null -> TransactionExpenseLine(
                                expense = row.expense,
                                displayDate = row.date,
                                datePattern = datePattern,
                                isAdmin = isAdmin,
                                canManage = canManageExpense,
                                onEdit = { expenseToEdit = row.expense },
                                onValidate = { expenseToValidate = row.expense },
                                onDelete = { expenseToDelete = row.expense }
                            )
                        }
                        HorizontalDivider(color = TransactionsTheme.Divider, thickness = 1.dp)
                    }
                }
            }
        }
    }

    if (showTypeSheet) {
        TransactionTypeChoiceSheet(
            canAddIncome = isAdmin,
            canAddExpense = canManageExpense,
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

    invoiceToMarkPaid?.let { invoice ->
        FullPaymentConfirmDialog(
            invoice = invoice,
            onDismiss = { invoiceToMarkPaid = null },
            onConfirm = { date, method ->
                onRecordPayment(
                    invoice.id,
                    invoice.remainingAmount,
                    date,
                    method
                ) { error ->
                    if (error == null) invoiceToMarkPaid = null else paymentError = error
                }
            }
        )
    }

    invoiceForPartialPayment?.let { invoice ->
        PartialPaymentDialog(
            invoice = invoice,
            errorMessage = paymentError,
            onDismiss = {
                invoiceForPartialPayment = null
                paymentError = null
            },
            onConfirm = { amount, date, method ->
                onRecordPayment(invoice.id, amount, date, method) { error ->
                    if (error == null) {
                        invoiceForPartialPayment = null
                        paymentError = null
                    } else {
                        paymentError = error
                    }
                }
            }
        )
    }

    invoiceToEdit?.let { invoice ->
        InvoiceFormDialog(
            title = stringResource(R.string.edit),
            initialInvoice = invoice,
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

    invoiceToDelete?.let { invoice ->
        AlertDialog(
            onDismissRequest = { invoiceToDelete = null },
            title = { Text(stringResource(R.string.delete_invoice)) },
            text = { Text(stringResource(R.string.delete_invoice_confirm, invoice.clientName)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteInvoice(invoice.id)
                    invoiceToDelete = null
                }) { Text(stringResource(R.string.delete), color = Color(0xFFF44336)) }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    expenseToEdit?.let { expense ->
        ExpenseFormDialog(
            initialExpense = expense,
            selectedMonth = selectedMonth,
            onDismiss = { expenseToEdit = null },
            onConfirm = { label, amount, date, isRecurring, recurrence, recurrenceEndDate, isPaid ->
                onUpdateExpense(expense.id, label, amount, date, isRecurring, recurrence, recurrenceEndDate, isPaid)
                expenseToEdit = null
            },
            onStopRecurrence = { endDate ->
                onStopRecurrence(expense.id, endDate)
                expenseToEdit = null
            }
        )
    }

    expenseToValidate?.let { expense ->
        ExpensePaymentConfirmDialog(
            expense = expense,
            dueDate = expense.occurrenceDateIn(selectedMonth) ?: expense.date,
            onDismiss = { expenseToValidate = null },
            onConfirm = { paymentDate, method ->
                onValidateExpense(expense.id, paymentDate, method) { error ->
                    if (error == null) expenseToValidate = null
                }
            }
        )
    }

    expenseToDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text(stringResource(R.string.delete_expense)) },
            text = { Text(stringResource(R.string.delete_expense_confirm, expense.label)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteExpense(expense.id)
                    expenseToDelete = null
                }) { Text(stringResource(R.string.delete), color = Color(0xFFF44336)) }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun TransactionTypeTabs(
    filters: List<TransactionTypeFilter>,
    selected: TransactionTypeFilter,
    onSelect: (TransactionTypeFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        filters.forEach { filter ->
            val isSelected = filter == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) TransactionsTheme.Primary else Color.Transparent)
                    .clickable { onSelect(filter) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(filter.labelRes),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) Color.White else TransactionsTheme.ChipInactive,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TransactionSecondaryFilters(
    statusFilter: TransactionStatusFilter,
    onStatusFilterChange: (TransactionStatusFilter) -> Unit,
    selectedClient: String?,
    showClientMenu: Boolean,
    onShowClientMenu: (Boolean) -> Unit,
    availableClients: List<String>,
    onClientSelected: (String?) -> Unit,
    sortMode: TransactionSort,
    onSortClick: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(
            listOf(
                TransactionStatusFilter.ALL,
                TransactionStatusFilter.DUE,
                TransactionStatusFilter.PARTIAL,
                TransactionStatusFilter.PAID
            )
        ) { filter ->
            UnderlineFilterChip(
                label = stringResource(filter.labelRes),
                selected = statusFilter == filter && selectedClient == null,
                onClick = {
                    onClientSelected(null)
                    onStatusFilterChange(filter)
                }
            )
        }

        item {
            Box {
                UnderlineFilterChip(
                    label = if (selectedClient != null) {
                        selectedClient
                    } else {
                        stringResource(R.string.client)
                    },
                    selected = selectedClient != null,
                    onClick = { onShowClientMenu(true) }
                )
                DropdownMenu(
                    expanded = showClientMenu,
                    onDismissRequest = { onShowClientMenu(false) }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.filter_all)) },
                        onClick = { onClientSelected(null) }
                    )
                    availableClients.forEach { client ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    client,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            onClick = { onClientSelected(client) }
                        )
                    }
                }
            }
        }

        item {
            UnderlineFilterChip(
                label = stringResource(sortMode.labelRes),
                selected = sortMode != TransactionSort.DATE,
                onClick = onSortClick,
                trailingIcon = when (sortMode) {
                    TransactionSort.AMOUNT_DESC -> Icons.Default.KeyboardArrowDown
                    TransactionSort.AMOUNT_ASC -> Icons.Default.KeyboardArrowUp
                    TransactionSort.DATE -> null
                }
            )
        }
    }
}

@Composable
private fun UnderlineFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) TransactionsTheme.Primary else TransactionsTheme.ChipInactive,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            trailingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (selected) TransactionsTheme.Primary else TransactionsTheme.ChipInactive
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(if (selected) 24.dp else 0.dp)
                .height(2.dp)
                .background(
                    if (selected) TransactionsTheme.Primary else Color.Transparent,
                    RoundedCornerShape(1.dp)
                )
        )
    }
}

@Composable
private fun TransactionIncomeLine(
    invoice: Invoice,
    displayDate: LocalDate,
    datePattern: DateTimeFormatter,
    isAdmin: Boolean,
    canAddPayment: Boolean,
    onMarkPaid: () -> Unit,
    onPartialPayment: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val formatAmount = rememberFormatMoney()
    val isPaid = invoice.status == InvoiceStatus.PAID
    val statusColor = when (invoice.status) {
        InvoiceStatus.PAID -> TransactionsTheme.Income
        InvoiceStatus.PARTIAL -> Color(0xFFFF9800)
        InvoiceStatus.DUE -> Color(0xFFF44336)
    }
    val statusLabel = when (invoice.status) {
        InvoiceStatus.PAID -> stringResource(R.string.status_paid)
        InvoiceStatus.PARTIAL -> stringResource(R.string.status_partial)
        InvoiceStatus.DUE -> stringResource(R.string.status_due)
    }
    var showMenu by remember { mutableStateOf(false) }
    val canUseMenu = (canAddPayment && !isPaid) || isAdmin

    TransactionLineRow(
        iconTint = TransactionsTheme.Income,
        icon = Icons.Default.ArrowUpward,
        title = invoice.clientName,
        subtitle = "$statusLabel · ${displayDate.format(datePattern)}",
        subtitleColor = statusColor,
        detail = stringResource(
            R.string.invoice_due_meta,
            invoice.invoiceNumber,
            invoice.dueDate.format(datePattern)
        ),
        amount = formatAmount(invoice.totalAmount),
        amountColor = TransactionsTheme.Primary,
        showMenu = canUseMenu,
        menuExpanded = showMenu,
        onMenuToggle = { showMenu = it },
        menuContent = {
            if (canAddPayment && !isPaid) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_validate)) },
                    onClick = {
                        showMenu = false
                        onMarkPaid()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.partial_payment)) },
                    onClick = {
                        showMenu = false
                        onPartialPayment()
                    }
                )
            }
            if (isAdmin) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit)) },
                    onClick = {
                        showMenu = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete), color = Color(0xFFF44336)) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
    )
}

@Composable
private fun TransactionExpenseLine(
    expense: Expense,
    displayDate: LocalDate,
    datePattern: DateTimeFormatter,
    isAdmin: Boolean,
    canManage: Boolean,
    onEdit: () -> Unit,
    onValidate: () -> Unit,
    onDelete: () -> Unit
) {
    val formatAmount = rememberFormatMoney()
    val recurringBadge = stringResource(R.string.recurring_badge)
    val upcomingBadge = stringResource(R.string.upcoming_badge)
    var showMenu by remember { mutableStateOf(false) }

    val statusLabel = if (expense.isPaid) {
        stringResource(R.string.status_paid)
    } else {
        upcomingBadge
    }
    val statusColor = if (expense.isPaid) TransactionsTheme.Income else Color(0xFFFF9800)

    val detailParts = buildList {
        if (expense.isRecurring) {
            add(expense.recurrence?.localizedLabel() ?: recurringBadge)
        }
    }
    val detail = detailParts.takeIf { it.isNotEmpty() }?.joinToString(" · ")

    TransactionLineRow(
        iconTint = TransactionsTheme.Expense,
        icon = Icons.Default.ArrowDownward,
        title = expense.label,
        subtitle = "$statusLabel · ${displayDate.format(datePattern)}",
        subtitleColor = statusColor,
        detail = detail,
        amount = formatAmount(expense.amount),
        amountColor = TransactionsTheme.Expense,
        showMenu = isAdmin || canManage,
        menuExpanded = showMenu,
        onMenuToggle = { showMenu = it },
        menuContent = {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit)) },
                onClick = {
                    showMenu = false
                    onEdit()
                }
            )
            if (!expense.isPaid && (isAdmin || canManage)) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_validate)) },
                    onClick = {
                        showMenu = false
                        onValidate()
                    }
                )
            }
            if (isAdmin) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete), color = Color(0xFFF44336)) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
    )
}

@Composable
private fun TransactionLineRow(
    iconTint: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    subtitleColor: Color,
    detail: String?,
    amount: String,
    amountColor: Color,
    showMenu: Boolean,
    menuExpanded: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    menuContent: @Composable ColumnScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .border(1.dp, iconTint.copy(alpha = 0.35f), CircleShape)
                .background(iconTint.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(17.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TransactionsTheme.Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            detail?.let {
                Text(
                    text = it,
                    fontSize = 10.sp,
                    color = TransactionsTheme.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = amount,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor,
                maxLines = 1
            )
            if (showMenu) {
                Box {
                    IconButton(
                        onClick = { onMenuToggle(true) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.actions),
                            tint = TransactionsTheme.ChipInactive,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { onMenuToggle(false) }
                    ) {
                        menuContent()
                    }
                }
            }
        }
    }
}
