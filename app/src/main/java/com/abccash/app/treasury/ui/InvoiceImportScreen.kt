package com.abccash.app.treasury.ui

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.importer.InvoiceImportParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceImportScreen(
    onBack: () -> Unit,
    onImportInvoices: (List<Invoice>) -> Unit
) {
    val context = LocalContext.current
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else "import.csv"
            } ?: "import.csv"
            val imported = context.contentResolver.openInputStream(uri)?.use { input ->
                InvoiceImportParser.parse(fileName, input)
            }.orEmpty()
            if (imported.isEmpty()) {
                isError = true
                statusMessage = "Aucune facture importée. Vérifie les colonnes du fichier."
            } else {
                onImportInvoices(imported)
            }
        }.onFailure {
            isError = true
            statusMessage = "Import impossible : ${it.message ?: "fichier invalide"}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Importer des encaissements") },
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
                        text = "Structure du fichier",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    Text(
                        text = "Colonnes obligatoires : invoiceNumber, clientName, totalAmount, dueDate",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Exemple : FAC-2026-001 | Client ABC | 1500.000 | 30/06/2026",
                        fontSize = 13.sp,
                        color = Color(0xFF2563EB)
                    )
                    Text(
                        text = "CSV (virgule, point-virgule ou tabulation) ou Excel .xlsx",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Button(
                onClick = {
                    importLauncher.launch(
                        arrayOf(
                            "text/*",
                            "text/csv",
                            "application/csv",
                            "application/vnd.ms-excel",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Choisir le fichier")
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
                Text("Annuler")
            }
        }
    }
}
