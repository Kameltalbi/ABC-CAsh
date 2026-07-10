package com.abccash.app.treasury.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.treasury.data.BankAccountCalculations
import com.abccash.app.treasury.data.BankAccountSummary
import com.abccash.app.treasury.data.CategorySlice
import com.abccash.app.treasury.data.DashboardCalculations
import com.abccash.app.treasury.data.DashboardViewMode
import com.abccash.app.treasury.data.EcheanceForecast
import com.abccash.app.treasury.data.EcheanceType
import com.abccash.app.treasury.data.Expense
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.data.InvoiceStatus
import com.abccash.app.treasury.data.BankAccount
import com.abccash.app.treasury.data.TreasuryAccountKind
import com.abccash.app.treasury.data.TreasuryCalculations
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/* ============================================================
 * Palette & style — cockpit financier premium (Material 3)
 * ============================================================ */
private object Cockpit {
    val Blue = Color(0xFF1565C0)
    val Green = Color(0xFF2E7D32)
    val Orange = Color(0xFFFB8C00)
    val Red = Color(0xFFD32F2F)
    val Bg = Color(0xFFF8F9FA)
    val Card = Color.White
    val TextPrimary = Color(0xFF1A1C1E)
    val TextSecondary = Color(0xFF5F6368)
    val Track = Color(0xFFE8EAED)

    // Palette de rotation pour donuts / catégories
    val palette = listOf(
        Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFFFB8C00), Color(0xFFD32F2F),
        Color(0xFF6A1B9A), Color(0xFF00897B), Color(0xFF5D4037), Color(0xFF90A4AE)
    )
}

private val moneyFmt: NumberFormat = NumberFormat.getInstance(Locale.FRANCE).apply {
    maximumFractionDigits = 0
}
private val frLocale = Locale.FRENCH

private fun dt(value: Double): String = "${moneyFmt.format(value)} DT"
private fun dtCompact(value: Double): String {
    val k = value / 1000.0
    return if (abs(value) >= 1000) "${moneyFmt.format(k)}k" else moneyFmt.format(value)
}

/* ============================================================
 * Modèle de données du cockpit (calculé depuis les vraies données)
 * ============================================================ */
private data class KpiData(
    val label: String,
    val amount: Double,
    val delta: Double,
    val icon: ImageVector,
    val color: Color
)

private data class MonthBars(val label: String, val income: Double, val expense: Double)

private enum class RiskLevel(val label: String, val color: Color) {
    STABLE("Stable", Cockpit.Green),
    VIGILANCE("Vigilance", Cockpit.Orange),
    CRITIQUE("Critique", Cockpit.Red)
}

private data class ForecastPoint(val days: String, val balance: Double, val variation: Double, val risk: RiskLevel)
private data class LabeledSlice(val label: String, val value: Double, val color: Color)
private data class DueItem(val date: String, val label: String, val amount: Double, val color: Color, val incoming: Boolean)
private data class AlertData(val text: String, val icon: ImageVector, val color: Color)
private data class SeriesData(val points: List<Double>, val forecastStart: Int)

private class CockpitData(
    val kpis: List<KpiData>,
    val evolutionByPeriod: Map<String, SeriesData>,
    val bars: List<MonthBars>,
    val forecasts: List<ForecastPoint>,
    val expenseSlices: List<LabeledSlice>,
    val accountSlices: List<LabeledSlice>,
    val topExpenses: List<LabeledSlice>,
    val projection: List<Double>,
    val projectionDropText: String?,
    val dueItems: List<DueItem>,
    val score: Int,
    val scoreLabel: String,
    val scoreColor: Color,
    val factors: List<Triple<String, Float, Color>>,
    val alerts: List<AlertData>,
    val recommendations: List<String>
)

private val periods = listOf("7 j", "30 j", "3 m", "6 m", "12 m")

/* ============================================================
 * Écran principal
 * ============================================================ */
