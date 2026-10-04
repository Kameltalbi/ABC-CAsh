package com.abccash.app.treasury.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R
import com.abccash.app.locale.AppLocale
import com.abccash.app.treasury.data.BalanceCorrection
import com.abccash.app.treasury.data.BankAccount
import com.abccash.app.treasury.data.Expense
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.data.PilotEntry
import com.abccash.app.treasury.data.PilotEntryType
import com.abccash.app.treasury.data.TreasuryAccountKind
import com.abccash.app.treasury.data.TreasuryAccountMonth
import com.abccash.app.treasury.data.TreasuryLine
import com.abccash.app.treasury.data.TreasuryMonth
import com.abccash.app.treasury.data.TreasuryMonthSnapshot
import java.time.YearMonth
import java.util.Locale
import kotlin.math.abs

private val PageBg = Color(0xFFF4F6FA)
private val Ink = Color(0xFF0F2744)
private val Muted = Color(0xFF6B7A90)
private val Blue = Color(0xFF1D4ED8)
private val BlueSoft = Color(0xFFE8F0FE)
private val Green = Color(0xFF15803D)
private val GreenSoft = Color(0xFFE7F7ED)
private val Orange = Color(0xFFC2410C)
private val OrangeSoft = Color(0xFFFFF1E8)
private val Line = Color(0xFFE6EBF2)
private val CardShape = RoundedCornerShape(16.dp)
private val French = Locale.FRENCH

@Composable
fun TreasuryMonthScreen(
    entrepriseId: String,
    accounts: List<BankAccount>,
    invoices: List<Invoice>,
    expenses: List<Expense>,
    pilotEntries: List<PilotEntry>,
    corrections: List<BalanceCorrection>,
    onOpenPilot: (String) -> Unit
) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    val formatAmount = rememberFormatMoneyWhole()
    val snapshot = remember(month, entrepriseId, accounts, invoices, expenses, pilotEntries, corrections) {
        TreasuryMonth.snapshot(month, entrepriseId, accounts, invoices, expenses, pilotEntries, corrections)
    }
    val accountNames = remember(accounts) {
        accounts.associate { it.id to it.name }
    }
    val accountKinds = remember(accounts) {
        accounts.associate { it.id to it.kind }
    }

    Scaffold(containerColor = PageBg) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(PageBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.nav_treasury),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Ink
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MonthSwitcher(
                    label = AppLocale.monthYear(month, French),
                    onPrevious = { month = month.minusMonths(1) },
                    onNext = { month = month.plusMonths(1) }
                )
                KpiGrid(snapshot, formatAmount)
                if (snapshot.accounts.isNotEmpty()) {
                    AccountsSection(snapshot.accounts, accountKinds, formatAmount)
                }
                MovementsSection(
                    title = stringResource(R.string.treasury_month_inflows_section),
                    titleIcon = Icons.AutoMirrored.Filled.TrendingUp,
                    titleTint = Green,
                    titleBg = GreenSoft,
                    lines = snapshot.inflowLines,
                    accountNames = accountNames,
                    formatAmount = formatAmount,
                    inflow = true,
                    onOpenPilot = onOpenPilot
                )
                MovementsSection(
                    title = stringResource(R.string.treasury_month_outflows_section),
                    titleIcon = Icons.AutoMirrored.Filled.TrendingDown,
                    titleTint = Orange,
                    titleBg = OrangeSoft,
                    lines = snapshot.outflowLines,
                    accountNames = accountNames,
                    formatAmount = formatAmount,
                    inflow = false,
                    onOpenPilot = onOpenPilot
                )
            }
        }
    }
}

@Composable
private fun MonthSwitcher(label: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.previous_month),
                    tint = Muted
                )
            }
            Text(
                text = label.replaceFirstChar { if (it.isLowerCase()) it.titlecase(French) else it.toString() },
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Ink
            )
            IconButton(onClick = onNext) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.next_month),
                    tint = Muted
                )
            }
        }
    }
}

