package com.abccash.app.treasury.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import com.abccash.app.treasury.data.Contact
import com.abccash.app.treasury.data.DocumentPdfTemplate
import com.abccash.app.treasury.data.Entreprise
import com.abccash.app.treasury.data.LocalAppCurrency
import com.abccash.app.treasury.data.Quote
import com.abccash.app.treasury.data.QuoteStatus
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import com.abccash.app.treasury.data.hasPermission
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter

enum class QuoteFilter(@androidx.annotation.StringRes val labelRes: Int) {
    ALL(R.string.filter_all),
    DRAFT(R.string.quote_status_draft),
    SENT(R.string.quote_status_sent),
    ACCEPTED(R.string.quote_status_accepted),
    REFUSED(R.string.quote_status_refused),
    CONVERTED(R.string.quote_status_converted)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotesListScreen(
    userRole: UserRole,
    permissions: Set<UserPermission>,
    quotes: List<Quote>,
    selectedMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onBack: (() -> Unit)? = null,
    onNavigateToAddQuote: () -> Unit,
    onOpenQuote: (String) -> Unit,
    onDeleteQuote: (String) -> Unit = {},
    entreprise: Entreprise? = null,
    contacts: List<Contact> = emptyList(),
    pdfTemplate: DocumentPdfTemplate = DocumentPdfTemplate.CLASSIC_BLUE
) {
    var selectedFilter by remember { mutableStateOf(QuoteFilter.ALL) }
    var quoteToDelete by remember { mutableStateOf<Quote?>(null) }
    val canView = hasPermission(userRole, permissions, UserPermission.VIEW_INVOICES)
    val isAdmin = userRole == UserRole.ADMIN
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val formatAmount = rememberFormatMoney()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pdfDownloadSuccess = stringResource(R.string.pdf_download_success)
    val pdfDownloadFailed = stringResource(R.string.pdf_download_failed)
    val whatsappMissing = stringResource(R.string.whatsapp_not_installed)

    if (!canView) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.access_denied), color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
        }
        return
    }

    quoteToDelete?.let { quote ->
        AlertDialog(
            onDismissRequest = { quoteToDelete = null },
            title = { Text(stringResource(R.string.delete_quote_question)) },
            text = { Text(stringResource(R.string.delete_quote_confirm, quote.clientName)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteQuote(quote.id)
                    quoteToDelete = null
                }) {
                    Text(stringResource(R.string.delete), color = Color(0xFFF44336))
                }
            },
            dismissButton = {
                TextButton(onClick = { quoteToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    val monthQuotes = remember(quotes, selectedMonth) {
        quotes.filter { YearMonth.from(it.issueDate) == selectedMonth }
    }
    val filtered = remember(monthQuotes, selectedFilter) {
        when (selectedFilter) {
            QuoteFilter.ALL -> monthQuotes
            QuoteFilter.DRAFT -> monthQuotes.filter { it.status == QuoteStatus.DRAFT }
            QuoteFilter.SENT -> monthQuotes.filter { it.status == QuoteStatus.SENT }
            QuoteFilter.ACCEPTED -> monthQuotes.filter { it.status == QuoteStatus.ACCEPTED }
            QuoteFilter.REFUSED -> monthQuotes.filter { it.status == QuoteStatus.REFUSED }
            QuoteFilter.CONVERTED -> monthQuotes.filter { it.status == QuoteStatus.CONVERTED }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.quotes_title)) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
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
            if (isAdmin) {
                AbcCashFab(
                    onClick = onNavigateToAddQuote,
                    contentDescription = stringResource(R.string.quote_create_title)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MonthSelectorRow(selectedMonth = selectedMonth, onMonthChange = onMonthChange)

            LazyRow(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(QuoteFilter.entries) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(stringResource(filter.labelRes), fontSize = 12.sp) },
                        colors = abcFilterChipColors()
                    )
                }
            }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.no_quote_match_filters),
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { quote ->
                        val clientEmail = contacts.find { it.id == quote.clientContactId }?.email
                        QuoteListItem(
                            quote = quote,
                            formatAmount = formatAmount,
                            dateFormatter = dateFormatter,
                            isAdmin = isAdmin,
                            entreprise = entreprise,
                            pdfTemplate = pdfTemplate,
                            clientEmail = clientEmail,
                            onClick = { onOpenQuote(quote.id) },
                            onDelete = { quoteToDelete = quote },
                            onPdfDownloadResult = { success ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (success) pdfDownloadSuccess else pdfDownloadFailed
                                    )
                                }
                            },
                            onWhatsAppMissing = {
                                scope.launch { snackbarHostState.showSnackbar(whatsappMissing) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuoteListItem(
    quote: Quote,
    formatAmount: (Double) -> String,
    dateFormatter: DateTimeFormatter,
    isAdmin: Boolean,
    entreprise: Entreprise?,
    pdfTemplate: DocumentPdfTemplate,
    clientEmail: String?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onPdfDownloadResult: (Boolean) -> Unit,
    onWhatsAppMissing: () -> Unit
) {
    val context = LocalContext.current
    val currency = LocalAppCurrency.current
    var showMenu by remember { mutableStateOf(false) }

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
    val title = if (quote.isDraft) {
        stringResource(R.string.quote_status_draft)
    } else {
        quote.quoteNumber
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Surface(color = statusColor.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        statusLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.actions),
                            tint = Color(0xFF64748B)
                        )
                    }
                    QuoteRowActionsMenu(
                        expanded = showMenu,
                        onDismiss = { showMenu = false },
                        quote = quote,
                        isAdmin = isAdmin,
                        onDownloadPdf = {
                            onPdfDownloadResult(
                                DocumentPdfUi.downloadQuote(
                                    context, quote, entreprise, currency, pdfTemplate
                                )
                            )
                        },
                        onEmailPdf = {
                            DocumentPdfUi.emailQuote(
                                context, quote, entreprise, clientEmail, currency, pdfTemplate, formatAmount
                            )
                        },
                        onWhatsAppPdf = {
                            val sent = DocumentPdfUi.whatsAppQuote(
                                context, quote, entreprise, currency, pdfTemplate, formatAmount
                            )
                            if (!sent) onWhatsAppMissing()
                        },
                        onDelete = onDelete
                    )
                }
            }
            Text(quote.clientName, fontSize = 14.sp, color = Color(0xFF334155))
            Text(
                stringResource(
                    R.string.quote_list_meta,
                    formatAmount(quote.totalAmount),
                    quote.issueDate.format(dateFormatter)
                ),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}