@Composable
fun CockpitDashboardScreen(
    userName: String = "Kamel",
    companyName: String = "",
    invoices: List<Invoice> = emptyList(),
    expenses: List<Expense> = emptyList(),
    bankAccounts: List<BankAccount> = emptyList(),
    openingBalance: Double = 0.0,
    bankBalanceOverride: Double? = null,
    onOpenDrawer: () -> Unit = {},
    onNavigateToAddIncome: () -> Unit = {},
    onNavigateToAddExpense: () -> Unit = {}
) {
    val scroll = rememberScrollState()
    val context = LocalContext.current

    // Répartition des dépenses réelle, avec libellés de catégories localisés
    val expenseLabeled: List<Pair<String, Double>> = remember(expenses, invoices, openingBalance, bankAccounts) {
        val slices = DashboardCalculations.buildInnovativeDashboard(
            invoices = invoices,
            expenses = expenses,
            bankBalance = null,
            bankAccounts = bankAccounts,
            openingBalance = openingBalance,
            viewMode = DashboardViewMode.YEAR
        ).expenseByCategory
        slices.map { slice ->
            val label = when {
                slice.label.isNotBlank() -> slice.label
                slice.revenueCategory != null -> context.getString(slice.revenueCategory!!.labelRes)
                slice.expenseCategory != null -> context.getString(slice.expenseCategory!!.labelRes)
                else -> "Autre"
            }
            label to slice.amount
        }
    }

    val data = remember(invoices, expenses, bankAccounts, openingBalance, bankBalanceOverride, expenseLabeled) {
        buildCockpitData(
            invoices = invoices,
            expenses = expenses,
            bankAccounts = bankAccounts,
            openingBalance = openingBalance,
            bankBalanceOverride = bankBalanceOverride,
            expenseLabeled = expenseLabeled
        )
    }

    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, tween(900, easing = LinearOutSlowInEasing))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cockpit.Bg)
            .verticalScroll(scroll)
            .padding(bottom = 96.dp)
    ) {
        CockpitHeader(userName = userName, onOpenDrawer = onOpenDrawer)

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            KpiRow(data.kpis)
            TreasuryEvolutionCard(data.evolutionByPeriod, reveal.value)
            IncomeVsExpenseCard(data.bars, reveal.value)
            ForecastCard(data.forecasts)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DonutCard(
                    title = "Répartition des dépenses",
                    centerLabel = "Dépenses",
                    slices = data.expenseSlices,
                    showPercent = true,
                    modifier = Modifier.weight(1f),
                    progress = reveal.value
                )
                DonutCard(
                    title = "Répartition des comptes",
                    centerLabel = "Comptes",
                    slices = data.accountSlices,
                    showPercent = false,
                    modifier = Modifier.weight(1f),
                    progress = reveal.value
                )
            }
            TopExpensesCard(data.topExpenses, reveal.value)
            ForecastProjectionCard(data.projection, data.projectionDropText, reveal.value)
            DueTimelineCard(data.dueItems)
            FinancialScoreCard(data.score, data.scoreLabel, data.scoreColor, data.factors, reveal.value)
            AlertsCard(data.alerts)
            AiRecommendationsCard(data.recommendations)
            Spacer(Modifier.height(8.dp))
        }
    }
}

/* ============================================================
 * Construction des données réelles
 * ============================================================ */
private fun deltaPct(current: Double, previous: Double): Double = when {
    previous == 0.0 -> if (current > 0) 100.0 else if (current < 0) -100.0 else 0.0
    else -> (current - previous) / abs(previous) * 100.0
}

