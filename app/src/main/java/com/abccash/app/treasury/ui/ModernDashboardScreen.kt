package com.abccash.app.treasury.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.abccash.app.ui.theme.AppColors
import com.abccash.app.treasury.data.DashboardViewMode
import com.abccash.app.treasury.data.MonthlyBarPoint
import com.abccash.app.locale.AppLocale
import com.abccash.app.treasury.data.EcheanceForecast
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.max

private object ModernDashboardTheme {
    val Background = Color(0xFFF8F9FA)
    val Primary = Color(0xFF1E293B)
    val Positive = Color(0xFF10B981)
    val Negative = Color(0xFFEF4444)
    val Muted = Color(0xFF64748B)
    val Card = Color.White
    val ChartFill = Color(0xFF10B981).copy(alpha = 0.12f)

    val IncomeColors = listOf(
        Color(0xFF10B981),
        Color(0xFF059669),
        Color(0xFF34D399),
        Color(0xFF0EA5E9)
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

    val outflowsKpi = remember(data, isCurrentPeriod) {
        when {
            !isCurrentPeriod -> data.expensePaidTotal
            data.expensePendingTotal > 0 -> data.expensePendingTotal
            else -> data.forecastExpenses
        }
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
        containerColor = ModernDashboardTheme.Background,
        floatingActionButton = {
            // Prêt à déclencher l'ouverture du formulaire de saisie rapide.
            if (canAddIncome || canAddExpense) {
                FloatingActionButton(
                    onClick = { showTypeSheet = true },
                    containerColor = ModernDashboardTheme.Primary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.quick_entry))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
        ) {
            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    DashboardHeaderBar(
                        displayName = displayName,
                        notificationCount = notificationCount
                    )
                }
            }
            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
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
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.dashboard_planned_outflows),
                        amount = formatAmount(outflowsKpi),
                        trendPercent = expenseWeekTrend,
                        trendLabel = expenseWeekTrend?.let {
                            stringResource(
                                R.string.dashboard_trend_week,
                                formatTrendPercent(it)
                            )
                        },
                        iconTint = ModernDashboardTheme.Positive,
                        waveColor = ModernDashboardTheme.Positive.copy(alpha = 0.08f),
                        icon = { Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color.White) }
                    )
                    KpiSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.forecast_balance),
                        amount = formatAmount(balanceKpi),
                        trendPercent = balance30Trend,
                        trendLabel = balance30Trend?.let {
                            stringResource(
                                R.string.dashboard_trend_30d,
                                formatTrendPercent(it)
                            )
                        },
                        iconTint = Color(0xFF2563EB),
                        waveColor = Color(0xFF2563EB).copy(alpha = 0.08f),
                        icon = { Icon(Icons.Default.ShowChart, contentDescription = null, tint = Color.White) }
                    )
                }
            }
            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    TreasuryForecastCard(
                        points = data.balanceHistory,
                        formatAmount = formatAmount
                    )
                }
            }
            if (canManageExpense || userRole == UserRole.ADMIN) {
                item {
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        CategoryBreakdownCard(
                            title = stringResource(R.string.dashboard_expense_breakdown),
                            subtitle = data.monthLabel,
                            slices = data.expenseByCategory,
                            total = data.expenseTotal,
                            sliceColors = ModernDashboardTheme.ExpenseColors,
                            emptyLabel = stringResource(R.string.no_expense_month),
                            formatAmount = formatAmount
                        )
                    }
                }
            }
            if (canViewIncome || userRole == UserRole.ADMIN) {
                item {
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        CategoryBreakdownCard(
                            title = stringResource(R.string.dashboard_income_breakdown),
                            subtitle = data.monthLabel,
                            slices = data.incomeByCategory,
                            total = data.incomeTotal,
                            sliceColors = ModernDashboardTheme.IncomeColors,
                            emptyLabel = stringResource(R.string.no_income_month),
                            formatAmount = formatAmount
                        )
                    }
                }
            }
            if (canViewIncome || canManageExpense || userRole == UserRole.ADMIN) {
                item {
                    Box(Modifier.padding(horizontal = 16.dp)) {
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ModernDashboardTheme.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun DashboardHeaderBar(
    displayName: String,
    notificationCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BadgedBox(
            badge = {
                if (notificationCount > 0) {
                    Badge(containerColor = ModernDashboardTheme.Positive) {
                        Text(
                            text = notificationCount.coerceAtMost(99).toString(),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = stringResource(R.string.dashboard_notifications),
                tint = ModernDashboardTheme.Primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            tint = ModernDashboardTheme.Primary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = displayName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = ModernDashboardTheme.Primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = ModernDashboardTheme.Muted
        )
    }
}

@Composable
private fun KpiSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: String,
    trendPercent: Double?,
    trendLabel: String?,
    iconTint: Color,
    waveColor: Color,
    icon: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ModernDashboardTheme.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, waveColor)
                        )
                    )
            )
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconTint, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = ModernDashboardTheme.Muted
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ModernDashboardTheme.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (trendLabel != null && trendPercent != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (trendPercent >= 0) {
                                Icons.Default.TrendingUp
                            } else {
                                Icons.Default.TrendingDown
                            },
                            contentDescription = null,
                            tint = ModernDashboardTheme.Positive,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = trendLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = ModernDashboardTheme.Positive
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TreasuryForecastCard(
    points: List<DashboardBalancePoint>,
    formatAmount: (Double) -> String
) {
    DashboardCard {
        Text(
            text = stringResource(R.string.dashboard_treasury_forecasts),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = ModernDashboardTheme.Primary
        )
        Text(
            text = stringResource(R.string.forecasts_30_days),
            style = MaterialTheme.typography.bodySmall,
            color = ModernDashboardTheme.Muted
        )

        if (points.size < 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.not_enough_data), color = ModernDashboardTheme.Muted)
            }
        } else {
            ForecastBalanceLineChart(
                points = points,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                forecastAxisLabels().forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = ModernDashboardTheme.Muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            val lastForecast = points.lastOrNull { it.isForecast } ?: points.last()
            Text(
                text = "${stringResource(R.string.dashboard_plus_days, 30)} · ${formatAmount(lastForecast.balance)}",
                style = MaterialTheme.typography.labelSmall,
                color = ModernDashboardTheme.Muted
            )
        }
    }
}

@Composable
private fun CategoryBreakdownCard(
    title: String,
    subtitle: String,
    slices: List<CategorySlice>,
    total: Double,
    sliceColors: List<Color>,
    emptyLabel: String,
    formatAmount: (Double) -> String
) {
    DashboardCard {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = ModernDashboardTheme.Primary
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = ModernDashboardTheme.Muted
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.weight(0.9f),
                contentAlignment = Alignment.Center
            ) {
                val hasData = slices.isNotEmpty() && slices.any { it.amount > 0 }
                DonutChart(
                    slices = if (hasData) slices else emptyList(),
                    colors = sliceColors,
                    modifier = Modifier.size(120.dp),
                    emptyRing = !hasData
                )
            }
            CategoryBreakdownLegend(
                modifier = Modifier.weight(1.1f),
                slices = slices,
                total = total,
                colors = sliceColors,
                emptyLabel = emptyLabel,
                formatAmount = formatAmount
            )
        }
    }
}

