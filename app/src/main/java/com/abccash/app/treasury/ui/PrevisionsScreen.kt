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
import androidx.compose.material.icons.filled.Settings
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
import com.abccash.app.treasury.data.EcheanceForecast
import com.abccash.app.treasury.data.EcheanceItem
import com.abccash.app.treasury.data.EcheanceType
import com.abccash.app.treasury.data.Expense
import com.abccash.app.treasury.data.ExpenseCategory
import com.abccash.app.treasury.data.ExpenseRecurrence
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.data.PaymentMethod
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import com.abccash.app.treasury.data.hasPermission
import com.abccash.app.ui.theme.AppColors
import java.time.LocalDate
import java.time.YearMonth

private object PrevisionsTheme {
    val Background = Color.White
    val Accent = AppColors.BrandBlue
    val TextPrimary = AppColors.TextPrimary
    val Income = AppColors.IncomeGreen
    val Expense = AppColors.ExpenseRed
    val Muted = AppColors.TextSecondary
    val Divider = AppColors.Border
    val ChipInactive = AppColors.TextSecondary
    val Overdue = AppColors.ExpenseRed
}

private enum class PrevisionListFilter(@StringRes val labelRes: Int) {
    ALL(R.string.filter_all),
    INCOME(R.string.income_title),
    EXPENSE(R.string.expense_title),
    OVERDUE(R.string.forecasts_filter_overdue)
}

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
        ExpenseRecurrence?, LocalDate?, Boolean, PaymentMethod?, ExpenseCategory, String
    ) -> Unit,
    onDeleteExpense: (String) -> Unit,
    onValidateForecastExpense: (String, LocalDate, PaymentMethod, LocalDate, (String?) -> Unit) -> Unit,
    onNavigateToAddIncome: () -> Unit,
    onNavigateToAddExpense: () -> Unit,
    onForecastValidated: (YearMonth) -> Unit,
    onOpenDrawer: () -> Unit = {},
    customExpenseCategories: List<String> = emptyList()
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

    if (!hasPermission(userRole, permissions, UserPermission.VIEW_TREASURY)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.access_denied), color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
        }
        return
    }

    var showTypeSheet by remember { mutableStateOf(false) }
    var listFilter by remember { mutableStateOf(PrevisionListFilter.ALL) }

    var invoiceToEdit by remember { mutableStateOf<Invoice?>(null) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var itemToDelete by remember { mutableStateOf<EcheanceItem?>(null) }
    var invoiceToMarkPaid by remember { mutableStateOf<Invoice?>(null) }
    var expenseToMarkPaid by remember { mutableStateOf<Pair<Expense, LocalDate>?>(null) }
    var editError by remember { mutableStateOf<String?>(null) }

    val formatAmount = rememberFormatMoney()
    val dateFormatter = remember { AppLocale.shortDayMonthYearFormatter() }
    val today = remember { LocalDate.now() }

    val allItems = remember(invoices, expenses, selectedMonth, canViewIncome, canViewExpenses) {
        val filteredInvoices = if (canViewIncome) invoices else emptyList()
        val filteredExpenses = if (canViewExpenses) expenses else emptyList()
        EcheanceForecast.buildItemsForMonth(
            month = selectedMonth,
            invoices = filteredInvoices,
            expenses = filteredExpenses
        )
    }

    val listFilters = remember(canViewIncome, canViewExpenses) {
        buildList {
            add(PrevisionListFilter.ALL)
            if (canViewIncome) add(PrevisionListFilter.INCOME)
            if (canViewExpenses) add(PrevisionListFilter.EXPENSE)
            add(PrevisionListFilter.OVERDUE)
        }
    }

    LaunchedEffect(selectedMonth) {
        listFilter = PrevisionListFilter.ALL
    }

    val filteredItems = remember(allItems, listFilter, today) {
        val result = when (listFilter) {
            PrevisionListFilter.ALL -> allItems
            PrevisionListFilter.INCOME -> allItems.filter { it.type == EcheanceType.INCOME }
            PrevisionListFilter.EXPENSE -> allItems.filter { it.type == EcheanceType.EXPENSE }
            PrevisionListFilter.OVERDUE -> allItems.filter { it.dueDate.isBefore(today) }
        }
        result.sortedBy { it.dueDate }
    }

    val forecastIncome = remember(allItems) {
        allItems.filter { it.type == EcheanceType.INCOME }.sumOf { it.amount }
    }
    val forecastExpenses = remember(allItems) {
        allItems.filter { it.type == EcheanceType.EXPENSE }.sumOf { it.amount }
    }

    invoiceToEdit?.let { invoice ->
        InvoiceFormDialog(
            title = stringResource(R.string.edit),
            initialInvoice = invoice,
            selectedMonth = YearMonth.from(invoice.dueDate),
            onDismiss = {
                invoiceToEdit = null
                editError = null
            },
            onConfirm = { invoiceNumber, clientName, totalAmount, dueDate, _, paymentMethod ->
                // Note: InvoiceFormDialog only shows payment method when editing, not when creating
                // The actual payment is recorded separately when marking as paid
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
            customExpenseCategories = customExpenseCategories,
            onDismiss = {
                expenseToEdit = null
                editError = null
            },
            onConfirm = { label, amount, date, recurring, recurrence, endDate, paid, paymentMethod, category, categoryLabel ->
                onUpdateExpense(
                    expense.id, label, amount, date, recurring, recurrence, endDate, paid, paymentMethod,
                    category, categoryLabel
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
                onValidateForecastExpense(expense.id, paymentDate, method, dueDate) { error ->
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
        containerColor = PrevisionsTheme.Background,
        floatingActionButton = {
            if (canAddIncome || canAddExpense) {
                AbcCashFab(
                    onClick = { showTypeSheet = true },
                    contentDescription = stringResource(R.string.add),
                    containerColor = PrevisionsTheme.Accent
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.forecasts),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrevisionsTheme.TextPrimary
                )
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings),
                        tint = PrevisionsTheme.ChipInactive
                    )
                }
            }

            PrevisionListFilterRow(
                filters = listFilters,
                selected = listFilter,
                onSelect = { listFilter = it }
            )

            MonthSelectorRow(
                selectedMonth = selectedMonth,
                onMonthChange = onMonthChange,
                compact = true
            )

            PrevisionSummaryRow(
                incomeLabel = stringResource(R.string.expected_income),
                incomeAmount = formatAmount(forecastIncome),
                expenseLabel = stringResource(R.string.expected_expenses),
                expenseAmount = formatAmount(forecastExpenses)
            )

            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp),
                color = PrevisionsTheme.Divider
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .monthSwipeNavigation(selectedMonth, onMonthChange)
            ) {
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.no_forecasts),
                            fontSize = 13.sp,
                            color = PrevisionsTheme.Muted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        val isOverdue = item.dueDate.isBefore(today)
                        val statusLabel = when {
                            isOverdue -> stringResource(R.string.forecasts_filter_overdue)
                            else -> stringResource(R.string.upcoming_badge)
                        }
                        val statusColor = if (isOverdue) PrevisionsTheme.Overdue else PrevisionsTheme.Muted
                        val isIncome = item.type == EcheanceType.INCOME
                        val canMarkPaid = when (item.type) {
                            EcheanceType.INCOME -> canMarkPaidIncome
                            EcheanceType.EXPENSE -> canManageExpense
                        }
                        val canEdit = when (item.type) {
                            EcheanceType.INCOME -> isAdmin
                            EcheanceType.EXPENSE -> canManageExpense
                        }
                        val recurringLabel = if (item.isRecurring) "🔁 ${stringResource(R.string.recurring_badge)}" else null
                        val detail = when {
                            isIncome && item.isRecurring -> "${stringResource(R.string.upcoming_forecast)} · 🔁 ${stringResource(R.string.recurring_badge)}"
                            isIncome -> stringResource(R.string.upcoming_forecast)
                            else -> recurringLabel
                        }

                        PrevisionLineRow(
                            isIncome = isIncome,
                            title = item.label,
                            subtitle = "${statusLabel} · ${item.dueDate.format(dateFormatter)}",
                            subtitleColor = statusColor,
                            detail = detail,
                            amount = formatAmount(item.amount),
                            canMarkPaid = canMarkPaid,
                            canEdit = canEdit,
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
                        HorizontalDivider(color = PrevisionsTheme.Divider, thickness = 1.dp)
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun PrevisionSummaryRow(
    incomeLabel: String,
    incomeAmount: String,
    expenseLabel: String,
    expenseAmount: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PrevisionSummaryMetric(
            label = incomeLabel,
            amount = incomeAmount,
            color = PrevisionsTheme.Income,
            modifier = Modifier.weight(1f)
        )
        PrevisionSummaryMetric(
            label = expenseLabel,
            amount = expenseAmount,
            color = PrevisionsTheme.Expense,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PrevisionSummaryMetric(
    label: String,
    amount: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, fontSize = 11.sp, color = PrevisionsTheme.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.height(2.dp))
        Text(amount, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
    }
}

@Composable
private fun PrevisionListFilterRow(
    filters: List<PrevisionListFilter>,
    selected: PrevisionListFilter,
    onSelect: (PrevisionListFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(filters) { filter ->
            PrevisionUnderlineChip(
                label = stringResource(filter.labelRes),
                selected = filter == selected,
                onClick = { onSelect(filter) }
            )
        }
    }
}

@Composable
private fun PrevisionUnderlineChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) PrevisionsTheme.Accent else PrevisionsTheme.ChipInactive,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(if (selected) 24.dp else 0.dp)
                .height(2.dp)
                .background(
                    if (selected) PrevisionsTheme.Accent else Color.Transparent,
                    RoundedCornerShape(1.dp)
                )
        )
    }
}

@Composable
private fun PrevisionLineRow(
    isIncome: Boolean,
    title: String,
    subtitle: String,
    subtitleColor: Color,
    detail: String?,
    amount: String,
    canMarkPaid: Boolean,
    canEdit: Boolean,
    onMarkPaid: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val iconTint = if (isIncome) PrevisionsTheme.Income else PrevisionsTheme.Expense
    val amountColor = if (isIncome) PrevisionsTheme.Income else PrevisionsTheme.Expense
    val showMenu = canMarkPaid || canEdit

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
                imageVector = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
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
                color = PrevisionsTheme.TextPrimary,
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
                    color = PrevisionsTheme.Muted,
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
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.actions),
                            tint = PrevisionsTheme.ChipInactive,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    AbcDropdownMenu(
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
}