private fun buildCockpitData(
    invoices: List<Invoice>,
    expenses: List<Expense>,
    bankAccounts: List<BankAccount>,
    openingBalance: Double,
    bankBalanceOverride: Double?,
    expenseLabeled: List<Pair<String, Double>>
): CockpitData {
    val today = LocalDate.now()
    val month = YearMonth.from(today)
    val prevMonth = month.minusMonths(1)

    val accountSummaries: List<BankAccountSummary> =
        if (bankAccounts.isEmpty()) emptyList()
        else BankAccountCalculations.summarize(bankAccounts, invoices, expenses)

    // --- Soldes ---
    val totalRealized = TreasuryCalculations.realizedBalance(invoices, expenses, openingBalance)
    val bankToday = bankBalanceOverride
        ?: if (accountSummaries.isNotEmpty()) {
            accountSummaries.filter { it.account.kind == TreasuryAccountKind.BANK }.sumOf { it.balance }
        } else {
            TreasuryCalculations.computedBankBalance(invoices, expenses) + openingBalance
        }

    // Courbes quotidiennes (30j historique + 30j prévision)
    val totalCurve = DashboardCalculations.buildBalanceCurve(invoices, expenses, totalRealized, today, bankOnly = false)
    val bankCurve = DashboardCalculations.buildBalanceCurve(invoices, expenses, bankToday, today, bankOnly = true)

    val totalMonthAgo = totalCurve.firstOrNull()?.balance ?: totalRealized
    val bankMonthAgo = bankCurve.firstOrNull()?.balance ?: bankToday

    val encMonth = TreasuryCalculations.ytdCollections(invoices, month)
    val encPrev = TreasuryCalculations.ytdCollections(invoices, prevMonth)
    val decMonth = TreasuryCalculations.ytdPaidExpenses(expenses, month, today)
    val decPrev = TreasuryCalculations.ytdPaidExpenses(expenses, prevMonth, today)

    val kpis = listOf(
        KpiData("Solde bancaire", bankToday, deltaPct(bankToday, bankMonthAgo), Icons.Filled.AccountBalance, Cockpit.Blue),
        KpiData("Trésorerie disponible", totalRealized, deltaPct(totalRealized, totalMonthAgo), Icons.Filled.AccountBalanceWallet, Cockpit.Green),
        KpiData("Encaissements (année)", encMonth, deltaPct(encMonth, encPrev), Icons.Filled.TrendingUp, Cockpit.Green),
        KpiData("Décaissements (année)", decMonth, deltaPct(decMonth, decPrev), Icons.Filled.TrendingDown, Cockpit.Red)
    )

    // --- Graphique évolution (sélecteur de période) ---
    val evolutionByPeriod = buildEvolutionSeries(invoices, expenses, openingBalance, totalCurve, today)

    // --- Barres encaissements / décaissements (6 mois) ---
    val bars = DashboardCalculations.buildRollingMonthlyBarChart(invoices, expenses, month, 6).map {
        MonthBars(
            label = it.month.month.getDisplayName(TextStyle.SHORT, frLocale).replaceFirstChar { c -> c.uppercase() },
            income = it.income,
            expense = it.expenses
        )
    }

    // --- Prévisions 7 / 30 / 60 / 90 jours ---
    fun forecastAt(days: Long): Double {
        val items = EcheanceForecast.buildItems(invoices, expenses, today.plusDays(1), today.plusDays(days))
        val net = items.sumOf { if (it.type == EcheanceType.INCOME) it.amount else -it.amount }
        return totalRealized + net
    }
    fun riskOf(estimate: Double, variation: Double): RiskLevel = when {
        estimate < 0 -> RiskLevel.CRITIQUE
        variation <= -15.0 -> RiskLevel.VIGILANCE
        else -> RiskLevel.STABLE
    }
    val forecasts = listOf(7L, 30L, 60L, 90L).map { d ->
        val est = forecastAt(d)
        val varr = deltaPct(est, totalRealized)
        ForecastPoint("$d j", est, varr, riskOf(est, varr))
    }

    // --- Donut dépenses ---
    val expenseSlices = buildTopSlices(expenseLabeled, maxSlices = 7)

    // --- Donut comptes ---
    val accountSlices = accountSummaries
        .map { it.account.name to it.balance }
        .filter { it.second != 0.0 }
        .mapIndexed { i, (name, bal) -> LabeledSlice(name, abs(bal), Cockpit.palette[i % Cockpit.palette.size]) }

    // --- Top 5 dépenses ---
    val topExpenses = expenseLabeled
        .sortedByDescending { it.second }
        .take(5)
        .mapIndexed { i, (label, amount) -> LabeledSlice(label, amount, Cockpit.palette[i % Cockpit.palette.size]) }

    // --- Projection 90 jours ---
    val projectionOffsets = listOf(0L, 10L, 20L, 30L, 45L, 60L, 75L, 90L)
    val projection = projectionOffsets.map { off ->
        if (off == 0L) totalRealized
        else {
            val items = EcheanceForecast.buildItems(invoices, expenses, today.plusDays(1), today.plusDays(off))
            totalRealized + items.sumOf { if (it.type == EcheanceType.INCOME) it.amount else -it.amount }
        }
    }
    val projFirst = projection.first()
    val projLast = projection.last()
    val projectionDropText = if (projLast < projFirst && projFirst != 0.0) {
        val drop = ((projFirst - projLast) / abs(projFirst) * 100).roundToInt()
        "Au rythme actuel, le solde baisse de $drop % en 90 jours."
    } else null

    // --- Calendrier des échéances ---
    val dayFmt = DateTimeFormatter.ofPattern("dd MMM", frLocale)
    val dueItems = EcheanceForecast.buildItems(invoices, expenses, today, today.plusDays(90))
        .take(8)
        .mapIndexed { i, item ->
            val incoming = item.type == EcheanceType.INCOME
            DueItem(
                date = item.dueDate.format(dayFmt),
                label = item.label.ifBlank { if (incoming) "Encaissement" else "Décaissement" },
                amount = item.amount,
                color = if (incoming) Cockpit.Green else Cockpit.palette[(i + 2) % Cockpit.palette.size],
                incoming = incoming
            )
        }

    // --- Score financier ---
    val health = DashboardCalculations.buildFinancialHealth(
        invoices = invoices, expenses = expenses, bankAccounts = bankAccounts,
        openingBalance = openingBalance, focusMonth = month, today = today
    )
    val receivable = invoices.filter { it.status != InvoiceStatus.PAID }.sumOf { it.remainingAmount }
    val overdueAmount = invoices.filter { it.status != InvoiceStatus.PAID && it.dueDate.isBefore(today) }.sumOf { it.remainingAmount }
    val monthDep = TreasuryCalculations.monthlyDepenses(expenses, month)
    val monthUnpaid = TreasuryCalculations.monthlyUnpaidExpenses(expenses, month)
    val anyAccountNegative = accountSummaries.any { it.balance < 0 }

    val fLiquidite = when {
        totalRealized <= 0 -> 0f
        health.autonomyDays == null -> 1f
        else -> (health.autonomyDays!! / 90f).coerceIn(0f, 1f)
    }
    val fCreances = if (receivable > 0) (1f - (overdueAmount / receivable).toFloat()).coerceIn(0f, 1f) else 1f
    val fDettes = if (monthDep > 0) (1f - (monthUnpaid / monthDep).toFloat()).coerceIn(0f, 1f) else 1f
    val fPrevisions = when {
        health.riskMonth == null -> 1f
        !health.riskMonth!!.isAfter(month.plusMonths(1)) -> 0.2f
        else -> 0.5f
    }
    val fDecouvert = when {
        totalRealized < 0 -> 0f
        anyAccountNegative -> 0.3f
        else -> 1f
    }
    val score = (((fLiquidite + fCreances + fDettes + fPrevisions + fDecouvert) / 5f) * 100).roundToInt()
    val (scoreLabel, scoreColor) = when {
        score >= 75 -> "Bonne santé financière" to Cockpit.Green
        score >= 50 -> "Vigilance requise" to Cockpit.Orange
        else -> "Situation critique" to Cockpit.Red
    }
    val factors = listOf(
        Triple("Liquidité", fLiquidite, factorColor(fLiquidite)),
        Triple("Créances", fCreances, factorColor(fCreances)),
        Triple("Dettes", fDettes, factorColor(fDettes)),
        Triple("Prévisions", fPrevisions, factorColor(fPrevisions)),
        Triple("Découvert", fDecouvert, factorColor(fDecouvert))
    )

    // --- Alertes ---
    val alerts = buildAlerts(invoices, expenses, accountSummaries, health, today, totalRealized)

    // --- Recommandations IA ---
    val recommendations = buildRecommendations(invoices, expenseLabeled, overdueAmount, today)

    return CockpitData(
        kpis = kpis,
        evolutionByPeriod = evolutionByPeriod,
        bars = bars,
        forecasts = forecasts,
        expenseSlices = expenseSlices,
        accountSlices = accountSlices,
        topExpenses = topExpenses,
        projection = projection,
        projectionDropText = projectionDropText,
        dueItems = dueItems,
        score = score,
        scoreLabel = scoreLabel,
        scoreColor = scoreColor,
        factors = factors,
        alerts = alerts,
        recommendations = recommendations
    )
}

