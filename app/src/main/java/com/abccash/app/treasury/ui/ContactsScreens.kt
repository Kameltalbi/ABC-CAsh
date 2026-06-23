package com.abccash.app.treasury.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.abccash.app.locale.AppLocale
import com.abccash.app.treasury.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsListScreen(
    clientSummaries: List<ContactSummary>,
    supplierSummaries: List<ContactSummary>,
    onBack: () -> Unit,
    onAddContact: (ContactType) -> Unit,
    onOpenContact: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val summaries = if (selectedTab == 0) clientSummaries else supplierSummaries
    val contactType = if (selectedTab == 0) ContactType.CLIENT else ContactType.SUPPLIER

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.contacts_title)) },
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
        },
        floatingActionButton = {
            AbcCashFab(
                onClick = { onAddContact(contactType) },
                contentDescription = stringResource(R.string.contact_add)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.clients_tab)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.suppliers_tab)) }
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (summaries.isEmpty()) {
                    item {
                        Text(
                            stringResource(
                                if (contactType == ContactType.CLIENT) {
                                    R.string.contacts_clients_empty
                                } else {
                                    R.string.contacts_suppliers_empty
                                }
                            ),
                            color = Color(0xFF64748B),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else {
                    items(summaries, key = { it.contact.id }) { summary ->
                        ContactListCard(summary = summary, onClick = { onOpenContact(summary.contact.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactListCard(summary: ContactSummary, onClick: () -> Unit) {
    val formatAmount = rememberFormatMoney()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(summary.contact.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (summary.contact.email.isNotBlank()) {
                    Text(summary.contact.email, fontSize = 12.sp, color = Color(0xFF64748B))
                }
                if (summary.contact.phone.isNotBlank()) {
                    Text(summary.contact.phone, fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatAmount(summary.totalAmount), fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.contact_transactions_count, summary.transactionCount),
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    contact: Contact,
    summary: ContactSummary,
    invoices: List<Invoice>,
    expenses: List<Expense>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val formatAmount = rememberFormatMoney()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.contact_delete_title)) },
            text = { Text(stringResource(R.string.contact_delete_message, contact.name)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text(stringResource(R.string.delete), color = Color(0xFFDC2626))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contact.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF))) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(
                                if (contact.type == ContactType.CLIENT) R.string.client else R.string.supplier
                            ),
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                        if (contact.type == ContactType.CLIENT && contact.countryCode.isNotBlank()) {
                            val french = AppLocale.current().language == java.util.Locale.FRENCH.language
                            ContactInfoRow(
                                Icons.Default.Public,
                                ClientCountryProfiles.countryLabel(contact.countryCode, french)
                            )
                        }
                        if (contact.type == ContactType.CLIENT && contact.legalName.isNotBlank() && contact.legalName != contact.name) {
                            ContactInfoRow(Icons.Default.Business, contact.legalName)
                        }
                        if (contact.type == ContactType.CLIENT && contact.taxIdType != null && contact.taxIdType != TaxIdType.NONE && contact.taxIdValue.isNotBlank()) {
                            val french = AppLocale.current().language == java.util.Locale.FRENCH.language
                            val taxLabel = ClientCountryProfiles
                                .taxIdOptionsForCountry(contact.countryCode.ifBlank { "OTHER" })
                                .find { it.type == contact.taxIdType }
                                ?.let { if (french) it.labelFr else it.labelEn }
                                ?: contact.taxIdType.name
                            ContactInfoRow(Icons.Default.Receipt, "$taxLabel : ${contact.taxIdValue}")
                        }
                        if (contact.email.isNotBlank()) {
                            ContactInfoRow(Icons.Default.Email, contact.email)
                        }
                        if (contact.phone.isNotBlank()) {
                            ContactInfoRow(Icons.Default.Phone, contact.phone)
                        }
                        val billingAddress = if (contact.type == ContactType.CLIENT) {
                            contact.billingAddressFormatted
                        } else {
                            contact.address
                        }
                        if (billingAddress.isNotBlank()) {
                            ContactInfoRow(Icons.Default.LocationOn, billingAddress)
                        }
                        if (contact.notes.isNotBlank()) {
                            Text(contact.notes, fontSize = 13.sp, color = Color(0xFF475569))
                        }
                        HorizontalDivider()
                        Text(
                            stringResource(R.string.contact_total_volume, formatAmount(summary.totalAmount)),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            item {
                Text(
                    stringResource(
                        if (contact.type == ContactType.CLIENT) {
                            R.string.contact_invoice_history
                        } else {
                            R.string.contact_expense_history
                        }
                    ),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
            if (contact.type == ContactType.CLIENT) {
                if (invoices.isEmpty()) {
                    item { Text(stringResource(R.string.contact_no_invoices), color = Color(0xFF94A3B8), fontSize = 13.sp) }
                } else {
                    items(invoices, key = { it.id }) { invoice ->
                        ContactInvoiceRow(invoice, formatAmount)
                    }
                }
            } else {
                if (expenses.isEmpty()) {
                    item { Text(stringResource(R.string.contact_no_expenses), color = Color(0xFF94A3B8), fontSize = 13.sp) }
                } else {
                    items(expenses, key = { it.id }) { expense ->
                        ContactExpenseRow(expense, formatAmount)
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
        Text(text, fontSize = 13.sp)
    }
}

@Composable
private fun ContactInvoiceRow(invoice: Invoice, formatAmount: (Double) -> String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(invoice.invoiceNumber, fontWeight = FontWeight.Medium)
                Text(AppLocale.dayMonth(invoice.dueDate), fontSize = 12.sp, color = Color(0xFF64748B))
            }
            Text(formatAmount(invoice.totalAmount), fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
        }
    }
}

@Composable
private fun ContactExpenseRow(expense: Expense, formatAmount: (Double) -> String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(expense.label, fontWeight = FontWeight.Medium)
                Text(AppLocale.dayMonth(expense.date), fontSize = 12.sp, color = Color(0xFF64748B))
            }
            Text(formatAmount(expense.amount), fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactFormSheet(
    visible: Boolean,
    initialContact: Contact?,
    defaultType: ContactType,
    entrepriseId: String,
    onDismiss: () -> Unit,
    onSave: (Contact) -> Unit
) {
    if (!visible) return

    val type = initialContact?.type ?: defaultType

    ModalBottomSheet(onDismissRequest = onDismiss) {
        if (type == ContactType.CLIENT) {
            ClientContactFormContent(
                initialContact = initialContact,
                entrepriseId = entrepriseId,
                onDismiss = onDismiss,
                onSave = onSave
            )
        } else {
            SupplierContactFormContent(
                initialContact = initialContact,
                entrepriseId = entrepriseId,
                onSave = onSave
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplierContactFormContent(
    initialContact: Contact?,
    entrepriseId: String,
    onSave: (Contact) -> Unit
) {
    var name by remember(initialContact) { mutableStateOf(initialContact?.name.orEmpty()) }
    var email by remember(initialContact) { mutableStateOf(initialContact?.email.orEmpty()) }
    var phone by remember(initialContact) { mutableStateOf(initialContact?.phone.orEmpty()) }
    var address by remember(initialContact) { mutableStateOf(initialContact?.address.orEmpty()) }
    var notes by remember(initialContact) { mutableStateOf(initialContact?.notes.orEmpty()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(
                if (initialContact == null) R.string.contact_add else R.string.contact_edit
            ),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.supplier)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.email)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text(stringResource(R.string.phone)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text(stringResource(R.string.address)) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(R.string.note_optional)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Button(
            onClick = {
                onSave(
                    (initialContact ?: Contact(entrepriseId = entrepriseId, type = ContactType.SUPPLIER, name = "")).copy(
                        name = name.trim(),
                        email = email.trim(),
                        phone = phone.trim(),
                        address = address.trim(),
                        notes = notes.trim(),
                        type = ContactType.SUPPLIER,
                        entrepriseId = entrepriseId
                    )
                )
            },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.save))
        }
    }
}
