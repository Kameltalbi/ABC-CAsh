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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.treasury.data.defaultDateForMonth
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewInvoiceScreen(
    selectedMonth: YearMonth,
    onBack: () -> Unit,
    onSave: (
        invoiceNumber: String,
        clientName: String,
        totalAmount: Double,
        dueDate: LocalDate,
        markAsCollected: Boolean,
        onResult: (String?) -> Unit
    ) -> Unit
) {
    val defaultDate = remember(selectedMonth) { defaultDateForMonth(selectedMonth) }

    var invoiceNumber by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(defaultDate) }
    var markAsCollected by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouvel encaissement") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TreasurySelectedMonthHint(selectedMonth)

            OutlinedTextField(
                value = invoiceNumber,
                onValueChange = { invoiceNumber = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("N° facture") },
                singleLine = true
            )

            OutlinedTextField(
                value = clientName,
                onValueChange = { clientName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Client") },
                singleLine = true
            )

            OutlinedTextField(
                value = totalAmount,
                onValueChange = { totalAmount = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Montant total") },
                placeholder = { Text("0.000") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix = { CurrencySuffix() }
            )

            TreasuryDateField(
                label = "Date d'échéance",
                date = dueDate,
                onDateChange = { dueDate = it }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = markAsCollected,
                    onCheckedChange = { markAsCollected = it }
                )
                Column {
                    Text(
                        text = "Encaissé intégralement",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Marquer comme soldé dès l'enregistrement",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            saveError?.let { error ->
                Text(text = error, color = Color(0xFFF44336), fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val amount = totalAmount.replace(" ", "").replace(",", ".").toDoubleOrNull()
                    when {
                        invoiceNumber.isBlank() -> saveError = "Le numéro de facture est obligatoire"
                        clientName.isBlank() -> saveError = "Le client est obligatoire"
                        amount == null || amount <= 0 -> saveError = "Montant invalide"
                        else -> {
                            saveError = null
                            isSaving = true
                            onSave(invoiceNumber, clientName, amount, dueDate, markAsCollected) { error ->
                                isSaving = false
                                if (error == null) {
                                    onBack()
                                } else {
                                    saveError = error
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !isSaving,
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Enregistrer l'encaissement")
                }
            }
        }
    }
}
