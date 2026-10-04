package com.abccash.app.treasury.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

private val PageBg = Color(0xFFF4F6FA)
private val Ink = Color(0xFF0F2744)
private val Muted = Color(0xFF6B7A90)
private val Blue = Color(0xFF2563EB)
private val BlueSoft = Color(0xFFE8F0FE)
private val Green = Color(0xFF15803D)
private val GreenSoft = Color(0xFFE7F7ED)
private val Orange = Color(0xFFC2410C)
private val OrangeSoft = Color(0xFFFFF1E8)
private val PriorGray = Color(0xFF94A3B8)
private val Grid = Color(0xFFE6EBF2)
private val CardShape = RoundedCornerShape(18.dp)
private val French = Locale.FRENCH

@Composable
fun DashboardYearScreen(
    entrepriseId: String,
    accounts: List<BankAccount>,
    invoices: List<Invoice>,
    expenses: List<Expense>,
    pilotEntries: List<PilotEntry>,
    corrections: List<BalanceCorrection>
) {
    val today = remember { LocalDate.now() }
    var year by remember { mutableIntStateOf(today.year) }
    val formatAmount = rememberFormatMoneyWhole()
    val snapshot = remember(year, today, entrepriseId, accounts, invoices, expenses, pilotEntries, corrections) {
        DashboardYear.snapshot(
            year, today, entrepriseId, accounts, invoices, expenses, pilotEntries, corrections
        )
    }
    val insight = remember(snapshot) {
        treasuryInsight(snapshot.selected, snapshot.previousYear, year - 1)
    }

    Scaffold(containerColor = PageBg) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(PageBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.nav_home),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Ink
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                YearSwitcher(
                    year = year,
                    onPrevious = { year -= 1 },
                    onNext = { year += 1 }
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KpiCard(
                            label = stringResource(R.string.dash_sales),
                            kpi = snapshot.sales,
                            year = year,
                            formatAmount = formatAmount,
                            icon = Icons.Filled.Savings,
                            iconTint = Blue,
                            iconBg = BlueSoft,
                            invertChangeColor = false,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            label = stringResource(R.string.dash_expenses),
                            kpi = snapshot.expenses,
                            year = year,
                            formatAmount = formatAmount,
                            icon = Icons.AutoMirrored.Filled.TrendingDown,
                            iconTint = Orange,
                            iconBg = OrangeSoft,
                            invertChangeColor = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KpiCard(
                            label = stringResource(R.string.dash_result),
                            kpi = snapshot.result,
                            year = year,
                            formatAmount = formatAmount,
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            iconTint = Green,
                            iconBg = GreenSoft,
                            invertChangeColor = false,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            label = stringResource(R.string.dash_treasury),
                            kpi = snapshot.treasury,
                            year = year,
                            formatAmount = formatAmount,
                            icon = Icons.Filled.AccountBalanceWallet,
                            iconTint = Blue,
                            iconBg = BlueSoft,
                            invertChangeColor = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                ChartCard(
                    selectedYear = year,
                    selected = snapshot.selected,
                    previous = snapshot.previousYear
                )
                if (insight != null) {
                    InsightBar(insight)
                }
            }
        }
    }
}

@Composable
private fun YearSwitcher(year: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.dash_previous_year),
                    tint = Muted
                )
            }
            Text(
                text = year.toString(),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Ink
            )
            IconButton(onClick = onNext) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.dash_next_year),
                    tint = Muted
                )
            }
        }
    }
}

@Composable
private fun KpiCard(
    label: String,
    kpi: DashboardKpi,
    year: Int,
    formatAmount: (Double) -> String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    invertChangeColor: Boolean,
    modifier: Modifier = Modifier
) {
    val change = kpi.changePercent
    val good = when {
        change == null -> null
        invertChangeColor -> change < 0
        else -> change > 0
    }
    val changeColor = when (good) {
        true -> Green
        false -> Orange
        null -> Muted
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(17.dp))
                }
                Text(
                    text = label,
                    modifier = Modifier.padding(start = 8.dp),
                    color = Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 13.sp
                )
            }
            Text(
                text = formatAmount(kpi.amount),
                modifier = Modifier.padding(top = 10.dp),
                color = Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = changeLabel(change, year - 1),
                modifier = Modifier.padding(top = 4.dp),
                color = changeColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun changeLabel(change: Double?, previousYear: Int): String {
    if (change == null) return stringResource(R.string.dash_no_baseline)
    val res = when {
        change > 0 -> R.string.dash_change_up
        change < 0 -> R.string.dash_change_down
        else -> R.string.dash_change_flat
    }
    return stringResource(res, change, previousYear)
}

private data class TreasuryInsight(val above: Boolean, val previousYear: Int, val sinceMonth: String)

private fun treasuryInsight(
    selected: List<DashboardTreasuryPoint>,
    previous: List<DashboardTreasuryPoint>,
    previousYear: Int
): TreasuryInsight? {
    if (selected.size != previous.size || selected.isEmpty()) return null
    val pairs = selected.zip(previous)
    val firstAbove = pairs.indexOfFirst { (now, then) -> now.closing > then.closing }
    val firstBelow = pairs.indexOfFirst { (now, then) -> now.closing < then.closing }
    return when {
        firstAbove >= 0 && pairs.drop(firstAbove).all { (now, then) -> now.closing >= then.closing } ->
            TreasuryInsight(true, previousYear, AppLocale.shortMonth(selected[firstAbove].month.atDay(1), French))
        firstBelow >= 0 && pairs.drop(firstBelow).all { (now, then) -> now.closing <= then.closing } ->
            TreasuryInsight(false, previousYear, AppLocale.shortMonth(selected[firstBelow].month.atDay(1), French))
        else -> null
    }
}

@Composable
private fun InsightBar(insight: TreasuryInsight) {
    val text = if (insight.above) {
        stringResource(R.string.dash_insight_above, insight.previousYear, insight.sinceMonth)
    } else {
        stringResource(R.string.dash_insight_below, insight.previousYear, insight.sinceMonth)
    }
    val bg = if (insight.above) GreenSoft else OrangeSoft
    val tint = if (insight.above) Green else Orange
    val icon = if (insight.above) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown
    Card(
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            }
            Text(
                text = text,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .weight(1f),
                color = tint,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = tint.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
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
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.dash_chart),
                color = Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
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
                    .padding(top = 10.dp)
            ) {
                YAxisLabels(
                    values = selected.map { it.closing } + previous.map { it.closing },
                    modifier = Modifier
                        .width(44.dp)
                        .height(168.dp)
                        .padding(end = 4.dp)
                )
                Column(Modifier.weight(1f)) {
                    TreasuryYearChart(
                        selected = selected,
                        previous = previous,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(168.dp)
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
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
        // Toujours laisser de la marge : une courbe plate reste au milieu, jamais collée au bord.
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
                stroke = 2.6.dp.toPx(),
                fillUnder = false
            )
        }
        if (selected.size >= 2) {
            drawTreasurySeries(
                points = selected,
                color = Blue,
                xAt = ::xAt,
                yAt = ::yAt,
                stroke = 3.4.dp.toPx(),
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

    // 1) Toujours tracer toute la série en continu (évite les trous du découpage prévision).
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

    // 2) Segment réalisé (plein) + segment prévision (pointillé), sans jamais tout masquer.
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
    } else if (realizedEnd == 0 && points.size >= 2 && points[0].forecast) {
        // Année entièrement en prévision : la ligne pleine légère + pointillés ci-dessus suffisent.
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
