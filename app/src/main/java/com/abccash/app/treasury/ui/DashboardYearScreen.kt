package com.abccash.app.treasury.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.abccash.app.treasury.data.PilotEntryType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R
import com.abccash.app.locale.AppLocale
import com.abccash.app.treasury.data.BalanceCorrection
import com.abccash.app.treasury.data.BankAccount
import com.abccash.app.treasury.data.DashboardKpi
import com.abccash.app.treasury.data.DashboardTreasuryPoint
import com.abccash.app.treasury.data.DashboardYear
import com.abccash.app.treasury.data.Expense
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.data.PilotEntry
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs

private val PageBg = Color(0xFFF3F6FB)
private val Ink = Color(0xFF0F2744)
private val Muted = Color(0xFF7A889C)
private val Blue = Color(0xFF2563EB)
private val BlueSoft = Color(0xFFE8F0FE)
private val Green = Color(0xFF16A34A)
private val GreenSoft = Color(0xFFE8F8EE)
private val Red = Color(0xFFDC2626)
private val RedSoft = Color(0xFFFEECEC)
private val Orange = Color(0xFFEA580C)
private val OrangeSoft = Color(0xFFFFF1E8)
private val PriorGray = Color(0xFF94A3B8)
private val Grid = Color(0xFFE8EEF5)
private val ChipBg = Color(0xFFF1F4F8)
private val CardShape = RoundedCornerShape(20.dp)
private val French = Locale.FRENCH

@Composable
fun DashboardYearScreen(
    entrepriseId: String,
    accounts: List<BankAccount>,
    invoices: List<Invoice>,
    expenses: List<Expense>,
    pilotEntries: List<PilotEntry>,
    corrections: List<BalanceCorrection>,
    onOpenAccounts: () -> Unit = {},
    onOpenPilotage: () -> Unit = {}
) {
    val today = remember { LocalDate.now() }
    var year by remember { mutableIntStateOf(today.year) }
    val formatAmount = rememberFormatMoneyWhole()
    val ownAccounts = remember(accounts, entrepriseId) {
        accounts.filter { it.entrepriseId == entrepriseId }
    }
    val ownActivity = remember(pilotEntries, entrepriseId) {
        pilotEntries.filter {
            it.entrepriseId == entrepriseId &&
                (it.type == PilotEntryType.SALE || it.type == PilotEntryType.EXPENSE)
        }
    }
    val hasAccount = ownAccounts.isNotEmpty()
    val hasActivity = ownActivity.isNotEmpty()
    val showSetup = entrepriseId.isNotBlank() && (!hasAccount || !hasActivity)
    val snapshot = remember(year, today, entrepriseId, accounts, invoices, expenses, pilotEntries, corrections) {
        DashboardYear.snapshot(
            year, today, entrepriseId, accounts, invoices, expenses, pilotEntries, corrections
        )
    }

    Scaffold(containerColor = PageBg) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(PageBg)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 6.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DashboardHeader(
                year = year,
                onPrevious = { year -= 1 },
                onNext = { year += 1 }
            )
            if (showSetup) {
                FirstLaunchSetupCard(
                    hasAccount = hasAccount,
                    hasActivity = hasActivity,
                    onOpenAccounts = onOpenAccounts,
                    onOpenPilotage = onOpenPilotage
                )
            }
            TreasuryHeroCard(
                amount = formatAmount(snapshot.treasury.amount),
                changePercent = snapshot.treasury.changePercent,
                previousYear = year - 1,
                curve = snapshot.selected
            )
            PerformanceCard(
                year = year,
                sales = snapshot.sales,
                expenses = snapshot.expenses,
                result = snapshot.result,
                formatAmount = formatAmount
            )
            ChartCard(
                selectedYear = year,
                selected = snapshot.selected,
                previous = snapshot.previousYear
            )
        }
    }
}

@Composable
private fun FirstLaunchSetupCard(
    hasAccount: Boolean,
    hasActivity: Boolean,
    onOpenAccounts: () -> Unit,
    onOpenPilotage: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.dash_setup_title),
                color = Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = stringResource(R.string.dash_setup_subtitle),
                color = Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )
            SetupStepRow(
                done = hasAccount,
                title = stringResource(R.string.dash_setup_account_title),
                hint = stringResource(R.string.dash_setup_account_hint),
                actionLabel = stringResource(R.string.dash_setup_account_cta),
                onAction = onOpenAccounts,
                primary = !hasAccount,
                pendingIcon = Icons.Filled.AccountBalance
            )
            Spacer(Modifier.height(8.dp))
            SetupStepRow(
                done = hasActivity,
                title = stringResource(R.string.dash_setup_activity_title),
                hint = stringResource(R.string.dash_setup_activity_hint),
                actionLabel = stringResource(R.string.dash_setup_activity_cta),
                onAction = onOpenPilotage,
                primary = hasAccount && !hasActivity,
                pendingIcon = Icons.Filled.BarChart
            )
        }
    }
}

