package com.abccash.app.treasury.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R
import com.abccash.app.locale.AppLocale
import com.abccash.app.treasury.data.PilotCalculations
import com.abccash.app.treasury.data.PilotCoverageState
import com.abccash.app.treasury.data.PilotMoney
import com.abccash.app.treasury.viewmodel.PilotageUiState
import com.abccash.app.ui.theme.AppColors
import java.time.YearMonth
import kotlin.math.abs

@Composable
fun PilotageAnalysis(
    state: PilotageUiState,
    onMonthSelected: (YearMonth) -> Unit,
    onSaveTarget: (Double?) -> Unit
) {
    val formatMoney = rememberFormatMoney()
    val formatCompact = rememberFormatMoneyCompact()
    val month = state.month
    val figures = remember(state.entries, state.entrepriseId, month) {
        PilotCalculations.monthFigures(state.entries, state.entrepriseId, month)
    }
    val year = remember(state.entries, state.entrepriseId, month.year) {
        PilotCalculations.yearFigures(state.entries, state.entrepriseId, month.year)
    }
    val shares = remember(state.entries, state.categories, state.entrepriseId, month) {
        PilotCalculations.salesByCategory(state.entries, state.categories, state.entrepriseId, month)
    }
    val target = state.targets.find {
        it.entrepriseId == state.entrepriseId && it.year == month.year && it.month == month.monthValue &&
            PilotMoney.isPositive(it.salesTarget)
    }
    var selectedCategory by remember(month) { mutableStateOf<String?>(null) }
    var editingTarget by remember { mutableStateOf(false) }
    val coverageColor = when (PilotCalculations.coverageState(figures)) {
        PilotCoverageState.ABOVE, PilotCoverageState.AT_ZERO -> AppColors.IncomeGreen
        PilotCoverageState.BELOW -> AppColors.ExpenseRed
        PilotCoverageState.NO_CHARGES -> AppColors.TextSecondary
    }
    val resultColor = when {
        figures.result > 0 -> AppColors.IncomeGreen
        figures.result < 0 -> AppColors.ExpenseRed
        else -> AppColors.TextPrimary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PilotKpi(stringResource(R.string.pilot_sales), formatMoney(figures.sales), AppColors.TextPrimary, Modifier.weight(1f))
            PilotKpi(stringResource(R.string.pilot_charges), formatMoney(figures.charges), AppColors.TextPrimary, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PilotKpi(stringResource(R.string.pilot_coverage), pilotPercent(figures.coverage), coverageColor, Modifier.weight(1f))
            PilotKpi(
                stringResource(R.string.pilot_result),
                pilotSignedMoney(figures.result, formatMoney),
                resultColor,
                Modifier.weight(1f)
            )
        }
        CoverageStatus(figures = figures, formatMoney = formatMoney)
        if (target != null) {
            val progress = PilotCalculations.targetProgress(figures.sales, target.salesTarget)
            val remaining = PilotCalculations.targetRemaining(figures.sales, target.salesTarget)
            Card(
                colors = CardDefaults.cardColors(containerColor = AppColors.InfoCardBackground),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { editingTarget = true }
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.pilot_target_title, AppLocale.monthYear(month), formatMoney(target.salesTarget)),
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(stringResource(R.string.pilot_target_done, formatMoney(figures.sales)), color = AppColors.TextSecondary)
                    Text(
                        stringResource(R.string.pilot_target_rate, pilotPercent(progress)),
                        color = AppColors.TextPrimary
                    )
                    Text(stringResource(R.string.pilot_target_left, formatMoney(remaining)), color = AppColors.TextSecondary)
                }
            }
        } else {
            TextButton(onClick = { editingTarget = true }) {
                Text(stringResource(R.string.pilot_target_set))
            }
        }

        Text(stringResource(R.string.pilot_chart_vs), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.TextPrimary)
        SalesChargesChart(year.months, formatCompact)

        Text(stringResource(R.string.pilot_chart_categories), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.TextPrimary)
        if (shares.isEmpty()) {
            Text(stringResource(R.string.pilot_no_sales), color = AppColors.TextSecondary)
        } else {
            shares.forEach { share ->
                val selected = selectedCategory == share.categoryId
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedCategory = if (selected) null else share.categoryId }
                        .padding(vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(share.name, modifier = Modifier.weight(1f), color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatMoney(share.amount), color = AppColors.TextPrimary, fontWeight = FontWeight.Medium)
                        Text(
                            "  ${pilotPercent(share.share)}",
                            color = AppColors.TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (share.share ?: 0.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = AppColors.BrandBlue,
                        trackColor = AppColors.Border
                    )
                    if (selected) {
                        val evolution = PilotCalculations.categoryEvolution(
                            state.entries,
                            state.entrepriseId,
                            share.categoryId,
                            month
                        )
                        Column(Modifier.padding(top = 6.dp, start = 4.dp)) {
                            evolution.forEach { (evoMonth, amount) ->
                                Text(
                                    "${AppLocale.shortMonth(evoMonth.atDay(1))}  ${formatCompact(amount)}",
                                    color = AppColors.TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(stringResource(R.string.pilot_year_title, month.year), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.TextPrimary)
        YearSummaryRow(stringResource(R.string.pilot_year_sales), formatMoney(year.sales))
        YearSummaryRow(stringResource(R.string.pilot_year_charges), formatMoney(year.charges))
        YearSummaryRow(stringResource(R.string.pilot_year_result), pilotSignedMoney(year.result, formatMoney))
        YearSummaryRow(stringResource(R.string.pilot_year_coverage), pilotPercent(year.coverage))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    TableHead(stringResource(R.string.pilot_col_month), Modifier.weight(0.8f), TextAlign.Start)
                    TableHead(stringResource(R.string.pilot_sales), Modifier.weight(1f))
                    TableHead(stringResource(R.string.pilot_charges), Modifier.weight(1f))
                    TableHead(stringResource(R.string.pilot_coverage), Modifier.weight(0.9f))
                    TableHead(stringResource(R.string.pilot_result), Modifier.weight(1f))
                }
                year.months.forEach { row ->
                    val current = row.month == month
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (current) AppColors.BrandBlueLight else Color.Transparent)
                            .clickable { onMonthSelected(row.month) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            AppLocale.shortMonth(row.month.atDay(1)),
                            modifier = Modifier.weight(0.8f),
                            fontSize = 12.sp,
                            color = AppColors.TextPrimary
                        )
                        TableCell(formatCompact(row.sales), Modifier.weight(1f))
                        TableCell(formatCompact(row.charges), Modifier.weight(1f))
                        TableCell(pilotPercent(row.coverage), Modifier.weight(0.9f))
                        TableCell(pilotSignedMoney(row.result, formatCompact), Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    if (editingTarget) {
        var text by remember(target?.id) { mutableStateOf(target?.salesTarget?.let { trimTarget(it) }.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editingTarget = false },
            title = { Text(stringResource(R.string.pilot_target_set)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.pilot_field_amount)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = PilotMoney.parse(text)
                    onSaveTarget(if (parsed != null && PilotMoney.isPositive(parsed)) parsed else null)
                    editingTarget = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    onSaveTarget(null)
                    editingTarget = false
                }) { Text(stringResource(R.string.pilot_target_clear)) }
            }
        )
    }
}

@Composable
private fun PilotKpi(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.InfoCardBackground)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            Text(label, color = AppColors.TextSecondary, fontSize = 13.sp)
            Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp)
        }
    }
}

