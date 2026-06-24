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
import com.abccash.app.ui.theme.AppColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private object TransactionsTheme {
    val Background = Color.White
    val Accent = AppColors.BrandBlue
    val TextPrimary = AppColors.TextPrimary
    val Income = AppColors.IncomeGreen
    val Expense = AppColors.ExpenseRed
    val Muted = AppColors.TextSecondary
    val Divider = AppColors.Border
    val ChipInactive = AppColors.TextSecondary
}

private data class TransactionRow(
    val date: LocalDate,
    val invoice: Invoice? = null,
    val expense: Expense? = null
) {
    val isIncome: Boolean get() = invoice != null

    fun amountValue(): Double = invoice?.totalAmount ?: expense?.amount ?: 0.0

    fun isUnpaid(): Boolean = when {
        invoice != null -> invoice.status != InvoiceStatus.PAID
        expense != null -> !expense.isPaid
        else -> false
    }
}

private enum class TransactionListFilter(@StringRes val labelRes: Int) {
    ALL(R.string.filter_all),
    INCOME(R.string.income_title),
    EXPENSE(R.string.expense_title),
    UNPAID(R.string.filter_unpaid)
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
    onNavigateToAddIncome: () -> Unit,
    onNavigateToAddExpense: () -> Unit,
    onUpdateInvoice: (String, String, String, Double, LocalDate, (String?) -> Unit) -> Unit,
    onRecordPayment: (String, Double, LocalDate, PaymentMethod, (String?) -> Unit) -> Unit,
    onDeleteInvoice: (String, (String?) -> Unit) -> Unit,
    onDeleteInvoices: (Collection<String>) -> Unit = {},
    onUpdateExpense: (String, String, Double, LocalDate, Boolean, ExpenseRecurrence?, LocalDate?, Boolean, PaymentMethod?) -> Unit,
    onStopRecurrence: (String, LocalDate) -> Unit,
    onDeleteExpense: (String) -> Unit,
    onDeleteExpenses: (Collection<String>) -> Unit = {},
    onValidateExpense: (String, LocalDate, PaymentMethod, (String?) -> Unit) -> Unit = { _, _, _, onResult -> onResult(null) },
    onOpenDrawer: () -> Unit = {}
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
    var listFilter by remember { mutableStateOf(TransactionListFilter.ALL) }

