package com.abccash.app.treasury.ui

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R
import com.abccash.app.locale.AppLocale
import com.abccash.app.treasury.data.*
import com.abccash.app.treasury.export.ReceiptImageStorage
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseNotesListScreen(
    notes: List<Expense>,
    onBack: () -> Unit,
    onCreateNote: () -> Unit,
    onDeleteNote: (String) -> Unit
) {
    val formatAmount = rememberFormatMoney()
    var noteToDelete by remember { mutableStateOf<Expense?>(null) }

    noteToDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text(stringResource(R.string.expense_note_delete_title)) },
            text = { Text(stringResource(R.string.expense_note_delete_message, expense.label)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteNote(expense.id)
                    noteToDelete = null
                }) {
                    Text(stringResource(R.string.delete), color = Color(0xFFDC2626))
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.expense_notes_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            AbcCashFab(
                onClick = onCreateNote,
                contentDescription = stringResource(R.string.expense_note_add)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = Color(0xFFF57C00))
                        Text(
                            stringResource(R.string.expense_notes_ocr_hint),
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
            if (notes.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.expense_notes_empty),
                        color = Color(0xFF64748B),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else {
                items(notes, key = { it.id }) { note ->
                    ExpenseNoteCard(
                        note = note,
                        formatAmount = formatAmount,
                        onDelete = { noteToDelete = note }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseNoteCard(
    note: Expense,
    formatAmount: (Double) -> String,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(note.label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    "${AppLocale.dayMonth(note.date)} · ${note.displayCategory()}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
                if (note.note.isNotBlank()) {
                    Text(note.note, fontSize = 12.sp, color = Color(0xFF94A3B8), maxLines = 2)
                }
                if (note.receiptImagePath != null) {
                    Text(
                        stringResource(R.string.expense_note_has_receipt),
                        fontSize = 11.sp,
                        color = Color(0xFF2563EB)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(formatAmount(note.amount), fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@Composable
private fun Expense.displayCategory(): String {
    if (categoryLabel.isNotBlank()) return categoryLabel
    return stringResource(category.labelRes)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseNoteFormScreen(
    customExpenseCategories: List<String>,
    entrepriseId: String,
    onBack: () -> Unit,
    onSave: (
        expenseId: String,
        label: String,
        amount: Double,
        date: LocalDate,
        category: ExpenseCategory,
        categoryLabel: String,
        note: String,
        receiptImagePath: String?,
        onResult: (String?) -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val categoryOptions = expenseCategoryOptions(customExpenseCategories)
    val defaultCategoryLabel = categoryOptions.firstOrNull().orEmpty()

    var amountText by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var selectedCategoryLabel by remember { mutableStateOf(defaultCategoryLabel) }
    var showExpenseMenu by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var pendingReceiptUri by remember { mutableStateOf<Uri?>(null) }

    val invalidAmountError = stringResource(R.string.invalid_amount)
    val labelRequiredError = stringResource(R.string.label_required)
    val categoryRequiredError = stringResource(R.string.category_required)

    val (scanState, scanActions) = rememberReceiptScan(snackbarHostState) { result ->
        result.amount?.let { amount ->
            amountText = if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString().replace('.', ',')
        }
        result.date?.let { date = it }
        result.merchantHint?.let { hint -> if (label.isBlank()) label = hint }
    }

    LaunchedEffect(scanState.lastScannedUri) {
        scanState.lastScannedUri?.let { pendingReceiptUri = it }
    }

    ReceiptSourcePickerSheet(
        visible = scanActions.isSourcePickerVisible(),
        onDismiss = scanActions.onDismissSourcePicker,
        onTakePhoto = scanActions.onTakePhoto,
        onPickGallery = scanActions.onPickGallery
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.expense_note_add)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ReceiptScanPlaceholder(
                isScanning = scanState.isScanning,
                onClick = scanActions.onOpenSourcePicker
            )
            scanState.successMessage?.let { ReceiptScanSuccessBanner(it) }
            if (pendingReceiptUri != null) {
                Text(
                    stringResource(R.string.expense_note_receipt_attached),
                    fontSize = 12.sp,
                    color = Color(0xFF16A34A)
                )
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(stringResource(R.string.amount)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix = { CurrencySuffix() }
            )

            TreasuryDateField(
                label = stringResource(R.string.date),
                date = date,
                onDateChange = { date = it }
            )

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(R.string.supplier)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            ExposedDropdownMenuBox(
                expanded = showExpenseMenu,
                onExpandedChange = { showExpenseMenu = it }
            ) {
                OutlinedTextField(
                    value = selectedCategoryLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.expense_category)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showExpenseMenu) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showExpenseMenu,
                    onDismissRequest = { showExpenseMenu = false }
                ) {
                    categoryOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                selectedCategoryLabel = option
                                showExpenseMenu = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.note_optional)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            saveError?.let { Text(it, color = Color(0xFFDC2626), fontSize = 13.sp) }

            Button(
                onClick = {
                    val amount = amountText.replace(" ", "").replace(",", ".").toDoubleOrNull()
                    when {
                        amount == null || amount <= 0 -> saveError = invalidAmountError
                        label.isBlank() -> saveError = labelRequiredError
                        selectedCategoryLabel.isBlank() -> saveError = categoryRequiredError
                        else -> {
                            saveError = null
                            isSaving = true
                            val expenseId = UUID.randomUUID().toString()
                            val receiptPath = pendingReceiptUri?.let { uri ->
                                ReceiptImageStorage.persistReceipt(context, uri, expenseId)
                            }
                            val resolved = CategorySelection.resolveExpense(selectedCategoryLabel, customExpenseCategories)
                            onSave(
                                expenseId,
                                label,
                                amount,
                                date,
                                resolved.expenseCategory ?: ExpenseCategory.OTHER,
                                resolved.customLabel.orEmpty(),
                                note,
                                receiptPath
                            ) { error ->
                                isSaving = false
                                if (error == null) onBack() else saveError = error
                            }
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
