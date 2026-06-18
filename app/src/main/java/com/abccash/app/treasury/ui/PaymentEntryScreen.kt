package com.abccash.app.treasury.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.data.Payment
import com.abccash.app.treasury.data.PaymentMethod
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentEntryScreen(
    invoice: Invoice,
    onBack: () -> Unit,
    onSavePayment: (Double, LocalDate, PaymentMethod) -> Boolean
) {
    var paymentAmount by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var showMethodMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var paymentError by remember { mutableStateOf<String?>(null) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = java.time.ZoneId.systemDefault()
            .rules
            .getOffset(selectedDate.atStartOfDay())
            .let { selectedDate.atStartOfDay().toInstant(it).toEpochMilli() }
    )
    
    val formatAmount = rememberFormatMoney()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enregistrer un paiement") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = invoice.clientName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = invoice.invoiceNumber,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Montant total",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = formatAmount(invoice.totalAmount),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Déjà payé",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = formatAmount(invoice.paidAmount),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        HorizontalDivider()
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Reste à recouvrer",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = formatAmount(invoice.remainingAmount),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF44336)
                                )
                            }
                        }
                    }
                }
            }
            
            item {
                Text(
                    text = "Nouveau paiement",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                OutlinedTextField(
                    value = paymentAmount,
                    onValueChange = { paymentAmount = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Montant de l'avance") },
                    placeholder = { Text("0.000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    suffix = { CurrencySuffix() }
                )
            }
            
            item {
                OutlinedTextField(
                    value = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth(),
                    label = { Text("Date de paiement") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Choisir date")
                        }
                    }
                )
            }
            
            item {
                ExposedDropdownMenuBox(
                    expanded = showMethodMenu,
                    onExpandedChange = { showMethodMenu = it }
                ) {
                    OutlinedTextField(
                        value = selectedMethod.label,
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = { Text("Mode de règlement") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                    )
                    
                    ExposedDropdownMenu(
                        expanded = showMethodMenu,
                        onDismissRequest = { showMethodMenu = false }
                    ) {
                        PaymentMethod.values().forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method.label) },
                                onClick = {
                                    selectedMethod = method
                                    showMethodMenu = false
                                }
                            )
                        }
                    }
                }
            }
            
            item {
                paymentError?.let { error ->
                    Text(text = error, color = Color(0xFFF44336), fontSize = 13.sp)
                }
            }

            item {
                Button(
                    onClick = {
                        val amount = paymentAmount.replace(",", ".").toDoubleOrNull()
                        when {
                            amount == null || amount <= 0 -> {
                                paymentError = "Montant invalide"
                            }
                            amount > invoice.remainingAmount -> {
                                paymentError = "Maximum : ${formatAmount(invoice.remainingAmount)}"
                            }
                            else -> {
                                val saved = onSavePayment(amount, selectedDate, selectedMethod)
                                if (saved) {
                                    paymentError = null
                                } else {
                                    paymentError = "Impossible d'enregistrer le paiement"
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = paymentAmount.replace(",", ".").toDoubleOrNull()?.let { it > 0 } ?: false,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Enregistrer le paiement",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (invoice.payments.isNotEmpty()) {
                item {
                    Text(
                        text = "Historique des versements",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(invoice.payments.sortedByDescending { it.date }) { payment ->
                    PaymentHistoryItem(payment)
                }
            }
        }
    }
    
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = java.time.Instant.ofEpochMilli(millis)
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
}

@Composable
fun PaymentHistoryItem(payment: Payment) {
    val formatAmount = rememberFormatMoney()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = formatAmount(payment.amount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
                Text(
                    text = payment.method.label,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            Text(
                text = payment.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}
