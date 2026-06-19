package com.abccash.app.treasury.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import com.abccash.app.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.treasury.data.CategorySelection
import com.abccash.app.treasury.data.ExpenseCategory
import com.abccash.app.treasury.data.ExpenseRecurrence
import com.abccash.app.treasury.data.PaymentMethod
import com.abccash.app.treasury.data.RevenueCategory
import com.abccash.app.treasury.data.TransactionType
import com.abccash.app.treasury.data.defaultDateForMonth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

private val FormBackground = Color(0xFFFAF9F6)
private val FieldBackground = Color.White
private val FieldBorder = Color(0xFFE8E4DD)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTransactionScreen(
    type: TransactionType,
    selectedMonth: YearMonth,
    forecastMode: Boolean = false,
    customIncomeCategories: List<String> = emptyList(),
    customExpenseCategories: List<String> = emptyList(),
    onBack: () -> Unit,
    onSaveIncome: (
        clientName: String,
        amount: Double,
        date: LocalDate,
        category: RevenueCategory,
        categoryLabel: String,
        markAsCollected: Boolean,
        paymentMethod: PaymentMethod,
        onResult: (String?) -> Unit
    ) -> Unit,
    onSaveExpense: (
        label: String,
        amount: Double,
        date: LocalDate,
        category: ExpenseCategory,
        categoryLabel: String,
        isRecurring: Boolean,
        recurrence: ExpenseRecurrence?,
        recurrenceEndDate: LocalDate?,
        isPaid: Boolean,
        paymentMethod: PaymentMethod,
        onResult: (String?) -> Unit
    ) -> Unit
) {
    val defaultDate = remember(selectedMonth) { defaultDateForMonth(selectedMonth) }
    val isIncome = type == TransactionType.INCOME
    val scope = rememberCoroutineScope()

    val categoryOptions = if (isIncome) {
        incomeCategoryOptions(customIncomeCategories)
    } else {
        expenseCategoryOptions(customExpenseCategories)
    }
    val defaultCategoryLabel = categoryOptions.firstOrNull().orEmpty()

    var amountText by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(defaultDate) }
    var revenueCategory by remember { mutableStateOf(RevenueCategory.OTHER) }
    var expenseCategory by remember { mutableStateOf(ExpenseCategory.OTHER) }
    var selectedCategoryLabel by remember(type, defaultCategoryLabel) {
        mutableStateOf(defaultCategoryLabel)
    }
    var markAsCollected by remember(forecastMode) { mutableStateOf(!forecastMode) }
    var isPaid by remember(forecastMode) { mutableStateOf(!forecastMode) }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var isRecurring by remember { mutableStateOf(false) }
    var selectedRecurrence by remember { mutableStateOf(ExpenseRecurrence.MONTHLY) }
    var showRecurrenceMenu by remember { mutableStateOf(false) }
    var hasRecurrenceEnd by remember { mutableStateOf(false) }
    var recurrenceEndDate by remember(selectedMonth) { mutableStateOf(selectedMonth.atEndOfMonth()) }
    var showRevenueMenu by remember { mutableStateOf(false) }
    var showExpenseMenu by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val amountFocus = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }

    val (scanState, scanActions) = rememberReceiptScan(snackbarHostState) { result ->
        result.amount?.let { amount ->
            amountText = if (amount % 1.0 == 0.0) {
                amount.toLong().toString()
            } else {
                amount.toString().replace('.', ',')
            }
        }
        result.date?.let { date = it }
        result.merchantHint?.let { hint ->
            if (label.isBlank()) label = hint
        }
    }

    val categoryRequiredError = stringResource(R.string.category_required)
    val invalidAmountError = stringResource(R.string.invalid_amount)
    val labelRequiredError = stringResource(R.string.label_required)
    val endDateAfterExpenseError = stringResource(R.string.end_date_after_expense)
    val amountLabel = stringResource(R.string.amount)
    val dateLabel = stringResource(R.string.date)
    val clientLabel = stringResource(R.string.client)
    val supplierLabel = stringResource(R.string.supplier)
    val clientPlaceholder = stringResource(R.string.client_placeholder)
    val supplierPlaceholder = stringResource(R.string.supplier_placeholder)
    val incomeCategoryLabel = stringResource(R.string.income_category)
    val expenseCategoryLabel = stringResource(R.string.expense_category)
    val noteLabel = stringResource(R.string.note_optional)
    val notePlaceholder = stringResource(R.string.note_placeholder)

    LaunchedEffect(Unit) {
        amountFocus.requestFocus()
    }

    Scaffold(
        containerColor = FormBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(color = FormBackground, shadowElevation = 0.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onBack,
                        shape = RoundedCornerShape(10.dp),
                        color = FieldBackground,
                        border = BorderStroke(1.dp, FieldBorder)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            modifier = Modifier.padding(10.dp).size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = type.localizedTitle(forecastMode),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!isIncome) {
                ReceiptScanPlaceholder(
                    isScanning = scanState.isScanning,
                    onClick = scanActions.onOpenSourcePicker
                )
                scanState.successMessage?.let { ReceiptScanSuccessBanner(it) }
            }

            FormLabeledField(
                label = amountLabel,
                value = amountText,
                onValueChange = { amountText = it },
                leadingIcon = Icons.Default.Euro,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.focusRequester(amountFocus),
                suffix = appCurrencySymbol()
            )

            TreasuryDateField(
                label = dateLabel,
                date = date,
                onDateChange = { date = it }
            )

            FormLabeledField(
                label = if (isIncome) clientLabel else supplierLabel,
                value = label,
                onValueChange = { label = it },
                leadingIcon = if (isIncome) Icons.Default.Person else Icons.Default.Store,
                placeholder = if (isIncome) clientPlaceholder else supplierPlaceholder
            )

            CategoryDropdownField(
                label = if (isIncome) incomeCategoryLabel else expenseCategoryLabel,
                value = selectedCategoryLabel,
                expanded = if (isIncome) showRevenueMenu else showExpenseMenu,
                onExpandedChange = { expanded ->
                    if (isIncome) showRevenueMenu = expanded else showExpenseMenu = expanded
                },
                options = categoryOptions,
                onSelect = { selected ->
                    selectedCategoryLabel = selected
                    if (isIncome) {
                        val resolved = CategorySelection.resolveIncome(
                            selected,
                            customIncomeCategories
                        )
                        revenueCategory = resolved.revenueCategory ?: RevenueCategory.OTHER
                        showRevenueMenu = false
                    } else {
                        val resolved = CategorySelection.resolveExpense(
                            selected,
                            customExpenseCategories
                        )
                        expenseCategory = resolved.expenseCategory ?: ExpenseCategory.OTHER
                        showExpenseMenu = false
                    }
                }
            )

            if (categoryOptions.isEmpty()) {
                Text(
                    text = stringResource(R.string.add_category_in_settings),
                    fontSize = 13.sp,
                    color = Color(0xFFFF9800)
                )
            }

            if (!isIncome) {
                FormLabeledField(
                    label = noteLabel,
                    value = note,
                    onValueChange = { note = it },
                    leadingIcon = Icons.Default.Category,
                    placeholder = notePlaceholder,
                    singleLine = false,
                    minLines = 2
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it }
                    )
                    Text(stringResource(R.string.recurring_expense), fontSize = 14.sp)
                }
                if (isRecurring) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = showRecurrenceMenu,
                            onExpandedChange = { showRecurrenceMenu = it }
                        ) {
                            OutlinedTextField(
                                value = selectedRecurrence.localizedLabel(),
                                onValueChange = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                label = { Text(stringResource(R.string.frequency)) },
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = showRecurrenceMenu)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = FieldBackground,
                                    unfocusedContainerColor = FieldBackground,
                                    focusedBorderColor = FieldBorder,
                                    unfocusedBorderColor = FieldBorder
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = showRecurrenceMenu,
                                onDismissRequest = { showRecurrenceMenu = false }
                            ) {
                                ExpenseRecurrence.entries.forEach { recurrence ->
                                    DropdownMenuItem(
                                        text = { Text(recurrence.localizedLabel()) },
                                        onClick = {
                                            selectedRecurrence = recurrence
                                            showRecurrenceMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = hasRecurrenceEnd,
                                onCheckedChange = { hasRecurrenceEnd = it }
                            )
                            Text(stringResource(R.string.end_date), fontSize = 14.sp)
                        }
                        if (hasRecurrenceEnd) {
                            TreasuryDateField(
                                label = stringResource(R.string.recurrence_end),
                                date = recurrenceEndDate,
                                onDateChange = { recurrenceEndDate = it }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPaid) {
                            stringResource(R.string.already_paid_expense)
                        } else {
                            stringResource(R.string.upcoming_forecast)
                        },
                        fontSize = 14.sp
                    )
                    Switch(checked = isPaid, onCheckedChange = { isPaid = it })
                }
                if (isPaid) {
                    TreasuryPaymentMethodField(
                        selectedMethod = paymentMethod,
                        onMethodChange = { paymentMethod = it }
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = markAsCollected, onCheckedChange = { markAsCollected = it })
                    Text(
                        text = if (forecastMode) {
                            stringResource(R.string.already_collected)
                        } else {
                            stringResource(R.string.fully_collected)
                        },
                        fontSize = 14.sp
                    )
                }
                if (markAsCollected) {
                    TreasuryPaymentMethodField(
                        selectedMethod = paymentMethod,
                        onMethodChange = { paymentMethod = it }
                    )
                }
            }

            saveError?.let { Text(it, color = Color(0xFFF44336), fontSize = 13.sp) }

            OutlinedButton(
                onClick = {
                    val amount = amountText.replace(" ", "").replace(",", ".").toDoubleOrNull()
                    val finalLabel = if (!isIncome && note.isNotBlank()) "$label — $note" else label
                    when {
                        amount == null || amount <= 0 -> saveError = invalidAmountError
                        label.isBlank() -> saveError = labelRequiredError
                        selectedCategoryLabel.isBlank() -> saveError = categoryRequiredError
                        !isIncome && isRecurring && hasRecurrenceEnd && recurrenceEndDate.isBefore(date) ->
                            saveError = endDateAfterExpenseError
                        else -> {
                            saveError = null
                            isSaving = true
                            if (isIncome) {
                                val resolved = CategorySelection.resolveIncome(
                                    selectedCategoryLabel,
                                    customIncomeCategories
                                )
                                onSaveIncome(
                                    finalLabel,
                                    amount,
                                    date,
                                    resolved.revenueCategory ?: RevenueCategory.OTHER,
                                    resolved.customLabel.orEmpty(),
                                    markAsCollected,
                                    paymentMethod
                                ) { error ->
                                    isSaving = false
                                    if (error == null) onBack() else saveError = error
                                }
                            } else {
                                val resolved = CategorySelection.resolveExpense(
                                    selectedCategoryLabel,
                                    customExpenseCategories
                                )
                                onSaveExpense(
                                    finalLabel,
                                    amount,
                                    date,
                                    resolved.expenseCategory ?: ExpenseCategory.OTHER,
                                    resolved.customLabel.orEmpty(),
                                    isRecurring,
                                    if (isRecurring) selectedRecurrence else null,
                                    if (isRecurring && hasRecurrenceEnd) recurrenceEndDate else null,
                                    isPaid,
                                    paymentMethod
                                ) { error ->
                                    isSaving = false
                                    if (error == null) onBack() else saveError = error
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, Color(0xFF1A1A1A))
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = if (isIncome) {
                            stringResource(R.string.save_invoice)
                        } else {
                            stringResource(R.string.save_expense)
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A1A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (!isIncome) {
        ReceiptSourcePickerSheet(
            visible = scanActions.isSourcePickerVisible(),
            onDismiss = scanActions.onDismissSourcePicker,
            onTakePhoto = scanActions.onTakePhoto,
            onPickGallery = scanActions.onPickGallery
        )
    }
}

@Composable
private fun ReceiptScanPlaceholder(
    isScanning: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, FieldBorder, RoundedCornerShape(14.dp))
            .background(FieldBackground, RoundedCornerShape(14.dp))
            .clickable(enabled = !isScanning, onClick = onClick)
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isScanning) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.reading_receipt), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF424242))
        } else {
            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF9E9E9E), modifier = Modifier.size(28.dp))
            Text(stringResource(R.string.take_receipt_photo), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF424242))
            Text(stringResource(R.string.receipt_auto_fill), fontSize = 12.sp, color = Color(0xFF9E9E9E))
        }
    }
}

