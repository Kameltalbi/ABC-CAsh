package com.abccash.app.treasury.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abccash.app.R
import com.abccash.app.locale.AppLocale
import com.abccash.app.treasury.data.PilotCalculations
import com.abccash.app.treasury.data.PilotCategory
import com.abccash.app.treasury.data.PilotEntry
import com.abccash.app.treasury.data.PilotEntryType
import com.abccash.app.treasury.data.PilotRecurrence
import com.abccash.app.treasury.data.PilotMoney
import com.abccash.app.treasury.data.PilotRules
import com.abccash.app.treasury.data.BankAccount
import com.abccash.app.treasury.repository.PilotCodes
import com.abccash.app.treasury.viewmodel.PilotageViewModel
import com.abccash.app.ui.theme.AppColors
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val importMimeTypes = arrayOf(
    "text/csv",
    "application/csv",
    "text/comma-separated-values",
    "text/plain",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "*/*"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PilotageScreen(
    viewModel: PilotageViewModel,
    accounts: List<BankAccount>,
    onOpenAccounts: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val formatMoney = rememberFormatMoney()
    val formatDashboard = rememberFormatMoneyWhole()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var editor by remember { mutableStateOf<PilotEditorRequest?>(null) }
    var otherKind by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<PilotEntry?>(null) }
    var pendingBulkDelete by remember { mutableStateOf<List<PilotEntry>?>(null) }
    var bulkEdit by remember { mutableStateOf<List<PilotEntry>?>(null) }
    var clearExpenseSelectionTick by remember { mutableIntStateOf(0) }
    var showCategories by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showRecurringConfirm by remember { mutableStateOf(false) }
    var showImportKind by remember { mutableStateOf(false) }
    var localMessage by remember { mutableStateOf<String?>(null) }
    var importType by remember { mutableStateOf(PilotEntryType.SALE) }

    val amountInvalid = stringResource(R.string.pilot_error_amount)
    val categoryRequired = stringResource(R.string.pilot_error_category)
    val nameRequired = stringResource(R.string.pilot_error_name)
    val nameExists = stringResource(R.string.pilot_error_exists)
    val categoryUsed = stringResource(R.string.pilot_error_used)
    val missing = stringResource(R.string.pilot_error_missing)
    val accountRequired = stringResource(R.string.pilot_error_account)
    val dateRequired = stringResource(R.string.pilot_error_date)
    val transferInvalid = stringResource(R.string.pilot_error_transfer)
    val tenantInvalid = stringResource(R.string.pilot_error_tenant)
    val typeInvalid = stringResource(R.string.pilot_error_type)
    val duplicateEntry = stringResource(R.string.pilot_error_duplicate)

    fun codeMessage(code: String?): String? = when (code) {
        null -> null
        PilotCodes.AMOUNT -> amountInvalid
        PilotCodes.CATEGORY -> categoryRequired
        PilotCodes.NAME -> nameRequired
        PilotCodes.EXISTS -> nameExists
        PilotCodes.USED -> categoryUsed
        PilotCodes.MISSING -> missing
        PilotCodes.ACCOUNT -> accountRequired
        PilotCodes.DATE -> dateRequired
        PilotCodes.TRANSFER -> transferInvalid
        PilotCodes.TENANT -> tenantInvalid
        PilotCodes.TYPE -> typeInvalid
        PilotCodes.DUPLICATE -> duplicateEntry
        else -> code
    }

    LaunchedEffect(state.pendingEntryId, state.entries) {
        val id = state.pendingEntryId ?: return@LaunchedEffect
        val entry = state.entries.find { it.id == id } ?: return@LaunchedEffect
        viewModel.setMonth(YearMonth.from(entry.date))
        editor = PilotEditorRequest(entry.type, entry)
        viewModel.consumePendingEntry()
    }

    LaunchedEffect(state.feedback, localMessage) {
        val text = state.feedback ?: localMessage ?: return@LaunchedEffect
        snackbar.showSnackbar(text)
        if (state.feedback != null) viewModel.clearFeedback()
        localMessage = null
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val mime = context.contentResolver.getType(uri)
            val name = displayName(context.contentResolver, uri)
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("read")
            viewModel.startImport(importType, name, bytes, mime)
        }.onFailure {
            localMessage = context.getString(R.string.pilot_import_unreadable)
        }
    }

    val month = state.month
    val figures = remember(state.entries, state.entrepriseId, month) {
        PilotCalculations.monthFigures(state.entries, state.entrepriseId, month)
    }
    val recurring = remember(state.entries, state.entrepriseId, month) {
        PilotCalculations.recurringToCopy(state.entries, state.entrepriseId, month)
    }
    val sales = state.entries.filter {
        it.type == PilotEntryType.SALE && YearMonth.from(it.date) == month && it.entrepriseId == state.entrepriseId
    }
    val charges = state.entries.filter {
        it.type == PilotEntryType.EXPENSE && YearMonth.from(it.date) == month && it.entrepriseId == state.entrepriseId
    }
    val movements = state.entries.filter {
        it.type != PilotEntryType.SALE &&
            it.type != PilotEntryType.EXPENSE &&
            YearMonth.from(it.date) == month &&
            it.entrepriseId == state.entrepriseId
    }
    val ownAccounts = accounts.filter { it.entrepriseId == state.entrepriseId }

    var showAddSheet by remember { mutableStateOf(false) }
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = Color(0xFFF5F7FB),
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = Color(0xFF2563EB),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.pilot_fab_add)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F7FB))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.nav_pilotage),
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color(0xFF0F172A)
                )
                IconButton(onClick = { showImportKind = true }) {
                    Icon(
                        Icons.Default.FileUpload,
                        contentDescription = stringResource(R.string.pilot_import),
                        tint = Color(0xFF2563EB)
                    )
                }
                TextButton(onClick = { showCategories = true }) {
                    Text(
                        stringResource(R.string.pilot_categories),
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 72.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (ownAccounts.isEmpty()) {
                    PilotSetupBanner(
                        title = stringResource(R.string.pilot_setup_account_title),
                        body = stringResource(R.string.pilot_setup_account_body),
                        actionLabel = stringResource(R.string.dash_setup_account_cta),
                        onAction = onOpenAccounts
                    )
                } else if (sales.isEmpty() && charges.isEmpty() && movements.isEmpty()) {
                    PilotSetupBanner(
                        title = stringResource(R.string.pilot_setup_activity_title),
                        body = stringResource(R.string.pilot_setup_activity_body),
                        actionLabel = stringResource(R.string.pilot_fab_add),
                        onAction = { showAddSheet = true }
                    )
                }
                if (recurring.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.pilot_recurring_prompt, recurring.size),
                                modifier = Modifier.weight(1f),
                                color = Color(0xFF1E293B),
                                fontSize = 12.sp
                            )
                            TextButton(onClick = { showRecurringConfirm = true }) {
                                Text(stringResource(R.string.confirm))
                            }
                        }
                    }
                }
                PilotageHome(
                    month = month,
                    figures = figures,
                    sales = sales,
                    expenses = charges,
                    movements = movements,
                    accounts = ownAccounts,
                    categories = state.categories,
                    formatAmount = formatDashboard,
                    onPreviousMonth = { viewModel.setMonth(month.minusMonths(1)) },
                    onNextMonth = { viewModel.setMonth(month.plusMonths(1)) },
                    onPickMonth = { showMonthPicker = true },
                    onEdit = { editor = PilotEditorRequest(it.type, it) },
                    onDuplicate = { entry ->
                        editor = PilotEditorRequest(entry.type, entry, asNew = true)
                    },
                    onDelete = { pendingDelete = it },
                    onEditExpenses = { chosen ->
                        if (chosen.size == 1) {
                            editor = PilotEditorRequest(chosen.first().type, chosen.first())
                            clearExpenseSelectionTick++
                        } else {
                            bulkEdit = chosen
                        }
                    },
                    onDeleteExpenses = { pendingBulkDelete = it },
                    clearSelectionTick = clearExpenseSelectionTick
                )
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = addSheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.pilot_fab_add),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                PilotAddChoiceRow(
                    icon = Icons.Filled.BarChart,
                    tint = Color(0xFF2563EB),
                    label = stringResource(R.string.pilot_btn_sale),
                    onClick = {
                        showAddSheet = false
                        editor = PilotEditorRequest(PilotEntryType.SALE, null)
                    }
                )
                PilotAddChoiceRow(
                    icon = Icons.Filled.AccountBalanceWallet,
                    tint = Color(0xFFF97316),
                    label = stringResource(R.string.pilot_btn_expense),
                    onClick = {
                        showAddSheet = false
                        editor = PilotEditorRequest(PilotEntryType.EXPENSE, null)
                    }
                )
                PilotAddChoiceRow(
                    icon = Icons.Filled.SwapHoriz,
                    tint = Color(0xFF0369A1),
                    label = stringResource(R.string.pilot_btn_other),
                    onClick = {
                        showAddSheet = false
                        otherKind = true
                    }
                )
            }
        }
    }

    if (otherKind) {
        AlertDialog(
            onDismissRequest = { otherKind = false },
            title = { Text(stringResource(R.string.pilot_other_title)) },
            text = {
                Column {
                    TextButton(onClick = {
                        otherKind = false
                        editor = PilotEditorRequest(PilotEntryType.OTHER_INFLOW, null)
                    }) { Text(stringResource(R.string.pilot_other_in)) }
                    TextButton(onClick = {
                        otherKind = false
                        editor = PilotEditorRequest(PilotEntryType.OTHER_OUTFLOW, null)
                    }) { Text(stringResource(R.string.pilot_other_out)) }
                    TextButton(onClick = {
                        otherKind = false
                        editor = PilotEditorRequest(PilotEntryType.TRANSFER, null)
                    }) { Text(stringResource(R.string.pilot_other_transfer)) }
                }
            },
            confirmButton = {
                TextButton(onClick = { otherKind = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    editor?.let { request ->
        PilotEntrySheet(
            request = request,
            month = month,
            categories = state.categories.filter { it.type == request.type && it.active },
            accounts = ownAccounts,
            onDismiss = { editor = null },
            onCreateRubrique = { name, onResult ->
                viewModel.saveCategory(request.type, name, null) { id, error ->
                    if (error != null) localMessage = codeMessage(error)
                    onResult(id, error)
                }
            },
            onSave = { draft, onFinished ->
                val existing = if (request.asNew) null else request.entry
                viewModel.saveMovement(
                    entryId = existing?.id,
                    type = request.type,
                    date = draft.date,
                    categoryId = draft.categoryId,
                    amount = draft.amount,
                    note = draft.note,
                    recurrenceMonths = draft.recurrenceMonths,
                    treasuryDate = draft.treasuryDate,
                    bankAccountId = draft.bankAccountId,
                    counterAccountId = draft.counterAccountId,
                    strictCash = PilotRules.requiresStrictCash(
                        existing = existing,
                        type = request.type,
                        treasuryDate = draft.treasuryDate,
                        bankAccountId = draft.bankAccountId
                    ),
                    requireAccount = ownAccounts.isNotEmpty()
                ) { code ->
                    val message = codeMessage(code)
                    if (message == null) editor = null else localMessage = message
                    onFinished()
                }
            }
        )
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            text = {
                Text(
                    when (entry.type) {
                        PilotEntryType.SALE -> stringResource(R.string.pilot_delete_sale, formatMoney(entry.amount))
                        PilotEntryType.EXPENSE -> stringResource(R.string.pilot_delete_charge, formatMoney(entry.amount))
                        else -> stringResource(R.string.pilot_delete_movement, formatMoney(entry.amount))
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteEntry(entry.id)
                    pendingDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    pendingBulkDelete?.let { entries ->
        AlertDialog(
            onDismissRequest = { pendingBulkDelete = null },
            text = {
                Text(stringResource(R.string.pilot_delete_charges, entries.size))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteEntries(entries.map { it.id })
                    pendingBulkDelete = null
                    clearExpenseSelectionTick++
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingBulkDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    bulkEdit?.let { entries ->
        PilotBulkExpenseSheet(
            entries = entries,
            categories = state.categories.filter { it.type == PilotEntryType.EXPENSE && it.active },
            accounts = ownAccounts,
            onDismiss = { bulkEdit = null },
            onSave = { categoryId, accountId, date ->
                viewModel.bulkUpdateExpenses(
                    entryIds = entries.map { it.id },
                    categoryId = categoryId,
                    bankAccountId = accountId,
                    date = date
                ) { code ->
                    val message = codeMessage(code)
                    if (message == null) {
                        bulkEdit = null
                        clearExpenseSelectionTick++
                    } else {
                        localMessage = message
                    }
                }
            }
        )
    }

    if (showRecurringConfirm) {
        AlertDialog(
            onDismissRequest = { showRecurringConfirm = false },
            text = { Text(stringResource(R.string.pilot_recurring_prompt, recurring.size)) },
            confirmButton = {
                TextButton(onClick = {
                    showRecurringConfirm = false
                    viewModel.copyRecurring { count ->
                        localMessage = context.getString(R.string.pilot_recurring_copied, count)
                    }
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showRecurringConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showImportKind) {
        AlertDialog(
            onDismissRequest = { showImportKind = false },
            title = { Text(stringResource(R.string.pilot_import)) },
            text = { Text(stringResource(R.string.pilot_import_choose)) },
            confirmButton = {
                TextButton(onClick = {
                    showImportKind = false
                    importType = PilotEntryType.SALE
                    importLauncher.launch(importMimeTypes)
                }) { Text(stringResource(R.string.pilot_import_sales)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportKind = false
                    importType = PilotEntryType.EXPENSE
                    importLauncher.launch(importMimeTypes)
                }) { Text(stringResource(R.string.pilot_import_charges)) }
            }
        )
    }

    if (showMonthPicker) {
        MonthYearDialog(
            month = month,
            onDismiss = { showMonthPicker = false },
            onSelect = {
                viewModel.setMonth(it)
                showMonthPicker = false
            }
        )
    }

    if (showCategories) {
        PilotCategorySheet(
            categories = state.categories,
            entries = state.entries,
            onDismiss = { showCategories = false },
            onRename = { category, name ->
                viewModel.saveCategory(category.type, name, category.id) { _, error ->
                    localMessage = codeMessage(error)
                }
            },
            onCreate = { type, name ->
                viewModel.saveCategory(type, name, null) { _, error ->
                    localMessage = codeMessage(error)
                }
            },
            onDelete = { category, onResult ->
                viewModel.deleteCategory(category.id, onResult)
            },
            onImportSales = {
                showCategories = false
                importType = PilotEntryType.SALE
                importLauncher.launch(importMimeTypes)
            },
            onImportExpenses = {
                showCategories = false
                importType = PilotEntryType.EXPENSE
                importLauncher.launch(importMimeTypes)
            }
        )
    }

    if (state.importSession != null) {
        PilotImportFlow(
            viewModel = viewModel,
            formatMoney = formatMoney,
            onMessage = { localMessage = codeMessage(it) ?: it }
        )
    }
}

private data class PilotEditorRequest(
    val type: PilotEntryType,
    val entry: PilotEntry?,
    val asNew: Boolean = false
) {
    val formKey: String
        get() = when {
            asNew -> "copy-${entry?.id ?: type.name}"
            entry != null -> entry.id
            else -> "new-${type.name}"
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PilotBulkExpenseSheet(
    entries: List<PilotEntry>,
    categories: List<PilotCategory>,
    accounts: List<BankAccount>,
    onDismiss: () -> Unit,
    onSave: (categoryId: String?, accountId: String?, date: LocalDate?) -> Unit
) {
    val commonCategory = entries.map { it.categoryId }.distinct().singleOrNull()
    val commonAccount = entries.map { it.bankAccountId }.distinct().singleOrNull()
    val commonDate = entries.map { it.date }.distinct().singleOrNull()
    var changeCategory by remember { mutableStateOf(false) }
    var changeAccount by remember { mutableStateOf(false) }
    var changeDate by remember { mutableStateOf(false) }
    var categoryId by remember { mutableStateOf(commonCategory) }
    var accountId by remember { mutableStateOf(commonAccount) }
    var date by remember { mutableStateOf(commonDate ?: LocalDate.now()) }
    var categoryMenu by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val canSave = (changeCategory || changeAccount || changeDate) && !saving &&
        (!changeCategory || !categoryId.isNullOrBlank())

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.pilot_bulk_edit_title, entries.size),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF1E293B)
            )
            Text(
                stringResource(R.string.pilot_bulk_edit_hint),
                color = AppColors.TextSecondary,
                fontSize = 13.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = changeCategory, onCheckedChange = { changeCategory = it })
                Text(
                    stringResource(R.string.pilot_field_category),
                    modifier = Modifier.padding(start = 8.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (changeCategory) {
                ExposedDropdownMenuBox(expanded = categoryMenu, onExpandedChange = { categoryMenu = it }) {
                    OutlinedTextField(
                        value = categories.find { it.id == categoryId }?.name.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.pilot_field_category)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    categoryId = category.id
                                    categoryMenu = false
                                }
                            )
                        }
                    }
                }
            }
            if (accounts.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = changeAccount, onCheckedChange = { changeAccount = it })
                    Text(
                        stringResource(R.string.pilot_field_account),
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (changeAccount) {
                    AccountField(
                        label = stringResource(R.string.pilot_field_account),
                        accounts = accounts,
                        selectedId = accountId,
                        onSelect = { accountId = it }
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = changeDate, onCheckedChange = { changeDate = it })
                Text(
                    stringResource(R.string.pilot_field_date),
                    modifier = Modifier.padding(start = 8.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (changeDate) {
                TreasuryDateField(
                    label = stringResource(R.string.pilot_field_date),
                    date = date,
                    onDateChange = { date = it }
                )
            }
            Button(
                enabled = canSave,
                onClick = {
                    if (!canSave) return@Button
                    saving = true
                    onSave(
                        if (changeCategory) categoryId else null,
                        if (changeAccount) accountId else null,
                        if (changeDate) date else null
                    )
                    saving = false
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.save)) }
        }
    }
}

private data class PilotDraft(
    val date: LocalDate,
    val categoryId: String?,
    val amount: Double,
    val note: String,
    val recurrenceMonths: Int,
    val treasuryDate: LocalDate?,
    val bankAccountId: String?,
    val counterAccountId: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PilotEntrySheet(
    request: PilotEditorRequest,
    month: YearMonth,
    categories: List<PilotCategory>,
    accounts: List<BankAccount>,
    onDismiss: () -> Unit,
    onCreateRubrique: (name: String, onResult: (id: String?, error: String?) -> Unit) -> Unit,
    onSave: (PilotDraft, onFinished: () -> Unit) -> Unit
) {
    val entry = request.entry
    val needsCategory = request.type.needsCategory
    val isTransfer = request.type == PilotEntryType.TRANSFER
    var saving by remember(request.formKey) { mutableStateOf(false) }
    var date by remember(request.formKey, request.type) {
        mutableStateOf(
            entry?.date ?: if (request.type == PilotEntryType.EXPENSE) LocalDate.now() else defaultDate(month)
        )
    }
    var treasuryDate by remember(request.formKey, request.type) {
        mutableStateOf(
            when {
                request.type != PilotEntryType.SALE -> null
                entry?.treasuryDate != null -> entry.treasuryDate
                entry != null -> entry.date
                else -> defaultDate(month)
            }
        )
    }
    var bankAccountId by remember(request.formKey, accounts) {
        mutableStateOf(
            entry?.bankAccountId ?: PilotRules.suggestedAccount(
                accounts,
                accounts.firstOrNull()?.entrepriseId.orEmpty()
            )?.id
        )
    }
    var counterAccountId by remember(request.formKey) { mutableStateOf(entry?.counterAccountId) }
    var categoryId by remember(request.formKey) { mutableStateOf(entry?.categoryId) }
    var selectedName by remember(request.formKey) {
        mutableStateOf(categories.find { it.id == entry?.categoryId }?.name.orEmpty())
    }
    var addingRubrique by remember(request.formKey) { mutableStateOf(false) }
    var rubriqueDraft by remember(request.formKey) { mutableStateOf("") }
    var creatingRubrique by remember(request.formKey) { mutableStateOf(false) }
    var amountText by remember(request.formKey) { mutableStateOf(entry?.amount?.let { trimAmount(it) }.orEmpty()) }
    var note by remember(request.formKey) { mutableStateOf(entry?.note.orEmpty()) }
    var recurrence by remember(request.formKey) {
        mutableStateOf(PilotRecurrence.fromMonths(entry?.recurrenceMonths ?: 0, entry?.recurring == true))
    }
    var recurrenceMenu by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val rubriqueFocus = remember { FocusRequester() }
    LaunchedEffect(categories, categoryId) {
        categories.find { it.id == categoryId }?.let { selectedName = it.name }
    }
    LaunchedEffect(addingRubrique) {
        if (addingRubrique) {
            kotlinx.coroutines.delay(80)
            runCatching { rubriqueFocus.requestFocus() }
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val title = if (entry == null || request.asNew) {
        stringResource(pilotEntryTitle(request.type))
    } else {
        stringResource(R.string.edit)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = AppColors.TextPrimary)
            TreasuryDateField(
                label = stringResource(R.string.pilot_field_date),
                date = date,
                onDateChange = { newDate ->
                    if (request.type == PilotEntryType.SALE &&
                        (treasuryDate == null || treasuryDate == date)
                    ) {
                        treasuryDate = newDate
                    }
                    date = newDate
                }
            )
            if (request.type == PilotEntryType.SALE) {
                TreasuryDateField(
                    label = stringResource(R.string.pilot_field_collection),
                    date = treasuryDate ?: date,
                    onDateChange = { treasuryDate = it }
                )
            }
            if (needsCategory) {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = categories.find { it.id == categoryId }?.name ?: selectedName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.pilot_field_category)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RubriqueSwatch(category.type, category.colorIndex)
                                    Text(category.name, modifier = Modifier.padding(start = 8.dp))
                                }
                            },
                            onClick = {
                                categoryId = category.id
                                selectedName = category.name
                                addingRubrique = false
                                expanded = false
                            }
                        )
                    }
                    if (categories.isNotEmpty()) {
                        HorizontalDivider(color = AppColors.Border)
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.pilot_add_rubrique),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        onClick = {
                            rubriqueDraft = ""
                            addingRubrique = true
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    )
                }
            }
            if (addingRubrique) {
                OutlinedTextField(
                    value = rubriqueDraft,
                    onValueChange = { rubriqueDraft = it },
                    label = { Text(stringResource(R.string.pilot_rubrique_name)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(rubriqueFocus),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        submitNewRubrique(
                            name = rubriqueDraft,
                            busy = creatingRubrique,
                            onBusy = { creatingRubrique = it },
                            onCreate = onCreateRubrique,
                            onCreated = { id, name ->
                                categoryId = id
                                selectedName = name
                                addingRubrique = false
                                rubriqueDraft = ""
                            }
                        )
                    })
                )
                Button(
                    onClick = {
                        submitNewRubrique(
                            name = rubriqueDraft,
                            busy = creatingRubrique,
                            onBusy = { creatingRubrique = it },
                            onCreate = onCreateRubrique,
                            onCreated = { id, name ->
                                categoryId = id
                                selectedName = name
                                addingRubrique = false
                                rubriqueDraft = ""
                            }
                        )
                    },
                    enabled = rubriqueDraft.isNotBlank() && !creatingRubrique,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.pilot_create_rubrique)) }
            }
            }
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(stringResource(R.string.pilot_field_amount)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (accounts.isEmpty() && (isTransfer || request.type == PilotEntryType.OTHER_INFLOW || request.type == PilotEntryType.OTHER_OUTFLOW)) {
                Text(stringResource(R.string.pilot_no_account), color = AppColors.TextSecondary, fontSize = 13.sp)
            } else if (isTransfer) {
                AccountField(
                    label = stringResource(R.string.pilot_field_source),
                    accounts = accounts,
                    selectedId = bankAccountId,
                    onSelect = { bankAccountId = it }
                )
                AccountField(
                    label = stringResource(R.string.pilot_field_destination),
                    accounts = accounts,
                    selectedId = counterAccountId,
                    onSelect = { counterAccountId = it }
                )
            } else if (accounts.isNotEmpty()) {
                AccountField(
                    label = stringResource(R.string.pilot_field_account),
                    accounts = accounts,
                    selectedId = bankAccountId,
                    onSelect = { bankAccountId = it }
                )
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.pilot_field_note)) },
                modifier = Modifier.fillMaxWidth()
            )
            if (request.type == PilotEntryType.SALE || request.type == PilotEntryType.EXPENSE) {
            ExposedDropdownMenuBox(expanded = recurrenceMenu, onExpandedChange = { recurrenceMenu = it }) {
                OutlinedTextField(
                    value = stringResource(recurrence.labelRes),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.pilot_field_recurrence)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(recurrenceMenu) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = recurrenceMenu, onDismissRequest = { recurrenceMenu = false }) {
                    PilotRecurrence.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(stringResource(option.labelRes)) },
                            onClick = {
                                recurrence = option
                                recurrenceMenu = false
                            }
                        )
                    }
                }
            }
            }
            Button(
                enabled = !saving,
                onClick = {
                    if (saving) return@Button
                    saving = true
                    val months = if (request.type == PilotEntryType.SALE || request.type == PilotEntryType.EXPENSE) {
                        recurrence.months
                    } else {
                        0
                    }
                    val amount = PilotMoney.parse(amountText)
                    onSave(
                        PilotDraft(
                            date = date,
                            categoryId = if (needsCategory) categoryId else null,
                            amount = amount ?: 0.0,
                            note = note,
                            recurrenceMonths = months,
                            treasuryDate = if (request.type == PilotEntryType.SALE) {
                                treasuryDate ?: date
                            } else {
                                null
                            },
                            bankAccountId = bankAccountId,
                            counterAccountId = if (isTransfer) counterAccountId else null
                        )
                    ) { saving = false }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.save)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PilotCategorySheet(
    categories: List<PilotCategory>,
    entries: List<PilotEntry>,
    onDismiss: () -> Unit,
    onRename: (PilotCategory, String) -> Unit,
    onCreate: (PilotEntryType, String) -> Unit,
    onDelete: (PilotCategory, (String?) -> Unit) -> Unit,
    onImportSales: () -> Unit,
    onImportExpenses: () -> Unit
) {
    var rename by remember { mutableStateOf<PilotCategory?>(null) }
    var renameText by remember { mutableStateOf("") }
    var blocked by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<PilotCategory?>(null) }
    var createType by remember { mutableStateOf<PilotEntryType?>(null) }
    var createText by remember { mutableStateOf("") }
    val usedMessage = stringResource(R.string.pilot_error_used)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.pilot_categories), fontWeight = FontWeight.Bold, fontSize = 20.sp)
            listOf(
                PilotEntryType.SALE,
                PilotEntryType.EXPENSE,
                PilotEntryType.OTHER_INFLOW,
                PilotEntryType.OTHER_OUTFLOW
            ).forEach { type ->
                Text(
                    stringResource(pilotCategoryTitle(type)),
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextSecondary
                )
                val ofType = categories.filter { it.type == type }
                if (ofType.isEmpty()) {
                    Text(stringResource(R.string.pilot_no_rubrique), color = AppColors.TextSecondary, fontSize = 13.sp)
                }
                ofType.forEach { category ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        RubriqueSwatch(category.type, category.colorIndex)
                        Text(
                            category.name,
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                            color = AppColors.TextPrimary
                        )
                        TextButton(onClick = {
                            rename = category
                            renameText = category.name
                        }) { Text(stringResource(R.string.edit)) }
                        TextButton(onClick = {
                            if (PilotCalculations.isCategoryUsed(entries, category.id)) {
                                blocked = usedMessage
                            } else {
                                pendingDelete = category
                            }
                        }) { Text(stringResource(R.string.delete)) }
                    }
                }
                TextButton(onClick = {
                    createType = type
                    createText = ""
                }) { Text(stringResource(R.string.pilot_add_rubrique)) }
            }
            TextButton(onClick = onImportSales) { Text(stringResource(R.string.pilot_import_sales)) }
            TextButton(onClick = onImportExpenses) { Text(stringResource(R.string.pilot_import_charges)) }
        }
    }

    rename?.let { category ->
        AlertDialog(
            onDismissRequest = { rename = null },
            text = {
                OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(category, renameText)
                    rename = null
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { rename = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    createType?.let { type ->
        AlertDialog(
            onDismissRequest = { createType = null },
            text = {
                OutlinedTextField(
                    value = createText,
                    onValueChange = { createText = it },
                    label = { Text(stringResource(R.string.pilot_rubrique_name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onCreate(type, createText)
                    createType = null
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { createType = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    pendingDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            text = { Text(stringResource(R.string.pilot_delete_rubrique, category.name)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(category) { code ->
                        blocked = when (code) {
                            null -> null
                            PilotCodes.USED -> usedMessage
                            else -> code
                        }
                    }
                    pendingDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    blocked?.let { message ->
        AlertDialog(
            onDismissRequest = { blocked = null },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { blocked = null }) { Text(stringResource(R.string.ok)) }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthYearDialog(
    month: YearMonth,
    onDismiss: () -> Unit,
    onSelect: (YearMonth) -> Unit
) {
    var year by remember(month) { mutableStateOf(month.year) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pilot_pick_month)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { year -= 1 }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null)
                    }
                    Text(year.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = { year += 1 }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..12).forEach { value ->
                        val selected = year == month.year && value == month.monthValue
                        val label = Month.of(value).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            .replaceFirstChar { it.uppercase() }
                        if (selected) {
                            Button(onClick = { onSelect(YearMonth.of(year, value)) }) { Text(label) }
                        } else {
                            OutlinedButton(onClick = { onSelect(YearMonth.of(year, value)) }) { Text(label) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

private fun submitNewRubrique(
    name: String,
    busy: Boolean,
    onBusy: (Boolean) -> Unit,
    onCreate: (String, (String?, String?) -> Unit) -> Unit,
    onCreated: (id: String, name: String) -> Unit
) {
    val trimmed = name.trim()
    if (trimmed.isEmpty() || busy) return
    onBusy(true)
    onCreate(trimmed) { id, error ->
        onBusy(false)
        if (error == null && id != null) onCreated(id, trimmed)
    }
}

private fun defaultDate(month: YearMonth): LocalDate {
    val today = LocalDate.now()
    return if (YearMonth.from(today) == month) today else month.atDay(1)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionalDateField(
    label: String,
    date: LocalDate?,
    onDateChange: (LocalDate) -> Unit
) {
    if (date == null) {
        var showPicker by remember { mutableStateOf(false) }
        TreasuryDatePickerDialog(
            visible = showPicker,
            selectedDate = LocalDate.now(),
            onDismiss = { showPicker = false },
            onConfirm = {
                onDateChange(it)
                showPicker = false
            }
        )
        OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text(label)
        }
    } else {
        TreasuryDateField(label = label, date = date, onDateChange = onDateChange)
    }
}

@Composable
private fun PilotSetupBanner(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF0F172A)
            )
            Text(
                text = body,
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text(actionLabel, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PilotAddChoiceRow(
    icon: ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .background(tint.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Text(
            text = label,
            modifier = Modifier.padding(start = 12.dp),
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = Color(0xFF0F172A)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountField(
    label: String,
    accounts: List<BankAccount>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = accounts.find { it.id == selectedId }?.name.orEmpty()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text(account.name) },
                    onClick = {
                        onSelect(account.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun pilotEntryTitle(type: PilotEntryType): Int = when (type) {
    PilotEntryType.SALE -> R.string.pilot_new_sale
    PilotEntryType.EXPENSE -> R.string.pilot_new_charge
    PilotEntryType.OTHER_INFLOW -> R.string.pilot_new_inflow
    PilotEntryType.OTHER_OUTFLOW -> R.string.pilot_new_outflow
    PilotEntryType.TRANSFER -> R.string.pilot_new_transfer
}

private fun pilotCategoryTitle(type: PilotEntryType): Int = when (type) {
    PilotEntryType.SALE -> R.string.pilot_sales
    PilotEntryType.EXPENSE -> R.string.pilot_charges
    PilotEntryType.OTHER_INFLOW -> R.string.pilot_other_in_plural
    PilotEntryType.OTHER_OUTFLOW -> R.string.pilot_other_out_plural
    PilotEntryType.TRANSFER -> R.string.pilot_other_transfer
}

private fun trimAmount(amount: Double): String {
    val text = PilotMoney.decimal(amount).stripTrailingZeros().toPlainString()
    return if (text.contains('.')) text.replace('.', ',') else text
}

private fun displayName(resolver: android.content.ContentResolver, uri: Uri): String {
    return resolver.query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
    } ?: "import.csv"
}

@Composable
internal fun pilotSignedMoney(amount: Double, format: (Double) -> String): String {
    val absolute = format(kotlin.math.abs(amount))
    return when {
        amount > 0.00005 -> "+$absolute"
        amount < -0.00005 -> "-$absolute"
        else -> format(0.0)
    }
}

@Composable
internal fun pilotPercent(ratio: Double?): String {
    if (ratio == null) return stringResource(R.string.pilot_coverage_none)
    val percent = PilotMoney.percent(ratio)
    return String.format(Locale.getDefault(), "%.1f %%", percent)
}
