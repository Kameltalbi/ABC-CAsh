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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import java.time.format.DateTimeFormatter
import java.util.Locale

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
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(32.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🔐", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Accès refusé",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Vous n'avez pas la permission de voir la trésorerie",
                        fontSize = 14.sp,
                        color = Color.Gray
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

    val formatAmount = rememberFormatMoney()

    val totals = remember(invoices, expenses, displayYear) {
        val rows = TreasuryCalculations.yearlyRows(invoices, expenses, displayYear)
        TreasuryYearTotals(
            collected = TreasuryCalculations.yearlyCollections(invoices, displayYear),
            pendingIncome = TreasuryCalculations.yearlyPendingIncome(invoices, displayYear),
            expenses = TreasuryCalculations.yearlyPaidExpenses(expenses, displayYear),
            pendingExpenses = TreasuryCalculations.yearlyPendingExpenses(expenses, displayYear),
            balance = TreasuryCalculations.yearlyForecastBalance(invoices, expenses, displayYear),
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Trésorerie $displayYear",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = "Janvier → Décembre",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canManageBank) {
                    BankAdjustIconButton(onClick = onNavigateToBankReconciliation)
                }
                IconButton(
                    onClick = {
                        val csv = onExportCsv(displayYear) ?: return@IconButton
                        pendingCsv = csv
                        exportLauncher.launch("abc-cash-$displayYear.csv")
                    }
                ) {
                    Icon(
                        Icons.Default.FileDownload,
                        contentDescription = "Exporter CSV",
                        tint = Color(0xFF64748B)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactTreasuryCard(
                title = "Encaissements",
                amount = formatAmount(totals.collected + totals.pendingIncome),
                subtitle = if (totals.pendingIncome > 0) {
                    "dont ${formatAmount(totals.pendingIncome)} à venir"
                } else null,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
            CompactTreasuryCard(
                title = "Dépenses",
                amount = formatAmount(totals.expenses + totals.pendingExpenses),
                subtitle = if (totals.pendingExpenses > 0) {
                    "dont ${formatAmount(totals.pendingExpenses)} à venir"
                } else null,
                color = Color(0xFFF44336),
                modifier = Modifier.weight(1f)
            )
            CompactTreasuryCard(
                title = "Solde prévisionnel",
                amount = formatAmount(totals.balance),
                color = if (totals.balance >= 0) Color(0xFF2563EB) else Color(0xFFF44336),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TreasuryYearlyChart(rows = totals.rows)
    }
}

@Composable
private fun BankAdjustIconButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Box(modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = "Ajuster le solde bancaire",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.Center)
            )
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(11.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
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

@Composable
private fun CompactTreasuryCard(
    title: String,
    amount: String,
    subtitle: String? = null,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.heightIn(min = 88.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.92f),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = amount,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 16.sp,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = it,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 11.sp
                )
            }
        }
    }
}

@Composable
private fun TreasuryYearlyChart(
    rows: List<TreasuryCalculations.MonthlyTreasuryRow>
) {
    val green = Color(0xFF4CAF50)
    val red = Color(0xFFF44336)
    val blue = Color(0xFF2563EB)

    val minValue = rows
        .flatMap { listOf(0.0, it.totalIncome, it.totalExpenses, it.forecastBalance) }
        .minOrNull()
        ?: 0.0
    val maxValue = rows
        .flatMap { listOf(0.0, it.totalIncome, it.totalExpenses, it.forecastBalance) }
        .maxOrNull()
        ?.takeIf { it > minValue }
        ?: 1.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Évolution sur 12 mois (prévisions incluses)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChartLegend("Encaissements", green)
                ChartLegend("Dépenses", red)
                ChartLegend("Solde prév.", blue)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                val leftPadding = 10.dp.toPx()
                val rightPadding = 10.dp.toPx()
                val topPadding = 14.dp.toPx()
                val bottomPadding = 26.dp.toPx()
                val chartWidth = size.width - leftPadding - rightPadding
                val chartHeight = size.height - topPadding - bottomPadding
                val stepX = if (rows.size > 1) chartWidth / (rows.size - 1) else chartWidth

                fun yFor(value: Double): Float {
                    val range = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0
                    return topPadding + chartHeight - (((value - minValue) / range).toFloat() * chartHeight)
                }

                for (i in 0..4) {
                    val y = topPadding + chartHeight * i / 4f
                    drawLine(
                        color = Color(0xFFE5E7EB),
                        start = Offset(leftPadding, y),
                        end = Offset(size.width - rightPadding, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                fun drawSeries(values: List<Double>, color: Color) {
                    val points = values.mapIndexed { index, value ->
                        Offset(leftPadding + stepX * index, yFor(value))
                    }
                    points.zipWithNext().forEach { (start, end) ->
                        drawLine(
                            color = color,
                            start = start,
                            end = end,
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    points.forEach { point ->
                        drawCircle(color = color, radius = 3.dp.toPx(), center = point)
                    }
                }

                drawSeries(rows.map { it.totalIncome }, green)
                drawSeries(rows.map { it.totalExpenses }, red)
                drawSeries(rows.map { it.forecastBalance }, blue)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                rows.forEach { row ->
                    Text(
                        text = row.month.format(DateTimeFormatter.ofPattern("MMM", Locale.FRENCH)),
                        fontSize = 9.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(50))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = Color.Gray)
    }
}
