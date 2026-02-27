package com.abccash.app.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abccash.app.presentation.CashflowViewModel
import com.abccash.app.sync.readBackupFromTree
import com.abccash.app.sync.writeBackupToTree
import com.abccash.app.data.local.entity.RecurrenceRule
import com.abccash.app.data.local.entity.TransactionStatus
import com.abccash.app.data.local.entity.TransactionType
import com.abccash.app.ui.components.AddTransactionDialog
import com.abccash.app.ui.components.HeaderMenu
import com.abccash.app.ui.screens.CalendarScreen
import java.time.LocalDate
import com.abccash.app.ui.screens.CategoriesScreen
import com.abccash.app.ui.screens.DashboardScreen
import com.abccash.app.ui.screens.PlanningScreen
import com.abccash.app.ui.screens.SettingsScreen
import com.abccash.app.ui.screens.TransactionsScreen
import com.abccash.app.ui.exportDashboardPdf
import com.abccash.app.ui.exportTransactionsByMonthPdf
import com.abccash.app.ui.theme.appColorScheme
import java.io.BufferedReader
import java.io.InputStreamReader
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
    
    val pickDriveFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            driveSyncFolderUri = it.toString()
            settingsStore.setDriveSyncFolderUri(it.toString())
            triggerDriveSync(showToast = true)
        }
    }

    var showCsvImportScreen by remember { mutableStateOf(false) }
    var csvTransactions by remember { mutableStateOf<List<com.abccash.app.csv.CsvTransaction>>(emptyList()) }
    
    val pickCsvFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                inputStream?.use { stream ->
                    val result = com.abccash.app.csv.CsvParser.parseCsv(stream)
                    result.onSuccess { transactions ->
                        csvTransactions = transactions
                        showCsvImportScreen = true
                    }.onFailure { error ->
                        Toast.makeText(context, "Erreur: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur lecture fichier: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
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
                    0 -> DashboardScreen(
                        openingMinor = state.openingBalanceMinor,
                        forecastRows = state.forecast,
                        currency = state.selectedCurrency,
                        onUpdateOpeningBalance = { newBalance ->
                            viewModel.saveOpeningBalance(newBalance)
                        }
                    )

                    1 -> {
                        var showAddDialog by remember { mutableStateOf(false) }
                        var addAmountText by remember { mutableStateOf("") }
                        var addCategoryText by remember { mutableStateOf("Divers") }
                        var addType by remember { mutableStateOf(TransactionType.EXPENSE) }
                        var addStatus by remember { mutableStateOf(TransactionStatus.PLANIFIEE) }
                        var addRecurrence by remember { mutableStateOf(RecurrenceRule.NONE) }
                        var selectedDate by remember { mutableStateOf(LocalDate.now()) }
                        
                        CalendarScreen(
                            openingBalance = state.openingBalanceMinor / 100.0,
                            transactions = state.transactions,
                            categories = state.categories,
                            currency = state.selectedCurrency,
                            onAddTransaction = {
                                showAddDialog = true
                            },
                            onTransactionClick = { transaction ->
                                // TODO: Ouvrir le dialog d'édition
                            }
                        )
                        
                        if (showAddDialog) {
                            val filteredCategories = state.categories.filter { it.type == addType }
                            AddTransactionDialog(
                                selectedDate = selectedDate,
                                amountText = addAmountText,
                                categoryText = addCategoryText,
                                type = addType,
                                status = addStatus,
                                recurrence = addRecurrence,
                                filteredCategories = filteredCategories,
                                onAmountChange = { addAmountText = it },
                                onCategoryChange = { addCategoryText = it },
                                onTypeChange = { addType = it },
                                onStatusChange = { addStatus = it },
                                onRecurrenceChange = { addRecurrence = it },
                                onConfirm = {
                                    val amountMinor = (addAmountText.toDoubleOrNull() ?: 0.0) * 100
                                    viewModel.addQuickTransaction(
                                        addType,
                                        amountMinor.toLong(),
                                        addCategoryText,
                                        addStatus,
                                        selectedDate,
                                        addRecurrence
                                    )
                                    addAmountText = ""
                                    addCategoryText = "Divers"
                                    showAddDialog = false
                                },
                                onDismiss = {
                                    showAddDialog = false
                                }
                            )
                        }
                    }
                    
                    99 -> TransactionsScreen(
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

                    2 -> PlanningScreen(
                        transactions = state.transactions,
                        currency = state.selectedCurrency,
                        dateFormat = dateFormat
                    )
                    
                    4 -> CategoriesScreen(
                        categories = state.categories,
                        onAddCategory = viewModel::addCategory,
                        onDeleteCategory = viewModel::deleteCategory
                    )
                    
                    else -> SettingsScreen(
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
                        onImportCsv = {
                            pickCsvFileLauncher.launch("text/*")
                        },
                        driveSyncConfigured = !driveSyncFolderUri.isNullOrBlank(),
                        lastDriveSyncStatus = lastDriveSyncStatus,
                        onConnectDriveFolder = { pickDriveFolderLauncher.launch(null) },
                        onSyncNow = { triggerDriveSync(showToast = true) }
                    )
                }
            }
        }
        
        if (showCsvImportScreen) {
            com.abccash.app.ui.screens.CsvImportPreviewScreen(
                transactions = csvTransactions,
                categories = state.categories,
                onConfirmImport = { transactionsToImport ->
                    transactionsToImport.forEach { csvTx ->
                        viewModel.addQuickTransaction(
                            csvTx.type,
                            (csvTx.amount * 100).toLong(),
                            csvTx.suggestedCategory,
                            TransactionStatus.VALIDEE,
                            csvTx.date,
                            RecurrenceRule.NONE
                        )
                    }
                    showCsvImportScreen = false
                    Toast.makeText(context, "${transactionsToImport.size} transactions importées", Toast.LENGTH_LONG).show()
                },
                onCancel = {
                    showCsvImportScreen = false
                }
            )
        }
    }
}
