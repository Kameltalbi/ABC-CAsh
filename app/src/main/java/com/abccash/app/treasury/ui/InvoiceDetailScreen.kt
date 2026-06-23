package com.abccash.app.treasury.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R
import com.abccash.app.locale.AppLocale
import com.abccash.app.treasury.data.*
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    invoice: Invoice,
    entreprise: Entreprise?,
    clientEmail: String?,
    pdfTemplate: DocumentPdfTemplate = DocumentPdfTemplate.CLASSIC_BLUE,
    userRole: UserRole,
    permissions: Set<UserPermission>,
    canAddPayment: Boolean,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (onResult: (String?) -> Unit) -> Unit,
    onValidate: () -> Unit,
    onMarkPaid: (java.time.LocalDate, PaymentMethod) -> Unit,
    onPartialPayment: () -> Unit
) {
    val context = LocalContext.current
    val currency = LocalAppCurrency.current
    val formatAmount = rememberFormatMoney()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMarkPaidDialog by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pdfDownloadSuccess = stringResource(R.string.pdf_download_success)
    val pdfDownloadFailed = stringResource(R.string.pdf_download_failed)
    val xmlDownloadSuccess = stringResource(R.string.xml_download_success)
    val xmlDownloadFailed = stringResource(R.string.xml_download_failed)
    val whatsappMissing = stringResource(R.string.whatsapp_not_installed)
    var showActionsMenu by remember { mutableStateOf(false) }

    val titleNumber = if (invoice.isDraft) {
        stringResource(R.string.invoice_status_draft)
    } else {
        stringResource(R.string.invoice_title_number, invoice.invoiceNumber)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_invoice_question)) },
            text = {
                Column {
                    Text(stringResource(R.string.delete_invoice_confirm, invoice.clientName))
                    deleteError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete { error ->
                        if (error == null) {
                            showDeleteDialog = false
                            onBack()
                        } else {
                            deleteError = error
                        }
                    }
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

    if (showMarkPaidDialog) {
        FullPaymentConfirmDialog(
            invoice = invoice,
            onDismiss = { showMarkPaidDialog = false },
            onConfirm = { date, method ->
                onMarkPaid(date, method)
                showMarkPaidDialog = false
            }
        )
    }

    val statusColor = when (invoice.status) {
        InvoiceStatus.PAID -> Color(0xFF16A34A)
        InvoiceStatus.PARTIAL -> Color(0xFFF59E0B)
        InvoiceStatus.DUE -> Color(0xFFDC2626)
    }
    val statusLabel = when (invoice.status) {
        InvoiceStatus.PAID -> stringResource(R.string.status_paid)
        InvoiceStatus.PARTIAL -> stringResource(R.string.status_partial)
        InvoiceStatus.DUE -> stringResource(R.string.status_due)
    }

    fun emailInvoice() {
        DocumentPdfUi.emailInvoice(
            context, invoice, entreprise, clientEmail, currency, pdfTemplate, formatAmount
        )
    }

    fun whatsAppInvoice() {
        val sent = DocumentPdfUi.whatsAppInvoice(
            context, invoice, entreprise, currency, pdfTemplate, formatAmount
        )
        if (!sent) {
            scope.launch { snackbarHostState.showSnackbar(whatsappMissing) }
        }
    }

    fun downloadPdf() {
        scope.launch {
            val saved = DocumentPdfUi.downloadInvoice(context, invoice, entreprise, currency, pdfTemplate)
            snackbarHostState.showSnackbar(if (saved) pdfDownloadSuccess else pdfDownloadFailed)
        }
    }

    fun downloadXml() {
        scope.launch {
            val saved = DocumentPdfUi.downloadInvoiceXml(context, invoice, entreprise, currency)
            snackbarHostState.showSnackbar(if (saved) xmlDownloadSuccess else xmlDownloadFailed)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(titleNumber) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showActionsMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.actions),
                                tint = Color.White
                            )
                        }
                        InvoiceRowActionsMenu(
                            expanded = showActionsMenu,
                            onDismiss = { showActionsMenu = false },
                            invoice = invoice,
                            isAdmin = isAdmin,
                            canAddPayment = canAddPayment,
                            onWhatsApp = { whatsAppInvoice() },
                            onEmail = { emailInvoice() },
                            onDownloadPdf = { downloadPdf() },
                            onEdit = onEdit,
                            onDelete = { showDeleteDialog = true },
                            onMarkPaid = { showMarkPaidDialog = true }
                        )
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
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(invoice.clientName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = {
                                    Text(
                                        if (invoice.isDraft) {
                                            stringResource(R.string.invoice_status_draft)
                                        } else {
                                            stringResource(R.string.invoice_status_validated)
                                        },
                                        color = if (invoice.isDraft) Color(0xFF64748B) else Color(0xFF2563EB)
                                    )
                                }
                            )
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = { Text(statusLabel, color = statusColor) }
                            )
                        }
                        Text(
                            stringResource(
                                R.string.invoice_due_meta,
                                if (invoice.isDraft) stringResource(R.string.invoice_status_draft) else invoice.invoiceNumber,
                                invoice.dueDate.format(dateFormatter)
                            ),
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                        if (invoice.lineItems.isNotEmpty()) {
                            HorizontalDivider()
                            Text(
                                stringResource(R.string.invoice_lines_section),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            invoice.lineItems.forEach { line ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(line.description, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        Text(
                                            "${formatQty(line.quantity)} × ${formatAmount(line.unitPriceExclTax)}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    Text(
                                        formatAmount(line.lineTotalExclTax),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                        invoice.taxBreakdown?.let { tax ->
                            HorizontalDivider()
                            InvoiceAmountRow(stringResource(R.string.invoice_amount_ht), formatAmount(tax.amountExclTax))
                            if (tax.tvaRate > 0) {
                                InvoiceAmountRow(
                                    stringResource(R.string.invoice_tva_line, tax.tvaRate),
                                    formatAmount(tax.tvaAmount)
                                )
                            }
                            if (tax.hasOtherTax) {
                                InvoiceAmountRow(
                                    otherTaxLineLabel(tax),
                                    formatAmount(tax.otherTaxAmount)
                                )
                            }
                        }
                        HorizontalDivider()
                        InvoiceAmountRow(stringResource(R.string.total_amount), formatAmount(invoice.totalAmount), bold = true)
                        InvoiceAmountRow(stringResource(R.string.invoice_collected), formatAmount(invoice.paidAmount))
                        InvoiceAmountRow(
                            stringResource(R.string.invoice_remaining),
                            formatAmount(invoice.remainingAmount),
                            color = statusColor
                        )
                        LinearProgressIndicator(
                            progress = { invoice.progressPercentage / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = statusColor
                        )
                    }
                }
            }

            if (invoice.isDraft && isAdmin) {
                item {
                    Button(onClick = onValidate, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.invoice_validate))
                    }
                }
            }
            if (canAddPayment && invoice.status != InvoiceStatus.PAID && !invoice.isDraft) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showMarkPaidDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.mark_paid))
                        }
                        OutlinedButton(onClick = onPartialPayment, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.partial_payment))
                        }
                    }
                }
            }

            if (!invoice.isDraft) {
                item {
                    InvoiceFilesSection(
                        onDownloadPdf = { downloadPdf() },
                        onDownloadXml = { downloadXml() }
                    )
                }
            }

            item {
                Text(stringResource(R.string.invoice_payments_history), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            if (invoice.payments.isEmpty()) {
                item {
                    Text(stringResource(R.string.invoice_no_payments), color = Color(0xFF94A3B8), fontSize = 13.sp)
                }
            } else {
                items(invoice.payments.sortedByDescending { it.date }, key = { it.id }) { payment ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(AppLocale.dayMonth(payment.date), fontWeight = FontWeight.Medium)
                                Text(payment.method.localizedLabel(), fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                            Text(
                                formatAmount(payment.amount),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatQty(qty: Double): String =
    if (qty == qty.toLong().toDouble()) qty.toLong().toString() else "%.2f".format(qty)

@Composable
private fun InvoiceAmountRow(
    label: String,
    value: String,
    bold: Boolean = false,
    color: Color = Color(0xFF1E293B)
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = Color(0xFF64748B))
        Text(
            value,
            fontSize = if (bold) 18.sp else 14.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = color
        )
    }
}