@Composable
private fun FormLabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    suffix: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B), modifier = Modifier.padding(bottom = 6.dp))
        if (onClick != null && readOnly) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    enabled = false,
                    placeholder = if (placeholder.isNotBlank()) {
                        { Text(placeholder, color = Color(0xFFBDBDBD)) }
                    } else {
                        null
                    },
                    singleLine = singleLine,
                    minLines = minLines,
                    keyboardOptions = keyboardOptions,
                    leadingIcon = {
                        Icon(leadingIcon, contentDescription = null, tint = Color(0xFF9E9E9E), modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = suffix?.let { { Text(it, fontSize = 14.sp, color = Color(0xFF64748B)) } },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = FieldBackground,
                        unfocusedContainerColor = FieldBackground,
                        disabledContainerColor = FieldBackground,
                        focusedBorderColor = FieldBorder,
                        unfocusedBorderColor = FieldBorder,
                        disabledBorderColor = FieldBorder,
                        disabledTextColor = Color(0xFF1A1A1A)
                    )
                )
            }
        } else {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                readOnly = readOnly,
                placeholder = if (placeholder.isNotBlank()) {
                    { Text(placeholder, color = Color(0xFFBDBDBD)) }
                } else {
                    null
                },
                singleLine = singleLine,
                minLines = minLines,
                keyboardOptions = keyboardOptions,
                leadingIcon = {
                    Icon(leadingIcon, contentDescription = null, tint = Color(0xFF9E9E9E), modifier = Modifier.size(20.dp))
                },
                trailingIcon = suffix?.let { { Text(it, fontSize = 14.sp, color = Color(0xFF64748B)) } },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = FieldBackground,
                    unfocusedContainerColor = FieldBackground,
                    disabledContainerColor = FieldBackground,
                    focusedBorderColor = FieldBorder,
                    unfocusedBorderColor = FieldBorder
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdownField(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B), modifier = Modifier.padding(bottom = 6.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                readOnly = true,
                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, tint = Color(0xFF9E9E9E)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = FieldBackground,
                    unfocusedContainerColor = FieldBackground,
                    focusedBorderColor = FieldBorder,
                    unfocusedBorderColor = FieldBorder
                )
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionTypeChoiceSheet(
    canAddIncome: Boolean,
    canAddExpense: Boolean,
    forecastMode: Boolean = false,
    onDismiss: () -> Unit,
    onSelectIncome: () -> Unit,
    onSelectExpense: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.new_transaction),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (canAddIncome) {
                OutlinedButton(
                    onClick = onSelectIncome,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50))
                ) {
                    Text(
                        text = if (forecastMode) {
                            "➕ ${stringResource(R.string.forecast_income)}"
                        } else {
                            "➕ ${stringResource(R.string.new_collection)}"
                        },
                        fontSize = 15.sp
                    )
                }
            }
            if (canAddExpense) {
                OutlinedButton(
                    onClick = onSelectExpense,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336))
                ) {
                    Text(
                        text = if (forecastMode) {
                            "➖ ${stringResource(R.string.forecast_expense)}"
                        } else {
                            "➖ ${stringResource(R.string.new_expense)}"
                        },
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
