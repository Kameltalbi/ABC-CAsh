package com.abccash.app.treasury.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abccash.app.R
import com.abccash.app.locale.AppLocale
import com.abccash.app.treasury.data.*
import com.abccash.app.treasury.datastore.UserPreferences
import com.abccash.app.ui.theme.AppColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.max

private object DashboardTheme {
    val HeaderTop = AppColors.BrandBlue
    val HeaderBottom = AppColors.BrandBlueDark
    val ScreenBg = AppColors.Background
    val Card = AppColors.Surface
    val Income = AppColors.IncomeGreen
    val Expense = AppColors.ExpenseRed
    val TextPrimary = AppColors.TextPrimary
    val TextSecondary = AppColors.TextSecondary
    val Fab = AppColors.BrandBlue
    val Track = AppColors.Border
    val SummaryArcTrack = Color(0xFFD8D8D8)
    val Accent = AppColors.BrandBlue
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernDashboardScreen(
    userRole: UserRole,
    permissions: Set<UserPermission>,
    userName: String,
    companyName: String,
    invoices: List<Invoice>,
    expenses: List<Expense>,
    bankAccounts: List<BankAccount>,
    entrepriseId: String?,
    userPreferences: UserPreferences,
    onNavigateToAddIncome: () -> Unit,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToBankAccounts: () -> Unit = {},
    onOpenDrawer: () -> Unit = {}
) {
    val formatAmount = rememberFormatMoney()
    val formatSummaryAmount = rememberFormatDashboardSummary()
    val today = remember { LocalDate.now() }
    val focusMonth = remember { YearMonth.now() }
    val bankBalance by userPreferences
        .observeBankBalance(entrepriseId.orEmpty(), focusMonth.year)
        .collectAsStateWithLifecycle(initialValue = null)

    val canViewIncome = hasPermission(userRole, permissions, UserPermission.VIEW_INVOICES)
    val canManageExpense = hasPermission(userRole, permissions, UserPermission.MANAGE_EXPENSES)
    val isAdmin = userRole == UserRole.ADMIN
    val visibleInvoices = if (canViewIncome || isAdmin) invoices else emptyList()
    val visibleExpenses = if (canManageExpense || isAdmin) expenses else emptyList()

    val monthComparison = remember(visibleInvoices, visibleExpenses, focusMonth) {
        DashboardCalculations.buildMonthComparison(visibleInvoices, visibleExpenses, focusMonth)
    }
    val upcomingPayments = remember(visibleInvoices, visibleExpenses, today) {
        DashboardCalculations.buildUpcomingExpensePayments(visibleInvoices, visibleExpenses, today)
    }
    val monthlyBars = remember(visibleInvoices, visibleExpenses, focusMonth) {
        DashboardCalculations.buildRollingMonthlyBarChart(
            invoices = visibleInvoices,
            expenses = visibleExpenses,
            focusMonth = focusMonth,
            monthCount = 6
        )
    }
    val expenseCategories = remember(visibleExpenses, focusMonth) {
        DashboardCalculations.buildInnovativeDashboard(
            invoices = visibleInvoices,
            expenses = visibleExpenses,
            bankBalance = bankBalance,
            focusMonth = focusMonth,
            viewMode = DashboardViewMode.MONTH
        ).expenseByCategory.take(4)
    }
    val expenseCategoryTotal = expenseCategories.sumOf { it.amount }.coerceAtLeast(1.0)

    val accountSummaries = remember(bankAccounts, visibleInvoices, visibleExpenses) {
        if (bankAccounts.isEmpty()) emptyList()
        else BankAccountCalculations.summarize(bankAccounts, visibleInvoices, visibleExpenses)
    }
    val defaultAccountId = remember(bankAccounts) {
        bankAccounts.firstOrNull { it.isDefault && it.kind == TreasuryAccountKind.BANK }?.id
            ?: bankAccounts.firstOrNull { it.kind == TreasuryAccountKind.BANK }?.id
    }
    val defaultCashAccountId = remember(bankAccounts) {
        bankAccounts.firstOrNull { it.isDefault && it.kind == TreasuryAccountKind.CASH }?.id
            ?: bankAccounts.firstOrNull { it.kind == TreasuryAccountKind.CASH }?.id
    }
    val treasuryBalance = remember(visibleInvoices, visibleExpenses, bankBalance) {
        bankBalance ?: DashboardCalculations.computedBankBalance(visibleInvoices, visibleExpenses)
    }

    val notificationCount = remember(visibleInvoices, visibleExpenses) {
        EcheanceForecast.buildItems(
            invoices = visibleInvoices,
            expenses = visibleExpenses,
            from = today,
            to = today.plusDays(7)
        ).size
    }

    var showTypeSheet by remember { mutableStateOf(false) }
    val canAddExpense = isAdmin || hasPermission(userRole, permissions, UserPermission.MANAGE_EXPENSES)
    val canAddIncome = isAdmin

    if (showTypeSheet) {
        TransactionTypeChoiceSheet(
            canAddIncome = canAddIncome,
            canAddExpense = canAddExpense,
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
        containerColor = DashboardTheme.ScreenBg,
        floatingActionButton = {
            if (canAddIncome || canAddExpense) {
                AbcCashFab(
                    onClick = { showTypeSheet = true },
                    contentDescription = stringResource(R.string.quick_entry),
                    containerColor = DashboardTheme.Fab
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            FastBudgetHeader(
                companyName = companyName,
                notificationCount = notificationCount,
                onOpenDrawer = onOpenDrawer
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (canViewIncome || canManageExpense || isAdmin) {
                    item {
                        SummaryComparisonCard(
                            current = monthComparison.first,
                            previous = monthComparison.second,
                            formatAmount = formatSummaryAmount,
                            showIncome = canViewIncome || isAdmin,
                            showExpenses = canManageExpense || isAdmin
                        )
                    }
                }
                item {
                    AccountsCard(
                        summaries = accountSummaries,
                        treasuryBalance = treasuryBalance,
                        invoices = visibleInvoices,
                        expenses = visibleExpenses,
                        defaultAccountId = defaultAccountId,
                        defaultCashAccountId = defaultCashAccountId,
                        formatAmount = formatSummaryAmount,
                        onAddAccount = onNavigateToBankAccounts
                    )
                }
                if (canViewIncome || canManageExpense || isAdmin) {
                    item {
                        TransactionsChartsCard(
                            upcomingBars = upcomingPayments,
                            monthlyBars = monthlyBars,
                            formatAmount = formatAmount,
                            showIncome = canViewIncome || isAdmin,
                            showExpenses = canManageExpense || isAdmin
                        )
                    }
                }
                if (canManageExpense || isAdmin) {
                    if (expenseCategories.isNotEmpty()) {
                        item {
                            TopCategoriesCard(
                                slices = expenseCategories,
                                total = expenseCategoryTotal,
                                formatAmount = formatAmount
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FastBudgetHeader(
    companyName: String,
    notificationCount: Int,
    onOpenDrawer: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DashboardTheme.HeaderTop, DashboardTheme.HeaderBottom)
                )
            )
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.dashboard_header_label),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1
                )
                Text(
                    text = companyName.trim().ifBlank { stringResource(R.string.company_fallback) },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                BadgedBox(
                    badge = {
                        if (notificationCount > 0) {
                            Badge(containerColor = DashboardTheme.Expense) {
                                Text(
                                    notificationCount.coerceAtMost(99).toString(),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = stringResource(R.string.dashboard_notifications),
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun DashboardSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    onMenuClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DashboardTheme.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DashboardTheme.TextPrimary
                )
                if (onMenuClick != null) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = null,
                        tint = DashboardTheme.TextSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(onClick = onMenuClick)
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun SummaryComparisonCard(
    current: MonthFinancialSummary,
    previous: MonthFinancialSummary,
    formatAmount: (Double) -> String,
    showIncome: Boolean,
    showExpenses: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DashboardTheme.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.dashboard_summary),
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DashboardTheme.TextPrimary,
                    textAlign = TextAlign.Center
                )
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = DashboardTheme.TextSecondary,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(20.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MonthSummaryColumn(
                    modifier = Modifier.weight(1f),
                    summary = previous,
                    formatAmount = formatAmount,
                    showIncome = showIncome,
                    showExpenses = showExpenses
                )
                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    thickness = 1.dp,
                    color = DashboardTheme.SummaryArcTrack
                )
                MonthSummaryColumn(
                    modifier = Modifier.weight(1f),
                    summary = current,
                    formatAmount = formatAmount,
                    showIncome = showIncome,
                    showExpenses = showExpenses
                )
            }
        }
    }
}

@Composable
private fun MonthSummaryColumn(
    summary: MonthFinancialSummary,
    formatAmount: (Double) -> String,
    showIncome: Boolean,
    showExpenses: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = summary.label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = DashboardTheme.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SemiDonutChart(
                income = if (showIncome) summary.income else 0.0,
                expenses = if (showExpenses) summary.expenses else 0.0,
                modifier = Modifier
                    .width(38.dp)
                    .height(96.dp)
            )
            Spacer(Modifier.width(6.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (showIncome) {
                    SummaryAmountRow(
                        label = stringResource(R.string.dashboard_summary_income),
                        amount = formatAmount(summary.income)
                    )
                }
                if (showExpenses) {
                    SummaryAmountRow(
                        label = stringResource(R.string.dashboard_summary_expense),
                        amount = formatSignedExpense(summary.expenses, formatAmount)
                    )
                }
                SummaryAmountRow(
                    label = stringResource(R.string.total),
                    amount = formatAmount(summary.total),
                    bold = true
                )
            }
        }
    }
}

private fun formatSignedExpense(amount: Double, formatAmount: (Double) -> String): String {
    if (amount <= 0.0) return formatAmount(0.0)
    return "-${formatAmount(amount)}"
}

@Composable
private fun SummaryAmountRow(
    label: String,
    amount: String,
    bold: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = DashboardTheme.TextSecondary,
            maxLines = 1
        )
        Text(
            text = amount,
            fontSize = if (bold) 13.sp else 12.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            color = DashboardTheme.TextPrimary,
            maxLines = 2,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun SemiDonutChart(
    income: Double,
    expenses: Double,
    modifier: Modifier = Modifier
) {
    val total = (income + expenses).coerceAtLeast(1.0)
    val hasData = income > 0.0 || expenses > 0.0
    Canvas(modifier = modifier) {
        val stroke = 16.dp.toPx()
        val diameter = (size.height - stroke).coerceAtMost(size.width * 2f - stroke)
        val topLeft = Offset(0f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        val startAngle = 90f
        val sweepIncome = (income / total * 180f).toFloat()
        val sweepExpense = (expenses / total * 180f).toFloat()

        drawArc(
            color = DashboardTheme.SummaryArcTrack,
            startAngle = startAngle,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Butt)
        )
        if (hasData && sweepIncome > 0f) {
            drawArc(
                color = DashboardTheme.Income,
                startAngle = startAngle,
                sweepAngle = sweepIncome,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
        }
        if (hasData && sweepExpense > 0f) {
            drawArc(
                color = DashboardTheme.Expense,
                startAngle = startAngle + sweepIncome,
                sweepAngle = sweepExpense,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
        }
    }
}

@Composable
private fun AccountsCard(
    summaries: List<BankAccountSummary>,
    treasuryBalance: Double,
    invoices: List<Invoice>,
    expenses: List<Expense>,
    defaultAccountId: String?,
    defaultCashAccountId: String?,
    formatAmount: (Double) -> String,
    onAddAccount: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy", AppLocale.current()) }

    DashboardSectionCard(
        title = stringResource(R.string.dashboard_accounts),
        onMenuClick = onAddAccount
    ) {
        if (summaries.isEmpty()) {
                AccountRow(
                    name = stringResource(R.string.dashboard_treasury_wallet),
                    subtitle = stringResource(R.string.dashboard_no_accounts_hint),
                    amount = formatAmount(treasuryBalance),
                amountColor = if (treasuryBalance >= 0) DashboardTheme.Income else DashboardTheme.Expense
            )
        } else {
            summaries.forEach { summary ->
                val lastUsed = BankAccountCalculations.lastMovementDate(
                    summary.account,
                    invoices,
                    expenses,
                    defaultAccountId,
                    defaultCashAccountId
                )
                val subtitle = when {
                    lastUsed != null -> stringResource(
                        R.string.dashboard_last_used,
                        lastUsed.format(dateFormatter)
                    )
                    summary.account.bankName.isNotBlank() -> summary.account.bankName
                    else -> stringResource(R.string.bank_account)
                }

                AccountRow(
                    name = summary.account.name,
                    subtitle = subtitle,
                    amount = formatAmount(summary.balance),
                    amountColor = if (summary.balance >= 0) DashboardTheme.Income else DashboardTheme.Expense,
                    showAlert = summary.hasLowBalanceAlert
                )
                if (summary != summaries.last()) {
                    HorizontalDivider(color = DashboardTheme.Track)
                }
            }
        }
    }
}

@Composable
private fun AccountRow(
    name: String,
    subtitle: String,
    amount: String,
    amountColor: Color,
    showAlert: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = DashboardTheme.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = DashboardTheme.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showAlert) {
                Text(
                    text = stringResource(R.string.bank_account_low_balance_alert),
                    fontSize = 11.sp,
                    color = DashboardTheme.Expense
                )
            }
        }
        Text(
            text = amount,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = amountColor,
            textAlign = TextAlign.End,
            letterSpacing = 0.2.sp,
            modifier = Modifier.widthIn(min = 96.dp)
        )
    }
}

@Composable
private fun TransactionsChartsCard(
    upcomingBars: List<UpcomingPaymentBar>,
    monthlyBars: List<MonthlyBarPoint>,
    formatAmount: (Double) -> String,
    showIncome: Boolean,
    showExpenses: Boolean
) {
    DashboardSectionCard(title = stringResource(R.string.dashboard_upcoming_payments)) {
        UpcomingPaymentsChart(
            bars = upcomingBars,
            formatAmount = formatAmount,
            showExpenses = showExpenses
        )

        HorizontalDivider(color = DashboardTheme.Track)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.dashboard_monthly_bars_title),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = DashboardTheme.TextPrimary
            )
            Text(
                text = stringResource(R.string.dashboard_monthly_bars_subtitle_6m),
                fontSize = 12.sp,
                color = DashboardTheme.TextSecondary
            )
            MonthlyIncomeExpenseBarChart(
                bars = monthlyBars,
                showIncome = showIncome,
                showExpenses = showExpenses
            )
            if (showIncome || showExpenses) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                ) {
                    if (showIncome) {
                        ChartLegendDot(
                            color = DashboardTheme.Income,
                            label = stringResource(R.string.income_title)
                        )
                    }
                    if (showExpenses) {
                        ChartLegendDot(
                            color = DashboardTheme.Expense,
                            label = stringResource(R.string.expense_title)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = DashboardTheme.TextSecondary
        )
    }
}

@Composable
private fun UpcomingPaymentsChart(
    bars: List<UpcomingPaymentBar>,
    formatAmount: (Double) -> String,
    showExpenses: Boolean
) {
    if (!showExpenses) return

    val maxAmount = bars.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
    val hasData = bars.isNotEmpty()

    if (!hasData) {
        Text(
            text = stringResource(R.string.treasury_no_upcoming),
            fontSize = 13.sp,
            color = DashboardTheme.TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            textAlign = TextAlign.Center
        )
    } else {
        val chartPlotHeight = 88.dp
        val amountLabelHeight = 18.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartPlotHeight + amountLabelHeight + 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEach { bar ->
                val heightFraction = (bar.amount / maxAmount).toFloat().coerceIn(0.04f, 1f)
                val barHeight = (chartPlotHeight * heightFraction).coerceAtLeast(4.dp)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartPlotHeight + amountLabelHeight),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = formatAmount(bar.amount),
                                fontSize = 7.sp,
                                lineHeight = 9.sp,
                                color = DashboardTheme.TextSecondary,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 1.dp)
                            )
                            Spacer(Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                    .background(DashboardTheme.Expense)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = bar.label,
                        fontSize = 9.sp,
                        color = DashboardTheme.TextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyIncomeExpenseBarChart(
    bars: List<MonthlyBarPoint>,
    showIncome: Boolean,
    showExpenses: Boolean,
    modifier: Modifier = Modifier
) {
    val monthFormatter = remember {
        DateTimeFormatter.ofPattern("MMM", AppLocale.current())
    }
    val maxAmount = bars.maxOfOrNull { max(it.income, it.expenses) }?.coerceAtLeast(1.0) ?: 1.0
    val barCount = (if (showIncome) 1 else 0) + (if (showExpenses) 1 else 0)

    if (barCount == 0) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEach { bar ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (showIncome) {
                        val incomeFraction = (bar.income / maxAmount).toFloat().coerceIn(0.04f, 1f)
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .fillMaxHeight(incomeFraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (bar.income > 0) DashboardTheme.Income
                                    else DashboardTheme.Track.copy(alpha = 0.5f)
                                )
                        )
                    }
                    if (showExpenses) {
                        val expenseFraction = (bar.expenses / maxAmount).toFloat().coerceIn(0.04f, 1f)
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .fillMaxHeight(expenseFraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (bar.expenses > 0) DashboardTheme.Expense
                                    else DashboardTheme.Track.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = bar.month.atDay(1).format(monthFormatter)
                        .replaceFirstChar { it.uppercase() },
                    fontSize = 10.sp,
                    color = DashboardTheme.TextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun TopCategoriesCard(
    slices: List<CategorySlice>,
    total: Double,
    formatAmount: (Double) -> String
) {
    val colors = listOf(
        DashboardTheme.Fab,
        DashboardTheme.Income,
        Color(0xFF00796B),
        DashboardTheme.Accent
    )

    DashboardSectionCard(title = stringResource(R.string.dashboard_top_categories)) {
        slices.forEachIndexed { index, slice ->
            val share = (slice.amount / total).toFloat().coerceIn(0.05f, 1f)
            val color = colors[index % colors.size]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = localizedCategoryLabel(slice).take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = color,
                        fontSize = 16.sp
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = localizedCategoryLabel(slice),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DashboardTheme.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    LinearProgressIndicator(
                        progress = { share },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = color,
                        trackColor = DashboardTheme.Track
                    )
                    Text(
                        text = formatAmount(slice.amount),
                        fontSize = 12.sp,
                        color = DashboardTheme.TextSecondary
                    )
                }
            }
            if (index < slices.lastIndex) {
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
