package com.abccash.app.treasury.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val TreasuryFormDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun localDateToMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun millisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
fun TreasurySelectedMonthHint(selectedMonth: YearMonth) {
    Text(
        text = "Mois affiché : ${
            selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH))
                .replaceFirstChar { it.uppercase() }
        }",
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
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
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
        onConfirm = onDateChange
    )

    OutlinedTextField(
        value = date.format(TreasuryFormDateFormatter),
        onValueChange = {},
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        readOnly = true,
        enabled = enabled,
        trailingIcon = {
            IconButton(
                onClick = { showPicker = true },
                enabled = enabled
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = "Choisir la date")
            }
        }
    )
}
