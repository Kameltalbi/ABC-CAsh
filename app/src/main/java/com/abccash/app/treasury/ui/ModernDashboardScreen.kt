package com.abccash.app.treasury.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abccash.app.treasury.data.CategorySlice
import com.abccash.app.treasury.data.DashboardBalancePoint
import com.abccash.app.treasury.data.DashboardCalculations
import com.abccash.app.treasury.data.Expense
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import com.abccash.app.treasury.data.hasPermission
import com.abccash.app.treasury.datastore.UserPreferences
import androidx.compose.ui.res.stringResource
import com.abccash.app.R
import androidx.compose.ui.graphics.drawscope.Stroke
import com.abccash.app.treasury.data.DashboardViewMode
import com.abccash.app.treasury.data.MonthlyBarPoint
import com.abccash.app.locale.AppLocale
import com.abccash.app.treasury.data.EcheanceForecast
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.max

private object ModernDashboardTheme {
    val Background = Color(0xFFF5F7FA)
    val Primary = Color(0xFF1A1A1A)
    val Positive = Color(0xFF22C55E)
    val AccentGreen = Color(0xFF5EE371)
    val Negative = Color(0xFFEF4444)
    val Muted = Color(0xFF94A3B8)
    val Card = Color.White
    val ChartFill = Color(0xFF22C55E).copy(alpha = 0.10f)
    val SectionLabel = Color(0xFF9CA3AF)

