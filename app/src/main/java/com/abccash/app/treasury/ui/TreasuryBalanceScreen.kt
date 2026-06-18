package com.abccash.app.treasury.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.treasury.data.Expense
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import com.abccash.app.treasury.data.appliesToMonth
import com.abccash.app.treasury.data.hasPermission
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.ui.platform.LocalContext
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TreasuryBalanceScreen(
    userRole: UserRole,
    permissions: Set<UserPermission>,
    selectedMonth: YearMonth,
    totalCollected: Double,
    totalExpenses: Double,
    forecastedBalance: Double,
    invoices: List<Invoice>,
    expenses: List<Expense>,
    onMonthChange: (YearMonth) -> Unit,
    onExportCsv: () -> String?
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
                    Text(
                        text = "🔐",
                        fontSize = 64.sp
                    )
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
    
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("fr", "TN")).apply {
            maximumFractionDigits = 3
        }
    }
    
    val monthBalance = totalCollected - totalExpenses
    val context = LocalContext.current
    var pendingCsv by remember { mutableStateOf<String?>(null) }
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
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = {
                    val csv = onExportCsv() ?: return@OutlinedButton
                    pendingCsv = csv
                    exportLauncher.launch("abc-cash-${selectedMonth}.csv")
                }
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Exporter CSV")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onMonthChange(selectedMonth.minusMonths(1)) }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Mois précédent")
                }
                
                Text(
                    text = selectedMonth.format(
                        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH)
                    ).replaceFirstChar { it.uppercase() },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = { onMonthChange(selectedMonth.plusMonths(1)) }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Mois suivant")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactTreasuryCard(
                title = "Encaissé",
                amount = currencyFormatter.format(totalCollected),
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
            CompactTreasuryCard(
                title = "Dép. payées",
                amount = currencyFormatter.format(totalExpenses),
                color = Color(0xFFF44336),
                modifier = Modifier.weight(1f)
            )
            CompactTreasuryCard(
                title = "Solde",
                amount = currencyFormatter.format(monthBalance),
                color = if (monthBalance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TreasuryMonthlyChart(
            selectedMonth = selectedMonth,
            invoices = invoices,
            expenses = expenses
        )
        
        Spacer(modifier = Modifier.height(10.dp))

        ForecastSummaryRow(
            forecastedBalance = currencyFormatter.format(forecastedBalance),
            isPositive = forecastedBalance >= 0
        )
    }
}

@Composable
private fun CompactTreasuryCard(
    title: String,
    amount: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = amount,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ForecastSummaryRow(
    forecastedBalance: String,
    isPositive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Prévision fin de mois",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1A1A1A)
            )
            Text(
                text = forecastedBalance,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}

@Composable
private fun TreasuryMonthlyChart(
    selectedMonth: YearMonth,
    invoices: List<Invoice>,
    expenses: List<Expense>
) {
    val green = Color(0xFF4CAF50)
    val red = Color(0xFFF44336)
    val blue = Color(0xFF2563EB)
    val months = remember(selectedMonth) {
        (5 downTo 0).map { selectedMonth.minusMonths(it.toLong()) }
    }
    val rows = remember(selectedMonth, invoices, expenses) {
        months.map { month ->
            val collected = invoices
                .flatMap { it.payments }
                .filter { YearMonth.from(it.date) == month }
                .sumOf { it.amount }
            val spent = expenses
                .filter { it.appliesToMonth(month) }
                .sumOf { it.amount }
            MonthlyTreasuryPoint(month, collected, spent, collected - spent)
        }
    }
    val minValue = rows
        .flatMap { listOf(0.0, it.collected, it.expenses, it.balance) }
        .minOrNull()
        ?: 0.0
    val maxValue = rows
        .flatMap { listOf(0.0, it.collected, it.expenses, it.balance) }
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
                text = "Évolution mois par mois",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChartLegend("Encaissements", green)
                ChartLegend("Dépenses", red)
                ChartLegend("Solde", blue)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
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
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    points.forEach { point ->
                        drawCircle(color = color, radius = 4.dp.toPx(), center = point)
                    }
                }

                drawSeries(rows.map { it.collected }, green)
                drawSeries(rows.map { it.expenses }, red)
                drawSeries(rows.map { it.balance }, blue)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                rows.forEach { row ->
                    Text(
                        text = row.month.format(DateTimeFormatter.ofPattern("MMM", Locale.FRENCH)),
                        fontSize = 10.sp,
                        color = Color.Gray
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

private data class MonthlyTreasuryPoint(
    val month: YearMonth,
    val collected: Double,
    val expenses: Double,
    val balance: Double
)
