package com.abccash.app.treasury.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
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
import com.abccash.app.treasury.data.defaultDateForMonth
import java.time.LocalDate
import java.time.YearMonth

enum class TransferDirection {
    BANK_TO_CASH,
    CASH_TO_BANK
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InternalTransferScreen(
    selectedMonth: YearMonth,
    onBack: () -> Unit,
    onSave: (amount: Double, date: LocalDate, direction: TransferDirection, onResult: (String?) -> Unit) -> Unit
) {
    val defaultDate = remember(selectedMonth) { defaultDateForMonth(selectedMonth) }

    var amountText by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(defaultDate) }
    var direction by remember { mutableStateOf(TransferDirection.BANK_TO_CASH) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val invalidAmountError = stringResource(R.string.invalid_amount)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.internal_transfer_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.transfer_amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                suffix = { CurrencySuffix() }
            )

            TreasuryDateField(
                label = stringResource(R.string.date),
                date = date,
                onDateChange = { date = it }
            )

            Text(
                text = stringResource(R.string.transfer_direction),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1A1A1A)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = direction == TransferDirection.BANK_TO_CASH,
                    onClick = { direction = TransferDirection.BANK_TO_CASH },
                    label = { Text(stringResource(R.string.transfer_from_bank_to_cash), fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = direction == TransferDirection.CASH_TO_BANK,
                    onClick = { direction = TransferDirection.CASH_TO_BANK },
                    label = { Text(stringResource(R.string.transfer_from_cash_to_bank), fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F7FF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (direction == TransferDirection.BANK_TO_CASH) "🏦 Bank" else "💵 Cash",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(text = "−", fontSize = 18.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(28.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (direction == TransferDirection.BANK_TO_CASH) "💵 Cash" else "🏦 Bank",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(text = "+", fontSize = 18.sp, color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
                    }
                }
            }

            saveError?.let {
                Text(text = it, color = Color(0xFFF44336), fontSize = 13.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        val amount = amountText.replace(",", ".").replace(" ", "").toDoubleOrNull()
                        when {
                            amount == null || amount <= 0 -> saveError = invalidAmountError
                            else -> {
                                saveError = null
                                isSaving = true
                                onSave(amount, date, direction) { error ->
                                    isSaving = false
                                    if (error == null) onBack() else saveError = error
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.save_transfer))
                    }
                }
            }
        }
    }
}
