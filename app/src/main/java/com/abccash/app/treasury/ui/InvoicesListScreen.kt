package com.abccash.app.treasury.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.data.InvoiceStatus
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import com.abccash.app.treasury.data.defaultDateForMonth
import com.abccash.app.treasury.data.hasPermission
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.abccash.app.treasury.data.PaymentMethod
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class InvoiceFilter(val label: String) {
    ALL("Toutes"),
    DUE("Dues"),
    PARTIAL("Partielles"),
    PAID("Soldées")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesListScreen(
    userRole: UserRole,
    permissions: Set<UserPermission>,
    invoices: List<Invoice>,
    selectedMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    importFeedback: String? = null,
    onClearImportFeedback: () -> Unit = {},
    onNavigateToImport: () -> Unit,
    onNavigateToAddInvoice: () -> Unit,
    onUpdateInvoice: (String, String, String, Double, LocalDate, (String?) -> Unit) -> Unit,
    onRecordPayment: (String, Double, LocalDate, PaymentMethod, (String?) -> Unit) -> Unit,
    onDeleteInvoice: (String) -> Unit,
    onDeleteInvoices: (Collection<String>) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(InvoiceFilter.ALL) }
    var invoiceToEdit by remember { mutableStateOf<Invoice?>(null) }
    var invoiceToDelete by remember { mutableStateOf<Invoice?>(null) }
    var invoiceForPartialPayment by remember { mutableStateOf<Invoice?>(null) }
    var invoiceToMarkPaid by remember { mutableStateOf<Invoice?>(null) }
    var paymentError by remember { mutableStateOf<String?>(null) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    var selectedInvoiceIds by remember { mutableStateOf(setOf<String>()) }
    var editError by remember { mutableStateOf<String?>(null) }
    val isAdmin = userRole == UserRole.ADMIN
    val canView = hasPermission(userRole, permissions, UserPermission.VIEW_INVOICES)
    val canAddPayment = hasPermission(userRole, permissions, UserPermission.ADD_PAYMENTS)

    if (!canView) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Accès refusé", color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
        }
        return
    }

    LaunchedEffect(importFeedback) {
        if (importFeedback != null) {
            delay(4000)
            onClearImportFeedback()
        }
    }
    
    val filteredInvoices = remember(invoices, searchQuery, selectedFilter, selectedMonth) {
        invoices.filter { invoice ->
            val matchesMonth = YearMonth.from(invoice.dueDate) == selectedMonth

            val matchesSearch = searchQuery.isEmpty() ||
                invoice.clientName.contains(searchQuery, ignoreCase = true) ||
                invoice.invoiceNumber.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                InvoiceFilter.ALL -> true
                InvoiceFilter.DUE -> invoice.status == InvoiceStatus.DUE
                InvoiceFilter.PARTIAL -> invoice.status == InvoiceStatus.PARTIAL
                InvoiceFilter.PAID -> invoice.status == InvoiceStatus.PAID
            }

            matchesMonth && matchesSearch && matchesFilter
        }
    }

    LaunchedEffect(selectedMonth, selectedFilter, searchQuery) {
        selectedInvoiceIds = emptySet()
    }
    
    invoiceToEdit?.let { invoice ->
        InvoiceFormDialog(
            title = "Modifier encaissement",
            initialInvoice = invoice,
            onDismiss = {
                invoiceToEdit = null
                editError = null
            },
            onConfirm = { invoiceNumber, clientName, totalAmount, dueDate, _ ->
                onUpdateInvoice(
                    invoice.id,
                    invoiceNumber,
                    clientName,
                    totalAmount,
                    dueDate
                ) { error ->
                    if (error == null) {
                        invoiceToEdit = null
                        editError = null
                    } else {
                        editError = error
                    }
                }
            },
            errorMessage = editError
        )
    }

    invoiceToMarkPaid?.let { invoice ->
        AlertDialog(
            onDismissRequest = { invoiceToMarkPaid = null },
            title = { Text("Marquer comme soldé") },
            text = {
                Text(
                    "Encaisser ${formatMoney(invoice.remainingAmount)} pour ${invoice.clientName} ?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRecordPayment(
                            invoice.id,
                            invoice.remainingAmount,
                            LocalDate.now(),
                            PaymentMethod.CASH
                        ) { error ->
                            if (error == null) invoiceToMarkPaid = null
                        }
                    }
                ) {
                    Text("Soldé", color = Color(0xFF4CAF50))
                }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToMarkPaid = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    invoiceForPartialPayment?.let { invoice ->
        PartialPaymentDialog(
            invoice = invoice,
            errorMessage = paymentError,
            onDismiss = {
                invoiceForPartialPayment = null
                paymentError = null
            },
            onConfirm = { amount, date, method ->
                onRecordPayment(invoice.id, amount, date, method) { error ->
                    if (error == null) {
                        invoiceForPartialPayment = null
                        paymentError = null
                    } else {
                        paymentError = error
                    }
                }
            }
        )
    }

    invoiceToDelete?.let { invoice ->
        AlertDialog(
            onDismissRequest = { invoiceToDelete = null },
            title = { Text("Supprimer l'encaissement ?") },
            text = {
                Text(
                    "Supprimer ${invoice.clientName} (${invoice.invoiceNumber}) ?" +
                        if (invoice.payments.isNotEmpty()) {
                            " Les ${invoice.payments.size} paiement(s) associé(s) seront aussi supprimés."
                        } else {
                            ""
                        }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteInvoice(invoice.id)
                        invoiceToDelete = null
                    }
                ) {
                    Text("Supprimer", color = Color(0xFFF44336))
                }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    if (showBulkDeleteConfirm) {
        val selectedInvoices = filteredInvoices.filter { it.id in selectedInvoiceIds }
        val paymentsCount = selectedInvoices.sumOf { it.payments.size }
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text("Supprimer la sélection ?") },
            text = {
                Text(
                    "Supprimer ${selectedInvoices.size} encaissement(s) ?" +
                        if (paymentsCount > 0) {
                            " $paymentsCount paiement(s) associé(s) seront aussi supprimés."
                        } else {
                            ""
                        }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteInvoices(selectedInvoiceIds)
                        selectedInvoiceIds = emptySet()
                        showBulkDeleteConfirm = false
                    }
                ) {
                    Text("Supprimer", color = Color(0xFFF44336))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = onNavigateToAddInvoice,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nouvel encaissement")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            MonthSelectorRow(
                selectedMonth = selectedMonth,
                onMonthChange = onMonthChange
            )

            Text(
                text = "Affichage par date d'échéance",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showSearch = !showSearch }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Rechercher",
                        tint = if (showSearch || searchQuery.isNotEmpty()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color(0xFF64748B)
                        }
                    )
                }
                if (isAdmin) {
                    IconButton(onClick = onNavigateToImport) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Importer",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (showSearch) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Client ou n° facture", fontSize = 14.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Effacer")
                            }
                        }
                    }
                )
            }

            importFeedback?.let { message ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message,
                    fontSize = 12.sp,
                    color = Color(0xFF4CAF50)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(InvoiceFilter.values()) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isAdmin && filteredInvoices.isNotEmpty()) {
                AdminBulkSelectionBar(
                    totalCount = filteredInvoices.size,
                    selectedCount = selectedInvoiceIds.size,
                    onToggleSelectAll = {
                        selectedInvoiceIds = if (selectedInvoiceIds.size == filteredInvoices.size) {
                            emptySet()
                        } else {
                            filteredInvoices.map { it.id }.toSet()
                        }
                    },
                    onDeleteSelected = { showBulkDeleteConfirm = true }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (filteredInvoices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (invoices.any { YearMonth.from(it.dueDate) == selectedMonth }) {
                            "Aucun encaissement ne correspond aux filtres"
                        } else {
                            "Aucun encaissement avec échéance en ${selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH))}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredInvoices, key = { it.id }) { invoice ->
                        InvoiceCard(
                            invoice = invoice,
                            isAdmin = isAdmin,
                            canAddPayment = canAddPayment,
                            showSelection = isAdmin,
                            isSelected = invoice.id in selectedInvoiceIds,
                            onSelectionChange = { selected ->
                                selectedInvoiceIds = if (selected) {
                                    selectedInvoiceIds + invoice.id
                                } else {
                                    selectedInvoiceIds - invoice.id
                                }
                            },
                            onMarkPaid = { invoiceToMarkPaid = invoice },
                            onPartialPayment = {
                                paymentError = null
                                invoiceForPartialPayment = invoice
                            },
                            onEdit = { invoiceToEdit = invoice },
                            onDelete = { invoiceToDelete = invoice }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun InvoiceFormDialog(
    title: String,
    initialInvoice: Invoice? = null,
    selectedMonth: YearMonth? = null,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, LocalDate, Boolean) -> Unit
) {
    val isNew = initialInvoice == null
    var markAsCollected by remember { mutableStateOf(false) }
    val defaultDueDate = remember(initialInvoice, selectedMonth) {
        initialInvoice?.dueDate
            ?: selectedMonth?.let { defaultDateForMonth(it) }
            ?: LocalDate.now().plusDays(30)
    }
    var invoiceNumber by remember(initialInvoice) {
        mutableStateOf(initialInvoice?.invoiceNumber.orEmpty())
    }
    var clientName by remember(initialInvoice) {
        mutableStateOf(initialInvoice?.clientName.orEmpty())
    }
    var totalAmount by remember(initialInvoice) {
        mutableStateOf(initialInvoice?.totalAmount?.toString().orEmpty())
    }
    var dueDate by remember(initialInvoice, selectedMonth) {
        mutableStateOf(defaultDueDate)
    }
    var localError by remember(initialInvoice, selectedMonth) { mutableStateOf<String?>(null) }
    val displayError = errorMessage ?: localError
    val parsedAmount = totalAmount.replace(",", ".").toDoubleOrNull()
    val minAmount = initialInvoice?.paidAmount ?: 0.0
    val amountTooLow = parsedAmount != null && parsedAmount < minAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = { CurrencySuffix() },
                    isError = amountTooLow,
                    supportingText = if (amountTooLow) {
                        { Text("Minimum : $minAmount") }
                    } else if (initialInvoice != null && initialInvoice.paidAmount > 0) {
                        { Text("Déjà encaissé : ${initialInvoice.paidAmount}") }
                    } else {
                        null
                    }
                )
                TreasuryDateField(
                    label = "Date d'échéance",
                    date = dueDate,
                    onDateChange = { dueDate = it }
                )
                displayError?.let { message ->
                    Text(
                        text = message,
                        fontSize = 12.sp,
                        color = Color(0xFFF44336)
                    )
                }
                if (isNew) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = markAsCollected,
                            onCheckedChange = { markAsCollected = it }
                        )
                        Text(
                            text = "Encaissé intégralement (soldé)",
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = parsedAmount ?: return@Button
                    onConfirm(invoiceNumber, clientName, amount, dueDate, markAsCollected)
                },
                enabled = invoiceNumber.isNotBlank() &&
                    clientName.isNotBlank() &&
                    parsedAmount != null &&
                    !amountTooLow
            ) {
                Text(if (initialInvoice == null) "Ajouter" else "Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun InvoiceCard(
    invoice: Invoice,
    isAdmin: Boolean = false,
    canAddPayment: Boolean = true,
    showSelection: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChange: (Boolean) -> Unit = {},
    onMarkPaid: () -> Unit = {},
    onPartialPayment: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val formatAmount = rememberFormatMoney()

    val isPaid = invoice.status == InvoiceStatus.PAID
    val statusColor = when (invoice.status) {
        InvoiceStatus.PAID -> Color(0xFF4CAF50)
        InvoiceStatus.PARTIAL -> Color(0xFFFF9800)
        InvoiceStatus.DUE -> Color(0xFFF44336)
    }

    val statusLabel = when (invoice.status) {
        InvoiceStatus.PAID -> "Soldé"
        InvoiceStatus.PARTIAL -> "Partiel"
        InvoiceStatus.DUE -> "Dû"
    }

    var showMenu by remember { mutableStateOf(false) }
    val canUseMenu = canAddPayment && !isPaid || isAdmin

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPaid) 0.dp else 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaid) Color(0xFFE8F5E9) else Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (showSelection) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = onSelectionChange
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = invoice.clientName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPaid) Color(0xFF2E7D32) else Color(0xFF1A1A1A),
                        maxLines = 1
                    )
                    Text(
                        text = "${invoice.invoiceNumber} · éch. ${invoice.dueDate.format(DateTimeFormatter.ofPattern("dd/MM/yy"))}",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total ${formatAmount(invoice.totalAmount)}",
                        fontSize = 12.sp,
                        color = Color(0xFF424242)
                    )
                    if (!isPaid) {
                        Text(
                            text = "Encaissé ${formatAmount(invoice.paidAmount)} · Reste ${formatAmount(invoice.remainingAmount)}",
                            fontSize = 11.sp,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = statusLabel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                    if (canUseMenu) {
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Actions",
                                    tint = Color(0xFF64748B)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                if (canAddPayment && !isPaid) {
                                    DropdownMenuItem(
                                        text = { Text("Soldé") },
                                        onClick = {
                                            showMenu = false
                                            onMarkPaid()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Paiement partiel") },
                                        onClick = {
                                            showMenu = false
                                            onPartialPayment()
                                        }
                                    )
                                }
                                if (isAdmin) {
                                    DropdownMenuItem(
                                        text = { Text("Modifier") },
                                        onClick = {
                                            showMenu = false
                                            onEdit()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Supprimer", color = Color(0xFFF44336)) },
                                        onClick = {
                                            showMenu = false
                                            onDelete()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!isPaid) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { invoice.progressPercentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = statusColor,
                    trackColor = Color(0xFFE8E8E8)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PartialPaymentDialog(
    invoice: Invoice,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (Double, LocalDate, PaymentMethod) -> Unit
) {
    var amountText by remember(invoice.id) {
        mutableStateOf(invoice.remainingAmount.toString().replace('.', ','))
    }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var showMethodMenu by remember { mutableStateOf(false) }

    val parsedAmount = amountText.replace(" ", "").replace(",", ".").toDoubleOrNull()
    val formatAmount = rememberFormatMoney()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paiement partiel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${invoice.clientName} · Reste ${formatAmount(invoice.remainingAmount)}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Montant encaissé") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = { CurrencySuffix() },
                    isError = parsedAmount != null && parsedAmount > invoice.remainingAmount
                )
                TreasuryDateField(
                    label = "Date d'encaissement",
                    date = selectedDate,
                    onDateChange = { selectedDate = it }
                )
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
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMethodMenu) }
                    )
                    ExposedDropdownMenu(
                        expanded = showMethodMenu,
                        onDismissRequest = { showMethodMenu = false }
                    ) {
                        PaymentMethod.entries.forEach { method ->
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
                errorMessage?.let {
                    Text(text = it, color = Color(0xFFF44336), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = parsedAmount ?: return@TextButton
                    onConfirm(amount, selectedDate, selectedMethod)
                },
                enabled = parsedAmount != null &&
                    parsedAmount > 0 &&
                    parsedAmount <= invoice.remainingAmount
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
