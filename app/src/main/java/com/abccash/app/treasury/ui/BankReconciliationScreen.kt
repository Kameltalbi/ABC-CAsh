package com.abccash.app.treasury.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abccash.app.treasury.data.Expense
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.data.TreasuryCalculations
import com.abccash.app.treasury.data.UserRole
import com.abccash.app.treasury.datastore.UserPreferences
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankReconciliationScreen(
    entrepriseId: String,
    userRole: UserRole,
    invoices: List<Invoice>,
    expenses: List<Expense>,
    onBack: () -> Unit,
    onReconcileTreasury: (
        bankBalance: Double,
        calculatedBalance: Double,
        createAdjustments: Boolean,
        userRole: UserRole,
        onResult: (String?) -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    val displayYear = remember { YearMonth.now().year }
    val userPreferences = remember { UserPreferences(context) }
    val bankBalance by userPreferences
        .observeBankBalance(entrepriseId, displayYear)
        .collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val formatAmount = rememberFormatMoney()

    val calculatedBalance = remember(invoices, expenses, displayYear) {
        TreasuryCalculations.yearlyBalance(invoices, expenses, displayYear)
    }

    var amountText by remember(bankBalance) {
        mutableStateOf(bankBalance?.toString()?.replace('.', ',').orEmpty())
    }
    val parsedAmount = amountText.replace(" ", "").replace(",", ".").toDoubleOrNull()
    val gap = parsedAmount?.let { it - calculatedBalance }
    val hasGap = gap != null && kotlin.math.abs(gap) > 0.001
    var createAdjustments by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compte bancaire") },
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Comparez le solde réel de votre compte avec la trésorerie calculée par l'application ($displayYear).",
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                lineHeight = 20.sp
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Comment ça marche ?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• Saisissez le solde affiché sur votre relevé bancaire\n" +
                            "• Si un écart existe, l'app peut créer un ajustement automatique\n" +
                            "• Banque plus élevée → encaissement d'ajustement\n" +
                            "• Banque plus basse → dépense d'ajustement",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 18.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryChip(
                    label = "Trésorerie calculée",
                    value = formatAmount(calculatedBalance),
                    modifier = Modifier.weight(1f)
                )
                bankBalance?.let { saved ->
                    SummaryChip(
                        label = "Dernier solde saisi",
                        value = formatAmount(saved),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    saveError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Solde bancaire actuel") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix = { CurrencySuffix() },
                isError = amountText.isNotBlank() && parsedAmount == null,
                supportingText = if (amountText.isNotBlank() && parsedAmount == null) {
                    { Text("Montant invalide") }
                } else {
                    null
                }
            )

            if (hasGap) {
                Text(
                    text = "Écart : ${formatAmount(gap!!)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFF57C00)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = createAdjustments,
                        onCheckedChange = { createAdjustments = it }
                    )
                    Column {
                        Text(
                            text = "Aligner la trésorerie",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (gap > 0) {
                                "Créer un encaissement d'ajustement"
                            } else {
                                "Créer une dépense d'ajustement"
                            },
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            saveError?.let {
                Text(text = it, color = Color(0xFFF44336), fontSize = 13.sp)
            }

            Button(
                onClick = {
                    val amount = parsedAmount
                    when {
                        amount == null -> saveError = "Montant invalide"
                        else -> {
                            isSaving = true
                            saveError = null
                            onReconcileTreasury(
                                amount,
                                calculatedBalance,
                                createAdjustments && hasGap,
                                userRole
                            ) { error ->
                                scope.launch {
                                    isSaving = false
                                    if (error != null) {
                                        saveError = error
                                        return@launch
                                    }
                                    userPreferences.saveBankBalance(entrepriseId, displayYear, amount)
                                    val message = when {
                                        createAdjustments && hasGap ->
                                            "Trésorerie alignée sur le compte bancaire"
                                        hasGap ->
                                            "Solde bancaire enregistré"
                                        else -> "Solde conforme à la trésorerie"
                                    }
                                    snackbarHostState.showSnackbar(message)
                                    onBack()
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !isSaving && parsedAmount != null,
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (hasGap && createAdjustments) "Aligner la trésorerie" else "Enregistrer le solde")
                }
            }

            if (bankBalance != null) {
                TextButton(
                    onClick = {
                        scope.launch {
                            userPreferences.saveBankBalance(entrepriseId, displayYear, null)
                            amountText = ""
                            snackbarHostState.showSnackbar("Solde bancaire effacé")
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Effacer le solde enregistré", color = Color(0xFFF44336))
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = label, fontSize = 11.sp, color = Color(0xFF64748B))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                maxLines = 2
            )
        }
    }
}
