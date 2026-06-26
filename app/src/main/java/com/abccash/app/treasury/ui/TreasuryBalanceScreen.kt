package com.abccash.app.treasury.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.abccash.app.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.locale.AppLocale
import com.abccash.app.treasury.data.BankAccount
import com.abccash.app.treasury.data.EcheanceForecast
import com.abccash.app.treasury.data.EcheanceItem
import com.abccash.app.treasury.data.EcheanceType
import com.abccash.app.treasury.data.Expense
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.data.TreasuryCalculations
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import com.abccash.app.treasury.data.hasPermission
import com.abccash.app.ui.theme.AppColors
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.max
import kotlin.math.min

private object TreasuryScreenTheme {
    val Background = Color.White
    val Primary = Color(0xFF1E293B)
    val Positive = Color(0xFF10B981)
    val Negative = Color(0xFFEF4444)
    val Accent = Color(0xFFF05E31)
    val Muted = Color(0xFF64748B)
    val Card = Color.White
    val Grid = Color(0xFFE2E8F0)
    val Line = Color(0xFF1E293B)

    // Per-rubric accents (derived from green / red families)
    val RealizedAccent = Color(0xFF0D9488)  // teal green
    val ForecastAccent = Color(0xFF10B981)  // emerald green
    val IncomeAccent = Color(0xFF16A34A)    // leaf green
    val ExpenseAccent = Color(0xFFEF4444)   // red
}

