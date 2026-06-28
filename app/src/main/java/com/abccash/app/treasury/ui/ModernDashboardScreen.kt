package com.abccash.app.treasury.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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
    val annualTreasury = remember(visibleInvoices, visibleExpenses, bankAccounts, focusMonth) {
        DashboardCalculations.buildAnnualTreasuryForecast(
            invoices = visibleInvoices,
            expenses = visibleExpenses,
            bankAccounts = bankAccounts,
            focusYear = focusMonth.year
        )
    }
    val recommendations = remember(visibleInvoices, visibleExpenses, bankAccounts, focusMonth, today) {
        DashboardCalculations.buildTreasuryRecommendations(
            invoices = visibleInvoices,
            expenses = visibleExpenses,
            bankAccounts = bankAccounts,
            focusMonth = focusMonth,
            today = today
        )
    }
    val breakEven = remember(visibleInvoices, visibleExpenses, bankAccounts, focusMonth, today) {
        DashboardCalculations.buildBreakEvenSummary(
            invoices = visibleInvoices,
            expenses = visibleExpenses,
            bankAccounts = bankAccounts,
            focusMonth = focusMonth,
            today = today
        )
    }
    val expenseCategories = remember(visibleExpenses, focusMonth) {
        DashboardCalculations.buildInnovativeDashboard(
            invoices = visibleInvoices,
            expenses = visibleExpenses,
            bankBalance = bankBalance,
            bankAccounts = bankAccounts,
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
                item {
                    BreakEvenCard(
                        summary = breakEven,
                        formatAmount = formatSummaryAmount
                    )
                }
                if (canViewIncome || canManageExpense || isAdmin) {
                    item {
                        AnnualTreasuryHeatmap(
                            points = annualTreasury,
                            formatAmount = formatSummaryAmount,
                            onMonthClick = { /* TODO: open month detail */ }
                        )
                    }
                    item {
                        RecommendedActionsCard(
                            recommendations = recommendations,
                            onActionClick = { /* TODO: navigate based on action */ }
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
    subtitle: String? = null,
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
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = DashboardTheme.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            content()
        }
    }
}

/** Charts and month comparisons always read left (older) → right (newer), even in RTL locales. */
@Composable
private fun ChronologicalLtr(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        content()
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

            ChronologicalLtr {
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
                        amount = formatAmount(summary.income),
                        amountColor = DashboardTheme.Income
                    )
                }
                if (showExpenses) {
                    SummaryAmountRow(
                        label = stringResource(R.string.dashboard_summary_expense),
                        amount = formatSignedExpense(summary.expenses, formatAmount),
                        amountColor = DashboardTheme.Expense
                    )
                }
                SummaryAmountRow(
                    label = stringResource(R.string.total),
                    amount = formatAmount(summary.total),
                    bold = true,
                    amountColor = if (summary.total < 0) DashboardTheme.Expense else DashboardTheme.Income
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
    bold: Boolean = false,
    amountColor: Color = DashboardTheme.TextPrimary
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
            color = amountColor,
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
        ChronologicalLtr {
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

    ChronologicalLtr {
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

@Composable
private fun BreakEvenCard(
    summary: BreakEvenSummary,
    formatAmount: (Double) -> String
) {
    val progress = if (summary.targetRevenue > 0) {
        (summary.achievedRevenue / summary.targetRevenue).toFloat().coerceIn(0f, 1f)
    } else {
        1f
    }
    DashboardSectionCard(
        title = stringResource(R.string.dashboard_break_even_title),
        subtitle = stringResource(R.string.dashboard_break_even_subtitle)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (summary.isAchieved) {
                Text(
                    text = stringResource(R.string.dashboard_break_even_achieved),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
                Text(
                    text = stringResource(R.string.dashboard_break_even_additional_margin, formatAmount(summary.additionalMargin)),
                    fontSize = 12.sp,
                    color = DashboardTheme.TextSecondary
                )
            } else {
                Text(
                    text = stringResource(R.string.dashboard_break_even_target, formatAmount(summary.targetRevenue)),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DashboardTheme.TextPrimary
                )
                Text(
                    text = stringResource(R.string.dashboard_break_even_remaining, formatAmount(summary.remainingRevenue)),
                    fontSize = 12.sp,
                    color = Color(0xFFFF9800)
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (summary.isAchieved) Color(0xFF4CAF50) else Color(0xFFFF9800),
                trackColor = DashboardTheme.Track
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = stringResource(R.string.dashboard_break_even_achieved_label, formatAmount(summary.achievedRevenue)),
                    fontSize = 10.sp,
                    color = DashboardTheme.TextSecondary
                )
                Text(
                    text = stringResource(R.string.dashboard_break_even_expenses_label, formatAmount(summary.projectedExpenses)),
                    fontSize = 10.sp,
                    color = DashboardTheme.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun RecommendedActionsCard(
    recommendations: List<TreasuryRecommendation>,
    onActionClick: (TreasuryRecommendation) -> Unit
) {
    if (recommendations.isEmpty()) return
    val severityColor = when (recommendations.maxOf { it.severity }) {
        RecommendationSeverity.CRITICAL -> Color(0xFFB71C1C)
        RecommendationSeverity.SEVERE -> Color(0xFFF44336)
        RecommendationSeverity.MODERATE -> Color(0xFFFF9800)
        RecommendationSeverity.LIGHT -> Color(0xFF4CAF50)
    }
    val severityTitle = when (recommendations.maxOf { it.severity }) {
        RecommendationSeverity.CRITICAL -> "Trésorerie en crise"
        RecommendationSeverity.SEVERE -> "Trésorerie sous tension"
        RecommendationSeverity.MODERATE -> "Attention trésorerie"
        RecommendationSeverity.LIGHT -> "Situation maîtrisée"
    }

    val titleMap = mapOf(
        "credit_financing" to R.string.recommendation_credit_financing,
        "partner_contribution" to R.string.recommendation_partner_contribution,
        "cut_expenses" to R.string.recommendation_cut_expenses,
        "accelerate_collection" to R.string.recommendation_accelerate_collection,
        "cash_sale" to R.string.recommendation_cash_sale,
        "postpone_expenses" to R.string.recommendation_postpone_expenses,
        "send_reminders" to R.string.recommendation_send_reminders,
        "advance_invoice" to R.string.recommendation_advance_invoice,
        "partner_advance" to R.string.recommendation_partner_advance,
        "light_reminder" to R.string.recommendation_light_reminder,
        "healthy_treasury" to R.string.recommendation_healthy_treasury
    )
    val descMap = mapOf(
        "credit_financing" to R.string.recommendation_credit_financing_desc,
        "partner_contribution" to R.string.recommendation_partner_contribution_desc,
        "cut_expenses" to R.string.recommendation_cut_expenses_desc,
        "accelerate_collection" to R.string.recommendation_accelerate_collection_desc,
        "cash_sale" to R.string.recommendation_cash_sale_desc,
        "postpone_expenses" to R.string.recommendation_postpone_expenses_desc,
        "send_reminders" to R.string.recommendation_send_reminders_desc,
        "advance_invoice" to R.string.recommendation_advance_invoice_desc,
        "partner_advance" to R.string.recommendation_partner_advance_desc,
        "light_reminder" to R.string.recommendation_light_reminder_desc,
        "healthy_treasury" to R.string.recommendation_healthy_treasury_desc
    )
    val actionMap = mapOf(
        "view_overdue" to R.string.recommendation_action_view_overdue,
        "contact_bank" to R.string.recommendation_action_contact_bank,
        "ask_associates" to R.string.recommendation_action_ask_associates,
        "review_expenses" to R.string.recommendation_action_review_expenses,
        "add_income" to R.string.recommendation_action_add_income,
        "view_upcoming" to R.string.recommendation_action_view_upcoming,
        "view_invoices" to R.string.recommendation_action_view_invoices,
        "view_forecasts" to R.string.recommendation_action_view_forecasts
    )

    DashboardSectionCard(
        title = stringResource(R.string.dashboard_recommended_actions),
        subtitle = severityTitle
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            recommendations.forEachIndexed { index, recommendation ->
                val color = when (recommendation.severity) {
                    RecommendationSeverity.CRITICAL -> Color(0xFFB71C1C)
                    RecommendationSeverity.SEVERE -> Color(0xFFF44336)
                    RecommendationSeverity.MODERATE -> Color(0xFFFF9800)
                    RecommendationSeverity.LIGHT -> Color(0xFF4CAF50)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(titleMap[recommendation.title] ?: R.string.dashboard_recommended_actions),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DashboardTheme.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(descMap[recommendation.description] ?: R.string.dashboard_recommended_actions),
                            fontSize = 11.sp,
                            color = DashboardTheme.TextSecondary,
                            lineHeight = 16.sp
                        )
                        recommendation.estimateImpact?.let { impact ->
                            Text(
                                text = impact,
                                fontSize = 10.sp,
                                color = color,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    recommendation.actionLabel?.let { actionKey ->
                        val actionRes = actionMap[actionKey]
                        if (actionRes != null) {
                            TextButton(
                                onClick = { onActionClick(recommendation) },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = stringResource(actionRes),
                                    fontSize = 11.sp,
                                    color = color,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                if (index < recommendations.lastIndex) {
                    HorizontalDivider(thickness = 0.5.dp, color = DashboardTheme.Track)
                }
            }
        }
    }
}

@Composable
private fun AnnualTreasuryHeatmap(
    points: List<AnnualTreasuryPoint>,
    formatAmount: (Double) -> String,
    onMonthClick: (AnnualTreasuryPoint) -> Unit
) {
    if (points.isEmpty()) return
    val todayMonth = remember { YearMonth.now() }
    val formatK = remember { { value: Double ->
        val k = value / 1000.0
        val rounded = kotlin.math.round(k).toInt()
        "${rounded}k"
    } }
    val maxAbs = remember(points) {
        points.maxOf { kotlin.math.abs(it.forecastBalance) }.coerceAtLeast(1.0)
    }
    val positiveCount = points.count { it.forecastBalance >= 0 }
    val negativeCount = points.size - positiveCount
    val worstMonth = points.minByOrNull { it.forecastBalance }
    val bestMonth = points.maxByOrNull { it.forecastBalance }

    DashboardSectionCard(
        title = stringResource(R.string.dashboard_annual_treasury),
        subtitle = buildString {
            append("$positiveCount mois positifs · $negativeCount à risque")
        }
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                points.forEach { point ->
                    val isCurrent = point.month == todayMonth
                    val color = when {
                        point.forecastBalance < 0 -> Color(0xFFF44336)
                        point.forecastBalance < maxAbs * 0.15 -> Color(0xFFFF9800)
                        else -> Color(0xFF4CAF50)
                    }
                    val heightFraction = (kotlin.math.abs(point.forecastBalance) / maxAbs)
                        .toFloat()
                        .coerceIn(0.05f, 1f)

                    Column(
                        modifier = Modifier
                            .width(22.dp)
                            .fillMaxHeight()
                            .clickable { onMonthClick(point) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = formatK(point.forecastBalance),
                            fontSize = 7.sp,
                            color = if (point.forecastBalance < 0) Color(0xFFF44336) else DashboardTheme.TextSecondary,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(1.dp))
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .fillMaxHeight(heightFraction)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(color)
                                .then(if (isCurrent) Modifier.border(1.dp, Color(0xFF1976D2), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)) else Modifier)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = point.label.uppercase().take(3),
                            fontSize = 8.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) Color(0xFF1976D2) else DashboardTheme.TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                bestMonth?.let {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Meilleur mois", fontSize = 10.sp, color = DashboardTheme.TextSecondary)
                        Text("${it.label} ${formatK(it.forecastBalance)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4CAF50))
                    }
                }
                worstMonth?.takeIf { it.forecastBalance < 0 }?.let {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Pire mois", fontSize = 10.sp, color = DashboardTheme.TextSecondary)
                        Text("${it.label} ${formatK(it.forecastBalance)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFF44336))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val advice = when {
                    negativeCount >= 3 -> "Plusieurs mois de déficit prévus. Envisagez un apport de trésorerie ou un report de dépenses."
                    negativeCount >= 1 -> "Un déficit est prévu. Relancez les encaissements ou négociez les échéances."
                    positiveCount >= 10 -> "Trésorerie saine sur l'année. Vous pouvez investir ou constituer une réserve."
                    else -> "Situation globalement positive. Surveillez les mois en orange."
                }
                Text(
                    text = advice,
                    fontSize = 11.sp,
                    color = DashboardTheme.TextSecondary,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
