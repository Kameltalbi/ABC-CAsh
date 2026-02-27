package com.abccash.app.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abccash.app.data.local.entity.CurrencyCode
import com.abccash.app.data.local.entity.CategoryEntity
import com.abccash.app.data.local.entity.RecurrenceRule
import com.abccash.app.data.local.entity.TransactionEntity
import com.abccash.app.data.local.entity.TransactionStatus
import com.abccash.app.data.local.entity.TransactionType
import com.abccash.app.domain.model.CumulativeForecastPoint
import com.abccash.app.presentation.CashflowViewModel
import com.abccash.app.sync.readBackupFromTree
import com.abccash.app.sync.writeBackupToTree
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CashflowApp(viewModel: CashflowViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(1) }
    val context = LocalContext.current
    val settingsStore = remember { AppSettingsStore(context) }
    var darkMode by remember { mutableStateOf(settingsStore.isDarkMode()) }
    var dateFormat by remember { mutableStateOf(settingsStore.getDateFormat()) }
    var palette by remember { mutableStateOf(settingsStore.getPalette()) }
    var driveSyncFolderUri by remember { mutableStateOf(settingsStore.getDriveSyncFolderUri()) }
    var lastDriveSyncStatus by remember {
        mutableStateOf(
            if (settingsStore.getDriveSyncFolderUri().isNullOrBlank()) "Drive non configure"
            else "Drive connecte - en attente de sync"
        )
    }
    var restoringFromDrive by remember { mutableStateOf(false) }
    var startupRestoreDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.setCurrency(settingsStore.getDefaultCurrency())
    }

    var pendingBackupJson by remember { mutableStateOf<String?>(null) }
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = pendingBackupJson
        if (uri != null && json != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            }.onSuccess {
                Toast.makeText(context, "Sauvegarde terminee", Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(context, "Erreur sauvegarde", Toast.LENGTH_LONG).show()
            }
        }
        pendingBackupJson = null
    }
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BufferedReader(InputStreamReader(input)).readText()
                } ?: ""
            }.onSuccess { json ->
                viewModel.importBackup(
                    json = json,
                    onSuccess = {
                        Toast.makeText(context, "Restauration terminee", Toast.LENGTH_LONG).show()
                    },
                    onError = {
                        Toast.makeText(context, "Erreur restauration: $it", Toast.LENGTH_LONG).show()
                    }
                )
            }.onFailure {
                Toast.makeText(context, "Erreur lecture fichier", Toast.LENGTH_LONG).show()
            }
        }
    }
    val pickDriveFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            settingsStore.setDriveSyncFolderUri(uri.toString())
            driveSyncFolderUri = uri.toString()
            lastDriveSyncStatus = "Drive connecte - pret"
            Toast.makeText(context, "Dossier Drive connecte", Toast.LENGTH_LONG).show()
        }
    }

    fun triggerDriveSync(showToast: Boolean) {
        val uri = driveSyncFolderUri
        if (uri.isNullOrBlank()) {
            if (showToast) Toast.makeText(context, "Choisis d'abord un dossier Drive", Toast.LENGTH_LONG).show()
            return
        }
        lastDriveSyncStatus = "Sync Drive en cours..."
        viewModel.exportBackup(
            onSuccess = { json ->
                writeBackupToTree(context, uri, json)
                    .onSuccess {
                        lastDriveSyncStatus = "Derniere sync: ${java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))}"
                        if (showToast) Toast.makeText(context, "Sync Drive terminee", Toast.LENGTH_LONG).show()
                    }
                    .onFailure { err ->
                        lastDriveSyncStatus = "Erreur sync: ${err.message ?: "inconnue"}"
                        if (showToast) Toast.makeText(context, "Erreur sync Drive", Toast.LENGTH_LONG).show()
                    }
            },
            onError = { err ->
                lastDriveSyncStatus = "Erreur export: $err"
                if (showToast) Toast.makeText(context, "Erreur export backup", Toast.LENGTH_LONG).show()
            }
        )
    }

    LaunchedEffect(driveSyncFolderUri) {
        if (startupRestoreDone || driveSyncFolderUri.isNullOrBlank()) return@LaunchedEffect
        startupRestoreDone = true
        restoringFromDrive = true
        lastDriveSyncStatus = "Restauration Drive au demarrage..."
        readBackupFromTree(context, driveSyncFolderUri!!)
            .onSuccess { json ->
                viewModel.importBackup(
                    json = json,
                    onSuccess = {
                        restoringFromDrive = false
                        lastDriveSyncStatus = "Restauration Drive terminee"
                        Toast.makeText(context, "Restauration Drive terminee", Toast.LENGTH_LONG).show()
                    },
                    onError = { err ->
                        restoringFromDrive = false
                        lastDriveSyncStatus = "Erreur restauration: $err"
                    }
                )
            }
            .onFailure {
                restoringFromDrive = false
                lastDriveSyncStatus = "Drive connecte - aucune sauvegarde trouvee"
            }
    }

    LaunchedEffect(state.dataChangeVersion, driveSyncFolderUri) {
        if (state.dataChangeVersion > 0 && !driveSyncFolderUri.isNullOrBlank() && !restoringFromDrive) {
            triggerDriveSync(showToast = false)
        }
    }

    MaterialTheme(colorScheme = appColorScheme(darkMode, palette)) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("ABC Cash", color = MaterialTheme.colorScheme.onPrimary) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                HeaderMenu(
                    selectedTab = selectedTab,
                    onSelectedTab = { selectedTab = it },
                    onOpenSettings = { selectedTab = 3 },
                    onOpenCategories = { selectedTab = 4 },
                    onExportDashboard = {
                        runCatching {
                            exportDashboardPdf(
                                context = context,
                                openingMinor = state.openingBalanceMinor,
                                forecastRows = state.forecast,
                                currency = state.selectedCurrency
                            )
                        }.onSuccess { file ->
                            Toast.makeText(context, "PDF dashboard exporte: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                        }.onFailure {
                            Toast.makeText(context, "Erreur export dashboard", Toast.LENGTH_LONG).show()
                        }
                    },
                    onExportTransactions = {
                        runCatching {
                            exportTransactionsByMonthPdf(
                                context = context,
                                transactions = state.transactions,
                                currency = state.selectedCurrency
                            )
                        }.onSuccess { file ->
                            Toast.makeText(context, "PDF transactions exporte: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                        }.onFailure {
                            Toast.makeText(context, "Erreur export transactions", Toast.LENGTH_LONG).show()
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> DashboardTab(
                        openingMinor = state.openingBalanceMinor,
                        forecastRows = state.forecast,
                        currency = state.selectedCurrency
                    )

                    1 -> TransactionsTab(
                        openingMinor = state.openingBalanceMinor,
                        onAddTransaction = viewModel::addQuickTransaction,
                        onValidate = viewModel::validateTransaction,
                        onEditTransaction = viewModel::editTransaction,
                        onSetStatus = viewModel::setTransactionStatus,
                        onPostpone = viewModel::postponeTransactionOneMonth,
                        onDelete = viewModel::deleteTransaction,
                        transactions = state.transactions,
                        categories = state.categories,
                        currency = state.selectedCurrency,
                        dateFormat = dateFormat
                    )

                    2 -> PlanningTab(
                        transactions = state.transactions,
                        currency = state.selectedCurrency,
                        dateFormat = dateFormat
                    )
                    4 -> CategoriesTab(
                        categories = state.categories,
                        onAddCategory = viewModel::addCategory,
                        onDeleteCategory = viewModel::deleteCategory
                    )
                    else -> SettingsTab(
                        currency = state.selectedCurrency,
                        darkMode = darkMode,
                        dateFormat = dateFormat,
                        palette = palette,
                        onDarkModeChange = {
                            darkMode = it
                            settingsStore.setDarkMode(it)
                        },
                        onDateFormatChange = {
                            dateFormat = it
                            settingsStore.setDateFormat(it)
                        },
                        onPaletteChange = {
                            palette = it
                            settingsStore.setPalette(it)
                        },
                        onDefaultCurrencyChange = { currency ->
                            settingsStore.setDefaultCurrency(currency)
                            viewModel.setCurrency(currency)
                        },
                        onBackup = {
                            viewModel.exportBackup(
                                onSuccess = { json ->
                                    pendingBackupJson = json
                                    createBackupLauncher.launch("abc_cash_backup.json")
                                },
                                onError = {
                                    Toast.makeText(context, "Erreur export: $it", Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        onRestore = {
                            restoreBackupLauncher.launch(arrayOf("application/json", "text/plain"))
                        },
                        driveSyncConfigured = !driveSyncFolderUri.isNullOrBlank(),
                        lastDriveSyncStatus = lastDriveSyncStatus,
                        onConnectDriveFolder = { pickDriveFolderLauncher.launch(null) },
                        onSyncNow = { triggerDriveSync(showToast = true) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderMenu(
    selectedTab: Int,
    onSelectedTab: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCategories: () -> Unit,
    onExportDashboard: () -> Unit,
    onExportTransactions: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        HeaderMenuItem(
            label = "Dashboard",
            selected = selectedTab == 0,
            onClick = { onSelectedTab(0) }
        ) {
            Icon(Icons.Filled.Dashboard, contentDescription = "Dashboard")
        }
        HeaderMenuItem(
            label = "Calendrier",
            selected = selectedTab == 1,
            onClick = { onSelectedTab(1) }
        ) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = "Calendrier")
        }
        HeaderMenuItem(
            label = "Planning",
            selected = selectedTab == 2,
            onClick = { onSelectedTab(2) }
        ) {
            Icon(Icons.Filled.EventNote, contentDescription = "Planning")
        }
        Box {
            HeaderMenuItem(
                label = "Menu",
                selected = selectedTab == 3,
                onClick = { menuExpanded = true }
            ) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Parametres") },
                    onClick = {
                        menuExpanded = false
                        onOpenSettings()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Categories") },
                    onClick = {
                        menuExpanded = false
                        onOpenCategories()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Exporter Dashboard PDF") },
                    onClick = {
                        menuExpanded = false
                        onExportDashboard()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Exporter Transactions PDF") },
                    onClick = {
                        menuExpanded = false
                        onExportTransactions()
                    }
                )
            }
        }
    }
}

@Composable
private fun HeaderMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            icon()
        }
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencySelector(
    selected: CurrencyCode,
    onSelected: (CurrencyCode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            label = { Text("Devise") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CurrencyCode.entries.forEach { code ->
                DropdownMenuItem(
                    text = { Text(code.name) },
                    onClick = {
                        expanded = false
                        onSelected(code)
                    }
                )
            }
        }
    }
}

@Composable
private fun DashboardTab(
    openingMinor: Long,
    forecastRows: List<CumulativeForecastPoint>,
    currency: CurrencyCode
) {
    val context = LocalContext.current
    val stressMonths = forecastRows.filter { it.cumulativeMinor < 0L }
    val worstStress = stressMonths.minByOrNull { it.cumulativeMinor }
    val maxExcedent = forecastRows.maxByOrNull { it.cumulativeMinor }
    Column {
        Text(
            text = "Solde actuel: ${formatAmount(openingMinor, currency)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Alerte tresorerie (12 mois)", fontWeight = FontWeight.SemiBold)
                Text("Mois en stress: ${stressMonths.size}")
                worstStress?.let {
                    Text(
                        "Pire mois: ${it.yearMonth} (manque ${formatAmount(-it.cumulativeMinor, currency)})",
                        color = Color(0xFFB3261E)
                    )
                }
                maxExcedent?.let {
                    if (it.cumulativeMinor > 0L) {
                        Text(
                            "Meilleur excedent: ${it.yearMonth} (${formatAmount(it.cumulativeMinor, currency)})",
                            color = Color(0xFF1B8F3A)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                runCatching {
                    exportDashboardPdf(
                        context = context,
                        openingMinor = openingMinor,
                        forecastRows = forecastRows,
                        currency = currency
                    )
                }.onSuccess { file ->
                    Toast.makeText(context, "PDF exporte: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                }.onFailure {
                    Toast.makeText(context, "Erreur export PDF", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Exporter PDF")
        }
        Spacer(Modifier.height(12.dp))
        ProfessionalNetBarChart(forecastRows = forecastRows)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(forecastRows) { row ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Mois: ${row.yearMonth}", fontWeight = FontWeight.SemiBold)
                        Text("Net: ${formatAmount(row.netMinor, currency)}")
                        Spacer(Modifier.height(4.dp))
                        Text("Cumul: ${formatAmount(row.cumulativeMinor, currency)}")
                        if (row.cumulativeMinor < 0L) {
                            Text(
                                "Besoin de tresorerie: ${formatAmount(-row.cumulativeMinor, currency)}",
                                color = Color(0xFFB3261E),
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                "Excedent: ${formatAmount(row.cumulativeMinor, currency)}",
                                color = Color(0xFF1B8F3A),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfessionalNetBarChart(forecastRows: List<CumulativeForecastPoint>) {
    val positive = Color(0xFF1B8F3A).toArgb()
    val negative = Color(0xFFB3261E).toArgb()
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setDrawGridBackground(false)
                setFitBars(true)
                axisRight.isEnabled = false
                setScaleEnabled(false)
                setPinchZoom(false)
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 1f
                xAxis.setDrawGridLines(false)
                axisLeft.setDrawGridLines(true)
            }
        },
        update = { chart ->
            val entries = forecastRows.mapIndexed { index, point ->
                BarEntry(index.toFloat(), point.netMinor / 100f)
            }
            val dataSet = BarDataSet(entries, "Net mensuel").apply {
                valueTextSize = 10f
                colors = entries.map { if (it.y >= 0f) positive else negative }
            }
            chart.data = BarData(dataSet).apply { barWidth = 0.62f }
            chart.xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val idx = value.toInt()
                    if (idx !in forecastRows.indices) return ""
                    return forecastRows[idx].yearMonth.substring(5)
                }
            }
            chart.invalidate()
        }
    )
}

@Composable
private fun TransactionsTab(
    openingMinor: Long,
    onAddTransaction: (
        TransactionType,
        Long,
        String,
        TransactionStatus,
        LocalDate,
        RecurrenceRule
    ) -> Unit,
    onValidate: (Long) -> Unit,
    onEditTransaction: (Long, Long, LocalDate, Boolean) -> Unit,
    onSetStatus: (Long, TransactionStatus) -> Unit,
    onPostpone: (Long) -> Unit,
    onDelete: (Long, Boolean) -> Unit,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currency: CurrencyCode,
    dateFormat: DateFormatOption
) {
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    var addDialogOpen by remember { mutableStateOf(false) }
    var addAmountText by remember { mutableStateOf("") }
    var addCategoryText by remember { mutableStateOf("Divers") }
    var addType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var addStatus by remember { mutableStateOf(TransactionStatus.PLANIFIEE) }
    var addRecurrence by remember { mutableStateOf(RecurrenceRule.NONE) }

    val context = LocalContext.current
    val filteredCategories = categories.filter { it.type == addType }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var editAmountText by remember { mutableStateOf("") }
    var editDateText by remember { mutableStateOf(LocalDate.now().toString()) }
    var editApplySeries by remember { mutableStateOf(false) }
    var deleteTargetId by remember { mutableStateOf<Long?>(null) }
    var deleteIsRecurring by remember { mutableStateOf(false) }
    var deleteApplySeries by remember { mutableStateOf(false) }
    val dayTransactions = transactions.filter { it.date == selectedDate }
    val dailyBalances = remember(transactions, openingMinor, selectedMonth) {
        computeDailyBalancesForMonth(transactions, selectedMonth, openingMinor / 100.0)
    }
    val transactionDatesInMonth = remember(transactions, selectedMonth) {
        transactions
            .asSequence()
            .map { it.date }
            .filter { it.year == selectedMonth.year && it.month == selectedMonth.month }
            .toSet()
    }
    val monthCells = remember(selectedMonth) { buildCalendarCells(selectedMonth) }
    val today = LocalDate.now()
    val currentBalanceTodayMinor = remember(transactions, openingMinor) {
        openingMinor + transactions
            .filter { !it.date.isAfter(today) }
            .sumOf { if (it.type == TransactionType.INCOME) it.amountMinor else -it.amountMinor }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = {
                    selectedMonth = selectedMonth.minusMonths(1)
                    selectedDate = selectedMonth.atDay(1)
                }) { Text("<") }
                Text("${selectedMonth.month} ${selectedMonth.year}", fontWeight = FontWeight.Bold)
                Button(onClick = {
                    selectedMonth = selectedMonth.plusMonths(1)
                    selectedDate = selectedMonth.atDay(1)
                }) { Text(">") }
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("L", "M", "M", "J", "V", "S", "D").forEach {
                    Text(it, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                }
            }
            monthCells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { date ->
                        if (date == null) {
                            Spacer(modifier = Modifier.weight(1f).height(56.dp))
                        } else {
                            val isSelected = date == selectedDate
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(68.dp)
                                    .clickable { selectedDate = date },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFE8F0FF) else Color(0xFFF8F8F8)
                                )
                            ) {
                                Column(Modifier.padding(4.dp)) {
                                    Text("${date.dayOfMonth}", fontWeight = FontWeight.SemiBold)
                                    if (date in transactionDatesInMonth) {
                                        val dayBalance = dailyBalances[date] ?: 0L
                                        if (dayBalance != 0L) {
                                            Text(
                                                text = formatCompactAmount(dayBalance),
                                                color = if (dayBalance >= 0) Color(0xFF1B8F3A) else Color(0xFFB3261E)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.height(6.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F7FF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Solde actuel", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = formatAmount(currentBalanceTodayMinor, currency),
                        color = if (currentBalanceTodayMinor >= 0L) Color(0xFF1B8F3A) else Color(0xFFB3261E),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Transactions du ${formatDate(selectedDate, dateFormat)}", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(dayTransactions) { tx ->
                val amountColor = when (tx.type) {
                    TransactionType.INCOME -> Color(0xFF1B8F3A)
                    TransactionType.EXPENSE -> Color(0xFFB3261E)
                }
                val cardAlpha = if (tx.status == TransactionStatus.PLANIFIEE) 0.65f else 1f
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(cardAlpha),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "${tx.type} - ${formatAmount(tx.amountMinor, currency)}",
                            color = amountColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text("${tx.category} / ${tx.date} / ${tx.status}")
                        if (tx.status == TransactionStatus.EN_RETARD) {
                            Text("Echeance depassee", color = Color(0xFFE65100), fontWeight = FontWeight.SemiBold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (tx.status != TransactionStatus.VALIDEE) {
                                Button(onClick = { onValidate(tx.id) }) {
                                    Text("Valider")
                                }
                            }
                            if (tx.status == TransactionStatus.EN_RETARD) {
                                Button(onClick = { onSetStatus(tx.id, TransactionStatus.PLANIFIEE) }) {
                                    Text("Planifier")
                                }
                            }
                            if (tx.status == TransactionStatus.PLANIFIEE) {
                                Button(onClick = { onSetStatus(tx.id, TransactionStatus.EN_RETARD) }) {
                                    Text("Retard")
                                }
                            }
                            Button(onClick = {
                                editingId = tx.id
                                editAmountText = (tx.amountMinor / 100.0).toString()
                                editDateText = tx.date.toString()
                                editApplySeries = tx.isRecurring
                            }) {
                                Text("Editer")
                            }
                            Button(onClick = { onPostpone(tx.id) }) {
                                Text("+1 mois")
                            }
                            Button(onClick = {
                                deleteTargetId = tx.id
                                deleteIsRecurring = tx.isRecurring
                                deleteApplySeries = tx.isRecurring
                            }) {
                                Text("Supprimer")
                            }
                        }
                    }
                }
            }
        }
        }
        FloatingActionButton(
            onClick = {
                addDialogOpen = true
                addAmountText = ""
                addCategoryText = "Divers"
                addType = TransactionType.EXPENSE
                addStatus = TransactionStatus.PLANIFIEE
                addRecurrence = RecurrenceRule.NONE
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("+")
        }
    }

    if (addDialogOpen) {
        AlertDialog(
            onDismissRequest = { addDialogOpen = false },
            title = { Text("Ajouter transaction (${selectedDate})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = addAmountText,
                        onValueChange = { addAmountText = it },
                        label = { Text("Montant") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = addCategoryText,
                        onValueChange = { addCategoryText = it },
                        label = { Text("Categorie") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AssistChip(
                            onClick = { addType = TransactionType.INCOME },
                            label = { Text("Recette") },
                            modifier = Modifier.alpha(if (addType == TransactionType.INCOME) 1f else 0.65f)
                        )
                        AssistChip(
                            onClick = { addType = TransactionType.EXPENSE },
                            label = { Text("Depense") },
                            modifier = Modifier.alpha(if (addType == TransactionType.EXPENSE) 1f else 0.65f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TransactionStatus.entries.forEach { status ->
                            AssistChip(
                                onClick = { addStatus = status },
                                label = { Text(status.name) },
                                modifier = Modifier.alpha(if (addStatus == status) 1f else 0.65f)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        filteredCategories.take(6).forEach { category ->
                            AssistChip(onClick = { addCategoryText = category.name }, label = { Text(category.name) })
                        }
                    }
                    AssistChip(
                        onClick = { addRecurrence = nextRecurrenceRule(addRecurrence) },
                        label = { Text("Recurrence: ${recurrenceLabel(addRecurrence)}") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = ((addAmountText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                    onAddTransaction(addType, amount, addCategoryText, addStatus, selectedDate, addRecurrence)
                    addDialogOpen = false
                }) { Text("Ajouter") }
            },
            dismissButton = {
                TextButton(onClick = { addDialogOpen = false }) { Text("Annuler") }
            }
        )
    }

    if (editingId != null) {
        AlertDialog(
            onDismissRequest = { editingId = null },
            title = { Text("Modifier transaction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editAmountText,
                        onValueChange = { editAmountText = it },
                        label = { Text("Montant") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDateText,
                        onValueChange = { editDateText = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Button(onClick = {
                                val current = runCatching { LocalDate.parse(editDateText) }.getOrElse { LocalDate.now() }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        editDateText = LocalDate.of(year, month + 1, day).toString()
                                    },
                                    current.year,
                                    current.monthValue - 1,
                                    current.dayOfMonth
                                ).show()
                            }) {
                                Text("Choisir")
                            }
                        }
                    )
                    if (transactions.firstOrNull { it.id == editingId }?.isRecurring == true) {
                        Text("Appliquer les modifications a:")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = { editApplySeries = false },
                                label = { Text("Cette transaction") },
                                modifier = Modifier.alpha(if (!editApplySeries) 1f else 0.65f)
                            )
                            AssistChip(
                                onClick = { editApplySeries = true },
                                label = { Text("Toute la serie") },
                                modifier = Modifier.alpha(if (editApplySeries) 1f else 0.65f)
                            )
                        }
                    } else {
                        Text("Modification sur cette transaction uniquement")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = editingId ?: return@TextButton
                    val amountMinor = ((editAmountText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                    val newDate = runCatching { LocalDate.parse(editDateText) }.getOrElse { LocalDate.now() }
                    onEditTransaction(id, amountMinor, newDate, editApplySeries)
                    editingId = null
                }) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingId = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    if (deleteTargetId != null) {
        AlertDialog(
            onDismissRequest = { deleteTargetId = null },
            title = { Text("Supprimer transaction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (deleteIsRecurring) {
                        Text("Choisissez la portee:")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = { deleteApplySeries = false },
                                label = { Text("Cette transaction") },
                                modifier = Modifier.alpha(if (!deleteApplySeries) 1f else 0.65f)
                            )
                            AssistChip(
                                onClick = { deleteApplySeries = true },
                                label = { Text("Toute la serie") },
                                modifier = Modifier.alpha(if (deleteApplySeries) 1f else 0.65f)
                            )
                        }
                    } else {
                        Text("Confirmer la suppression de cette transaction ?")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = deleteTargetId ?: return@TextButton
                    onDelete(id, deleteIsRecurring && deleteApplySeries)
                    deleteTargetId = null
                }) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetId = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

private fun buildCalendarCells(month: YearMonth): List<LocalDate?> {
    val days = month.lengthOfMonth()
    val leading = month.atDay(1).dayOfWeek.value - 1
    val cells = mutableListOf<LocalDate?>()
    repeat(leading) { cells += null }
    (1..days).forEach { day -> cells += month.atDay(day) }
    while (cells.size % 7 != 0) cells += null
    return cells
}

private fun computeDailyBalancesForMonth(
    transactions: List<TransactionEntity>,
    month: YearMonth,
    manualOpeningMajor: Double?
): Map<LocalDate, Long> {
    val monthStart = month.atDay(1)
    val monthEnd = month.atEndOfMonth()
    val openingMinor = ((manualOpeningMajor ?: 0.0) * 100).toLong()
    val beforeMonthNet = transactions.filter { it.date < monthStart }.sumOf {
        if (it.type == TransactionType.INCOME) it.amountMinor else -it.amountMinor
    }
    var running = openingMinor + beforeMonthNet
    val byDate = transactions.groupBy { it.date }
    val result = mutableMapOf<LocalDate, Long>()
    var current = monthStart
    while (!current.isAfter(monthEnd)) {
        val dayNet = (byDate[current] ?: emptyList()).sumOf {
            if (it.type == TransactionType.INCOME) it.amountMinor else -it.amountMinor
        }
        running += dayNet
        result[current] = running
        current = current.plusDays(1)
    }
    return result
}

private fun formatCompactAmount(amountMinor: Long): String {
    val major = amountMinor / 100.0
    return "%.0f".format(major)
}

@Composable
private fun PlanningTab(
    transactions: List<TransactionEntity>,
    currency: CurrencyCode,
    dateFormat: DateFormatOption
) {
    val grouped = transactions.groupBy { it.date.withDayOfMonth(1) }.toSortedMap()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Planning", fontWeight = FontWeight.Bold)
                Text("Planifiees: ${transactions.count { it.status == TransactionStatus.PLANIFIEE }}")
                Text("En retard: ${transactions.count { it.status == TransactionStatus.EN_RETARD }}")
                Text("Validees: ${transactions.count { it.status == TransactionStatus.VALIDEE }}")
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            grouped.forEach { (monthStart, monthItems) ->
                item {
                    val planned = monthItems.filter { it.status != TransactionStatus.VALIDEE }
                    val income = planned.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
                    val expense = planned.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Mois: ${formatDate(monthStart, dateFormat)}", fontWeight = FontWeight.SemiBold)
                            Text("Echeances: ${planned.size}")
                            Text("Entrees prevues: ${formatAmount(income, currency)}", color = Color(0xFF1B8F3A))
                            Text("Sorties prevues: ${formatAmount(expense, currency)}", color = Color(0xFFB3261E))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTab(
    currency: CurrencyCode,
    darkMode: Boolean,
    dateFormat: DateFormatOption,
    palette: AppPalette,
    onDarkModeChange: (Boolean) -> Unit,
    onDateFormatChange: (DateFormatOption) -> Unit,
    onPaletteChange: (AppPalette) -> Unit,
    onDefaultCurrencyChange: (CurrencyCode) -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    driveSyncConfigured: Boolean,
    lastDriveSyncStatus: String,
    onConnectDriveFolder: () -> Unit,
    onSyncNow: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Parametres", fontWeight = FontWeight.Bold)
                Text("Devise active: ${currency.name}")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CurrencyCode.entries.forEach { code ->
                        AssistChip(
                            onClick = { onDefaultCurrencyChange(code) },
                            label = { Text(code.name) },
                            modifier = Modifier.alpha(if (currency == code) 1f else 0.65f)
                        )
                    }
                }
                Text("Format date")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DateFormatOption.entries.forEach { option ->
                        AssistChip(
                            onClick = { onDateFormatChange(option) },
                            label = { Text(option.label) },
                            modifier = Modifier.alpha(if (dateFormat == option) 1f else 0.65f)
                        )
                    }
                }
                Text("Palette couleur")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppPalette.entries.forEach { option ->
                        PaletteCircleButton(
                            color = palettePreviewColor(option),
                            selected = palette == option,
                            onClick = { onPaletteChange(option) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Mode sombre")
                    Switch(checked = darkMode, onCheckedChange = onDarkModeChange)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onBackup, modifier = Modifier.weight(1f)) {
                        Text("Sauvegarder")
                    }
                    Button(onClick = onRestore, modifier = Modifier.weight(1f)) {
                        Text("Restaurer")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onConnectDriveFolder, modifier = Modifier.weight(1f)) {
                        Text(if (driveSyncConfigured) "Changer dossier Drive" else "Connecter Drive")
                    }
                    Button(
                        onClick = onSyncNow,
                        modifier = Modifier.weight(1f),
                        enabled = driveSyncConfigured
                    ) {
                        Text("Sync maintenant")
                    }
                }
                Text("Sync auto: active apres chaque modification")
                Text(lastDriveSyncStatus)
                Text("Version: MVP")
            }
        }
        Text("Preferences enregistrees localement.")
    }
}

@Composable
private fun CategoriesTab(
    categories: List<CategoryEntity>,
    onAddCategory: (String, TransactionType) -> Unit,
    onDeleteCategory: (Long) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var pendingDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    val incomeCategories = categories.filter { it.type == TransactionType.INCOME }
    val expenseCategories = categories.filter { it.type == TransactionType.EXPENSE }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            CategorySectionTitle("Revenus", Color(0xFF1DB954))
            CategoryGrid(
                categories = incomeCategories,
                type = TransactionType.INCOME,
                onLongPress = { pendingDelete = it }
            )

            CategorySectionTitle("Depenses", Color(0xFFE53935))
            CategoryGrid(
                categories = expenseCategories,
                type = TransactionType.EXPENSE,
                onLongPress = { pendingDelete = it }
            )
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomEnd)
                .padding(16.dp)
        ) { Text("+") }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nouvelle categorie") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Nom categorie") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = { newCategoryType = TransactionType.INCOME },
                            label = { Text("Revenu") },
                            modifier = Modifier.alpha(if (newCategoryType == TransactionType.INCOME) 1f else 0.65f)
                        )
                        AssistChip(
                            onClick = { newCategoryType = TransactionType.EXPENSE },
                            label = { Text("Depense") },
                            modifier = Modifier.alpha(if (newCategoryType == TransactionType.EXPENSE) 1f else 0.65f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onAddCategory(newCategoryName, newCategoryType)
                    newCategoryName = ""
                    showAddDialog = false
                }) { Text("Ajouter") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Annuler") }
            }
        )
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Supprimer categorie") },
            text = { Text("Supprimer ${pendingDelete?.name} ?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete?.let { onDeleteCategory(it.id) }
                    pendingDelete = null
                }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun CategorySectionTitle(title: String, dotColor: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("●", color = dotColor, fontWeight = FontWeight.Bold)
        Text(title, color = dotColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CategoryGrid(
    categories: List<CategoryEntity>,
    type: TransactionType,
    onLongPress: (CategoryEntity) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.height(220.dp)
    ) {
        items(categories) { category ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLongPress(category) },
                colors = CardDefaults.cardColors(
                    containerColor = categoryCardColor(category, type)
                )
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon(category.name, type),
                            contentDescription = null,
                            tint = Color(0xFF1F2937)
                        )
                    }
                    Text(
                        text = category.name,
                        maxLines = 2,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun categoryIcon(name: String, type: TransactionType) = when {
    name.contains("logement", true) || name.contains("loyer", true) -> Icons.Filled.Home
    name.contains("transport", true) -> Icons.Filled.DirectionsCar
    name.contains("sante", true) -> Icons.Filled.MedicalServices
    name.contains("aliment", true) -> Icons.Filled.Restaurant
    name.contains("shopping", true) -> Icons.Filled.ShoppingCart
    type == TransactionType.INCOME -> Icons.Filled.Work
    else -> Icons.Filled.Star
}

private fun categoryCardColor(category: CategoryEntity, type: TransactionType): Color {
    val n = category.name.lowercase()
    if (type == TransactionType.INCOME) return Color(0xFFE9F7EF)
    return when {
        n.contains("transport") -> Color(0xFFEAF2FF)
        n.contains("sante") -> Color(0xFFFFEDEE)
        n.contains("loyer") || n.contains("logement") -> Color(0xFFFFF1E8)
        n.contains("aliment") -> Color(0xFFFFF3E6)
        n.contains("shopping") -> Color(0xFFFFEAF5)
        else -> Color(0xFFEDF3F7)
    }
}

private fun appColorScheme(darkMode: Boolean, palette: AppPalette) = when (palette) {
    AppPalette.SUNSET -> if (darkMode) {
        darkColorScheme(
            primary = Color(0xFFFF8A50),
            onPrimary = Color(0xFF1A1A1A),
            secondary = Color(0xFFFFB74D),
            background = Color(0xFF111318),
            surface = Color(0xFF1A1D24),
            surfaceVariant = Color(0xFF252A34)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFFFF5A00),
            onPrimary = Color.White,
            secondary = Color(0xFFFF9800),
            background = Color(0xFFF8FAFC),
            surface = Color.White,
            surfaceVariant = Color(0xFFF1F5F9)
        )
    }
    AppPalette.OCEAN -> if (darkMode) {
        darkColorScheme(
            primary = Color(0xFF4FC3F7),
            onPrimary = Color(0xFF0B2530),
            secondary = Color(0xFF90CAF9),
            background = Color(0xFF0F172A),
            surface = Color(0xFF111827),
            surfaceVariant = Color(0xFF1F2937)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF0D47A1),
            onPrimary = Color.White,
            secondary = Color(0xFF0288D1),
            background = Color(0xFFF6FAFF),
            surface = Color.White,
            surfaceVariant = Color(0xFFEFF6FF)
        )
    }
    AppPalette.FOREST -> if (darkMode) {
        darkColorScheme(
            primary = Color(0xFF66BB6A),
            onPrimary = Color(0xFF0E2A10),
            secondary = Color(0xFFA5D6A7),
            background = Color(0xFF111712),
            surface = Color(0xFF18211A),
            surfaceVariant = Color(0xFF243126)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF1B5E20),
            onPrimary = Color.White,
            secondary = Color(0xFF43A047),
            background = Color(0xFFF7FCF7),
            surface = Color.White,
            surfaceVariant = Color(0xFFECF7ED)
        )
    }
    AppPalette.VIOLET -> if (darkMode) {
        darkColorScheme(
            primary = Color(0xFFB388FF),
            onPrimary = Color(0xFF1D1038),
            secondary = Color(0xFF7C4DFF),
            background = Color(0xFF120F1F),
            surface = Color(0xFF1B172B),
            surfaceVariant = Color(0xFF2A2342)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF7C3AED),
            onPrimary = Color.White,
            secondary = Color(0xFF6D28D9),
            background = Color(0xFFF8FAFC),
            surface = Color.White,
            surfaceVariant = Color(0xFFF1F5F9)
        )
    }
    AppPalette.ROSE -> if (darkMode) {
        darkColorScheme(
            primary = Color(0xFFFF7AA2),
            onPrimary = Color(0xFF3A0F1D),
            secondary = Color(0xFFFF4D8D),
            background = Color(0xFF1A1015),
            surface = Color(0xFF24141C),
            surfaceVariant = Color(0xFF34212B)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFFE91E63),
            onPrimary = Color.White,
            secondary = Color(0xFFFF5C8A),
            background = Color(0xFFFFF7FA),
            surface = Color.White,
            surfaceVariant = Color(0xFFFFEAF1)
        )
    }
    AppPalette.MIDNIGHT -> if (darkMode) {
        darkColorScheme(
            primary = Color(0xFF38BDF8),
            onPrimary = Color(0xFF052335),
            secondary = Color(0xFF0EA5E9),
            background = Color(0xFF0A0F1C),
            surface = Color(0xFF10182A),
            surfaceVariant = Color(0xFF1B2640)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF0F172A),
            onPrimary = Color.White,
            secondary = Color(0xFF0284C7),
            background = Color(0xFFF6FAFF),
            surface = Color.White,
            surfaceVariant = Color(0xFFE8F1FF)
        )
    }
}

private fun formatAmount(amountMinor: Long, currency: CurrencyCode): String {
    val major = amountMinor / 100.0
    val symbol = when (currency) {
        CurrencyCode.DT -> "DT"
        CurrencyCode.USD -> "$"
        CurrencyCode.EUR -> "EUR"
    }
    return "%.2f %s".format(major, symbol)
}

private fun nextRecurrenceRule(current: RecurrenceRule): RecurrenceRule {
    return when (current) {
        RecurrenceRule.NONE -> RecurrenceRule.WEEKLY
        RecurrenceRule.WEEKLY -> RecurrenceRule.MONTHLY
        RecurrenceRule.MONTHLY -> RecurrenceRule.QUARTERLY
        RecurrenceRule.QUARTERLY -> RecurrenceRule.FOUR_MONTHLY
        RecurrenceRule.FOUR_MONTHLY -> RecurrenceRule.NONE
    }
}

private fun recurrenceLabel(rule: RecurrenceRule): String {
    return when (rule) {
        RecurrenceRule.NONE -> "Aucune"
        RecurrenceRule.WEEKLY -> "Hebdomadaire"
        RecurrenceRule.MONTHLY -> "Mensuelle"
        RecurrenceRule.QUARTERLY -> "Trimestrielle"
        RecurrenceRule.FOUR_MONTHLY -> "Quadrimestrielle"
    }
}

private fun formatDate(date: LocalDate, format: DateFormatOption): String {
    return date.format(DateTimeFormatter.ofPattern(format.pattern))
}

@Composable
private fun PaletteCircleButton(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    size: Dp = 36.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable { onClick() }
    ) {}
}

private fun palettePreviewColor(palette: AppPalette): Color {
    return when (palette) {
        AppPalette.SUNSET -> Color(0xFFFF5A00)
        AppPalette.OCEAN -> Color(0xFF0D47A1)
        AppPalette.FOREST -> Color(0xFF1B5E20)
        AppPalette.VIOLET -> Color(0xFF7C3AED)
        AppPalette.ROSE -> Color(0xFFE91E63)
        AppPalette.MIDNIGHT -> Color(0xFF0F172A)
    }
}
