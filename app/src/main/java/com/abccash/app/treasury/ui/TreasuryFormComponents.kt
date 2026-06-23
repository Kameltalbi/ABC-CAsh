package com.abccash.app.treasury.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.treasury.data.PaymentMethod
import androidx.compose.ui.res.stringResource
import com.abccash.app.R
import com.abccash.app.locale.AppLocale
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val AbcCashFabShape = RoundedCornerShape(16.dp)

@Composable
fun AbcCashFab(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Add,
    containerColor: Color = MaterialTheme.colorScheme.primary
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = Color.White,
        shape = AbcCashFabShape
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

val TreasuryFormDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun localDateToMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun millisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
fun TreasurySelectedMonthHint(selectedMonth: YearMonth) {
    Text(
        text = stringResource(R.string.displayed_month, AppLocale.monthYear(selectedMonth)),
        fontSize = 13.sp,
        color = Color.Gray
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreasuryDatePickerDialog(
    visible: Boolean,
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    if (!visible) return

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = localDateToMillis(selectedDate)
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onConfirm(millisToLocalDate(millis))
                    }
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState, showModeToggle = false)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreasuryDateField(
    label: String,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var showPicker by remember { mutableStateOf(false) }

    TreasuryDatePickerDialog(
        visible = showPicker,
        selectedDate = date,
        onDismiss = { showPicker = false },
        onConfirm = {
            onDateChange(it)
            showPicker = false
        }
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (enabled) {
                    Modifier.clickable { showPicker = true }
                } else {
                    Modifier
                }
            )
    ) {
        OutlinedTextField(
            value = date.format(TreasuryFormDateFormatter),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            readOnly = true,
            enabled = false,
            trailingIcon = {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = if (enabled) Color(0xFF64748B) else Color(0xFFBDBDBD)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledContainerColor = Color.White,
                disabledBorderColor = Color(0xFFE8E4DD),
                disabledTextColor = Color(0xFF1A1A1A),
                disabledLabelColor = Color(0xFF64748B)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreasuryPaymentMethodField(
    selectedMethod: PaymentMethod,
    onMethodChange: (PaymentMethod) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val fieldLabel = label ?: stringResource(R.string.payment_method)

    Column(modifier = modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedMethod.localizedLabel(),
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                label = { Text(fieldLabel) },
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                PaymentMethod.entries.forEach { method ->
                    DropdownMenuItem(
                        text = { Text(method.localizedLabel()) },
                        onClick = {
                            onMethodChange(method)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
