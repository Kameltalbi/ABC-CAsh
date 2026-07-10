package com.abccash.app.treasury.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.abccash.app.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TreasuryCorrectionDialog(
    calculatedBalance: Double,
    currentBankBalance: Double,
    formatAmount: (Double) -> String,
    onDismiss: () -> Unit,
    onValidate: (newBalance: Double, date: LocalDate, motif: String) -> Unit
) {
    var newBalanceText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }
    var motif by remember { mutableStateOf("") }
    var balanceError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }
    var motifError by remember { mutableStateOf<String?>(null) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val motifRequired = stringResource(R.string.treasury_correction_motif_required)
    val dateRequired = stringResource(R.string.treasury_correction_date_required)
    val balanceEmptyError = stringResource(R.string.treasury_init_empty_error)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.treasury_correction_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.treasury_correction_calculated),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Text(
                        text = formatAmount(calculatedBalance),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.treasury_correction_current_bank),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Text(
                        text = formatAmount(currentBankBalance),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = newBalanceText,
                    onValueChange = {
                        newBalanceText = it
                        balanceError = null
                    },
                    label = { Text(stringResource(R.string.treasury_correction_new_balance)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = balanceError != null,
                    supportingText = balanceError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = dateText,
                    onValueChange = {
                        dateText = it
                        dateError = null
                    },
                    label = { Text(stringResource(R.string.treasury_correction_date)) },
                    placeholder = { Text("JJ/MM/AAAA") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = dateError != null,
                    supportingText = dateError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = motif,
                    onValueChange = {
                        motif = it
                        motifError = null
                    },
                    label = { Text(stringResource(R.string.treasury_correction_motif)) },
                    placeholder = { Text(stringResource(R.string.treasury_correction_motif_hint)) },
                    isError = motifError != null,
                    supportingText = motifError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = {
                            val rawAmount = newBalanceText.trim().replace(",", ".").replace(" ", "")
                            val parsedAmount = rawAmount.toDoubleOrNull()
                            val parsedDate = runCatching {
                                LocalDate.parse(dateText.trim(), dateFormatter)
                            }.getOrNull()

                            var hasError = false
                            if (rawAmount.isEmpty() || parsedAmount == null) {
                                balanceError = balanceEmptyError
                                hasError = true
                            }
                            if (dateText.isBlank() || parsedDate == null) {
                                dateError = dateRequired
                                hasError = true
                            }
                            if (motif.isBlank()) {
                                motifError = motifRequired
                                hasError = true
                            }
                            if (!hasError && parsedAmount != null && parsedDate != null) {
                                onValidate(parsedAmount, parsedDate, motif.trim())
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.action_validate))
                    }
                }
            }
        }
    }
}

@Composable
fun TreasuryCorrectionConfirmDialog(
    oldBalance: Double,
    newBalance: Double,
    correctionDate: LocalDate,
    formatAmount: (Double) -> String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    titleRes: Int = R.string.treasury_confirm_title,
    bodyRes: Int = R.string.treasury_confirm_body,
    referenceNoteRes: Int = R.string.treasury_confirm_reference_note,
    referenceNoteText: String? = null
) {
    val ecart = newBalance - oldBalance
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(titleRes),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(bodyRes),
                    fontWeight = FontWeight.SemiBold
                )
                HorizontalDivider()
                CorrectionRow(
                    label = stringResource(R.string.treasury_confirm_old_balance),
                    value = formatAmount(oldBalance)
                )
                CorrectionRow(
                    label = stringResource(R.string.treasury_confirm_new_balance),
                    value = formatAmount(newBalance)
                )
                CorrectionRow(
                    label = stringResource(R.string.treasury_confirm_gap),
                    value = (if (ecart >= 0) "+" else "") + formatAmount(ecart),
                    valueColor = when {
                        ecart > 0 -> Color(0xFF16A34A)
                        ecart < 0 -> Color(0xFFDC2626)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                HorizontalDivider()
                Text(
                    text = referenceNoteText ?: stringResource(
                        referenceNoteRes,
                        correctionDate.format(dateFormatter)
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.treasury_confirm_question),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.treasury_confirm_yes))
            }
        }
    )
}

@Composable
fun TreasuryOpeningBalanceDialog(
    currentBalance: Double,
    currentDate: LocalDate,
    formatAmount: (Double) -> String,
    onDismiss: () -> Unit,
    onValidate: (newBalance: Double, date: LocalDate, motif: String) -> Unit
) {
    var newBalanceText by remember {
        mutableStateOf(currentBalance.toString().replace('.', ','))
    }
    var selectedDate by remember { mutableStateOf(currentDate) }
    var motif by remember { mutableStateOf("") }
    var balanceError by remember { mutableStateOf<String?>(null) }
    var motifError by remember { mutableStateOf<String?>(null) }

    val motifRequired = stringResource(R.string.treasury_correction_motif_required)
    val balanceEmptyError = stringResource(R.string.treasury_init_empty_error)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.treasury_opening_revision_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.treasury_opening_revision_current),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Text(
                        text = formatAmount(currentBalance),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = newBalanceText,
                    onValueChange = {
                        newBalanceText = it
                        balanceError = null
                    },
                    label = { Text(stringResource(R.string.treasury_opening_revision_new_balance)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = balanceError != null,
                    supportingText = balanceError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                TreasuryDateField(
                    label = stringResource(R.string.treasury_init_balance_date),
                    date = selectedDate,
                    onDateChange = { selectedDate = it }
                )

                OutlinedTextField(
                    value = motif,
                    onValueChange = {
                        motif = it
                        motifError = null
                    },
                    label = { Text(stringResource(R.string.treasury_correction_motif)) },
                    placeholder = { Text(stringResource(R.string.treasury_opening_revision_motif_hint)) },
                    isError = motifError != null,
                    supportingText = motifError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = {
                            val rawAmount = newBalanceText.trim().replace(",", ".").replace(" ", "")
                            val parsedAmount = rawAmount.toDoubleOrNull()

                            var hasError = false
                            if (rawAmount.isEmpty() || parsedAmount == null) {
                                balanceError = balanceEmptyError
                                hasError = true
                            }
                            if (motif.isBlank()) {
                                motifError = motifRequired
                                hasError = true
                            }
                            if (!hasError && parsedAmount != null) {
                                onValidate(parsedAmount, selectedDate, motif.trim())
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.action_validate))
                    }
                }
            }
        }
    }
}

@Composable
private fun CorrectionRow(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}