    var invoiceToEdit by remember { mutableStateOf<Invoice?>(null) }
    var invoiceToDelete by remember { mutableStateOf<Invoice?>(null) }
    var invoiceForPartialPayment by remember { mutableStateOf<Invoice?>(null) }
    var invoiceToMarkPaid by remember { mutableStateOf<Invoice?>(null) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var expenseToValidate by remember { mutableStateOf<Expense?>(null) }
    var paymentError by remember { mutableStateOf<String?>(null) }
    var editError by remember { mutableStateOf<String?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var selectedIncomeIds by remember { mutableStateOf(setOf<String>()) }
    var selectedExpenseIds by remember { mutableStateOf(setOf<String>()) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    var bulkDeleteIncome by remember { mutableStateOf(false) }

    LaunchedEffect(selectedMonth) {
        listFilter = TransactionListFilter.ALL
        selectedIncomeIds = emptySet()
        selectedExpenseIds = emptySet()
    }

    LaunchedEffect(listFilter) {
        selectedIncomeIds = emptySet()
        selectedExpenseIds = emptySet()
    }

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

    val listFilters = remember(canViewIncome, canManageExpense) {
        buildList {
            add(TransactionListFilter.ALL)
            if (canViewIncome) add(TransactionListFilter.INCOME)
            if (canManageExpense) add(TransactionListFilter.EXPENSE)
            add(TransactionListFilter.UNPAID)
        }
    }

    val filteredRows = remember(allRows, listFilter) {
        val result = when (listFilter) {
            TransactionListFilter.ALL -> allRows
            TransactionListFilter.INCOME -> allRows.filter { it.isIncome }
            TransactionListFilter.EXPENSE -> allRows.filter { !it.isIncome }
            TransactionListFilter.UNPAID -> allRows.filter { it.isUnpaid() }
        }
        result.sortedByDescending { it.date }
    }

    val incomeRows = remember(filteredRows) { filteredRows.filter { it.isIncome } }
    val expenseRows = remember(filteredRows) { filteredRows.filter { !it.isIncome } }
    val showIncomeBulk = listFilter == TransactionListFilter.INCOME && isAdmin && incomeRows.isNotEmpty()
    val showExpenseBulk = listFilter == TransactionListFilter.EXPENSE &&
        (isAdmin || canManageExpense) && expenseRows.isNotEmpty()

    Scaffold(
        containerColor = TransactionsTheme.Background,
        floatingActionButton = {
            if (canAdd) {
                AbcCashFab(
                    onClick = { showTypeSheet = true },
                    contentDescription = stringResource(R.string.new_transaction),
                    containerColor = TransactionsTheme.Accent
                )
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
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.transactions),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TransactionsTheme.TextPrimary
                )
            }

            TransactionListFilterRow(
                filters = listFilters,
                selected = listFilter,
                onSelect = { listFilter = it }
            )

            MonthSelectorRow(
                selectedMonth = selectedMonth,
                onMonthChange = onMonthChange,
                compact = true
            )

            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp),
                color = TransactionsTheme.Divider
            )

            if (showIncomeBulk) {
                AdminBulkSelectionBar(
                    totalCount = incomeRows.size,
                    selectedCount = selectedIncomeIds.size,
                    onToggleSelectAll = {
                        selectedIncomeIds = if (selectedIncomeIds.size == incomeRows.size) {
                            emptySet()
                        } else {
                            incomeRows.mapNotNull { it.invoice?.id }.toSet()
                        }
                    },
                    onDeleteSelected = {
                        bulkDeleteIncome = true
                        showBulkDeleteConfirm = true
                    }
                )
            }

            if (showExpenseBulk) {
                AdminBulkSelectionBar(
                    totalCount = expenseRows.size,
                    selectedCount = selectedExpenseIds.size,
                    onToggleSelectAll = {
                        selectedExpenseIds = if (selectedExpenseIds.size == expenseRows.size) {
                            emptySet()
                        } else {
                            expenseRows.mapNotNull { it.expense?.id }.toSet()
                        }
                    },
                    onDeleteSelected = {
                        bulkDeleteIncome = false
                        showBulkDeleteConfirm = true
                    }
                )
            }

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
                                showSelection = showIncomeBulk,
                                isSelected = row.invoice.id in selectedIncomeIds,
                                onSelectionChange = { selected ->
                                    selectedIncomeIds = if (selected) {
                                        selectedIncomeIds + row.invoice.id
                                    } else {
                                        selectedIncomeIds - row.invoice.id
                                    }
                                },
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
                                showSelection = showExpenseBulk,
                                isSelected = row.expense.id in selectedExpenseIds,
                                onSelectionChange = { selected ->
                                    selectedExpenseIds = if (selected) {
                                        selectedExpenseIds + row.expense.id
                                    } else {
                                        selectedExpenseIds - row.expense.id
                                    }
                                },
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
                    onDeleteInvoice(invoice.id) { error ->
                        if (error == null) {
                            invoiceToDelete = null
                        } else {
                            deleteError = error
                            invoiceToDelete = null
                        }
                    }
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
            onConfirm = { label, amount, date, isRecurring, recurrence, recurrenceEndDate, isPaid, paymentMethod ->
                onUpdateExpense(expense.id, label, amount, date, isRecurring, recurrence, recurrenceEndDate, isPaid, paymentMethod)
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

    if (showBulkDeleteConfirm) {
        val count = if (bulkDeleteIncome) selectedIncomeIds.size else selectedExpenseIds.size
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_selection_question)) },
            text = { Text(stringResource(R.string.delete_count, count)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (bulkDeleteIncome) {
                            onDeleteInvoices(selectedIncomeIds)
                            selectedIncomeIds = emptySet()
                        } else {
                            onDeleteExpenses(selectedExpenseIds)
                            selectedExpenseIds = emptySet()
                        }
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

    deleteError?.let { message ->
        AlertDialog(
            onDismissRequest = { deleteError = null },
            title = { Text(stringResource(R.string.error_generic)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { deleteError = null }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

@Composable
private fun TransactionListFilterRow(
    filters: List<TransactionListFilter>,
    selected: TransactionListFilter,
    onSelect: (TransactionListFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            val isSelected = filter == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(filter) },
                label = {
                    Text(
                        text = stringResource(filter.labelRes),
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                },
                shape = RoundedCornerShape(20.dp),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = TransactionsTheme.Divider,
                    selectedBorderColor = TransactionsTheme.Accent,
                    borderWidth = 1.dp
                ),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White,
                    labelColor = TransactionsTheme.TextPrimary,
                    selectedContainerColor = TransactionsTheme.Accent.copy(alpha = 0.14f),
                    selectedLabelColor = TransactionsTheme.Accent
                )
            )
        }
    }
}

@Composable
private fun TransactionIncomeLine(
    invoice: Invoice,
    displayDate: LocalDate,
    datePattern: DateTimeFormatter,
    isAdmin: Boolean,
    canAddPayment: Boolean,
    showSelection: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChange: (Boolean) -> Unit = {},
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
        amountColor = TransactionsTheme.Income,
        showSelection = showSelection,
        isSelected = isSelected,
        onSelectionChange = onSelectionChange,
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
    showSelection: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChange: (Boolean) -> Unit = {},
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
        showSelection = showSelection,
        isSelected = isSelected,
        onSelectionChange = onSelectionChange,
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
            if (isAdmin || canManage) {
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
    showSelection: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChange: (Boolean) -> Unit = {},
    showMenu: Boolean,
    menuExpanded: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    menuContent: @Composable ColumnScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = if (showSelection) 8.dp else 20.dp, vertical = 11.dp),
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
                color = TransactionsTheme.TextPrimary,
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
                    AbcDropdownMenu(
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
