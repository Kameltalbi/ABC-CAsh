package com.abccash.app.treasury.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abccash.app.R
import com.abccash.app.treasury.data.InvoiceSettings
import com.abccash.app.treasury.data.OtherTaxMode
import com.abccash.app.treasury.ui.DocumentPdfTemplateSelector
import com.abccash.app.treasury.datastore.UserPreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsInvoiceScreen(
    entrepriseId: String,
    userPreferences: UserPreferences,
    onBack: () -> Unit
) {
    val settings by userPreferences.observeInvoiceSettings(entrepriseId)
        .collectAsStateWithLifecycle(initialValue = InvoiceSettings())

    var prefix by remember(settings) { mutableStateOf(settings.prefix) }
    var quotePrefix by remember(settings) { mutableStateOf(settings.quotePrefix) }
    var tvaRate by remember(settings) { mutableStateOf(settings.tvaRate.toString()) }
    var otherTaxRate by remember(settings) { mutableStateOf(settings.otherTaxRate.toString()) }
    var otherTaxMode by remember(settings) { mutableStateOf(settings.otherTaxMode) }
    var otherTaxLabel by remember(settings) { mutableStateOf(settings.otherTaxLabel) }
    var pdfTemplate by remember(settings) { mutableStateOf(settings.pdfTemplate) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_invoice)) },
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
                .background(Color.White)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.settings_invoice_sub),
                fontSize = 14.sp,
                color = Color(0xFF64748B)
            )

            OutlinedTextField(
                value = prefix,
                onValueChange = { prefix = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.invoice_prefix)) },
                placeholder = { Text("FAC-") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = quotePrefix,
                onValueChange = { quotePrefix = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.quote_prefix)) },
                placeholder = { Text("DEV-") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = tvaRate,
                onValueChange = { tvaRate = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.invoice_tva_rate)) },
                suffix = { Text("%") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp)
            )

            Text(
                stringResource(R.string.invoice_other_tax),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = otherTaxMode == OtherTaxMode.PERCENTAGE,
                    onClick = {
                        otherTaxMode = OtherTaxMode.PERCENTAGE
                        saved = false
                    },
                    label = { Text(stringResource(R.string.invoice_other_tax_mode_percent)) }
                )
                FilterChip(
                    selected = otherTaxMode == OtherTaxMode.ABSOLUTE,
                    onClick = {
                        otherTaxMode = OtherTaxMode.ABSOLUTE
                        saved = false
                    },
                    label = { Text(stringResource(R.string.invoice_other_tax_mode_absolute)) }
                )
            }

            OutlinedTextField(
                value = otherTaxRate,
                onValueChange = { otherTaxRate = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        if (otherTaxMode == OtherTaxMode.PERCENTAGE) {
                            stringResource(R.string.invoice_other_tax_rate)
                        } else {
                            stringResource(R.string.invoice_other_tax_amount)
                        }
                    )
                },
                suffix = {
                    if (otherTaxMode == OtherTaxMode.PERCENTAGE) {
                        Text("%")
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = otherTaxLabel,
                onValueChange = { otherTaxLabel = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.invoice_other_tax_label)) },
                placeholder = { Text(stringResource(R.string.invoice_other_tax_label_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Text(
                stringResource(R.string.pdf_template_section),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                stringResource(R.string.pdf_template_section_sub),
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )
            DocumentPdfTemplateSelector(
                selected = pdfTemplate,
                onSelect = {
                    pdfTemplate = it
                    saved = false
                }
            )

            saveError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
            if (saved) {
                Text(stringResource(R.string.settings_saved), color = Color(0xFF16A34A), fontSize = 13.sp)
            }

            Button(
                onClick = {
                    val tva = tvaRate.replace(",", ".").toDoubleOrNull()
                    val other = otherTaxRate.replace(",", ".").toDoubleOrNull()
                    if (tva == null || tva < 0 || other == null || other < 0) {
                        saveError = "Taux invalide"
                        saved = false
                        return@Button
                    }
                    saveError = null
                    scope.launch {
                        userPreferences.saveInvoiceSettings(
                            entrepriseId,
                            InvoiceSettings(
                                prefix = prefix,
                                quotePrefix = quotePrefix,
                                tvaRate = tva,
                                otherTaxRate = other,
                                otherTaxMode = otherTaxMode,
                                otherTaxLabel = otherTaxLabel,
                                pdfTemplate = pdfTemplate
                            )
                        )
                        saved = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.save), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