@Composable
private fun KpiGrid(snapshot: TreasuryMonthSnapshot, formatAmount: (Double) -> String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard(
                label = stringResource(R.string.treasury_month_opening),
                amount = formatAmount(snapshot.opening),
                amountColor = Blue,
                icon = Icons.Filled.Savings,
                iconTint = Blue,
                iconBg = BlueSoft,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                label = stringResource(R.string.treasury_month_inflows),
                amount = "+${formatAmount(snapshot.inflows)}",
                amountColor = Green,
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                iconTint = Green,
                iconBg = GreenSoft,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard(
                label = stringResource(R.string.treasury_month_outflows),
                amount = "-${formatAmount(snapshot.outflows)}",
                amountColor = Orange,
                icon = Icons.AutoMirrored.Filled.TrendingDown,
                iconTint = Orange,
                iconBg = OrangeSoft,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                label = stringResource(R.string.treasury_month_closing),
                amount = formatAmount(snapshot.closing),
                amountColor = Blue,
                icon = Icons.Filled.AccountBalanceWallet,
                iconTint = Blue,
                iconBg = BlueSoft,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KpiCard(
    label: String,
    amount: String,
    amountColor: Color,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.padding(start = 8.dp)) {
                Text(label, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(
                    text = amount,
                    color = amountColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun AccountsSection(
    accounts: List<TreasuryAccountMonth>,
    accountKinds: Map<String, TreasuryAccountKind>,
    formatAmount: (Double) -> String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SoftIcon(Icons.Filled.AccountBalance, Blue, BlueSoft)
                Text(
                    text = stringResource(R.string.treasury_month_accounts),
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            accounts.forEachIndexed { index, account ->
                if (index > 0) {
                    HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 2.dp))
                }
                val kind = accountKinds[account.accountId] ?: TreasuryAccountKind.BANK
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SoftIcon(
                        icon = if (kind == TreasuryAccountKind.CASH) Icons.Filled.Payments else Icons.Filled.AccountBalance,
                        tint = if (kind == TreasuryAccountKind.CASH) Green else Blue,
                        bg = if (kind == TreasuryAccountKind.CASH) GreenSoft else BlueSoft
                    )
                    Text(
                        text = account.name,
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .weight(1f),
                        color = Ink,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatAmount(account.closing),
                        color = Ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = Muted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MovementsSection(
    title: String,
    titleIcon: ImageVector,
    titleTint: Color,
    titleBg: Color,
    lines: List<TreasuryLine>,
    accountNames: Map<String, String>,
    formatAmount: (Double) -> String,
    inflow: Boolean,
    onOpenPilot: (String) -> Unit
) {
    val unknownAccount = stringResource(R.string.treasury_month_unknown_account)
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SoftIcon(titleIcon, titleTint, titleBg)
                Text(
                    text = title,
                    modifier = Modifier.padding(start = 8.dp),
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            if (lines.isEmpty()) {
                Text(
                    text = stringResource(R.string.treasury_month_empty),
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                    color = Muted,
                    fontSize = 13.sp
                )
            }
            lines.forEachIndexed { index, line ->
                if (index > 0 || lines.isNotEmpty()) {
                    HorizontalDivider(
                        color = Line,
                        modifier = Modifier.padding(top = if (index == 0) 8.dp else 0.dp)
                    )
                }
                val entryId = line.pilotEntryId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (entryId != null) Modifier.clickable { onOpenPilot(entryId) } else Modifier)
                        .padding(vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SoftIcon(
                        icon = movementIcon(line, inflow),
                        tint = if (inflow) Blue else Orange,
                        bg = if (inflow) BlueSoft else OrangeSoft
                    )
                    Column(
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .weight(1f)
                    ) {
                        Text(
                            text = lineLabel(line),
                            color = Ink,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = buildString {
                                append(AppLocale.shortDayMonthYear(line.date, French))
                                append("  •  ")
                                append(accountNames[line.bankAccountId] ?: unknownAccount)
                            },
                            color = Muted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                    Text(
                        text = if (inflow) "+${formatAmount(abs(line.signedAmount))}" else "-${formatAmount(abs(line.signedAmount))}",
                        color = if (inflow) Green else Orange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (entryId != null) {
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = Muted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SoftIcon(icon: ImageVector, tint: Color, bg: Color) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
    }
}

private fun movementIcon(line: TreasuryLine, inflow: Boolean): ImageVector = when (line.pilotType) {
    PilotEntryType.SALE -> Icons.Filled.Payments
    PilotEntryType.EXPENSE -> Icons.Filled.AccountBalanceWallet
    PilotEntryType.OTHER_INFLOW -> Icons.Filled.AccountBalance
    PilotEntryType.OTHER_OUTFLOW -> Icons.AutoMirrored.Filled.TrendingDown
    PilotEntryType.TRANSFER -> Icons.Filled.AccountBalance
    null -> if (inflow) Icons.Filled.Payments else Icons.Filled.AccountBalanceWallet
}

@Composable
private fun lineLabel(line: TreasuryLine): String {
    if (line.label.isNotBlank()) return line.label
    return when (line.pilotType) {
        PilotEntryType.SALE -> stringResource(R.string.pilot_btn_sale)
        PilotEntryType.EXPENSE -> stringResource(R.string.pilot_btn_expense)
        PilotEntryType.OTHER_INFLOW -> stringResource(R.string.pilot_other_in)
        PilotEntryType.OTHER_OUTFLOW -> stringResource(R.string.pilot_other_out)
        PilotEntryType.TRANSFER -> stringResource(R.string.pilot_other_transfer)
        null -> stringResource(R.string.treasury_month_movement)
    }
}