private fun factorColor(v: Float): Color = when {
    v >= 0.7f -> Cockpit.Green
    v >= 0.4f -> Cockpit.Orange
    else -> Cockpit.Red
}

private fun buildTopSlices(labeled: List<Pair<String, Double>>, maxSlices: Int): List<LabeledSlice> {
    val positive = labeled.filter { it.second > 0 }.sortedByDescending { it.second }
    if (positive.isEmpty()) return emptyList()
    val head = positive.take(maxSlices - 1)
    val rest = positive.drop(maxSlices - 1)
    val result = head.mapIndexed { i, (label, amount) ->
        LabeledSlice(label, amount, Cockpit.palette[i % Cockpit.palette.size])
    }.toMutableList()
    if (rest.isNotEmpty()) {
        result.add(LabeledSlice("Divers", rest.sumOf { it.second }, Cockpit.palette[(maxSlices - 1) % Cockpit.palette.size]))
    }
    return result
}

private fun buildEvolutionSeries(
    invoices: List<Invoice>,
    expenses: List<Expense>,
    openingBalance: Double,
    totalCurve: List<com.abccash.app.treasury.data.DashboardBalancePoint>,
    today: LocalDate
): Map<String, SeriesData> {
    fun dailyWindow(backDays: Int, fwdDays: Int): SeriesData {
        val history = totalCurve.filter { !it.isForecast }
        val forecast = totalCurve.filter { it.isForecast }
        val hist = history.takeLast(backDays + 1)
        val fwd = forecast.take(fwdDays)
        val pts = (hist + fwd).map { it.balance }
        return SeriesData(pts, (hist.size - 1).coerceAtLeast(0))
    }

    val rows = TreasuryCalculations.calendarYearChartRows(
        invoices = invoices, expenses = expenses, year = today.year,
        today = YearMonth.from(today), openingBalance = openingBalance
    )
    val todayMonthIdx = today.monthValue - 1

    fun monthWindow(back: Int, fwd: Int): SeriesData {
        val fromIdx = (todayMonthIdx - back).coerceIn(0, 11)
        val toIdx = (todayMonthIdx + fwd).coerceIn(0, 11)
        val window = rows.subList(fromIdx, toIdx + 1)
        val pts = window.map { row ->
            if (row.month.monthValue - 1 <= todayMonthIdx) row.realizedCumulative else row.forecastCumulative
        }
        val fStart = (todayMonthIdx - fromIdx).coerceIn(0, pts.size - 1)
        return SeriesData(pts, fStart)
    }

    return mapOf(
        "7 j" to dailyWindow(7, 4),
        "30 j" to dailyWindow(30, 10),
        "3 m" to monthWindow(2, 1),
        "6 m" to monthWindow(4, 1),
        "12 m" to monthWindow(11, 0)
    )
}