@Composable
private fun SetupStepRow(
    done: Boolean,
    title: String,
    hint: String,
    actionLabel: String,
    onAction: () -> Unit,
    primary: Boolean,
    pendingIcon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (done) GreenSoft else BlueSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (done) Icons.Filled.Check else pendingIcon,
                contentDescription = null,
                tint = if (done) Green else Blue,
                modifier = Modifier.size(15.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
        ) {
            Text(
                text = title,
                color = Ink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (done) stringResource(R.string.dash_setup_done) else hint,
                color = if (done) Green else Muted,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!done) {
            if (primary) {
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(actionLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                TextButton(onClick = onAction) {
                    Text(actionLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Blue)
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(year: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.nav_home),
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Ink,
                lineHeight = 28.sp
            )
            Text(
                text = stringResource(R.string.dash_home_subtitle),
                color = Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
        CompactYearSwitcher(year = year, onPrevious = onPrevious, onNext = onNext)
    }
}

@Composable
private fun CompactYearSwitcher(year: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.dash_previous_year),
                tint = Muted,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = year.toString(),
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = Ink
        )
        IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.dash_next_year),
                tint = Muted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TreasuryHeroCard(
    amount: String,
    changePercent: Double?,
    previousYear: Int,
    curve: List<DashboardTreasuryPoint>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(BlueSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Blue,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.dash_treasury),
                    modifier = Modifier.padding(start = 6.dp),
                    color = Ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    tint = Muted.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(start = 3.dp)
                        .size(12.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = amount,
                        color = Ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        lineHeight = 28.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (changePercent != null) {
                        VariationBadge(
                            change = changePercent,
                            label = stringResource(R.string.dash_change_vs_year, changePercent, previousYear),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                HeroSparkline(
                    points = curve,
                    modifier = Modifier
                        .width(108.dp)
                        .height(44.dp)
                        .padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun VariationBadge(
    change: Double,
    label: String,
    modifier: Modifier = Modifier,
    positiveIsGood: Boolean = true
) {
    val positive = change > 0
    val good = if (positiveIsGood) positive else !positive
    val tint = when {
        change == 0.0 -> Muted
        good -> Green
        else -> Red
    }
    val bg = when {
        change == 0.0 -> ChipBg
        good -> GreenSoft
        else -> RedSoft
    }
    val icon = when {
        change > 0 -> Icons.AutoMirrored.Filled.TrendingUp
        change < 0 -> Icons.AutoMirrored.Filled.TrendingDown
        else -> Icons.AutoMirrored.Filled.TrendingUp
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
        Text(
            text = label,
            modifier = Modifier.padding(start = 3.dp),
            color = tint,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun HeroSparkline(
    points: List<DashboardTreasuryPoint>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val values = points.map { it.closing }
        val minV = (values.minOrNull() ?: 0.0) - 1.0
        val maxV = (values.maxOrNull() ?: 0.0) + 1.0
        val span = (maxV - minV).coerceAtLeast(1.0)
        val padH = 2.dp.toPx()
        fun xAt(i: Int) = padH + (size.width - padH * 2f) * i / (points.lastIndex).toFloat()
        fun yAt(v: Double) = size.height * (1f - ((v - minV) / span).toFloat().coerceIn(0.1f, 0.9f))
        val path = Path().apply {
            points.forEachIndexed { i, p ->
                val x = xAt(i)
                val y = yAt(p.closing)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        val fill = Path().apply {
            addPath(path)
            lineTo(xAt(points.lastIndex), size.height)
            lineTo(xAt(0), size.height)
            close()
        }
        drawPath(
            fill,
            brush = Brush.verticalGradient(
                listOf(Blue.copy(alpha = 0.2f), Blue.copy(alpha = 0.02f))
            )
        )
        drawPath(
            path,
            color = Blue,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        val end = Offset(xAt(points.lastIndex), yAt(points.last().closing))
        drawCircle(Color.White, radius = 3.5.dp.toPx(), center = end)
        drawCircle(Blue, radius = 2.5.dp.toPx(), center = end)
    }
}

@Composable
private fun PerformanceCard(
    year: Int,
    sales: DashboardKpi,
    expenses: DashboardKpi,
    result: DashboardKpi,
    formatAmount: (Double) -> String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(BlueSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.BarChart, null, tint = Blue, modifier = Modifier.size(13.dp))
                }
                Text(
                    text = stringResource(R.string.dash_performance),
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .weight(1f),
                    color = Ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    text = stringResource(R.string.dash_year_vs_year, year, year - 1),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(ChipBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(6.dp))
            PerformanceRow(
                label = stringResource(R.string.dash_sales_short),
                amount = formatAmount(sales.amount),
                change = sales.changePercent,
                icon = Icons.Filled.LocalOffer,
                iconTint = Blue,
                iconBg = BlueSoft,
                resultStyle = false
            )
            PerformanceDivider()
            PerformanceRow(
                label = stringResource(R.string.dash_expenses_short),
                amount = formatAmount(expenses.amount),
                change = expenses.changePercent,
                icon = Icons.AutoMirrored.Filled.TrendingDown,
                iconTint = Orange,
                iconBg = OrangeSoft,
                resultStyle = false
            )
            PerformanceDivider()
            PerformanceRow(
                label = stringResource(R.string.dash_result_short),
                amount = formatAmount(result.amount),
                change = result.changePercent,
                icon = Icons.Filled.BarChart,
                iconTint = if (result.amount >= 0) Green else Red,
                iconBg = if (result.amount >= 0) GreenSoft else RedSoft,
                resultStyle = true,
                amountColor = if (result.amount >= 0) Green else Red
            )
        }
    }
}

@Composable
private fun PerformanceDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .height(1.dp)
            .background(Grid)
    )
}

@Composable
private fun PerformanceRow(
    label: String,
    amount: String,
    change: Double?,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    resultStyle: Boolean,
    positiveIsGood: Boolean = true,
    amountColor: Color = Ink
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(14.dp))
        }
        Text(
            text = label,
            modifier = Modifier
                .padding(start = 10.dp)
                .width(78.dp),
            color = if (resultStyle) Ink else Muted,
            fontWeight = if (resultStyle) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 13.sp,
            maxLines = 1
        )
        Text(
            text = amount,
            modifier = Modifier.weight(1f),
            color = amountColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier.width(78.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (change != null) {
                VariationBadge(
                    change = change,
                    label = stringResource(R.string.dash_change_short, change),
                    positiveIsGood = positiveIsGood
                )
            }
        }
    }
}

@Composable
private fun ChartCard(
    selectedYear: Int,
    selected: List<DashboardTreasuryPoint>,
    previous: List<DashboardTreasuryPoint>
) {
    val hasForecast = selected.any { it.forecast }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BlueSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.ShowChart, null, tint = Blue, modifier = Modifier.size(14.dp))
                }
                Text(
                    text = stringResource(R.string.dash_chart_vs, selectedYear, selectedYear - 1),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f),
                    color = Ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(ChipBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dash_monthly),
                        color = Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Muted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(Blue, selectedYear.toString(), dashed = false)
                LegendDot(PriorGray, (selectedYear - 1).toString(), dashed = false)
                if (hasForecast) {
                    LegendDot(Blue, stringResource(R.string.dash_forecast), dashed = true)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                YAxisLabels(
                    values = selected.map { it.closing } + previous.map { it.closing },
                    modifier = Modifier
                        .width(40.dp)
                        .height(240.dp)
                        .padding(end = 4.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    TreasuryYearChart(
                        selected = selected,
                        previous = previous,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        selected.forEach { point ->
                            Text(
                                text = AppLocale.shortMonth(point.month.atDay(1), French)
                                    .removeSuffix(".")
                                    .replaceFirstChar { it.titlecase(French) },
                                modifier = Modifier.weight(1f),
                                color = if (point.forecast) Muted else Ink,
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center,
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
private fun YAxisLabels(values: List<Double>, modifier: Modifier = Modifier) {
    val rawMin = values.minOrNull() ?: 0.0
    val rawMax = values.maxOrNull() ?: 0.0
    val pad = maxOf((rawMax - rawMin) * 0.12, maxOf(abs(rawMax), abs(rawMin), 1.0) * 0.08)
    val maxValue = maxOf(rawMax + pad, 1.0)
    val minValue = minOf(rawMin - pad, 0.0)
    val span = maxOf(maxValue - minValue, 1.0)
    val ticks = (0..4).map { step -> maxValue - span * step / 4.0 }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End
    ) {
        ticks.forEach { tick ->
            Text(
                text = axisLabel(tick),
                color = Muted,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}

private fun axisLabel(value: Double): String {
    val absValue = abs(value)
    return when {
        absValue >= 1_000_000 -> String.format(French, "%.1fM", value / 1_000_000)
        absValue >= 1_000 -> String.format(French, "%.0fk", value / 1_000)
        else -> String.format(French, "%.0f", value)
    }
}

@Composable
private fun LegendDot(color: Color, label: String, dashed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(width = 18.dp, height = 10.dp)) {
            val y = size.height / 2f
            drawLine(
                color = color,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2.2.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.dp.toPx())) else null
            )
            drawCircle(color = color, radius = 2.6.dp.toPx(), center = Offset(size.width / 2f, y))
        }
        Text(
            text = label,
            modifier = Modifier.padding(start = 4.dp),
            color = Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TreasuryYearChart(
    selected: List<DashboardTreasuryPoint>,
    previous: List<DashboardTreasuryPoint>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        if (selected.isEmpty() || size.width <= 0f || size.height <= 0f) return@Canvas
        val padH = 8.dp.toPx()
        val padV = 14.dp.toPx()
        val chartW = (size.width - padH * 2f).coerceAtLeast(1f)
        val chartH = (size.height - padV * 2f).coerceAtLeast(1f)
        val values = buildList {
            addAll(selected.map { it.closing })
            addAll(previous.map { it.closing })
        }
        val rawMin = values.minOrNull() ?: 0.0
        val rawMax = values.maxOrNull() ?: 0.0
        val pad = maxOf(
            (rawMax - rawMin) * 0.18,
            maxOf(abs(rawMax), abs(rawMin), 1.0) * 0.15,
            1.0
        )
        val minValue = rawMin - pad
        val maxValue = rawMax + pad
        val span = (maxValue - minValue).coerceAtLeast(1.0)
        val count = selected.size.coerceAtLeast(1)

        fun xAt(index: Int): Float =
            if (count <= 1) padH + chartW / 2f
            else padH + chartW * index.toFloat() / (count - 1).toFloat()

        fun yAt(value: Double): Float {
            val safe = value.takeUnless { it.isNaN() || it.isInfinite() } ?: 0.0
            val ratio = ((safe - minValue) / span).toFloat().coerceIn(0f, 1f)
            return padV + chartH * (1f - ratio)
        }

        for (i in 0..4) {
            val y = padV + chartH * i / 4f
            drawLine(Grid, Offset(padH, y), Offset(size.width - padH, y), strokeWidth = 1.dp.toPx())
        }

        if (previous.size >= 2) {
            drawTreasurySeries(
                points = previous,
                color = PriorGray,
                xAt = ::xAt,
                yAt = ::yAt,
                stroke = 2.4.dp.toPx(),
                fillUnder = false
            )
        }
        if (selected.size >= 2) {
            drawTreasurySeries(
                points = selected,
                color = Blue,
                xAt = ::xAt,
                yAt = ::yAt,
                stroke = 3.2.dp.toPx(),
                fillUnder = true
            )
        } else if (selected.size == 1) {
            drawCircle(Blue, radius = 4.dp.toPx(), center = Offset(xAt(0), yAt(selected[0].closing)))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTreasurySeries(
    points: List<DashboardTreasuryPoint>,
    color: Color,
    xAt: (Int) -> Float,
    yAt: (Double) -> Float,
    stroke: Float,
    fillUnder: Boolean
) {
    if (points.size < 2) return

    val fullPath = Path().apply {
        points.forEachIndexed { index, point ->
            val x = xAt(index)
            val y = yAt(point.closing)
            if (index == 0) moveTo(x, y) else lineTo(x, y)
        }
    }
    if (fillUnder) {
        val baseY = size.height
        val fill = Path().apply {
            addPath(fullPath)
            lineTo(xAt(points.lastIndex), baseY)
            lineTo(xAt(0), baseY)
            close()
        }
        drawPath(
            fill,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.18f), color.copy(alpha = 0.02f))
            )
        )
    }
    drawPath(
        fullPath,
        color = color.copy(alpha = 0.35f),
        style = Stroke(width = stroke * 0.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    val firstForecast = points.indexOfFirst { it.forecast }
    val realizedEnd = when {
        firstForecast < 0 -> points.lastIndex
        firstForecast == 0 -> 0
        else -> firstForecast
    }

    if (realizedEnd >= 1) {
        val solid = Path().apply {
            for (index in 0..realizedEnd) {
                val x = xAt(index)
                val y = yAt(points[index].closing)
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(
            solid,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    if (realizedEnd < points.lastIndex) {
        val dash = Path().apply {
            for (index in realizedEnd..points.lastIndex) {
                val x = xAt(index)
                val y = yAt(points[index].closing)
                if (index == realizedEnd) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(
            dash,
            color = color,
            style = Stroke(
                width = stroke,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 7.dp.toPx()))
            )
        )
    }

    points.forEachIndexed { index, point ->
        val center = Offset(xAt(index), yAt(point.closing))
        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = center)
        drawCircle(
            color = color.copy(alpha = if (point.forecast) 0.65f else 1f),
            radius = if (point.forecast) 3.dp.toPx() else 3.6.dp.toPx(),
            center = center
        )
    }
}
