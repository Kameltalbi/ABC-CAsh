package com.abccash.app.treasury.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R
import com.abccash.app.treasury.data.ExpenseRecurrence
import com.abccash.app.treasury.data.defaultDateForMonth
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewExpenseScreen(
    selectedMonth: YearMonth,
    onBack: () -> Unit,
    onSave: (String, Double, LocalDate, Boolean, ExpenseRecurrence?, LocalDate?, Boolean) -> Unit
) {
    val defaultDate = remember(selectedMonth) { defaultDateForMonth(selectedMonth) }

    var expenseLabel by remember { mutableStateOf("") }
    var expenseAmount by remember { mutableStateOf("") }
    var expenseDate by remember { mutableStateOf(defaultDate) }
    var isRecurring by remember { mutableStateOf(false) }
    var selectedRecurrence by remember { mutableStateOf(ExpenseRecurrence.MONTHLY) }
    var showRecurrenceMenu by remember { mutableStateOf(false) }
    var hasRecurrenceEnd by remember { mutableStateOf(false) }
    var recurrenceEndDate by remember { mutableStateOf(selectedMonth.atEndOfMonth()) }
    var isPaid by remember { mutableStateOf(true) }
    var saveError by remember { mutableStateOf<String?>(null) }

    val labelRequiredError = stringResource(R.string.label_required)
    val invalidAmountError = stringResource(R.string.invalid_amount)
    val endDateAfterExpenseError = stringResource(R.string.end_date_after_expense)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_expense)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TreasurySelectedMonthHint(selectedMonth)

            OutlinedTextField(
                value = expenseLabel,
                onValueChange = { expenseLabel = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.label)) },
                placeholder = { Text(stringResource(R.string.expense_label_placeholder)) },
                singleLine = true
            )

            OutlinedTextField(
                value = expenseAmount,
                onValueChange = { expenseAmount = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.amount)) },
                placeholder = { Text(stringResource(R.string.amount_placeholder)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                suffix = { CurrencySuffix() }
            )

            TreasuryDateField(
                label = stringResource(R.string.date),
                date = expenseDate,
                onDateChange = { expenseDate = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isRecurring,
                    onCheckedChange = { isRecurring = it }
                )
                Text(stringResource(R.string.recurring_expense))
            }

            if (isRecurring) {
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
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showRecurrenceMenu) }
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isPaid) {
                        stringResource(R.string.already_paid_expense)
                    } else {
                        stringResource(R.string.upcoming_badge)
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = isPaid,
                    onCheckedChange = { isPaid = it }
                )
            }

            saveError?.let { error ->
                Text(text = error, color = Color(0xFFF44336), fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val amount = expenseAmount.replace(" ", "").replace(",", ".").toDoubleOrNull()
                    when {
                        expenseLabel.isBlank() -> saveError = labelRequiredError
                        amount == null || amount <= 0 -> saveError = invalidAmountError
                        isRecurring && hasRecurrenceEnd && recurrenceEndDate.isBefore(expenseDate) ->
                            saveError = endDateAfterExpenseError
                        else -> {
                            saveError = null
                            onSave(
                                expenseLabel,
                                amount,
                                expenseDate,
                                isRecurring,
                                selectedRecurrence,
                                if (isRecurring && hasRecurrenceEnd) recurrenceEndDate else null,
                                isPaid
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.save_expense))
            }
        }
    }
}
