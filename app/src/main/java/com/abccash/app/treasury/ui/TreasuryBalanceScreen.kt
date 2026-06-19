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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import com.abccash.app.treasury.data.Expense
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.data.TreasuryCalculations
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import com.abccash.app.treasury.data.hasPermission
import java.time.YearMonth
import kotlin.math.max

private object TreasuryScreenTheme {
    val Background = Color(0xFFF8FAFC)
    val Primary = Color(0xFF1E293B)
    val Positive = Color(0xFF10B981)
    val Negative = Color(0xFFEF4444)
    val Accent = Color(0xFFF05E31)
    val Muted = Color(0xFF64748B)
    val Card = Color.White
    val Grid = Color(0xFFE2E8F0)
    val Line = Color(0xFF1E293B)
}

@Composable
fun TreasuryBalanceScreen(
    userRole: UserRole,
    permissions: Set<UserPermission>,
    invoices: List<Invoice>,
    expenses: List<Expense>,
    onExportCsv: (Int) -> String?,
    onNavigateToBankReconciliation: () -> Unit
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
    var pendingCsv by remember { mutableStateOf<String?>(null) }
    val canManageBank = userRole == UserRole.ADMIN ||
        hasPermission(userRole, permissions, UserPermission.MANAGE_EXPENSES)

    val formatWhole = rememberFormatMoneyWhole()
    val formatChartAmount = rememberFormatTreasuryChartAmount()

    val totals = remember(invoices, expenses, displayYear) {
        val rows = TreasuryCalculations.yearlyRows(invoices, expenses, displayYear)
        val yearEndForecast = rows.lastOrNull()?.forecastBalance
            ?: TreasuryCalculations.yearlyForecastBalance(invoices, expenses, displayYear)
        TreasuryYearTotals(
            collected = TreasuryCalculations.yearlyCollections(invoices, displayYear),
            pendingIncome = TreasuryCalculations.yearlyPendingIncome(invoices, displayYear),
            expenses = TreasuryCalculations.yearlyPaidExpenses(expenses, displayYear),
            pendingExpenses = TreasuryCalculations.yearlyPendingExpenses(expenses, displayYear),
            balance = yearEndForecast,
            rows = rows
        )
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
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TreasuryKpiCard(
                title = stringResource(R.string.collections),
                amount = formatWhole(totals.collected + totals.pendingIncome),
                subtitle = if (totals.pendingIncome > 0) {
                    stringResource(R.string.treasury_upcoming_part, formatWhole(totals.pendingIncome))
                } else {
                    null
                },
                amountColor = TreasuryScreenTheme.Positive,
                modifier = Modifier.weight(1f)
            )
            TreasuryKpiCard(
                title = stringResource(R.string.expenses),
                amount = formatWhole(totals.expenses + totals.pendingExpenses),
                subtitle = if (totals.pendingExpenses > 0) {
                    stringResource(R.string.treasury_upcoming_part, formatWhole(totals.pendingExpenses))
                } else {
                    null
                },
                amountColor = TreasuryScreenTheme.Accent,
                modifier = Modifier.weight(1f)
            )
        }

        TreasuryKpiCard(
            title = stringResource(R.string.forecast_balance),
            amount = formatWhole(totals.balance),
            subtitle = stringResource(R.string.treasury_december_subtitle),
            amountColor = if (totals.balance >= 0) {
                TreasuryScreenTheme.Positive
            } else {
                TreasuryScreenTheme.Negative
            },
            modifier = Modifier.fillMaxWidth()
        )

        ForecastBalanceChart(
            rows = totals.rows,
            formatChartAmount = formatChartAmount,
            formatWhole = formatWhole
        )

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun TreasuryHeader(
    year: Int,
    canManageBank: Boolean,
    onBankClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TreasuryScreenTheme.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TreasuryScreenTheme.Muted,
                maxLines = 1
            )
            Text(
                text = amount,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.let {
                Text(
                    text = it,
                    fontSize = 10.sp,
                    color = TreasuryScreenTheme.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ForecastBalanceChart(
    rows: List<TreasuryCalculations.MonthlyTreasuryRow>,
    formatChartAmount: (Double) -> String,
    formatWhole: (Double) -> String
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
                text = stringResource(R.string.treasury_cumulative_formula),
                fontSize = 11.sp,
                color = TreasuryScreenTheme.Muted
            )

            val balances = rows.map { it.forecastBalance }
            val minY = balances.minOrNull() ?: 0.0
            val maxY = balances.maxOrNull() ?: 0.0
            val range = max(maxY - minY, 1.0)
            val yAxisWidth = 34.dp
            val chartHeight = 120.dp

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Spacer(modifier = Modifier.width(yAxisWidth))
                balances.forEach { balance ->
                    Text(
                        text = formatChartAmount(balance),
                        modifier = Modifier.weight(1f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (balance >= 0) TreasuryScreenTheme.Positive else TreasuryScreenTheme.Negative,
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
                ForecastBalanceLine(
                    balances = balances,
                    modifier = Modifier
                        .weight(1f)
                        .height(chartHeight)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.width(yAxisWidth))
                rows.forEach { row ->
                    Text(
                        text = "%02d".format(row.month.monthValue),
                        modifier = Modifier.weight(1f),
                        fontSize = 9.sp,
                        color = TreasuryScreenTheme.Muted,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }

            val lastBalance = rows.lastOrNull()?.forecastBalance
            if (lastBalance != null) {
                Text(
                    text = stringResource(R.string.treasury_december_end, formatWhole(lastBalance)),
                    fontSize = 12.sp,
                    color = TreasuryScreenTheme.Muted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ForecastBalanceLine(
    balances: List<Double>,
    modifier: Modifier = Modifier
) {
    if (balances.isEmpty()) return

    val minY = balances.minOrNull() ?: 0.0
    val maxY = balances.maxOrNull() ?: 0.0
    val range = max(maxY - minY, 1.0)

    Canvas(modifier = modifier) {
        val padH = 4.dp.toPx()
        val padTop = 8.dp.toPx()
        val padBottom = 8.dp.toPx()
        val chartW = size.width - padH * 2
        val chartH = size.height - padTop - padBottom

        fun xAt(index: Int): Float {
            val count = balances.size
            return if (count <= 1) {
                padH + chartW / 2f
            } else {
                padH + chartW * index / (count - 1)
            }
        }

        fun yAt(value: Double): Float {
            val ratio = ((value - minY) / range).toFloat()
            return padTop + chartH * (1f - ratio)
        }

        for (i in 0..3) {
            val y = padTop + chartH * i / 3f
            drawLine(
                color = TreasuryScreenTheme.Grid,
                start = Offset(padH, y),
                end = Offset(size.width - padH, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val linePath = Path().apply {
            balances.forEachIndexed { index, balance ->
                val x = xAt(index)
                val y = yAt(balance)
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }

        drawPath(
            path = linePath,
            color = TreasuryScreenTheme.Line,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        balances.forEachIndexed { index, balance ->
            val point = Offset(xAt(index), yAt(balance))
            val pointColor = if (balance >= 0) TreasuryScreenTheme.Positive else TreasuryScreenTheme.Negative
            drawCircle(
                color = TreasuryScreenTheme.Card,
                radius = 5.dp.toPx(),
                center = point
            )
            drawCircle(
                color = pointColor,
                radius = 3.5.dp.toPx(),
                center = point
            )
        }
    }
}

private data class TreasuryYearTotals(
    val collected: Double,
    val pendingIncome: Double,
    val expenses: Double,
    val pendingExpenses: Double,
    val balance: Double,
    val rows: List<TreasuryCalculations.MonthlyTreasuryRow>
)
