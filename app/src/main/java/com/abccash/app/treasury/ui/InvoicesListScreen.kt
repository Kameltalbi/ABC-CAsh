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
import com.abccash.app.treasury.data.hasPermission
import kotlinx.coroutines.delay
import java.text.NumberFormat
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
    onInvoiceClick: (String) -> Unit,
    onNavigateToImport: () -> Unit,
    onAddInvoice: (String, String, Double, LocalDate) -> Unit,
    onUpdateInvoice: (String, String, String, Double, LocalDate) -> Boolean,
    onDeleteInvoice: (String) -> Unit,
    onDeleteInvoices: (Collection<String>) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(InvoiceFilter.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }
    var invoiceToEdit by remember { mutableStateOf<Invoice?>(null) }
    var invoiceToDelete by remember { mutableStateOf<Invoice?>(null) }
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
    
    if (showAddDialog) {
        InvoiceFormDialog(
            title = "Ajouter encaissement",
            onDismiss = { showAddDialog = false },
            onConfirm = { invoiceNumber, clientName, totalAmount, dueDate ->
                onAddInvoice(invoiceNumber, clientName, totalAmount, dueDate)
                showAddDialog = false
            }
        )
    }

    invoiceToEdit?.let { invoice ->
        InvoiceFormDialog(
            title = "Modifier encaissement",
            initialInvoice = invoice,
            onDismiss = {
                invoiceToEdit = null
                editError = null
            },
            onConfirm = { invoiceNumber, clientName, totalAmount, dueDate ->
                val success = onUpdateInvoice(
                    invoice.id,
                    invoiceNumber,
                    clientName,
                    totalAmount,
                    dueDate
                )
                if (success) {
                    invoiceToEdit = null
                    editError = null
                } else {
                    editError = "Le montant total ne peut pas être inférieur au montant déjà encaissé."
                }
            },
            errorMessage = editError
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
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter facture")
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
                        text = "Aucune facture pour ce mois",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
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
                            onClick = {
                                if (canAddPayment && invoice.status != InvoiceStatus.PAID) {
                                    onInvoiceClick(invoice.id)
                                }
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
private fun InvoiceFormDialog(
    title: String,
    initialInvoice: Invoice? = null,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, LocalDate) -> Unit
) {
    var invoiceNumber by remember(initialInvoice) {
        mutableStateOf(initialInvoice?.invoiceNumber.orEmpty())
    }
    var clientName by remember(initialInvoice) {
        mutableStateOf(initialInvoice?.clientName.orEmpty())
    }
    var totalAmount by remember(initialInvoice) {
        mutableStateOf(initialInvoice?.totalAmount?.toString().orEmpty())
    }
    var dueDate by remember(initialInvoice) {
        mutableStateOf(
            initialInvoice?.dueDate?.format(DateTimeFormatter.ISO_LOCAL_DATE)
                ?: LocalDate.now().plusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
    }
    val parsedAmount = totalAmount.replace(",", ".").toDoubleOrNull()
    val parsedDate = runCatching { LocalDate.parse(dueDate) }.getOrNull()
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
                    isError = amountTooLow,
                    supportingText = if (amountTooLow) {
                        { Text("Minimum : $minAmount") }
                    } else if (initialInvoice != null && initialInvoice.paidAmount > 0) {
                        { Text("Déjà encaissé : ${initialInvoice.paidAmount}") }
                    } else {
                        null
                    }
                )
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Date échéance yyyy-MM-dd") },
                    singleLine = true,
                    isError = dueDate.isNotBlank() && parsedDate == null
                )
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        fontSize = 12.sp,
                        color = Color(0xFFF44336)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(invoiceNumber, clientName, parsedAmount ?: 0.0, parsedDate ?: LocalDate.now()) },
                enabled = invoiceNumber.isNotBlank() &&
                    clientName.isNotBlank() &&
                    parsedAmount != null &&
                    parsedDate != null &&
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
    onClick: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("fr", "TN")).apply {
            maximumFractionDigits = 3
        }
    }

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

    val displayAmount = when (invoice.status) {
        InvoiceStatus.PAID -> invoice.totalAmount
        else -> invoice.remainingAmount
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (canAddPayment && invoice.status != InvoiceStatus.PAID) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A1A),
                        maxLines = 1
                    )
                    Text(
                        text = "${invoice.invoiceNumber} · ${invoice.dueDate.format(DateTimeFormatter.ofPattern("dd/MM/yy"))}",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = currencyFormatter.format(displayAmount),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (invoice.status) {
                                InvoiceStatus.PAID -> Color(0xFF4CAF50)
                                InvoiceStatus.PARTIAL -> Color(0xFFFF9800)
                                InvoiceStatus.DUE -> Color(0xFFF44336)
                            }
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = statusColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = statusLabel,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }

                    if (isAdmin) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Modifier",
                                modifier = Modifier.size(16.dp),
                                tint = Color.Gray
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Supprimer",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFBDBDBD)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { invoice.progressPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = statusColor,
                trackColor = Color(0xFFE8E8E8)
            )
        }
    }
}