@Composable
fun TreasuryBalanceScreen(
    userRole: UserRole,
    permissions: Set<UserPermission>,
    invoices: List<Invoice>,
    expenses: List<Expense>,
    bankAccounts: List<BankAccount> = emptyList(),
    onExportCsv: (Int) -> String?,
    onNavigateToBankReconciliation: () -> Unit,
    onOpenDrawer: () -> Unit = {}
) {
    if (!hasPermission(userRole, permissions, UserPermission.VIEW_TREASURY)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TreasuryScreenTheme.Background),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(32.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = TreasuryScreenTheme.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🔐", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.access_denied),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TreasuryScreenTheme.Negative
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.no_treasury_permission),
                        fontSize = 14.sp,
                        color = TreasuryScreenTheme.Muted
                    )
                }
            }
        }
        return
    }

    val context = LocalContext.current
    val displayYear = remember { YearMonth.now().year }
    val today = remember { LocalDate.now() }
    var pendingCsv by remember { mutableStateOf<String?>(null) }
    val canManageBank = userRole == UserRole.ADMIN ||
        hasPermission(userRole, permissions, UserPermission.MANAGE_EXPENSES)

    val formatWhole = rememberFormatMoneyWhole()
    val formatChartAmount = rememberFormatTreasuryChartAmount()
    val shortDateFormatter = remember { AppLocale.shortDayMonthYearFormatter() }

    val yearTotals = remember(invoices, expenses, bankAccounts, displayYear) {
        val openingBalance = TreasuryCalculations.manualOpeningBalance(bankAccounts)
        val rows = TreasuryCalculations.yearlyRows(
            invoices = invoices,
            expenses = expenses,
            year = displayYear,
            openingBalance = openingBalance
        )
        val yearEndForecast = rows.lastOrNull()?.forecastBalance ?: openingBalance
        TreasuryYearTotals(
            collected = TreasuryCalculations.yearlyCollections(invoices, displayYear),
            pendingIncome = TreasuryCalculations.yearlyPendingIncome(invoices, displayYear),
            expenses = TreasuryCalculations.yearlyPaidExpenses(expenses, displayYear),
            pendingExpenses = TreasuryCalculations.yearlyPendingExpenses(expenses, displayYear),
            balance = yearEndForecast,
            openingFromAccounts = openingBalance,
            rows = rows
        )
    }
    val upcomingItems = remember(invoices, expenses, today) {
        EcheanceForecast.buildItems(
            invoices = invoices,
            expenses = expenses,
            from = today,
            to = today.plusDays(90)
        ).sortedBy { it.dueDate }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        val csv = pendingCsv
        if (uri != null && csv != null) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(csv.toByteArray(Charsets.UTF_8))
            }
        }
        pendingCsv = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TreasuryScreenTheme.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TreasuryHeader(
            year = displayYear,
            canManageBank = canManageBank,
            onBankClick = onNavigateToBankReconciliation,
            onExportClick = {
                val csv = onExportCsv(displayYear) ?: return@TreasuryHeader
                pendingCsv = csv
                exportLauncher.launch("abc-cash-$displayYear.csv")
            },
            onOpenDrawer = onOpenDrawer
        )

        val opening = yearTotals.openingFromAccounts
        val realizedMonthlyBalances = remember(yearTotals.rows, opening) {
            var running = opening
            yearTotals.rows.map { row ->
                running += row.collected - row.expenses
                running
            }
        }
        val lastTxIndex = yearTotals.rows.indexOfLast { it.collected != 0.0 || it.expenses != 0.0 }
        val lastActivityIndex = yearTotals.rows.indexOfLast {
            it.totalIncome != 0.0 || it.totalExpenses != 0.0
        }
        val totalAccountsBalance = if (lastTxIndex >= 0) realizedMonthlyBalances[lastTxIndex] else opening
        val forecastBalance = if (lastActivityIndex >= 0) yearTotals.rows[lastActivityIndex].forecastBalance else opening
        val totalAccountsMonth = lastTxIndex.takeIf { it >= 0 }?.let { yearTotals.rows[it].month }
        val forecastMonth = lastActivityIndex.takeIf { it >= 0 }?.let { yearTotals.rows[it].month }

        val totalAccountsColor = if (totalAccountsBalance >= 0) TreasuryScreenTheme.RealizedAccent else TreasuryScreenTheme.Negative
        val forecastColor = if (forecastBalance >= 0) TreasuryScreenTheme.ForecastAccent else TreasuryScreenTheme.Negative

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TreasuryKpiCard(
                title = stringResource(R.string.treasury_total_accounts_balance),
                amount = formatWhole(totalAccountsBalance),
                subtitle = totalAccountsMonth?.let {
                    stringResource(R.string.treasury_balance_as_of, AppLocale.monthYear(it))
                } ?: stringResource(R.string.treasury_total_accounts_balance_hint),
                amountColor = totalAccountsColor,
                accent = totalAccountsColor,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            TreasuryKpiCard(
                title = stringResource(R.string.forecast_balance),
                amount = formatWhole(forecastBalance),
                subtitle = forecastMonth?.let {
                    stringResource(R.string.treasury_balance_as_of, AppLocale.monthYear(it))
                } ?: stringResource(R.string.treasury_total_accounts_balance_hint),
                amountColor = forecastColor,
                accent = forecastColor,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TreasuryKpiCard(
                title = stringResource(R.string.collections),
                amount = formatWhole(yearTotals.collected + yearTotals.pendingIncome),
                subtitle = if (yearTotals.pendingIncome > 0) {
                    stringResource(R.string.treasury_upcoming_part, formatWhole(yearTotals.pendingIncome))
                } else {
                    null
                },
                amountColor = TreasuryScreenTheme.IncomeAccent,
                accent = TreasuryScreenTheme.IncomeAccent,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            TreasuryKpiCard(
                title = stringResource(R.string.expenses),
                amount = formatWhole(yearTotals.expenses + yearTotals.pendingExpenses),
                subtitle = if (yearTotals.pendingExpenses > 0) {
                    stringResource(R.string.treasury_upcoming_part, formatWhole(yearTotals.pendingExpenses))
                } else {
                    null
                },
                amountColor = TreasuryScreenTheme.ExpenseAccent,
                accent = TreasuryScreenTheme.ExpenseAccent,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        val todayYearMonth = remember { YearMonth.now() }
        val splitIndex = remember(yearTotals.rows, todayYearMonth) {
            yearTotals.rows.indexOfLast { it.month <= todayYearMonth }
        }
        // Une seule courbe continue : réalisé jusqu'au mois courant, forecast après
        val chartBalances = remember(yearTotals.rows, splitIndex, todayYearMonth) {
            yearTotals.rows.mapIndexed { index, row ->
                if (index <= splitIndex) row.collected - row.expenses
                else row.totalIncome - row.totalExpenses
            }
        }

        TreasuryTimelineChart(
            rows = yearTotals.rows,
            balances = chartBalances,
            splitIndex = splitIndex,
            formatChartAmount = formatChartAmount
        )

        TreasuryUpcomingSection(
            items = upcomingItems,
            formatAmount = rememberFormatMoney(),
            dateFormatter = shortDateFormatter,
            today = today
        )

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun TreasuryHeader(
    year: Int,
    canManageBank: Boolean,
    onBankClick: () -> Unit,
    onExportClick: () -> Unit,
    onOpenDrawer: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.treasury_year_title, year),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TreasuryScreenTheme.Primary
                )
                Text(
                    text = stringResource(R.string.treasury_year_range),
                    fontSize = 14.sp,
                    color = TreasuryScreenTheme.Muted
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (canManageBank) {
                IconButton(onClick = onBankClick) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = stringResource(R.string.bank_reconciliation_cd),
                        tint = TreasuryScreenTheme.Primary
                    )
                }
            }
            IconButton(onClick = onExportClick) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = stringResource(R.string.export_csv),
                    tint = TreasuryScreenTheme.Primary
                )
            }
        }
    }
}

