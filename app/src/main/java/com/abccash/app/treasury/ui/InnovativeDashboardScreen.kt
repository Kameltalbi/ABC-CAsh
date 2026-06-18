package com.abccash.app.treasury.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

private object InnovativeDashboardColors {
    val Background = Color(0xFFF8F9FA)
    val Primary = Color(0xFF1E293B)
    val Positive = Color(0xFF10B981)
    val Negative = Color(0xFFEF4444)
    val Muted = Color(0xFF64748B)
    val Card = Color.White
    val ChartFill = Color(0xFF10B981).copy(alpha = 0.14f)

    val IncomeSliceColors = listOf(
        Color(0xFF10B981),
        Color(0xFF059669),
        Color(0xFF34D399),
        Color(0xFF0EA5E9)
    )
    val ExpenseSliceColors = listOf(
        Color(0xFFEF4444),
        Color(0xFFF97316),
        Color(0xFFDC2626),
        Color(0xFFFB7185)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InnovativeDashboardScreen(
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
    val year = remember { LocalDate.now().year }
    val bankBalance by userPreferences
        .observeBankBalance(entrepriseId.orEmpty(), year)
        .collectAsStateWithLifecycle(initialValue = null)

    val data = remember(invoices, expenses, bankBalance) {
        DashboardCalculations.buildInnovativeDashboard(invoices, expenses, bankBalance)
    }

    val firstName = remember(userName) {
        userName.trim().substringBefore(" ").ifBlank { "Utilisateur" }
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
        containerColor = InnovativeDashboardColors.Background,
        floatingActionButton = {
            // Déclenche l'ouverture des formulaires de saisie rapide (encaissement / dépense).
            if (canAddIncome || canAddExpense) {
                FloatingActionButton(
                    onClick = { showTypeSheet = true },
                    containerColor = InnovativeDashboardColors.Primary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Saisie rapide")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 96.dp)
        ) {
            item {
                InnovativeDashboardHeader(
                    firstName = firstName,
                    companyName = companyName,
                    balance = formatAmount(data.bankBalance)
                )
            }
            item {
                InnovativeDonutChartsRow(
                    incomeSlices = data.incomeByCategory,
                    incomeTotal = data.incomeTotal,
                    expenseSlices = data.expenseByCategory,
                    expenseTotal = data.expenseTotal,
                    formatAmount = formatAmount
                )
            }
            item {
                InnovativeBalanceLineChartSection(
                    points = data.balanceHistory,
                    formatAmount = formatAmount
                )
            }
            item {
                InnovativeForecastBarsSection(
                    forecastIncome = data.forecastIncome,
                    forecastExpenses = data.forecastExpenses,
                    formatAmount = formatAmount
                )
            }
        }
    }
}

@Composable
private fun InnovativeDashboardHeader(
    firstName: String,
    companyName: String,
    balance: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Bonjour $firstName",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = InnovativeDashboardColors.Primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = companyName.ifBlank { "Votre entreprise" },
                style = MaterialTheme.typography.bodyMedium,
                color = InnovativeDashboardColors.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = balance,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = InnovativeDashboardColors.Primary,
            textAlign = TextAlign.End,
            maxLines = 2
        )
    }
}

@Composable
private fun InnovativeDonutChartsRow(
    incomeSlices: List<CategorySlice>,
    incomeTotal: Double,
    expenseSlices: List<CategorySlice>,
    expenseTotal: Double,
    formatAmount: (Double) -> String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InnovativeDonutCard(
            title = "Encaissements",
            slices = incomeSlices,
            total = incomeTotal,
            totalColor = InnovativeDashboardColors.Positive,
            sliceColors = InnovativeDashboardColors.IncomeSliceColors,
            formatAmount = formatAmount,
            modifier = Modifier.weight(1f)
        )
        InnovativeDonutCard(
            title = "Dépenses",
            slices = expenseSlices,
            total = expenseTotal,
            totalColor = InnovativeDashboardColors.Negative,
            sliceColors = InnovativeDashboardColors.ExpenseSliceColors,
            formatAmount = formatAmount,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun InnovativeDonutCard(
    title: String,
    slices: List<CategorySlice>,
    total: Double,
    totalColor: Color,
    sliceColors: List<Color>,
    formatAmount: (Double) -> String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = InnovativeDashboardColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = InnovativeDashboardColors.Primary
            )
            Box(
                modifier = Modifier.size(108.dp),
                contentAlignment = Alignment.Center
            ) {
                InnovativeDonutChart(
                    slices = slices,
                    colors = sliceColors,
                    modifier = Modifier.fillMaxSize()
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatAmount(total),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = totalColor,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        lineHeight = 14.sp,
                        fontSize = 11.sp
                    )
                }
            }
            InnovativeDonutLegend(
                slices = slices,
                colors = sliceColors
            )
        }
    }
}

