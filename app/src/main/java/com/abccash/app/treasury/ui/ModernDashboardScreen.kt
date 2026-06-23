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
    val dailyExpenses = remember(visibleExpenses, today) {
        DashboardCalculations.buildDailyExpensesLast7Days(visibleExpenses, today)
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
        bankAccounts.firstOrNull { it.isDefault }?.id ?: bankAccounts.firstOrNull()?.id
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
                title = stringResource(R.string.dashboard_overview_title),
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
                            formatAmount = formatAmount,
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
                        formatAmount = formatAmount,
                        onAddAccount = onNavigateToBankAccounts
                    )
                }
                if (canManageExpense || isAdmin) {
                    item {
                        WeeklyExpensesCard(
                            bars = dailyExpenses,
                            formatAmount = formatAmount
                        )
                    }
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
    title: String,
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
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DrawerMenuIconButton(onClick = onOpenDrawer, tint = Color.White)
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
    DashboardSectionCard(title = stringResource(R.string.dashboard_summary)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MonthSummaryColumn(
                modifier = Modifier.weight(1f),
                summary = current,
                formatAmount = formatAmount,
                showIncome = showIncome,
                showExpenses = showExpenses
            )
            MonthSummaryColumn(
                modifier = Modifier.weight(1f),
                summary = previous,
                formatAmount = formatAmount,
                showIncome = showIncome,
                showExpenses = showExpenses
            )
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
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = summary.label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = DashboardTheme.TextPrimary,
            textAlign = TextAlign.Center
        )
        SemiDonutChart(
            income = if (showIncome) summary.income else 0.0,
            expenses = if (showExpenses) summary.expenses else 0.0,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        )
        if (showIncome) {
            SummaryAmountRow(
                label = stringResource(R.string.income_title),
                amount = formatAmount(summary.income),
                color = DashboardTheme.Income
            )
        }
        if (showExpenses) {
            SummaryAmountRow(
                label = stringResource(R.string.expense_title),
                amount = "- ${formatAmount(summary.expenses)}",
                color = DashboardTheme.Expense
            )
        }
        HorizontalDivider(color = DashboardTheme.Track)
        SummaryAmountRow(
            label = stringResource(R.string.total),
            amount = formatAmount(summary.total),
            color = if (summary.total >= 0) DashboardTheme.Income else DashboardTheme.Expense,
            bold = true
        )
    }
}

@Composable
private fun SummaryAmountRow(
    label: String,
    amount: String,
    color: Color,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = DashboardTheme.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = amount,
            fontSize = if (bold) 13.sp else 12.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = color,
            maxLines = 1
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
    Canvas(modifier = modifier) {
        val stroke = 14f
        val diameter = size.minDimension - stroke
        val topLeft = Offset((size.width - diameter) / 2f, size.height - diameter / 2f)
        val arcSize = Size(diameter, diameter)
        val startAngle = 180f
        val sweepIncome = (income / total * 180f).toFloat()
        val sweepExpense = (expenses / total * 180f).toFloat()

        drawArc(
            color = DashboardTheme.Track,
            startAngle = startAngle,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        if (sweepIncome > 0f) {
            drawArc(
                color = DashboardTheme.Income,
                startAngle = startAngle,
                sweepAngle = sweepIncome,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        if (sweepExpense > 0f) {
            drawArc(
                color = DashboardTheme.Expense,
                startAngle = startAngle + sweepIncome,
                sweepAngle = sweepExpense,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
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
                    defaultAccountId
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
            color = amountColor
        )
    }
}

@Composable
private fun WeeklyExpensesCard(
    bars: List<DailyExpenseBar>,
    formatAmount: (Double) -> String
) {
    DashboardSectionCard(title = stringResource(R.string.dashboard_expenses_7d)) {
        val maxAmount = bars.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
        val hasData = bars.any { it.amount > 0 }

        if (!hasData) {
            Text(
                text = stringResource(R.string.not_enough_data),
                fontSize = 13.sp,
                color = DashboardTheme.TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                textAlign = TextAlign.Center
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                bars.forEach { bar ->
                    val heightFraction = (bar.amount / maxAmount).toFloat().coerceIn(0.04f, 1f)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (bar.amount > 0) {
                            Text(
                                text = formatAmount(bar.amount),
                                fontSize = 9.sp,
                                color = DashboardTheme.TextSecondary,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                        } else {
                            Spacer(Modifier.height(18.dp))
                        }
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .fillMaxHeight(heightFraction)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(DashboardTheme.Expense)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = bar.label,
                            fontSize = 11.sp,
                            color = DashboardTheme.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
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