@Composable
private fun TreasuryKpiCard(
    title: String,
    amount: String,
    subtitle: String? = null,
    amountColor: Color,
    accent: Color = amountColor,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TreasuryScreenTheme.Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = amount,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.let {
                Text(
                    text = it,
                    fontSize = 9.sp,
                    color = TreasuryScreenTheme.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TreasuryTimelineChart(
    rows: List<TreasuryCalculations.MonthlyTreasuryRow>,
    balances: List<Double>,
    splitIndex: Int,
    formatChartAmount: (Double) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TreasuryScreenTheme.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.treasury_month_end_balance),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TreasuryScreenTheme.Primary
            )
            Text(
                text = stringResource(R.string.treasury_monthly_balance_hint),
                fontSize = 11.sp,
                color = TreasuryScreenTheme.Muted
            )

            val now = YearMonth.now()
            val minY = min(balances.minOrNull() ?: 0.0, 0.0)
            val maxY = max(balances.maxOrNull() ?: 0.0, 0.0)
            val range = max(maxY - minY, 1.0)
            val yAxisWidth = 34.dp
            val chartHeight = 140.dp

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Spacer(modifier = Modifier.width(yAxisWidth))
                balances.forEachIndexed { i, balance ->
                    val isForecast = i > splitIndex
                    Text(
                        text = formatChartAmount(balance),
                        modifier = Modifier.weight(1f),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            balance < 0 -> TreasuryScreenTheme.Negative
                            isForecast -> TreasuryScreenTheme.ForecastAccent
                            else -> AppColors.BrandBlue
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .width(yAxisWidth)
                        .height(chartHeight),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    for (i in 0..3) {
                        val tickValue = maxY - range * i / 3.0
                        Text(
                            text = formatChartAmount(tickValue),
                            fontSize = 8.sp,
                            color = TreasuryScreenTheme.Muted,
                            textAlign = TextAlign.End,
                            maxLines = 1
                        )
                    }
                }
                TreasuryDualLineChart(
                    balances = balances,
                    splitIndex = splitIndex,
                    minY = minY,
                    maxY = maxY,
                    modifier = Modifier
                        .weight(1f)
                        .height(chartHeight)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Spacer(modifier = Modifier.width(yAxisWidth))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Canvas(modifier = Modifier.size(width = 14.dp, height = 2.dp)) {
                        drawLine(
                            color = AppColors.BrandBlue,
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                    Text(
                        text = stringResource(R.string.treasury_chart_realized),
                        fontSize = 8.sp,
                        color = TreasuryScreenTheme.Muted
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Canvas(modifier = Modifier.size(width = 14.dp, height = 2.dp)) {
                        drawLine(
                            color = TreasuryScreenTheme.ForecastAccent,
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
                        )
                    }
                    Text(
                        text = stringResource(R.string.treasury_chart_forecast),
                        fontSize = 8.sp,
                        color = TreasuryScreenTheme.Muted
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.width(yAxisWidth))
                rows.forEach { row ->
                    val isCurrent = row.month == now
                    Text(
                        text = "%02d".format(row.month.monthValue),
                        modifier = Modifier.weight(1f),
                        fontSize = 9.sp,
                        color = if (isCurrent) AppColors.BrandBlue else TreasuryScreenTheme.Muted,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private data class TreasuryYearTotals(
    val collected: Double,
    val pendingIncome: Double,
    val expenses: Double,
    val pendingExpenses: Double,
    val balance: Double,
    val openingFromAccounts: Double,
    val rows: List<TreasuryCalculations.MonthlyTreasuryRow>
)

@Composable
private fun TreasuryUpcomingSection(
    items: List<EcheanceItem>,
    formatAmount: (Double) -> String,
    dateFormatter: java.time.format.DateTimeFormatter,
    today: LocalDate
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TreasuryScreenTheme.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.treasury_upcoming_forecasts),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TreasuryScreenTheme.Primary
            )
            Text(
                text = stringResource(R.string.treasury_next_90_days),
                fontSize = 12.sp,
                color = TreasuryScreenTheme.Muted
            )
            if (items.isEmpty()) {
                Text(
                    text = stringResource(R.string.treasury_no_upcoming),
                    fontSize = 13.sp,
                    color = TreasuryScreenTheme.Muted,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                items.take(12).forEach { item ->
                    TreasuryUpcomingRow(
                        item = item,
                        formatAmount = formatAmount,
                        dateFormatter = dateFormatter,
                        today = today
                    )
                }
                if (items.size > 12) {
                    Text(
                        text = "+${items.size - 12}",
                        fontSize = 12.sp,
                        color = TreasuryScreenTheme.Muted
                    )
                }
            }
        }
    }
}

@Composable
private fun TreasuryUpcomingRow(
    item: EcheanceItem,
    formatAmount: (Double) -> String,
    dateFormatter: java.time.format.DateTimeFormatter,
    today: LocalDate
) {
    val isIncome = item.type == EcheanceType.INCOME
    val iconTint = if (isIncome) TreasuryScreenTheme.Positive else TreasuryScreenTheme.Accent
    val isOverdue = item.dueDate.isBefore(today)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TreasuryScreenTheme.Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = dateFormatter.format(item.dueDate),
                fontSize = 11.sp,
                color = if (isOverdue) TreasuryScreenTheme.Negative else TreasuryScreenTheme.Muted
            )
        }
        Text(
            text = formatAmount(item.amount),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = iconTint
        )
    }
}

@Composable
private fun TreasuryDualLineChart(
    balances: List<Double>,
    splitIndex: Int,
    minY: Double,
    maxY: Double,
    modifier: Modifier = Modifier
) {
    val range = max(maxY - minY, 1.0)
    val count = balances.size.coerceAtLeast(1)
    val realizedColor = AppColors.BrandBlue
    val forecastColor = TreasuryScreenTheme.ForecastAccent

    Canvas(modifier = modifier) {
        val padH = 4.dp.toPx()
        val padTop = 10.dp.toPx()
        val padBottom = 8.dp.toPx()
        val chartW = size.width - padH * 2
        val chartH = size.height - padTop - padBottom

        fun xAt(index: Int): Float =
            if (count <= 1) padH + chartW / 2f
            else padH + chartW * index / (count - 1)

        fun yAt(value: Double): Float {
            val ratio = ((value - minY) / range).toFloat()
            return padTop + chartH * (1f - ratio)
        }

        // Grille horizontale
        for (i in 0..3) {
            val y = padTop + chartH * i / 3f
            drawLine(
                color = TreasuryScreenTheme.Grid,
                start = Offset(padH, y),
                end = Offset(size.width - padH, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Ligne zéro
        if (minY < 0.0 && maxY > 0.0) {
            val zeroY = yAt(0.0)
            drawLine(
                color = TreasuryScreenTheme.Muted,
                start = Offset(padH, zeroY),
                end = Offset(size.width - padH, zeroY),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Zone forecast grisée (après le point de jonction)
        val fStart = (splitIndex + 1).coerceAtMost(count - 1)
        if (splitIndex in 0 until count - 1) {
            val zoneLeft = xAt(splitIndex)
            val zoneRight = xAt(count - 1)
            drawRect(
                color = Color(0xFF94A3B8).copy(alpha = 0.09f),
                topLeft = Offset(zoneLeft, padTop),
                size = androidx.compose.ui.geometry.Size(zoneRight - zoneLeft, chartH)
            )
        }

        // Ligne verticale de séparation au point de jonction
        if (splitIndex in 0 until count) {
            val sepX = xAt(splitIndex)
            drawLine(
                color = Color(0xFF94A3B8).copy(alpha = 0.55f),
                start = Offset(sepX, padTop),
                end = Offset(sepX, padTop + chartH),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
            )
        }

        // Segment réalisé : indices 0..splitIndex (ligne pleine bleue)
        if (splitIndex >= 1) {
            val path = Path().apply {
                for (i in 0..splitIndex) {
                    val x = xAt(i); val y = yAt(balances[i])
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(path, color = realizedColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        }

        // Segment forecast : indices splitIndex..count-1 (pointillés verts, enchaîné depuis le même point)
        if (splitIndex < count - 1) {
            val path = Path().apply {
                for (i in splitIndex until count) {
                    val x = xAt(i); val y = yAt(balances[i])
                    if (i == splitIndex) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(
                path,
                color = forecastColor.copy(alpha = 0.85f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                )
            )
        }

        // Points réalisés
        for (i in 0..splitIndex.coerceAtMost(count - 1)) {
            val pt = Offset(xAt(i), yAt(balances[i]))
            drawCircle(color = TreasuryScreenTheme.Card, radius = 4.dp.toPx(), center = pt)
            drawCircle(color = realizedColor, radius = 3.dp.toPx(), center = pt)
        }

        // Points forecast (sans le point de jonction déjà dessiné en bleu)
        for (i in (splitIndex + 1) until count) {
            val pt = Offset(xAt(i), yAt(balances[i]))
            drawCircle(color = TreasuryScreenTheme.Card, radius = 3.5.dp.toPx(), center = pt)
            drawCircle(color = forecastColor.copy(alpha = 0.85f), radius = 2.5.dp.toPx(), center = pt)
        }
    }
}