    val IncomeColors = listOf(
        Color(0xFF2D5150),
        Color(0xFF4A90E2),
        Color(0xFFF5C344),
        Color(0xFF22C55E)
    )
    val ExpenseColors = listOf(
        Color(0xFF882244),
        Color(0xFFC22E3A),
        Color(0xFFF05E31),
        Color(0xFFF9C03D)
    )
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
    entrepriseId: String?,
    userPreferences: UserPreferences,
    onNavigateToAddIncome: () -> Unit,
    onNavigateToAddExpense: () -> Unit
) {
    val formatAmount = rememberFormatMoney()
    val today = remember { LocalDate.now() }
    var viewMode by remember { mutableStateOf(DashboardViewMode.MONTH) }
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    val bankBalance by userPreferences
        .observeBankBalance(entrepriseId.orEmpty(), selectedMonth.year)
        .collectAsStateWithLifecycle(initialValue = null)

    val canViewIncome = hasPermission(userRole, permissions, UserPermission.VIEW_INVOICES)
    val canManageExpense = hasPermission(userRole, permissions, UserPermission.MANAGE_EXPENSES)
    val visibleInvoices = if (canViewIncome || userRole == UserRole.ADMIN) invoices else emptyList()
    val visibleExpenses = if (canManageExpense || userRole == UserRole.ADMIN) expenses else emptyList()

    val isCurrentPeriod = remember(selectedMonth, viewMode) {
        DashboardCalculations.isCurrentDashboardPeriod(selectedMonth, viewMode, today)
    }

    val data = remember(visibleInvoices, visibleExpenses, bankBalance, selectedMonth, viewMode) {
        DashboardCalculations.buildModernDashboardData(
            invoices = visibleInvoices,
            expenses = visibleExpenses,
            bankBalance = bankBalance,
            focusMonth = selectedMonth,
            viewMode = viewMode
        )
    }

    val balanceKpi = if (isCurrentPeriod) data.forecastBalance30Days else data.displayBalance

    val notificationCount = remember(visibleInvoices, visibleExpenses) {
        EcheanceForecast.buildItems(
            invoices = visibleInvoices,
            expenses = visibleExpenses,
            from = today,
            to = today.plusDays(30)
        ).size
    }

    val expenseWeekTrend = remember(visibleExpenses, isCurrentPeriod) {
        if (isCurrentPeriod) {
            DashboardCalculations.expenseWeekTrendPercent(visibleExpenses, today)
        } else {
            null
        }
    }
    val balance30Trend = remember(data.displayBalance, data.forecastBalance30Days, isCurrentPeriod) {
        if (!isCurrentPeriod || data.displayBalance == 0.0) {
            null
        } else {
            ((data.forecastBalance30Days - data.displayBalance) / abs(data.displayBalance)) * 100.0
        }
    }

    val monthlyBars = remember(visibleInvoices, visibleExpenses, selectedMonth, viewMode) {
        DashboardCalculations.buildMonthlyBarChart(
            invoices = visibleInvoices,
            expenses = visibleExpenses,
            focusMonth = selectedMonth,
            viewMode = viewMode
        )
    }

    val barChartSubtitle = selectedMonth.year.toString()

    val userFallback = stringResource(R.string.user_fallback)
    val displayName = remember(userName, userFallback) {
        userName.trim().ifBlank { userFallback }
    }

    var showTypeSheet by remember { mutableStateOf(false) }
    val isAdmin = userRole == UserRole.ADMIN
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
        containerColor = ModernDashboardTheme.Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
        ) {
            item {
                Box(Modifier.padding(horizontal = 20.dp)) {
                    ModernWelcomeHeader(
                        displayName = displayName,
                        companyName = companyName.ifBlank { stringResource(R.string.company_fallback) },
                        notificationCount = notificationCount
                    )
                }
            }
            item {
                Box(Modifier.padding(horizontal = 20.dp)) {
                    DashboardPeriodSelector(
                        viewMode = viewMode,
                        onViewModeChange = { viewMode = it },
                        selectedMonth = selectedMonth,
                        onMonthChange = { selectedMonth = it }
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (canViewIncome || userRole == UserRole.ADMIN) {
                        WalletMetricCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.income_title),
                            amount = formatAmount(data.incomeTotal),
                            meta = data.monthLabel,
                            shareLabel = if (data.incomeTotal > 0 && data.expenseTotal > 0) {
                                val pct = (data.incomeTotal / (data.incomeTotal + data.expenseTotal) * 100).toInt()
                                "$pct%"
                            } else null,
                            accentColor = ModernDashboardTheme.Positive,
                            icon = Icons.Default.TrendingUp
                        )
                    } else if (canManageExpense || userRole == UserRole.ADMIN) {
                        WalletMetricCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.expense_title),
                            amount = formatAmount(data.expenseTotal),
                            meta = data.monthLabel,
                            shareLabel = expenseWeekTrend?.let { formatTrendPercent(it) },
                            accentColor = ModernDashboardTheme.ExpenseColors[2],
                            icon = Icons.Default.TrendingDown
                        )
                    }
                    WalletMetricCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.forecast_balance),
                        amount = formatAmount(balanceKpi),
                        meta = if (isCurrentPeriod) {
                            stringResource(R.string.forecasts_30_days)
                        } else {
                            data.monthLabel
                        },
                        shareLabel = balance30Trend?.let { formatTrendPercent(it) },
                        accentColor = Color(0xFF4A90E2),
                        icon = Icons.Default.ShowChart
                    )
                }
            }
            if (canAddIncome || canAddExpense) {
                item {
                    Button(
                        onClick = { showTypeSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ModernDashboardTheme.AccentGreen,
                            contentColor = ModernDashboardTheme.Primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.quick_entry),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
            if (canManageExpense || userRole == UserRole.ADMIN) {
                item {
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        SemiDonutBreakdownCard(
                            sectionTitle = stringResource(R.string.dashboard_expense_breakdown),
                            subtitle = data.monthLabel,
                            slices = data.expenseByCategory,
                            total = data.expenseTotal,
                            sliceColors = ModernDashboardTheme.ExpenseColors,
                            centerLabel = stringResource(R.string.expense_title),
                            emptyLabel = stringResource(R.string.no_expense_month),
                            formatAmount = formatAmount
                        )
                    }
                }
            }
            item {
                Box(Modifier.padding(horizontal = 20.dp)) {
                    TreasuryTrendCard(
                        points = data.balanceHistory,
                        expenseTotal = data.expenseTotal,
                        monthLabel = data.monthLabel,
                        formatAmount = formatAmount
                    )
                }
            }
            if (canViewIncome || userRole == UserRole.ADMIN) {
                item {
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        SemiDonutBreakdownCard(
                            sectionTitle = stringResource(R.string.dashboard_income_breakdown),
                            subtitle = data.monthLabel,
                            slices = data.incomeByCategory,
                            total = data.incomeTotal,
                            sliceColors = ModernDashboardTheme.IncomeColors,
                            centerLabel = stringResource(R.string.income_title),
                            emptyLabel = stringResource(R.string.no_income_month),
                            formatAmount = formatAmount
                        )
                    }
                }
            }
            if (canViewIncome || canManageExpense || userRole == UserRole.ADMIN) {
                item {
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        MonthlyComparisonBarCard(
                            bars = monthlyBars,
                            subtitle = barChartSubtitle,
                            showIncome = canViewIncome || userRole == UserRole.ADMIN,
                            showExpenses = canManageExpense || userRole == UserRole.ADMIN
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ModernDashboardTheme.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        color = ModernDashboardTheme.SectionLabel
    )
}

@Composable
private fun ModernWelcomeHeader(
    displayName: String,
    companyName: String,
    notificationCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.dashboard_welcome_back),
                fontSize = 13.sp,
                color = ModernDashboardTheme.Muted
            )
            Text(
                text = displayName,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = ModernDashboardTheme.Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = companyName,
                fontSize = 12.sp,
                color = ModernDashboardTheme.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BadgedBox(
                badge = {
                    if (notificationCount > 0) {
                        Badge(containerColor = ModernDashboardTheme.Negative) {
                            Text(
                                text = notificationCount.coerceAtMost(99).toString(),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = stringResource(R.string.dashboard_notifications),
                    tint = ModernDashboardTheme.Primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(ModernDashboardTheme.AccentGreen.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = ModernDashboardTheme.Primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun WalletMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: String,
    meta: String,
    shareLabel: String?,
    accentColor: Color,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ModernDashboardTheme.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ModernDashboardTheme.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(accentColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = meta,
                fontSize = 10.sp,
                color = ModernDashboardTheme.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = amount,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = ModernDashboardTheme.Primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            shareLabel?.let { label ->
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ModernDashboardTheme.Positive
                )
            }
        }
    }
}

@Composable
private fun SemiDonutBreakdownCard(
    sectionTitle: String,
    subtitle: String,
    slices: List<CategorySlice>,
    total: Double,
    sliceColors: List<Color>,
    centerLabel: String,
    emptyLabel: String,
    formatAmount: (Double) -> String
) {
    DashboardCard {
        SectionTitle(sectionTitle)
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = ModernDashboardTheme.Muted
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            val hasData = slices.isNotEmpty() && slices.any { it.amount > 0 }
            SemiDonutChart(
                slices = if (hasData) slices else emptyList(),
                colors = sliceColors,
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .height(120.dp),
                emptyRing = !hasData
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = centerLabel,
                    fontSize = 11.sp,
                    color = ModernDashboardTheme.Muted
                )
                Text(
                    text = if (hasData) formatAmount(total) else "—",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ModernDashboardTheme.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        ModernBreakdownLegendGrid(
            slices = slices,
            colors = sliceColors,
            emptyLabel = emptyLabel,
            formatAmount = formatAmount
        )
    }
}

@Composable
private fun ModernBreakdownLegendGrid(
    slices: List<CategorySlice>,
    colors: List<Color>,
    emptyLabel: String,
    formatAmount: (Double) -> String
) {
    val items = slices.take(4)
    if (items.isEmpty()) {
        Text(emptyLabel, fontSize = 12.sp, color = ModernDashboardTheme.Muted)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { slice ->
                    val colorIndex = slices.indexOf(slice)
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .width(14.dp)
                                .height(3.dp)
                                .background(colors[colorIndex % colors.size], RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(
                                text = localizedCategoryLabel(slice),
                                fontSize = 11.sp,
                                color = ModernDashboardTheme.Primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatAmount(slice.amount),
                                fontSize = 10.sp,
                                color = ModernDashboardTheme.Muted
                            )
                        }
                    }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TreasuryTrendCard(
    points: List<DashboardBalancePoint>,
    expenseTotal: Double,
    monthLabel: String,
    formatAmount: (Double) -> String
) {
    DashboardCard {
        SectionTitle(stringResource(R.string.dashboard_treasury_forecasts))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(0.42f)) {
                Text(
                    text = stringResource(R.string.expense_title),
                    fontSize = 12.sp,
                    color = ModernDashboardTheme.Muted
                )
                Text(
                    text = formatAmount(expenseTotal),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ModernDashboardTheme.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = monthLabel,
                    fontSize = 11.sp,
                    color = ModernDashboardTheme.Muted
                )
                Spacer(Modifier.height(8.dp))
                TrendLegendRow(
                    color = ModernDashboardTheme.Positive,
                    label = stringResource(R.string.your_treasury)
                )
                Spacer(Modifier.height(6.dp))
                TrendLegendRow(
                    color = ModernDashboardTheme.Positive.copy(alpha = 0.45f),
                    label = stringResource(R.string.forecasts_30_days)
                )
            }
            if (points.size < 2) {
                Box(
                    modifier = Modifier
                        .weight(0.58f)
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.not_enough_data), color = ModernDashboardTheme.Muted, fontSize = 12.sp)
                }
            } else {
                Column(modifier = Modifier.weight(0.58f)) {
                    ForecastBalanceLineChart(
                        points = points,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        forecastAxisLabels().forEach { label ->
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                color = ModernDashboardTheme.Muted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendLegendRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(3.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(6.dp))
        Text(text = label, fontSize = 10.sp, color = ModernDashboardTheme.Muted, maxLines = 1)
    }
}

@Composable
private fun SemiDonutChart(
    slices: List<CategorySlice>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    emptyRing: Boolean = false
) {
    val displaySlices = remember(slices, emptyRing) {
        when {
            emptyRing -> listOf(CategorySlice(amount = 1.0))
            slices.isEmpty() -> listOf(CategorySlice(amount = 1.0))
            else -> slices
        }
    }
    val ringColor = if (emptyRing) ModernDashboardTheme.Muted.copy(alpha = 0.2f) else null
    val total = displaySlices.sumOf { it.amount }.coerceAtLeast(1.0)

    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.14f
        val diameter = size.width.coerceAtMost(size.height * 2f) - stroke
        val topLeft = Offset((size.width - diameter) / 2f, size.height - diameter / 2f - stroke / 2f)
        val arcSize = Size(diameter, diameter)

        if (emptyRing) {
            drawArc(
                color = ringColor!!,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            return@Canvas
        }

        var startAngle = 180f
        displaySlices.forEachIndexed { index, slice ->
            val sweep = (slice.amount / total * 180f).toFloat()
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            startAngle += sweep
        }
    }
}

private fun formatTrendPercent(value: Double): String {
    val sign = if (value >= 0) "+" else ""
    return "$sign${"%.1f".format(value)}%"
}

@Composable
private fun MonthlyComparisonBarCard(
    bars: List<MonthlyBarPoint>,
    subtitle: String,
    showIncome: Boolean,
    showExpenses: Boolean
) {
    val incomeColor = ModernDashboardTheme.Positive
    val expenseColor = Color(0xFFF05E31)
    val hasData = bars.any { (showIncome && it.income > 0) || (showExpenses && it.expenses > 0) }

    DashboardCard {
        SectionTitle(stringResource(R.string.dashboard_monthly_bars_title))
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = ModernDashboardTheme.Muted
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showIncome) {
                BarChartLegendItem(
                    color = incomeColor,
                    label = stringResource(R.string.income_title)
                )
            }
            if (showExpenses) {
                BarChartLegendItem(
                    color = expenseColor,
                    label = stringResource(R.string.expense_title)
                )
            }
        }
        if (!hasData) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.not_enough_data), color = ModernDashboardTheme.Muted)
            }
        } else {
            MonthlyGroupedBarChart(
                bars = bars,
                showIncome = showIncome,
                showExpenses = showExpenses,
                incomeColor = incomeColor,
                expenseColor = expenseColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                bars.forEach { point ->
                    Text(
                        text = AppLocale.shortMonth(point.month.atDay(1)),
                        style = MaterialTheme.typography.labelSmall,
                        color = ModernDashboardTheme.Muted,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BarChartLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ModernDashboardTheme.Muted
        )
    }
}

@Composable
private fun MonthlyGroupedBarChart(
    bars: List<MonthlyBarPoint>,
    showIncome: Boolean,
    showExpenses: Boolean,
    incomeColor: Color,
    expenseColor: Color,
    modifier: Modifier = Modifier
) {
    val maxValue = bars.maxOf { point ->
        max(
            if (showIncome) point.income else 0.0,
            if (showExpenses) point.expenses else 0.0
        )
    }.coerceAtLeast(1.0)

    Canvas(modifier = modifier) {
        val groupCount = bars.size.coerceAtLeast(1)
        val padTop = 12f
        val padBottom = 4f
        val chartHeight = size.height - padTop - padBottom
        val groupWidth = size.width / groupCount
        val barGap = 3f
        val barsInGroup = listOfNotNull(
            if (showIncome) "income" else null,
            if (showExpenses) "expense" else null
        ).size.coerceAtLeast(1)
        val barWidth = (groupWidth * 0.65f - barGap * (barsInGroup - 1)) / barsInGroup

        bars.forEachIndexed { index, point ->
            val groupStart = index * groupWidth + groupWidth * 0.175f
            var barOffset = 0f

            if (showIncome) {
                val height = (point.income / maxValue * chartHeight).toFloat().coerceAtLeast(2f)
                drawRoundRect(
                    color = incomeColor,
                    topLeft = Offset(groupStart + barOffset, padTop + chartHeight - height),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                barOffset += barWidth + barGap
            }
            if (showExpenses) {
                val height = (point.expenses / maxValue * chartHeight).toFloat().coerceAtLeast(2f)
                drawRoundRect(
                    color = expenseColor,
                    topLeft = Offset(groupStart + barOffset, padTop + chartHeight - height),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }
    }
}

@Composable
private fun forecastAxisLabels(): List<String> = listOf(
    stringResource(R.string.dashboard_today),
    stringResource(R.string.dashboard_plus_days, 7),
    stringResource(R.string.dashboard_plus_days, 14),
    stringResource(R.string.dashboard_plus_days, 21),
    stringResource(R.string.dashboard_plus_days, 30)
)

@Composable
private fun ForecastBalanceLineChart(
    points: List<DashboardBalancePoint>,
    modifier: Modifier = Modifier
) {
    val balances = points.map { it.balance }
    val minY = balances.minOrNull() ?: 0.0
    val maxY = balances.maxOrNull() ?: 1.0
    val range = max(maxY - minY, 1.0)
    val todayIndex = points.indexOfLast { !it.isForecast }.coerceAtLeast(0)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padTop = 8f
        val padBottom = 8f
        val chartH = h - padTop - padBottom

        fun xAt(index: Int): Float =
            if (points.size <= 1) w / 2f else index.toFloat() / (points.size - 1) * w

        fun yAt(value: Double): Float {
            val ratio = ((value - minY) / range).toFloat()
            return padTop + chartH * (1f - ratio)
        }

        fun buildPath(from: Int, to: Int): Path = Path().apply {
            for (i in from..to) {
                val x = xAt(i)
                val y = yAt(points[i].balance)
                if (i == from) moveTo(x, y) else {
                    val prevX = xAt(i - 1)
                    val prevY = yAt(points[i - 1].balance)
                    quadraticBezierTo((prevX + x) / 2f, prevY, x, y)
                }
            }
        }

        val historyPath = buildPath(0, todayIndex)
        val forecastPath = if (todayIndex < points.lastIndex) {
            buildPath(todayIndex, points.lastIndex)
        } else {
            null
        }

        val fillPath = Path().apply {
            addPath(historyPath)
            if (forecastPath != null) addPath(forecastPath)
            lineTo(xAt(points.lastIndex), h)
            lineTo(xAt(0), h)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(ModernDashboardTheme.ChartFill, Color.Transparent),
                startY = padTop,
                endY = h
            )
        )

        drawPath(
            path = historyPath,
            color = ModernDashboardTheme.Positive,
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )
        forecastPath?.let { path ->
            drawPath(
                path = path,
                color = ModernDashboardTheme.Positive,
                style = Stroke(
                    width = 3f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )
            )
        }
    }
}
