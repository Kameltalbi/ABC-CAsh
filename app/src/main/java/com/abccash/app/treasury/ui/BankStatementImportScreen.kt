package com.abccash.app.treasury.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R
import com.abccash.app.treasury.importer.BankStatementEntry
import com.abccash.app.treasury.importer.BankStatementImportParser
import com.abccash.app.treasury.importer.BankStatementImportResult
import com.abccash.app.ui.theme.AppColors
import java.time.format.DateTimeFormatter

private val statementMimeTypes = arrayOf(
    "text/csv",
    "application/csv",
    "text/comma-separated-values",
    "text/plain",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "text/*",
    "application/*",
    "*/*"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankStatementImportScreen(
    onBack: () -> Unit,
    onConfirm: (List<BankStatementEntry>, (imported: Int, skipped: Int) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val formatAmount = rememberFormatMoney()
    val datePattern = remember { DateTimeFormatter.ofPattern("dd/MM/yy") }

    var parseResult by remember { mutableStateOf<BankStatementImportResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var importDone by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    fun processFile(uri: Uri?) {
        if (uri == null) return
        errorMessage = null
        parseResult = null
        runCatching {
            val mimeType = context.contentResolver.getType(uri)
            val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
            } ?: "statement.csv"
            val result = context.contentResolver.openInputStream(uri)?.use { input ->
                BankStatementImportParser.parse(fileName, input, mimeType)
            } ?: BankStatementImportResult(errorMessage = context.getString(R.string.cannot_read_file))
            if (result.entries.isEmpty()) {
                errorMessage = result.errorMessage ?: context.getString(R.string.import_statement_none)
            } else {
                parseResult = result
            }
        }.onFailure {
            errorMessage = context.getString(R.string.import_failed_reason, it.message ?: "")
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        processFile(uri)
    }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_statement_title)) },
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.import_statement_intro),
                        fontSize = 12.sp,
                        color = Color(0xFF334155)
                    )
                }
            }

            Button(
                onClick = { picker.launch(statementMimeTypes) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.choose_file))
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = AppColors.ExpenseRed
                )
            }

            val result = parseResult
            if (result != null) {
                StatementSummaryCard(
                    result = result,
                    formatAmount = formatAmount
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(result.entries) { entry ->
                        StatementEntryRow(
                            entry = entry,
                            datePattern = datePattern,
                            formatAmount = formatAmount
                        )
                    }
                }

                Button(
                    onClick = {
                        isImporting = true
                        onConfirm(result.entries) { imported, skipped ->
                            isImporting = false
                            importDone = imported to skipped
                        }
                    },
                    enabled = !isImporting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            stringResource(R.string.import_statement_confirm, result.entries.size)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }

    importDone?.let { (imported, skipped) ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.import_statement_title)) },
            text = { Text(stringResource(R.string.import_statement_result, imported, skipped)) },
            confirmButton = {
                TextButton(onClick = {
                    importDone = null
                    onBack()
                }) { Text(stringResource(R.string.ok)) }
            }
        )
    }
}

@Composable
private fun StatementSummaryCard(
    result: BankStatementImportResult,
    formatAmount: (Double) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.import_statement_credits, result.creditCount),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.IncomeGreen
                )
                Text(
                    text = formatAmount(result.totalCredit),
                    fontSize = 13.sp,
                    color = AppColors.IncomeGreen
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.import_statement_debits, result.debitCount),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.ExpenseRed
                )
                Text(
                    text = "-${formatAmount(result.totalDebit)}",
                    fontSize = 13.sp,
                    color = AppColors.ExpenseRed
                )
            }
        }
        if (result.skippedSummaryRows > 0) {
            Text(
                text = stringResource(R.string.import_statement_skipped_summary, result.skippedSummaryRows),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 0.dp).padding(bottom = 10.dp),
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun StatementEntryRow(
    entry: BankStatementEntry,
    datePattern: DateTimeFormatter,
    formatAmount: (Double) -> String
) {
    val color = if (entry.isCredit) AppColors.IncomeGreen else AppColors.ExpenseRed
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (entry.isCredit) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = entry.date.format(datePattern),
                fontSize = 11.sp,
                color = AppColors.TextSecondary
            )
        }
        Text(
            text = if (entry.isCredit) formatAmount(entry.amount) else "-${formatAmount(entry.amount)}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1
        )
    }
    HorizontalDivider(color = AppColors.Border, thickness = 1.dp)
}
