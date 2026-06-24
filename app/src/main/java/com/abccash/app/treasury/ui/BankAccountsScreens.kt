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
fun BankAccountsListScreen(
    summaries: List<BankAccountSummary>,
    accountsUsed: Int,
    accountsLimit: Int,
    canAddAccount: Boolean,
    onBack: () -> Unit,
    onAddAccount: () -> Unit,
    onOpenAccount: (String) -> Unit,
    onOpenManualReconciliation: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bank_accounts_title)) },
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
            if (canAddAccount) {
                AbcCashFab(
                    onClick = onAddAccount,
                    contentDescription = stringResource(R.string.bank_account_add)
                )
            }
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
                TreasuryAccountsUsageCard(accountsUsed = accountsUsed, accountsLimit = accountsLimit)
            }
            item {
                Dsp2ComingSoonCard()
            }
            item {
                OutlinedButton(
                    onClick = onOpenManualReconciliation,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.bank_connection_manual_reconciliation))
                }
            }
            if (summaries.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                stringResource(R.string.bank_accounts_empty),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                stringResource(R.string.bank_accounts_empty_hint),
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            } else {
                items(summaries, key = { it.account.id }) { summary ->
                    BankAccountListCard(
                        summary = summary,
                        onClick = { onOpenAccount(summary.account.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TreasuryAccountsUsageCard(accountsUsed: Int, accountsLimit: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F8FF))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.treasury_accounts_usage, accountsUsed, accountsLimit),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                stringResource(R.string.treasury_accounts_usage_hint),
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun Dsp2ComingSoonCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFFF57C00))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.bank_connection_coming_soon),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    stringResource(R.string.bank_accounts_dsp2_hint),
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun BankAccountListCard(
    summary: BankAccountSummary,
    onClick: () -> Unit
) {
    val formatAmount = rememberFormatMoney()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(summary.account.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = {
                                Text(
                                    stringResource(summary.account.kind.labelRes),
                                    fontSize = 11.sp
                                )
                            }
                        )
                    }
                    if (summary.account.kind == TreasuryAccountKind.BANK && summary.account.bankName.isNotBlank()) {
                        Text(summary.account.bankName, fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    if (summary.account.kind == TreasuryAccountKind.BANK && summary.account.ibanLast4.isNotBlank()) {
                        Text(
                            "•••• ${summary.account.ibanLast4}",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                if (summary.account.isDefault) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(stringResource(R.string.bank_account_default), fontSize = 11.sp) }
                    )
                }
            }
            Text(
                text = formatAmount(summary.balance),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (summary.hasLowBalanceAlert) Color(0xFFDC2626) else Color(0xFF166534)
            )
            Text(
                text = stringResource(R.string.bank_account_movements_count, summary.movementCount),
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )
            if (summary.hasLowBalanceAlert) {
                Text(
                    text = stringResource(R.string.bank_account_low_balance_alert),
                    fontSize = 12.sp,
                    color = Color(0xFFDC2626),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankAccountDetailScreen(
    account: BankAccount,
    balance: Double,
    movements: List<BankAccountMovement>,
    hasLowBalanceAlert: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val formatAmount = rememberFormatMoney()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.bank_account_delete_title)) },
            text = { Text(stringResource(R.string.bank_account_delete_message, account.name)) },
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
                title = { Text(account.name) },
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.bank_account_balance), fontSize = 12.sp, color = Color(0xFF64748B))
                        Text(
                            formatAmount(balance),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasLowBalanceAlert) Color(0xFFDC2626) else Color(0xFF1E3A8A)
                        )
                        if (account.bankName.isNotBlank()) {
                            Text(account.bankName, fontSize = 13.sp)
                        }
                        account.alertLowBalance?.let { threshold ->
                            Text(
                                stringResource(R.string.bank_account_alert_threshold, formatAmount(threshold)),
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
            if (hasLowBalanceAlert) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))) {
                        Row(
                            Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626))
                            Text(
                                stringResource(R.string.bank_account_low_balance_alert),
                                color = Color(0xFFDC2626),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.bank_account_history),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
            if (movements.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.bank_account_no_movements),
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            } else {
                items(movements, key = { it.id + it.type.name }) { movement ->
                    BankAccountMovementRow(movement = movement, formatAmount = formatAmount)
                }
            }
        }
    }
}

