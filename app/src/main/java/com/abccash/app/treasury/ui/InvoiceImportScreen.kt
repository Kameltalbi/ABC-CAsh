package com.abccash.app.treasury.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.importer.InvoiceImportParser
import com.abccash.app.treasury.importer.InvoiceImportResult

private val importMimeTypes = arrayOf(
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
fun InvoiceImportScreen(
    onBack: () -> Unit,
    onImportInvoices: (List<Invoice>) -> Unit
) {
    val context = LocalContext.current
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    fun processImport(uri: Uri?) {
        if (uri == null) return
        runCatching {
            val mimeType = context.contentResolver.getType(uri)
            val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
            } ?: "import.csv"
            val result = context.contentResolver.openInputStream(uri)?.use { input ->
                InvoiceImportParser.parse(fileName, input, mimeType)
            } ?: InvoiceImportResult(emptyList(), "Impossible de lire le fichier.")
            if (result.invoices.isEmpty()) {
                isError = true
                statusMessage = result.errorMessage
                    ?: "Aucune facture importée. Vérifie les colonnes du fichier."
            } else {
                onImportInvoices(result.invoices)
            }
        }.onFailure {
            isError = true
            statusMessage = "Import impossible : ${it.message ?: "fichier invalide"}"
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        processImport(uri)
    }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_invoices_title)) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.file_structure),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    Text(
                        text = stringResource(R.string.import_columns),
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = stringResource(R.string.import_columns_en),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = stringResource(R.string.import_due_date_note),
                        fontSize = 12.sp,
                        color = Color(0xFF2563EB)
                    )
                    Text(
                        text = stringResource(R.string.import_formats),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

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
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.android_file_picker),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                        Text(
                            text = stringResource(R.string.android_file_picker_note),
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = stringResource(R.string.empty_file_list_hint),
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Button(
                onClick = { importLauncher.launch(importMimeTypes) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.choose_file))
            }

            statusMessage?.let { message ->
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = if (isError) Color(0xFFF44336) else Color(0xFF4CAF50)
                )
            }

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