private fun buildAlerts(
    invoices: List<Invoice>,
    expenses: List<Expense>,
    accounts: List<BankAccountSummary>,
    health: com.abccash.app.treasury.data.FinancialHealthSummary,
    today: LocalDate,
    totalRealized: Double
): List<AlertData> {
    val alerts = mutableListOf<AlertData>()

    health.riskMonth?.let { risk ->
        val label = risk.month.getDisplayName(TextStyle.FULL, frLocale)
        alerts.add(AlertData("Votre trésorerie prévisionnelle devient négative en $label ${risk.year}.", Icons.Filled.TrendingDown, Cockpit.Red))
    }
    if (alerts.isEmpty() && health.autonomyDays != null && health.autonomyDays!! < 60) {
        alerts.add(AlertData("Votre trésorerie ne couvre plus que ${health.autonomyDays} jours de charges.", Icons.Filled.TrendingDown, Cockpit.Orange))
    }

    val overdue = invoices.filter { it.status != InvoiceStatus.PAID && it.dueDate.isBefore(today) }
        .sortedBy { it.dueDate }
    overdue.firstOrNull()?.let { inv ->
        val days = ChronoUnit.DAYS.between(inv.dueDate, today)
        alerts.add(AlertData("Facture ${inv.clientName.ifBlank { inv.invoiceNumber }} de ${dt(inv.remainingAmount)} en retard depuis $days jours.", Icons.Filled.ReceiptLong, Cockpit.Orange))
    }

    val nextExpense = EcheanceForecast.buildItems(expenses = expenses, invoices = emptyList(), from = today, to = today.plusDays(15))
        .filter { it.type == EcheanceType.EXPENSE }
        .minByOrNull { it.dueDate }
    nextExpense?.let { item ->
        val days = ChronoUnit.DAYS.between(today, item.dueDate)
        alerts.add(AlertData("${item.label} (${dt(item.amount)}) à régler dans $days jours.", Icons.Filled.Schedule, Cockpit.Orange))
    }

    accounts.firstOrNull { it.balance < 0 }?.let { acc ->
        alerts.add(AlertData("Le compte ${acc.account.name} est en solde négatif (${dt(acc.balance)}).", Icons.Filled.AccountBalance, Cockpit.Red))
    } ?: accounts.firstOrNull { it.hasLowBalanceAlert }?.let { acc ->
        alerts.add(AlertData("Le compte ${acc.account.name} est sous son seuil d'alerte.", Icons.Filled.AccountBalance, Cockpit.Orange))
    }

    if (alerts.isEmpty()) {
        alerts.add(AlertData("Aucune alerte : votre trésorerie est sous contrôle.", Icons.Filled.CheckCircle, Cockpit.Green))
    }
    return alerts.take(4)
}

private fun buildRecommendations(
    invoices: List<Invoice>,
    expenseLabeled: List<Pair<String, Double>>,
    overdueAmount: Double,
    today: LocalDate
): List<String> {
    val recos = mutableListOf<String>()

    val topOverdue = invoices.filter { it.status != InvoiceStatus.PAID && it.dueDate.isBefore(today) }
        .maxByOrNull { it.remainingAmount }
    topOverdue?.let {
        recos.add("Relancer ${it.clientName.ifBlank { it.invoiceNumber }} : ${dt(it.remainingAmount)} en retard à encaisser.")
    }

    val totalExpenses = expenseLabeled.sumOf { it.second }
    if (totalExpenses > 0) {
        val top = expenseLabeled.maxByOrNull { it.second }
        top?.let {
            val pct = (it.second / totalExpenses * 100).roundToInt()
            recos.add("${it.first} représente $pct % de vos décaissements.")
        }
    }

    val byClient = invoices.groupBy { it.clientName.ifBlank { it.invoiceNumber } }
        .mapValues { entry -> entry.value.sumOf { it.totalAmount } }
    val totalCA = byClient.values.sum()
    if (totalCA > 0) {
        val topClient = byClient.maxByOrNull { it.value }
        topClient?.let {
            val pct = (it.value / totalCA * 100).roundToInt()
            if (pct >= 15) recos.add("Le client ${it.key} représente $pct % de votre chiffre d'affaires.")
        }
    }

    if (overdueAmount > 0) {
        recos.add("Encaisser vos créances en retard renforcerait votre trésorerie de ${dt(overdueAmount)}.")
    }

    if (recos.isEmpty()) {
        recos.add("Continuez ainsi : constituez une réserve de sécurité avec votre excédent de trésorerie.")
    }
    return recos.take(5)
}

/* ============================================================
 * Carte de base
 * ============================================================ */
@Composable
private fun CockpitCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = Color(0x14000000)),
        shape = RoundedCornerShape(16.dp),
        color = Cockpit.Card
    ) {
        Column(modifier = Modifier.padding(padding), content = content)
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String? = null) {
    Column {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Cockpit.TextPrimary)
        if (subtitle != null) {
            Text(subtitle, fontSize = 12.sp, color = Cockpit.TextSecondary)
        }
    }
}

/* ============================================================
 * HEADER
 * ============================================================ */
@Composable
private fun CockpitHeader(userName: String, onOpenDrawer: () -> Unit) {
    val todayText = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy", frLocale))
    }
    val syncText = remember {
        LocalDate.now().atTime(java.time.LocalTime.now())
            .format(DateTimeFormatter.ofPattern("HH:mm", frLocale))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Cockpit.Blue, Color(0xFF0D47A1))))
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp, bottom = 22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Bonjour ${userName.replaceFirstChar { it.uppercase() }}",
                    fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White
                )
                Text("Aujourd'hui : $todayText", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Sync, contentDescription = null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Dernière synchronisation à $syncText", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                }
            }
            HeaderIcon(Icons.Filled.NotificationsNone, badge = true) {}
            Spacer(Modifier.width(6.dp))
            HeaderIcon(Icons.Filled.Search) {}
            Spacer(Modifier.width(6.dp))
            HeaderIcon(Icons.Filled.AccountCircle) { onOpenDrawer() }
        }
    }
}

@Composable
private fun HeaderIcon(icon: ImageVector, badge: Boolean = false, onClick: () -> Unit) {
    Box {
        Surface(onClick = onClick, shape = CircleShape, color = Color.White.copy(alpha = 0.15f), modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
            }
        }
        if (badge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(8.dp)
                    .background(Cockpit.Orange, CircleShape)
            )
        }
    }
}

/* ============================================================
 * KPIs
 * ============================================================ */