@Composable
private fun CoverageStatus(
    figures: com.abccash.app.treasury.data.PilotMonthFigures,
    formatMoney: (Double) -> String
) {
    val state = PilotCalculations.coverageState(figures)
    val text = when (state) {
        PilotCoverageState.NO_CHARGES -> stringResource(R.string.pilot_no_charges)
        PilotCoverageState.AT_ZERO -> stringResource(R.string.pilot_zero_reached)
        PilotCoverageState.ABOVE -> stringResource(
            R.string.pilot_surplus,
            pilotSignedMoney(figures.result, formatMoney)
        )
        PilotCoverageState.BELOW -> stringResource(
            R.string.pilot_missing,
            formatMoney(abs(figures.result))
        )
    }
    val color = when (state) {
        PilotCoverageState.ABOVE, PilotCoverageState.AT_ZERO -> AppColors.IncomeGreen
        PilotCoverageState.BELOW -> AppColors.ExpenseRed
        PilotCoverageState.NO_CHARGES -> AppColors.TextSecondary
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (state != PilotCoverageState.NO_CHARGES) {
            Text(
                stringResource(R.string.pilot_zero_point, formatMoney(figures.zeroPoint)),
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
        Text(text, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SalesChargesChart(
    months: List<com.abccash.app.treasury.data.PilotMonthFigures>,
    formatCompact: (Double) -> String
) {
    val max = months.maxOf { maxOf(it.sales, it.charges) }.coerceAtLeast(1.0)
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LegendDot(AppColors.IncomeGreen, stringResource(R.string.pilot_sales))
            LegendDot(AppColors.ExpenseRed, stringResource(R.string.pilot_charges))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            months.forEach { month ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.height(112.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        ChartBar(month.sales / max, AppColors.IncomeGreen)
                        ChartBar(month.charges / max, AppColors.ExpenseRed)
                    }
                    Text(
                        AppLocale.shortMonth(month.month.atDay(1)).take(3),
                        fontSize = 9.sp,
                        color = AppColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
        Text(
            formatCompact(max),
            fontSize = 11.sp,
            color = AppColors.TextTertiary,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun ChartBar(fraction: Double, color: Color) {
    Box(
        modifier = Modifier
            .width(5.dp)
            .fillMaxHeight()
    ) {
        if (fraction > 0.0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(fraction.toFloat().coerceIn(0.04f, 1f))
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(10.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(label, modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp, color = AppColors.TextSecondary)
    }
}

@Composable
private fun YearSummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = AppColors.TextSecondary)
        Text(value, color = AppColors.TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TableHead(text: String, modifier: Modifier, align: TextAlign = TextAlign.End) {
    Text(text, modifier = modifier, fontSize = 11.sp, color = AppColors.TextSecondary, textAlign = align, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun TableCell(text: String, modifier: Modifier) {
    Text(text, modifier = modifier, fontSize = 11.sp, color = AppColors.TextPrimary, textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Clip)
}

private fun trimTarget(amount: Double): String =
    PilotMoney.decimal(amount).stripTrailingZeros().toPlainString().replace('.', ',')
