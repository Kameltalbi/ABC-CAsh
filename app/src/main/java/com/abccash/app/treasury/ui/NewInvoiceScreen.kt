package com.abccash.app.treasury.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import com.abccash.app.treasury.data.*
import com.abccash.app.treasury.data.defaultDateForMonth
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewInvoiceScreen(
    selectedMonth: YearMonth,
    entrepriseId: String,
    customIncomeCategories: List<String> = emptyList(),
    clientContacts: List<Contact> = emptyList(),
    products: List<Product> = emptyList(),
    invoiceSettings: InvoiceSettings = InvoiceSettings(),
    initialInvoice: Invoice? = null,
    onBack: () -> Unit,
    onCreateClient: (Contact, (String?) -> Unit) -> Unit,
    onCreateProduct: (Product, (String?) -> Unit) -> Unit,
    onSaveDraft: (
        clientName: String,
        clientContactId: String?,
        lineItems: List<InvoiceLineItem>,
        dueDate: LocalDate,
        category: RevenueCategory,
        categoryLabel: String,
        markAsCollected: Boolean,
        paymentMethod: PaymentMethod,
        onResult: (String?) -> Unit
    ) -> Unit,
    onValidate: (
        clientName: String,
        clientContactId: String?,
        lineItems: List<InvoiceLineItem>,
        dueDate: LocalDate,
        category: RevenueCategory,
        categoryLabel: String,
        markAsCollected: Boolean,
        paymentMethod: PaymentMethod,
        onResult: (String?) -> Unit
    ) -> Unit,
    onUpdate: (
        invoiceId: String,
        clientName: String,
        clientContactId: String?,
        lineItems: List<InvoiceLineItem>,
        dueDate: LocalDate,
        category: RevenueCategory,
        categoryLabel: String,
        validate: Boolean,
        onResult: (String?) -> Unit
    ) -> Unit = { _, _, _, _, _, _, _, _, _ -> }
) {
    val defaultDate = remember(selectedMonth) { defaultDateForMonth(selectedMonth) }
    val formatAmount = rememberFormatMoney()
    val clients = remember(clientContacts) { clientContacts.filter { it.type == ContactType.CLIENT } }
    val isEditMode = initialInvoice != null
    val isDraftEdit = initialInvoice?.isDraft == true

    var selectedContactId by remember(initialInvoice, clients) {
        mutableStateOf(
            initialInvoice?.clientContactId
                ?: clients.find { it.name.equals(initialInvoice?.clientName, ignoreCase = true) }?.id
        )
    }
    var lineItems by remember(initialInvoice) {
        mutableStateOf(
            initialInvoice?.lineItems
                ?.map { it.toLineItemDraft() }
                ?.takeIf { it.isNotEmpty() }
                ?: listOf(LineItemDraft())
        )
    }
    var dueDate by remember(initialInvoice, selectedMonth) {
        mutableStateOf(initialInvoice?.dueDate ?: defaultDate)
    }
    var showClientMenu by remember { mutableStateOf(false) }
    var showAddClient by remember { mutableStateOf(false) }
    var addProductLineIndex by remember { mutableStateOf<Int?>(null) }
    var markAsCollected by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.TRANSFER) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val selectedClient = clients.find { it.id == selectedContactId }
    val parsedLines = remember(lineItems) { lineItems.mapNotNull { it.toLineItemOrNull() } }
    val amountExclTax = InvoiceLineItemCodec.totalExclTax(parsedLines)

    val taxBreakdown = remember(amountExclTax, invoiceSettings) {
        if (amountExclTax > 0) {
            InvoiceTaxCalculations.fromAmountExclTax(amountExclTax, invoiceSettings)
        } else {
            null
        }
    }

    val clientRequiredError = stringResource(R.string.invoice_client_required)
    val linesRequiredError = stringResource(R.string.invoice_lines_required)
    val dueDateLabel = stringResource(R.string.date)

    ContactFormSheet(
        visible = showAddClient,
        initialContact = null,
        defaultType = ContactType.CLIENT,
        entrepriseId = entrepriseId,
        onDismiss = { showAddClient = false },
        onSave = { contact ->
            onCreateClient(contact) { error ->
                if (error == null) {
                    selectedContactId = contact.id
                    showAddClient = false
                }
            }
        }
    )

    ProductFormSheet(
        visible = addProductLineIndex != null,
        initialProduct = null,
        entrepriseId = entrepriseId,
        customIncomeCategories = customIncomeCategories,
        onDismiss = { addProductLineIndex = null },
        onSave = { product ->
            onCreateProduct(product) { error ->
                if (error == null) {
                    addProductLineIndex?.let { index ->
                        lineItems = lineItems.toMutableList().also {
                            it[index] = lineItems[index].copy(
                                selectedProductId = product.id,
                                description = product.name,
                                unitPrice = product.unitPriceExclTax.toString()
                            )
                        }
                    }
                    addProductLineIndex = null
                }
            }
        }
    )

    fun submit(validate: Boolean) {
        val client = selectedClient
        when {
            client == null -> saveError = clientRequiredError
            parsedLines.isEmpty() -> saveError = linesRequiredError
            else -> {
                saveError = null
                isSaving = true
                val (category, categoryLabel) = DocumentCategoryResolver.resolve(
                    lineDrafts = lineItems,
                    products = products,
                    fallbackCategory = initialInvoice?.category ?: RevenueCategory.OTHER,
                    fallbackLabel = initialInvoice?.categoryLabel.orEmpty()
                )
                val callback = { error: String? ->
                    isSaving = false
                    if (error == null) onBack() else saveError = error
                }
                if (isEditMode && initialInvoice != null) {
                    onUpdate(
                        initialInvoice.id,
                        client.name,
                        client.id,
                        parsedLines,
                        dueDate,
                        category,
                        categoryLabel,
                        validate,
                        callback
                    )
                    return@submit
                }
                val common: (
                    String,
                    String?,
                    List<InvoiceLineItem>,
                    LocalDate,
                    RevenueCategory,
                    String,
                    Boolean,
                    PaymentMethod,
                    (String?) -> Unit
                ) -> Unit = { name, contactId, lines, date, category, label, collected, method, onResult ->
                    if (validate) {
                        onValidate(name, contactId, lines, date, category, label, collected, method, onResult)
                    } else {
                        onSaveDraft(name, contactId, lines, date, category, label, collected, method, onResult)
                    }
                }
                common(
                    client.name,
                    client.id,
                    parsedLines,
                    dueDate,
                    category,
                    categoryLabel,
                    markAsCollected,
                    paymentMethod,
                    callback
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (isEditMode) R.string.invoice_edit_title else R.string.invoice_create_title
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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

            Text(
                stringResource(R.string.invoice_client_section),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = showClientMenu,
                    onExpandedChange = { showClientMenu = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedClient?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.invoice_select_client)) },
                        placeholder = { Text(stringResource(R.string.invoice_select_client_hint)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showClientMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showClientMenu,
                        onDismissRequest = { showClientMenu = false }
                    ) {
                        if (clients.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.contacts_clients_empty)) },
                                onClick = {},
                                enabled = false
                            )
                        } else {
                            clients.forEach { contact ->
                                DropdownMenuItem(
                                    text = { Text(contact.name) },
                                    onClick = {
                                        selectedContactId = contact.id
                                        showClientMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                FilledIconButton(
                    onClick = { showAddClient = true },
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.contact_add))
                }
            }

            Text(
                stringResource(R.string.invoice_lines_section),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            lineItems.forEachIndexed { index, draft ->
                InvoiceLineItemCard(
                    draft = draft,
                    products = products,
                    formatAmount = formatAmount,
                    canDelete = lineItems.size > 1,
                    onDescriptionChange = { value ->
                        lineItems = lineItems.toMutableList().also {
                            it[index] = draft.copy(description = value, selectedProductId = null)
                        }
                    },
                    onQuantityChange = { value ->
                        lineItems = lineItems.toMutableList().also { it[index] = draft.copy(quantity = value) }
                    },
                    onUnitPriceChange = { value ->
                        lineItems = lineItems.toMutableList().also { it[index] = draft.copy(unitPrice = value) }
                    },
                    onProductSelected = { product ->
                        if (product == null) return@InvoiceLineItemCard
                        lineItems = lineItems.toMutableList().also {
                            it[index] = draft.copy(
                                selectedProductId = product.id,
                                description = product.name,
                                unitPrice = product.unitPriceExclTax.toString()
                            )
                        }
                    },
                    onAddProduct = { addProductLineIndex = index },
                    onDelete = {
                        lineItems = lineItems.filterIndexed { i, _ -> i != index }
                    }
                )
            }
            OutlinedButton(
                onClick = { lineItems = lineItems + LineItemDraft() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.invoice_add_line))
            }

            taxBreakdown?.let { tax ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        InvoiceTaxLine(stringResource(R.string.invoice_amount_ht), formatAmount(tax.amountExclTax))
                        if (tax.tvaRate > 0) {
                            InvoiceTaxLine(
                                stringResource(R.string.invoice_tva_line, tax.tvaRate),
                                formatAmount(tax.tvaAmount)
                            )
                        }
                        if (tax.hasOtherTax) {
                            InvoiceTaxLine(
                                otherTaxLineLabel(tax),
                                formatAmount(tax.otherTaxAmount)
                            )
                        }
                        HorizontalDivider()
                        InvoiceTaxLine(
                            stringResource(R.string.invoice_amount_ttc),
                            formatAmount(tax.totalInclTax),
                            bold = true
                        )
                    }
                }
            }

            TreasuryDateField(label = dueDateLabel, date = dueDate, onDateChange = { dueDate = it })

            if (!isEditMode) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = markAsCollected, onCheckedChange = { markAsCollected = it })
                    Column {
                        Text(stringResource(R.string.fully_collected), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.mark_settled_on_save), fontSize = 12.sp, color = Color.Gray)
                    }
                }

                if (markAsCollected) {
                    TreasuryPaymentMethodField(
                        selectedMethod = paymentMethod,
                        onMethodChange = { paymentMethod = it }
                    )
                }
            }

            saveError?.let { error ->
                Text(text = error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            if (isEditMode && !isDraftEdit) {
                Button(
                    onClick = { submit(validate = false) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.save))
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { submit(validate = false) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.invoice_save_draft))
                    }
                }

                Button(
                    onClick = { submit(validate = true) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.invoice_validate))
                }
            }
        }
    }
}