@Composable
private fun KpiRow(kpis: List<KpiData>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        kpis.forEach { KpiCard(it) }
    }
}

@Composable
private fun KpiCard(kpi: KpiData) {
    val positive = kpi.delta >= 0
    val deltaColor = if (positive) Cockpit.Green else Cockpit.Red
    Surface(
        modifier = Modifier
            .width(158.dp)
            .shadow(5.dp, RoundedCornerShape(16.dp), spotColor = Color(0x14000000)),
        shape = RoundedCornerShape(16.dp),
        color = Cockpit.Card
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(kpi.color.copy(alpha = 0.12f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(kpi.icon, contentDescription = null, tint = kpi.color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(kpi.label, fontSize = 12.sp, color = Cockpit.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(dt(kpi.amount), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Cockpit.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (positive) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                    contentDescription = null, tint = deltaColor, modifier = Modifier.size(13.dp)
                )
                Text(
                    "${if (positive) "+" else ""}${"%.1f".format(kpi.delta)} %",
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = deltaColor
                )
                Text(" vs mois préc.", fontSize = 10.sp, color = Cockpit.TextSecondary, maxLines = 1)
            }
        }
    }
}

/* ============================================================
 * GRAPHIQUE PRINCIPAL — Évolution de la trésorerie
 * ============================================================ */
@Composable
private fun TreasuryEvolutionCard(series: Map<String, SeriesData>, progress: Float) {
    var selected by remember { mutableStateOf("30 j") }
    val current = series[selected] ?: SeriesData(emptyList(), 0)

    CockpitCard {
        SectionTitle("Évolution de la trésorerie", "Solde réel & prévisionnel")
        Spacer(Modifier.height(12.dp))
        SegmentedSelector(options = periods, selected = selected, onSelect = { selected = it })
        Spacer(Modifier.height(16.dp))
        if (current.points.size < 2) {
            EmptyChartHint()
        } else {
            LineChart(
                points = current.points,
                forecastStart = current.forecastStart,
                lineColor = Cockpit.Blue,
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(Cockpit.Blue, "Solde réel")
            LegendDot(Cockpit.Blue.copy(alpha = 0.5f), "Prévisionnel", dashed = true)
        }
    }
}

@Composable
private fun SegmentedSelector(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = Cockpit.Bg, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(4.dp)) {
            options.forEach { opt ->
                val active = opt == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (active) Cockpit.Blue else Color.Transparent)
                        .clickable { onSelect(opt) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        opt, fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) Color.White else Cockpit.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyChartHint() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Pas encore assez de données", fontSize = 13.sp, color = Cockpit.TextSecondary)
    }
}

@Composable
private fun LineChart(
    points: List<Double>,
    forecastStart: Int,
    lineColor: Color,
    progress: Float,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    val minV = minOf(points.minOrNull() ?: 0.0, 0.0)
    val maxV = max(points.maxOrNull() ?: 1.0, 1.0)
    val range = max(maxV - minV, 1.0)
    val count = points.size

    Canvas(modifier) {
        val padL = 8.dp.toPx(); val padR = 8.dp.toPx(); val padT = 10.dp.toPx(); val padB = 10.dp.toPx()
        val w = size.width - padL - padR
        val h = size.height - padT - padB

        fun x(i: Int) = padL + w * i / (count - 1).coerceAtLeast(1)
        fun y(v: Double) = padT + h * (1f - ((v - minV) / range).toFloat())

        for (i in 0..3) {
            val gy = padT + h * i / 3f
            drawLine(Cockpit.Track, Offset(padL, gy), Offset(size.width - padR, gy), 1.dp.toPx())
        }

        val shown = (count * progress).toInt().coerceIn(2, count)
        val realEnd = forecastStart.coerceIn(0, count - 1)

        val areaEnd = realEnd.coerceAtMost(shown - 1)
        val areaPath = Path().apply {
            moveTo(x(0), y(points[0]))
            for (i in 1..areaEnd) lineTo(x(i), y(points[i]))
            lineTo(x(areaEnd), padT + h)
            lineTo(x(0), padT + h)
            close()
        }
        drawPath(areaPath, brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.22f), lineColor.copy(alpha = 0f))))

        val realPath = Path().apply {
            moveTo(x(0), y(points[0]))
            for (i in 1..areaEnd) lineTo(x(i), y(points[i]))
        }
        drawPath(realPath, color = lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

        if (shown - 1 > realEnd) {
            val fPath = Path().apply {
                moveTo(x(realEnd), y(points[realEnd]))
                for (i in (realEnd + 1) until shown) lineTo(x(i), y(points[i]))
            }
            drawPath(
                fPath, color = lineColor.copy(alpha = 0.55f),
                style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            )
        }

        val lastIdx = shown - 1
        drawCircle(Color.White, 6.dp.toPx(), Offset(x(lastIdx), y(points[lastIdx])))
        drawCircle(lineColor, 4.dp.toPx(), Offset(x(lastIdx), y(points[lastIdx])))
    }
}

/* ============================================================
 * ENCAISSEMENTS VS DÉCAISSEMENTS
 * ============================================================ */
