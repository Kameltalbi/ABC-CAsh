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
import androidx.compose.material.icons.filled.Label
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import com.abccash.app.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.treasury.data.ExpenseCategory
import com.abccash.app.treasury.data.ExpenseRecurrence
import com.abccash.app.treasury.data.PaymentMethod
import com.abccash.app.treasury.data.RevenueCategory
import com.abccash.app.treasury.data.TransactionType
import java.time.LocalDate
import java.time.YearMonth

private val FormBackground = Color.White
private val FieldBackground = Color.White
private val FieldBorder = Color(0xFFE8E4DD)

private enum class PaymentChannel {
    BANK,
    CASH;

    fun toPaymentMethod(): PaymentMethod = when (this) {
        BANK -> PaymentMethod.TRANSFER
        CASH -> PaymentMethod.CASH
    }
}

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
        clientContactId: String?,
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
        note: String,
        onResult: (String?) -> Unit
    ) -> Unit
) {
    @Suppress("UNUSED_PARAMETER")
    val unusedMonth = selectedMonth
    @Suppress("UNUSED_PARAMETER")
    val unusedCustomIncome = customIncomeCategories
    @Suppress("UNUSED_PARAMETER")
    val unusedCustomExpense = customExpenseCategories
    @Suppress("UNUSED_PARAMETER")
    val unusedForecastMode = forecastMode

    val isIncome = type == TransactionType.INCOME
    val today = remember { LocalDate.now() }

    val incomeOptions = defaultIncomeFormOptions()
    val expenseOptions = defaultExpenseFormOptions()

    var amountText by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var selectedIncomeCategory by remember { mutableStateOf(RevenueCategory.SERVICE) }
    var selectedExpenseCategory by remember { mutableStateOf(ExpenseCategory.TAXES) }
    var paymentChannel by remember { mutableStateOf(PaymentChannel.BANK) }
    var isRecurring by remember { mutableStateOf(false) }
    var selectedRecurrence by remember { mutableStateOf(ExpenseRecurrence.MONTHLY) }
    var showRecurrenceMenu by remember { mutableStateOf(false) }
    var hasRecurrenceEnd by remember { mutableStateOf(false) }
    var recurrenceEndDate by remember { mutableStateOf(LocalDate.now().plusMonths(6)) }
    var showIncomeCategoryMenu by remember { mutableStateOf(false) }
    var showExpenseCategoryMenu by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val amountFocus = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }

    val incomeCategoryLabel = incomeOptions.firstOrNull { it.value == selectedIncomeCategory }
        ?.let { stringResource(it.labelRes) }.orEmpty()
    val expenseCategoryLabel = expenseOptions.firstOrNull { it.value == selectedExpenseCategory }
        ?.let { stringResource(it.labelRes) }.orEmpty()

    val isForecastDate = date.isAfter(today)
    val isRealized = !isForecastDate

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
    val titleLabel = stringResource(R.string.label)
    val incomeCategoryField = stringResource(R.string.income_category)
    val expenseCategoryField = stringResource(R.string.expense_category)
    val labelPlaceholder = if (isIncome) {
        stringResource(R.string.label_placeholder_income)
    } else {
        stringResource(R.string.label_placeholder_expense)
    }
    val paymentMethodLabel = stringResource(R.string.payment_method)

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
                        text = type.localizedTitle(),
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
            LargeAmountField(
                label = amountLabel,
                value = amountText,
                onValueChange = { amountText = it },
                modifier = Modifier.focusRequester(amountFocus),
                suffix = appCurrencySymbol()
            )

            FormLabeledField(
                label = titleLabel,
                value = label,
                onValueChange = { label = it },
                leadingIcon = Icons.Default.Label,
                placeholder = labelPlaceholder
            )

            TreasuryDateField(
                label = dateLabel,
                date = date,
                onDateChange = { date = it }
            )

            Text(
                text = if (isForecastDate) {
                    stringResource(R.string.transaction_date_forecast_hint)
                } else {
                    stringResource(R.string.transaction_date_real_hint)
                },
                fontSize = 12.sp,
                color = if (isForecastDate) Color(0xFFFF9800) else Color(0xFF64748B),
                lineHeight = 16.sp
            )

            if (isIncome) {
                EnumCategoryDropdownField(
                    label = incomeCategoryField,
                    value = incomeCategoryLabel,
                    expanded = showIncomeCategoryMenu,
                    onExpandedChange = { showIncomeCategoryMenu = it },
                    options = incomeOptions.map { stringResource(it.labelRes) },
                    onSelect = { index ->
                        incomeOptions.getOrNull(index)?.let { option ->
                            selectedIncomeCategory = option.value
                        }
                        showIncomeCategoryMenu = false
                    }
                )
            } else {
                EnumCategoryDropdownField(
                    label = expenseCategoryField,
                    value = expenseCategoryLabel,
                    expanded = showExpenseCategoryMenu,
                    onExpandedChange = { showExpenseCategoryMenu = it },
                    options = expenseOptions.map { stringResource(it.labelRes) },
                    onSelect = { index ->
                        expenseOptions.getOrNull(index)?.let { option ->
                            selectedExpenseCategory = option.value
                        }
                        showExpenseCategoryMenu = false
                    }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    paymentMethodLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
                BankCashPaymentChips(
                    selected = paymentChannel,
                    onSelect = { paymentChannel = it }
                )
            }

            if (!isIncome) {
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

                OutlinedButton(
                    onClick = scanActions.onOpenSourcePicker,
                    enabled = !scanState.isScanning,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, FieldBorder)
                ) {
                    if (scanState.isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.reading_receipt))
                    } else {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (scanState.lastScannedUri != null) {
                                stringResource(R.string.receipt_attached)
                            } else {
                                stringResource(R.string.attach_receipt)
                            }
                        )
                    }
                }
                scanState.successMessage?.let { ReceiptScanSuccessBanner(it) }
            }

            saveError?.let { Text(it, color = Color(0xFFF44336), fontSize = 13.sp) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.cancel))
                }

                Button(
                    onClick = {
                        val amount = amountText.replace(" ", "").replace(",", ".").toDoubleOrNull()
                        when {
                            amount == null || amount <= 0 -> saveError = invalidAmountError
                            label.isBlank() -> saveError = labelRequiredError
                            !isIncome && isRecurring && hasRecurrenceEnd && recurrenceEndDate.isBefore(date) ->
                                saveError = endDateAfterExpenseError
                            else -> {
                                saveError = null
                                isSaving = true
                                val method = paymentChannel.toPaymentMethod()
                                if (isIncome) {
                                    onSaveIncome(
                                        label.trim(),
                                        null,
                                        amount,
                                        date,
                                        selectedIncomeCategory,
                                        "",
                                        isRealized,
                                        method
                                    ) { error ->
                                        isSaving = false
                                        if (error == null) onBack() else saveError = error
                                    }
                                } else {
                                    onSaveExpense(
                                        label.trim(),
                                        amount,
                                        date,
                                        selectedExpenseCategory,
                                        "",
                                        isRecurring,
                                        if (isRecurring) selectedRecurrence else null,
                                        if (isRecurring && hasRecurrenceEnd) recurrenceEndDate else null,
                                        isRealized,
                                        method,
                                        ""
                                    ) { error ->
                                        isSaving = false
                                        if (error == null) onBack() else saveError = error
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = if (isIncome) {
                                stringResource(R.string.save_invoice)
                            } else {
                                stringResource(R.string.save_expense)
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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
private fun LargeAmountField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    stringResource(R.string.amount_placeholder),
                    fontSize = 28.sp,
                    color = Color(0xFFBDBDBD)
                )
            },
            textStyle = LocalTextStyle.current.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            leadingIcon = {
                Icon(Icons.Default.Euro, contentDescription = null, tint = Color(0xFF9E9E9E))
            },
            trailingIcon = suffix?.let {
                {
                    Text(it, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = FieldBackground,
                unfocusedContainerColor = FieldBackground,
                focusedBorderColor = FieldBorder,
                unfocusedBorderColor = FieldBorder
            )
        )
    }
}

@Composable
private fun BankCashPaymentChips(
    selected: PaymentChannel,
    onSelect: (PaymentChannel) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilterChip(
            selected = selected == PaymentChannel.BANK,
            onClick = { onSelect(PaymentChannel.BANK) },
            label = { Text(stringResource(R.string.payment_channel_bank)) },
            leadingIcon = if (selected == PaymentChannel.BANK) {
                { Text("🏦", fontSize = 14.sp) }
            } else {
                { Text("🏦", fontSize = 14.sp) }
            },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = selected == PaymentChannel.CASH,
            onClick = { onSelect(PaymentChannel.CASH) },
            label = { Text(stringResource(R.string.payment_channel_cash)) },
            leadingIcon = { Text("💵", fontSize = 14.sp) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ReceiptScanPlaceholder(
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
private fun EnumCategoryDropdownField(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    onSelect: (Int) -> Unit
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
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(index) })
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
    @Suppress("UNUSED_PARAMETER")
    val unused = forecastMode
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
                    Text("➕ ${stringResource(R.string.new_collection)}", fontSize = 15.sp)
                }
            }
            if (canAddExpense) {
                OutlinedButton(
                    onClick = onSelectExpense,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336))
                ) {
                    Text("➖ ${stringResource(R.string.new_expense)}", fontSize = 15.sp)
                }
            }
        }
    }
}