@Composable
private fun CategoryBreakdownLegend(
    modifier: Modifier = Modifier,
    slices: List<CategorySlice>,
    total: Double,
    colors: List<Color>,
    emptyLabel: String,
    formatAmount: (Double) -> String
) {
    val items = slices.take(4)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (items.isEmpty()) {
            Text(emptyLabel, style = MaterialTheme.typography.labelSmall, color = ModernDashboardTheme.Muted)
            return
        }
        items.forEachIndexed { index, slice ->
            val percent = if (total > 0) (slice.amount / total * 100).toInt() else 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(colors[index % colors.size], CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = localizedCategoryLabel(slice),
                    style = MaterialTheme.typography.labelSmall,
                    color = ModernDashboardTheme.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = ModernDashboardTheme.Muted,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.End
                )
                Text(
                    text = formatAmount(slice.amount),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = ModernDashboardTheme.Primary,
                    modifier = Modifier.width(72.dp),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
        Text(
            text = stringResource(R.string.dashboard_monthly_bars_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = ModernDashboardTheme.Primary
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
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
private fun DonutChart(
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
        val stroke = size.minDimension * 0.13f
        val diameter = size.minDimension - stroke
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)

        if (emptyRing) {
            drawArc(
                color = ringColor!!,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            return@Canvas
        }

        var startAngle = -90f
        displaySlices.forEachIndexed { index, slice ->
            val sweep = (slice.amount / total * 360f).toFloat()
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            startAngle += sweep
        }
    }
}

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