@Composable
private fun IncomeVsExpenseCard(data: List<MonthBars>, progress: Float) {
    CockpitCard {
        SectionTitle("Encaissements vs Décaissements", "6 derniers mois")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(Cockpit.Green, "Encaissements")
            LegendDot(Cockpit.Red, "Décaissements")
        }
        Spacer(Modifier.height(12.dp))
        GroupedBarChart(
            data = data, progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        )
    }
}

@Composable
private fun GroupedBarChart(data: List<MonthBars>, progress: Float, modifier: Modifier = Modifier) {
    val maxV = max(data.maxOfOrNull { max(it.income, it.expense) } ?: 1.0, 1.0)
    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (data.isEmpty()) return@Canvas
            val groupW = size.width / data.size
            val barW = groupW * 0.28f
            val gap = groupW * 0.10f
            data.forEachIndexed { i, m ->
                val cx = groupW * i + groupW / 2f
                val incH = (m.income / maxV).toFloat() * size.height * progress
                val expH = (m.expense / maxV).toFloat() * size.height * progress
                drawRoundRect(
                    color = Cockpit.Green,
                    topLeft = Offset(cx - barW - gap / 2, size.height - incH),
                    size = Size(barW, incH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = Cockpit.Red,
                    topLeft = Offset(cx + gap / 2, size.height - expH),
                    size = Size(barW, expH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            data.forEach {
                Text(it.label, modifier = Modifier.weight(1f), fontSize = 11.sp, color = Cockpit.TextSecondary, textAlign = TextAlign.Center)
            }
        }
    }
}

/* ============================================================
 * PRÉVISION
 * ============================================================ */
@Composable
private fun ForecastCard(forecasts: List<ForecastPoint>) {
    if (forecasts.isEmpty()) return
    var selected by remember { mutableStateOf(forecasts.getOrElse(1) { forecasts.first() }) }
    CockpitCard {
        SectionTitle("Prévision de trésorerie")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            forecasts.forEach { fp ->
                val active = fp == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (active) Cockpit.Blue.copy(alpha = 0.1f) else Cockpit.Bg)
                        .clickable { selected = fp }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        fp.days, fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) Cockpit.Blue else Cockpit.TextSecondary
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Solde estimé à ${selected.days}", fontSize = 12.sp, color = Cockpit.TextSecondary)
                Text(dt(selected.balance), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Cockpit.TextPrimary)
                val vColor = if (selected.variation >= 0) Cockpit.Green else Cockpit.Red
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (selected.variation >= 0) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                        contentDescription = null, tint = vColor, modifier = Modifier.size(14.dp)
                    )
                    Text("${if (selected.variation >= 0) "+" else ""}${"%.1f".format(selected.variation)} %", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = vColor)
                }
            }
            RiskPill(selected.risk)
        }
    }
}

@Composable
private fun RiskPill(risk: RiskLevel) {
    Surface(shape = RoundedCornerShape(50), color = risk.color.copy(alpha = 0.12f)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).background(risk.color, CircleShape))
            Spacer(Modifier.width(7.dp))
            Text(risk.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = risk.color)
        }
    }
}

/* ============================================================
 * DONUT (dépenses / comptes)
 * ============================================================ */
@Composable
private fun DonutCard(
    title: String,
    centerLabel: String,
    slices: List<LabeledSlice>,
    showPercent: Boolean,
    modifier: Modifier = Modifier,
    progress: Float
) {
    CockpitCard(modifier = modifier, padding = 14.dp) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Cockpit.TextPrimary)
        Spacer(Modifier.height(12.dp))
        if (slices.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Aucune donnée", fontSize = 12.sp, color = Cockpit.TextSecondary)
            }
        } else {
            DonutChart(slices, progress, centerLabel, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(12.dp))
            DonutLegend(slices, showPercent)
        }
    }
}

@Composable
private fun DonutChart(slices: List<LabeledSlice>, progress: Float, centerLabel: String, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.value }.coerceAtLeast(1.0)
    Box(modifier = modifier.size(130.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 20.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            var start = -90f
            slices.forEach { s ->
                val sweep = (s.value / total).toFloat() * 360f * progress
                drawArc(color = s.color, startAngle = start, sweepAngle = sweep, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width = stroke, cap = StrokeCap.Butt))
                start += (s.value / total).toFloat() * 360f
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(dtCompact(total), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Cockpit.TextPrimary)
            Text(centerLabel, fontSize = 11.sp, color = Cockpit.TextSecondary)
        }
    }
}

@Composable
private fun DonutLegend(slices: List<LabeledSlice>, showPercent: Boolean) {
    val total = slices.sumOf { it.value }.coerceAtLeast(1.0)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        slices.forEach { s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).background(s.color, CircleShape))
                Spacer(Modifier.width(7.dp))
                Text(s.label, fontSize = 11.sp, color = Cockpit.TextSecondary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (showPercent) "${(s.value / total * 100).roundToInt()} %" else dtCompact(s.value),
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Cockpit.TextPrimary
                )
            }
        }
    }
}

/* ============================================================
 * TOP DÉPENSES
 * ============================================================ */