@Composable
private fun BankAccountMovementRow(
    movement: BankAccountMovement,
    formatAmount: (Double) -> String
) {
    val isIncome = movement.type == BankAccountMovementType.INCOME
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(movement.label, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(
                    "${AppLocale.dayMonth(movement.date)} · ${movement.method.localizedLabel()}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
            Text(
                text = "${if (isIncome) "+" else "-"}${formatAmount(movement.amount)}",
                fontWeight = FontWeight.Bold,
                color = if (isIncome) Color(0xFF16A34A) else Color(0xFFDC2626)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankAccountFormSheet(
    visible: Boolean,
    initialAccount: BankAccount?,
    entrepriseId: String,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSave: (BankAccount) -> Unit
) {
    if (!visible) return

    var kind by remember(initialAccount) {
        mutableStateOf(initialAccount?.kind ?: TreasuryAccountKind.BANK)
    }
    var name by remember(initialAccount, kind) {
        mutableStateOf(
            initialAccount?.name.orEmpty().ifBlank {
                if (kind == TreasuryAccountKind.CASH) "" else ""
            }
        )
    }
    var bankName by remember(initialAccount) { mutableStateOf(initialAccount?.bankName.orEmpty()) }
    var ibanLast4 by remember(initialAccount) { mutableStateOf(initialAccount?.ibanLast4.orEmpty()) }
    var openingText by remember(initialAccount) {
        mutableStateOf(initialAccount?.openingBalance?.toString()?.replace('.', ',').orEmpty())
    }
    var alertText by remember(initialAccount) {
        mutableStateOf(initialAccount?.alertLowBalance?.toString()?.replace('.', ',').orEmpty())
    }
    var isDefault by remember(initialAccount) { mutableStateOf(initialAccount?.isDefault ?: false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(
                    if (initialAccount == null) R.string.bank_account_add else R.string.bank_account_edit
                ),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (initialAccount == null) {
                Text(
                    text = stringResource(R.string.treasury_account_kind_label),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = kind == TreasuryAccountKind.BANK,
                        onClick = { kind = TreasuryAccountKind.BANK },
                        label = { Text(stringResource(R.string.treasury_account_kind_bank)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = kind == TreasuryAccountKind.CASH,
                        onClick = { kind = TreasuryAccountKind.CASH },
                        label = { Text(stringResource(R.string.treasury_account_kind_cash)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = {
                    Text(
                        if (kind == TreasuryAccountKind.CASH) {
                            stringResource(R.string.treasury_cash_account_name)
                        } else {
                            stringResource(R.string.bank_account_name)
                        }
                    )
                },
                placeholder = {
                    if (kind == TreasuryAccountKind.CASH) {
                        Text(stringResource(R.string.treasury_cash_account_name_hint))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = openingText,
                onValueChange = { openingText = it },
                label = { Text(stringResource(R.string.bank_account_opening_balance)) },
                supportingText = { Text(stringResource(R.string.bank_account_opening_balance_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix = { CurrencySuffix() }
            )
            if (kind == TreasuryAccountKind.BANK) {
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text(stringResource(R.string.bank_account_bank_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = ibanLast4,
                    onValueChange = { if (it.length <= 4) ibanLast4 = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.bank_account_iban_last4)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            OutlinedTextField(
                value = alertText,
                onValueChange = { alertText = it },
                label = { Text(stringResource(R.string.bank_account_alert_threshold_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix = { CurrencySuffix() }
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isDefault, onCheckedChange = { isDefault = it })
                Text(
                    if (kind == TreasuryAccountKind.CASH) {
                        stringResource(R.string.treasury_cash_account_set_default)
                    } else {
                        stringResource(R.string.bank_account_set_default)
                    }
                )
            }
            errorMessage?.let { message ->
                Text(resolveTreasuryMessage(message) ?: message, color = Color(0xFFDC2626), fontSize = 13.sp)
            }
            Button(
                onClick = {
                    val opening = openingText.replace(" ", "").replace(",", ".").toDoubleOrNull() ?: 0.0
                    val alert = alertText.replace(" ", "").replace(",", ".").toDoubleOrNull()
                    onSave(
                        (initialAccount ?: BankAccount(entrepriseId = entrepriseId, name = "")).copy(
                            name = name.trim(),
                            bankName = if (kind == TreasuryAccountKind.BANK) bankName.trim() else "",
                            ibanLast4 = if (kind == TreasuryAccountKind.BANK) ibanLast4 else "",
                            openingBalance = opening,
                            alertLowBalance = alert,
                            isDefault = isDefault,
                            kind = kind,
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
}
