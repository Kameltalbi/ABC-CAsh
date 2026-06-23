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
import com.abccash.app.treasury.data.*
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDetailScreen(
    quote: Quote,
    entreprise: Entreprise?,
    clientEmail: String? = null,
    pdfTemplate: DocumentPdfTemplate = DocumentPdfTemplate.CLASSIC_BLUE,
    userRole: UserRole,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onDelete: (onResult: (String?) -> Unit) -> Unit,
    onValidate: () -> Unit,
    onAccept: () -> Unit,
    onRefuse: () -> Unit,
    onConvert: (onResult: (String?) -> Unit) -> Unit,
    onOpenInvoice: (String) -> Unit
) {
    val context = LocalContext.current
    val currency = LocalAppCurrency.current
    val formatAmount = rememberFormatMoney()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showConvertDialog by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pdfDownloadSuccess = stringResource(R.string.pdf_download_success)
    val pdfDownloadFailed = stringResource(R.string.pdf_download_failed)
    val whatsappMissing = stringResource(R.string.whatsapp_not_installed)
    var showActionsMenu by remember { mutableStateOf(false) }

    val titleNumber = if (quote.isDraft) {
        stringResource(R.string.quote_status_draft)
    } else {
        quote.quoteNumber
    }

    val statusColor = when (quote.status) {
        QuoteStatus.DRAFT -> Color(0xFF64748B)
        QuoteStatus.SENT -> Color(0xFF2563EB)
        QuoteStatus.ACCEPTED -> Color(0xFF16A34A)
        QuoteStatus.REFUSED -> Color(0xFFDC2626)
        QuoteStatus.CONVERTED -> Color(0xFF1976D2)
    }
    val statusLabel = when (quote.status) {
        QuoteStatus.DRAFT -> stringResource(R.string.quote_status_draft)
        QuoteStatus.SENT -> stringResource(R.string.quote_status_sent)
        QuoteStatus.ACCEPTED -> stringResource(R.string.quote_status_accepted)
        QuoteStatus.REFUSED -> stringResource(R.string.quote_status_refused)
        QuoteStatus.CONVERTED -> stringResource(R.string.quote_status_converted)
    }

    fun emailQuote() {
        DocumentPdfUi.emailQuote(
            context, quote, entreprise, clientEmail, currency, pdfTemplate, formatAmount
        )
    }

    fun whatsAppQuote() {
        val sent = DocumentPdfUi.whatsAppQuote(
            context, quote, entreprise, currency, pdfTemplate, formatAmount
        )
        if (!sent) {
            scope.launch { snackbarHostState.showSnackbar(whatsappMissing) }
        }
    }

    fun downloadPdf() {
        scope.launch {
            val saved = DocumentPdfUi.downloadQuote(context, quote, entreprise, currency, pdfTemplate)
            snackbarHostState.showSnackbar(if (saved) pdfDownloadSuccess else pdfDownloadFailed)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_quote_question)) },
            text = {
                Column {
                    Text(stringResource(R.string.delete_quote_confirm, quote.clientName))
                    actionError?.let {
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
                            actionError = error
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

    if (showConvertDialog) {
        AlertDialog(
            onDismissRequest = { showConvertDialog = false },
            title = { Text(stringResource(R.string.quote_convert_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.quote_convert_confirm))
                    actionError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onConvert { error ->
                        if (error == null) {
                            showConvertDialog = false
                        } else {
                            actionError = error
                        }
                    }
                }) {
                    Text(stringResource(R.string.quote_convert_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConvertDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
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
                        QuoteRowActionsMenu(
                            expanded = showActionsMenu,
                            onDismiss = { showActionsMenu = false },
                            quote = quote,
                            isAdmin = isAdmin,
                            onDownloadPdf = { downloadPdf() },
                            onEmailPdf = { emailQuote() },
                            onWhatsAppPdf = { whatsAppQuote() },
                            onDelete = { showDeleteDialog = true }
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
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(quote.clientName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Surface(color = statusColor.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    statusLabel,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    color = statusColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        DetailRow(stringResource(R.string.quote_issue_date), quote.issueDate.format(dateFormatter))
                        DetailRow(stringResource(R.string.quote_valid_until), quote.validUntil.format(dateFormatter))
                        DetailRow(stringResource(R.string.invoice_amount_ttc), formatAmount(quote.totalAmount))
                        quote.taxBreakdown?.let { tax ->
                            DetailRow(stringResource(R.string.invoice_amount_ht), formatAmount(tax.amountExclTax))
                            if (tax.tvaRate > 0) {
                                DetailRow(
                                    stringResource(R.string.invoice_tva_line, tax.tvaRate),
                                    formatAmount(tax.tvaAmount)
                                )
                            }
                            if (tax.hasOtherTax) {
                                DetailRow(
                                    otherTaxLineLabel(tax),
                                    formatAmount(tax.otherTaxAmount)
                                )
                            }
                        }
                        if (quote.notes.isNotBlank()) {
                            DetailRow(stringResource(R.string.quote_notes), quote.notes)
                        }
                    }
                }
            }

            if (quote.lineItems.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.invoice_lines_section), fontWeight = FontWeight.SemiBold)
                }
                items(quote.lineItems) { line ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(line.description, fontWeight = FontWeight.Medium)
                            Text(
                                "${line.quantity} × ${formatAmount(line.unitPriceExclTax)} = ${formatAmount(line.lineTotalExclTax)}",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            if (isAdmin && quote.isDraft) {
                item {
                    Button(onClick = onValidate, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.quote_validate))
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                }
            }

            if (isAdmin && quote.status == QuoteStatus.SENT) {
                item {
                    Button(onClick = onAccept, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.quote_accept))
                    }
                }
                item {
                    OutlinedButton(
                        onClick = onRefuse,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                    ) {
                        Text(stringResource(R.string.quote_refuse))
                    }
                }
            }

            if (isAdmin && quote.canConvert) {
                item {
                    Button(onClick = { showConvertDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.quote_convert_action))
                    }
                }
            }

            quote.convertedInvoiceId?.let { invoiceId ->
                item {
                    OutlinedButton(
                        onClick = { onOpenInvoice(invoiceId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.quote_open_invoice))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}