@Composable
private fun TopExpensesCard(items: List<LabeledSlice>, progress: Float) {
    CockpitCard {
        SectionTitle("Top 5 des dépenses", "Cette année")
        Spacer(Modifier.height(14.dp))
        if (items.isEmpty()) {
            Text("Aucune dépense enregistrée", fontSize = 13.sp, color = Cockpit.TextSecondary)
        } else {
            val maxV = items.maxOf { it.value }
            items.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 5.dp)) {
                    Text(s.label, fontSize = 12.sp, color = Cockpit.TextPrimary, modifier = Modifier.width(80.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Cockpit.Track)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((s.value / maxV).toFloat() * progress)
                                .clip(RoundedCornerShape(50))
                                .background(s.color)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(dtCompact(s.value), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Cockpit.TextPrimary, modifier = Modifier.width(46.dp), textAlign = TextAlign.End)
                }
            }
        }
    }
}

/* ============================================================
 * TRÉSORERIE PRÉVISIONNELLE (90 jours)
 * ============================================================ */
@Composable
private fun ForecastProjectionCard(points: List<Double>, dropText: String?, progress: Float) {
    CockpitCard {
        SectionTitle("Trésorerie prévisionnelle", "Projection sur 90 jours")
        Spacer(Modifier.height(14.dp))
        if (points.size < 2) {
            EmptyChartHint()
        } else {
            LineChart(
                points = points, forecastStart = 0, lineColor = Cockpit.Orange, progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
        }
        if (dropText != null) {
            Spacer(Modifier.height(10.dp))
            Surface(shape = RoundedCornerShape(10.dp), color = Cockpit.Orange.copy(alpha = 0.1f)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = Cockpit.Orange, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(dropText, fontSize = 12.sp, color = Color(0xFF8A5000), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/* ============================================================
 * CALENDRIER DES ÉCHÉANCES
 * ============================================================ */
@Composable
private fun DueTimelineCard(items: List<DueItem>) {
    CockpitCard {
        SectionTitle("Calendrier des échéances")
        Spacer(Modifier.height(12.dp))
        if (items.isEmpty()) {
            Text("Aucune échéance à venir", fontSize = 13.sp, color = Cockpit.TextSecondary)
        } else {
            items.forEachIndexed { i, item ->
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp)) {
                        Box(Modifier.size(12.dp).background(item.color, CircleShape))
                        if (i != items.lastIndex) {
                            Box(
                                Modifier
                                    .width(2.dp)
                                    .weight(1f)
                                    .background(Cockpit.Track)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = if (i != items.lastIndex) 14.dp else 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Cockpit.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.date, fontSize = 11.sp, color = Cockpit.TextSecondary)
                        }
                        Text(
                            "${if (item.incoming) "+" else "-"}${dt(item.amount)}",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = if (item.incoming) Cockpit.Green else Cockpit.TextPrimary
                        )
                    }
                }
            }
        }
    }
}

/* ============================================================
 * SCORE FINANCIER
 * ============================================================ */
@Composable
private fun FinancialScoreCard(
    score: Int,
    label: String,
    color: Color,
    factors: List<Triple<String, Float, Color>>,
    progress: Float
) {
    val animated by animateFloatAsState(targetValue = score / 100f * progress, label = "score")
    CockpitCard {
        SectionTitle("Score financier")
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(130.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 16.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    val topLeft = Offset(inset, inset)
                    drawArc(color = Cockpit.Track, startAngle = 135f, sweepAngle = 270f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    drawArc(color = color, startAngle = 135f, sweepAngle = 270f * animated, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$score", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Cockpit.TextPrimary)
                    Text("/ 100", fontSize = 12.sp, color = Cockpit.TextSecondary)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
                Spacer(Modifier.height(8.dp))
                factors.forEach { (name, value, c) -> ScoreFactor(name, value, c) }
            }
        }
    }
}

@Composable
private fun ScoreFactor(label: String, value: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, fontSize = 11.sp, color = Cockpit.TextSecondary, modifier = Modifier.width(70.dp))
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Cockpit.Track)
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(value.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

/* ============================================================
 * ALERTES
 * ============================================================ */
@Composable
private fun AlertsCard(alerts: List<AlertData>) {
    CockpitCard {
        SectionTitle("Alertes", "${alerts.size} à surveiller")
        Spacer(Modifier.height(12.dp))
        alerts.forEach { a ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(34.dp).background(a.color.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(a.icon, contentDescription = null, tint = a.color, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(a.text, fontSize = 13.sp, color = Cockpit.TextPrimary, modifier = Modifier.weight(1f))
            }
        }
    }
}

/* ============================================================
 * RECOMMANDATIONS IA
 * ============================================================ */
@Composable
private fun AiRecommendationsCard(recos: List<String>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color(0x22000000)),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(Color(0xFF1565C0), Color(0xFF6A1B9A))))
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Recommandations IA", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(14.dp))
            recos.forEach { r ->
                Row(modifier = Modifier.padding(vertical = 6.dp)) {
                    Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(r, fontSize = 13.sp, color = Color.White.copy(alpha = 0.95f), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/* ============================================================
 * Helpers communs
 * ============================================================ */
@Composable
private fun LegendDot(color: Color, label: String, dashed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (dashed) {
            Canvas(Modifier.size(width = 16.dp, height = 3.dp)) {
                drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)))
            }
        } else {
            Box(Modifier.size(10.dp).background(color, CircleShape))
        }
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = Cockpit.TextSecondary)
    }
}
