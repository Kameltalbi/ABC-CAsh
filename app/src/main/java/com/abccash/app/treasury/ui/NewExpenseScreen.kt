package com.abccash.app.treasury.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.treasury.data.ExpenseRecurrence
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewExpenseScreen(
    selectedMonth: YearMonth,
    onBack: () -> Unit,
    onSave: (String, Double, LocalDate, Boolean, ExpenseRecurrence?, LocalDate?, Boolean) -> Unit
) {
    val defaultDate = remember(selectedMonth) {
        val today = LocalDate.now()
        if (YearMonth.from(today) == selectedMonth) today else selectedMonth.atDay(1)
    }

    var expenseLabel by remember { mutableStateOf("") }
    var expenseAmount by remember { mutableStateOf("") }
    var expenseDate by remember { mutableStateOf(defaultDate) }
    var isRecurring by remember { mutableStateOf(false) }
    var selectedRecurrence by remember { mutableStateOf(ExpenseRecurrence.MONTHLY) }
    var showRecurrenceMenu by remember { mutableStateOf(false) }
    var hasRecurrenceEnd by remember { mutableStateOf(false) }
    var recurrenceEndDate by remember { mutableStateOf("") }
    var isPaid by remember { mutableStateOf(true) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = java.time.ZoneId.systemDefault()
            .rules
            .getOffset(expenseDate.atStartOfDay())
            .let { expenseDate.atStartOfDay().toInstant(it).toEpochMilli() }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            expenseDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Annuler")
                }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouvelle dépense") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = expenseLabel,
                onValueChange = { expenseLabel = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Libellé") },
                placeholder = { Text("Ex: Loyer bureau") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = expenseAmount,
                onValueChange = { expenseAmount = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Montant") },
                placeholder = { Text("0.000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                suffix = { Text("DT") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = expenseDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Date") },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Choisir date")
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isRecurring,
                    onCheckedChange = { isRecurring = it }
                )
                Text("Dépense récurrente")
            }

            if (isRecurring) {
                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = showRecurrenceMenu,
                    onExpandedChange = { showRecurrenceMenu = it }
                ) {
                    OutlinedTextField(
                        value = selectedRecurrence.label,
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = { Text("Fréquence") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showRecurrenceMenu) }
                    )

                    ExposedDropdownMenu(
                        expanded = showRecurrenceMenu,
                        onDismissRequest = { showRecurrenceMenu = false }
                    ) {
                        ExpenseRecurrence.entries.forEach { recurrence ->
                            DropdownMenuItem(
                                text = { Text(recurrence.label) },
                                onClick = {
                                    selectedRecurrence = recurrence
                                    showRecurrenceMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = hasRecurrenceEnd,
                        onCheckedChange = { hasRecurrenceEnd = it }
                    )
                    Text("Date de fin", fontSize = 14.sp)
                }

                if (hasRecurrenceEnd) {
                    OutlinedTextField(
                        value = recurrenceEndDate,
                        onValueChange = { recurrenceEndDate = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Fin récurrence yyyy-MM-dd") },
                        singleLine = true,
                        placeholder = { Text("2026-12-31") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isPaid) "Déjà payée" else "À venir",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = isPaid,
                    onCheckedChange = { isPaid = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val amount = expenseAmount.replace(",", ".").toDoubleOrNull()
                    val parsedEndDate = recurrenceEndDate.takeIf { hasRecurrenceEnd && it.isNotBlank() }
                        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    if (expenseLabel.isNotBlank() && amount != null && amount > 0) {
                        onSave(
                            expenseLabel,
                            amount,
                            expenseDate,
                            isRecurring,
                            selectedRecurrence,
                            if (isRecurring && hasRecurrenceEnd) parsedEndDate else null,
                            isPaid
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = expenseLabel.isNotBlank() &&
                    (expenseAmount.replace(",", ".").toDoubleOrNull()?.let { it > 0 } ?: false),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Enregistrer la dépense")
            }
        }
    }
}