@Composable
private fun InnovativeDonutChart(
    slices: List<CategorySlice>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val displaySlices = remember(slices) {
        if (slices.isEmpty()) {
            listOf(CategorySlice("Aucune donnée", 1.0))
        } else {
            slices
        }
    }
    val total = displaySlices.sumOf { it.amount }.coerceAtLeast(1.0)

    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.14f
        val diameter = size.minDimension - stroke
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
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
private fun InnovativeDonutLegend(
    slices: List<CategorySlice>,
    colors: List<Color>
) {
    val items = slices.take(3).ifEmpty {
        listOf(CategorySlice("—", 0.0))
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEachIndexed { index, slice ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(colors[index % colors.size], CircleShape)
                )
                Text(
                    text = slice.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = InnovativeDashboardColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun InnovativeBalanceLineChartSection(
    points: List<DashboardBalancePoint>,
    formatAmount: (Double) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = InnovativeDashboardColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Évolution du solde",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = InnovativeDashboardColors.Primary
            )
            Text(
                text = "30 derniers jours",
                style = MaterialTheme.typography.bodySmall,
                color = InnovativeDashboardColors.Muted
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (points.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Pas assez de données", color = InnovativeDashboardColors.Muted)
                }
            } else {
                InnovativeBalanceLineChart(
                    points = points,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    monthAxisLabels(points).forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = InnovativeDashboardColors.Muted
                        )
                    }
                }
                val first = points.first().balance
                val last = points.last().balance
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "J-30 ${formatAmount(first)} · Aujourd'hui ${formatAmount(last)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = InnovativeDashboardColors.Muted
                )
            }
        }
    }
}

private fun monthAxisLabels(points: List<DashboardBalancePoint>): List<String> {
    if (points.isEmpty()) return emptyList()
    val formatter = DateTimeFormatter.ofPattern("MMM", Locale.FRENCH)
    return points
        .map { java.time.YearMonth.from(it.date) }
        .distinct()
        .map { ym ->
            ym.atDay(1).format(formatter).replaceFirstChar { c -> c.uppercase() }
        }
        .take(3)
}

@Composable
private fun InnovativeBalanceLineChart(
    points: List<DashboardBalancePoint>,
    modifier: Modifier = Modifier
) {
    val balances = points.map { it.balance }
    val minY = balances.minOrNull() ?: 0.0
    val maxY = balances.maxOrNull() ?: 1.0
    val range = max(maxY - minY, 1.0)

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

        val fillPath = Path().apply {
            points.forEachIndexed { i, pt ->
                val x = xAt(i)
                val y = yAt(pt.balance)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            lineTo(xAt(points.lastIndex), h)
            lineTo(xAt(0), h)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(InnovativeDashboardColors.ChartFill, Color.Transparent),
                startY = padTop,
                endY = h
            )
        )

        for (i in 0 until points.lastIndex) {
            drawLine(
                color = InnovativeDashboardColors.Positive,
                start = Offset(xAt(i), yAt(points[i].balance)),
                end = Offset(xAt(i + 1), yAt(points[i + 1].balance)),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun InnovativeForecastBarsSection(
    forecastIncome: Double,
    forecastExpenses: Double,
    formatAmount: (Double) -> String
) {
    val maxValue = max(max(forecastIncome, forecastExpenses), 1.0)
    val incomeProgress = (forecastIncome / maxValue).toFloat().coerceIn(0.05f, 1f)
    val expenseProgress = (forecastExpenses / maxValue).toFloat().coerceIn(0.05f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = InnovativeDashboardColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Prévisions à venir",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = InnovativeDashboardColors.Primary
            )
            Text(
                text = "30 prochains jours · Recettes vs Dépenses",
                style = MaterialTheme.typography.bodySmall,
                color = InnovativeDashboardColors.Muted
            )
            InnovativeForecastBarRow(
                label = "Recettes prévues",
                amount = formatAmount(forecastIncome),
                progress = incomeProgress,
                color = InnovativeDashboardColors.Positive,
                trackColor = InnovativeDashboardColors.Positive.copy(alpha = 0.15f)
            )
            InnovativeForecastBarRow(
                label = "Dépenses prévues",
                amount = formatAmount(forecastExpenses),
                progress = expenseProgress,
                color = InnovativeDashboardColors.Negative,
                trackColor = InnovativeDashboardColors.Negative.copy(alpha = 0.15f)
            )
        }
    }
}

@Composable
private fun InnovativeForecastBarRow(
    label: String,
    amount: String,
    progress: Float,
    color: Color,
    trackColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = InnovativeDashboardColors.Primary
            )
            Text(
                text = amount,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            color = color,
            trackColor = trackColor,
            strokeCap = StrokeCap.Round
        )
    }
}
