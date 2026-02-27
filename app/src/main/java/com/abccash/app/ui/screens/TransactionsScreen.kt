package com.abccash.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abccash.app.data.local.entity.CategoryEntity
import com.abccash.app.data.local.entity.CurrencyCode
import com.abccash.app.data.local.entity.RecurrenceRule
import com.abccash.app.data.local.entity.TransactionEntity
import com.abccash.app.data.local.entity.TransactionStatus
import com.abccash.app.data.local.entity.TransactionType
import com.abccash.app.ui.DateFormatOption
import com.abccash.app.ui.components.AddTransactionDialog
import com.abccash.app.ui.components.DeleteTransactionDialog
import com.abccash.app.ui.components.EditTransactionDialog
import com.abccash.app.ui.theme.AppColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun TransactionsScreen(
    openingMinor: Long,
    onAddTransaction: (TransactionType, Long, String, TransactionStatus, LocalDate, RecurrenceRule) -> Unit,
    onValidate: (Long) -> Unit,
    onEditTransaction: (Long, Long, LocalDate, Boolean) -> Unit,
    onSetStatus: (Long, TransactionStatus) -> Unit,
    onPostpone: (Long) -> Unit,
    onDelete: (Long, Boolean) -> Unit,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currency: CurrencyCode,
    dateFormat: DateFormatOption
) {
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    var addDialogOpen by remember { mutableStateOf(false) }
    var addAmountText by remember { mutableStateOf("") }
    var addCategoryText by remember { mutableStateOf("Divers") }
    var addType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var addStatus by remember { mutableStateOf(TransactionStatus.PLANIFIEE) }
    var addRecurrence by remember { mutableStateOf(RecurrenceRule.NONE) }

    val filteredCategories = categories.filter { it.type == addType }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var editAmountText by remember { mutableStateOf("") }
    var editDateText by remember { mutableStateOf(LocalDate.now().toString()) }
    var editApplySeries by remember { mutableStateOf(false) }
    var deleteTargetId by remember { mutableStateOf<Long?>(null) }
    var deleteIsRecurring by remember { mutableStateOf(false) }
    var deleteApplySeries by remember { mutableStateOf(false) }
    
    val dayTransactions = transactions.filter { it.date == selectedDate }
    val dailyBalances = remember(transactions, openingMinor, selectedMonth) {
        computeDailyBalancesForMonth(transactions, selectedMonth, openingMinor / 100.0)
    }
    val transactionDatesInMonth = remember(transactions, selectedMonth) {
        transactions
            .asSequence()
            .map { it.date }
            .filter { it.year == selectedMonth.year && it.month == selectedMonth.month }
            .toSet()
    }
    val monthCells = remember(selectedMonth) { buildCalendarCells(selectedMonth) }
    val today = LocalDate.now()
    val currentBalanceTodayMinor = remember(transactions, openingMinor) {
        openingMinor + transactions
            .filter { !it.date.isAfter(today) }
            .sumOf { if (it.type == TransactionType.INCOME) it.amountMinor else -it.amountMinor }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            MonthNavigator(
                selectedMonth = selectedMonth,
                onPreviousMonth = {
                    selectedMonth = selectedMonth.minusMonths(1)
                    selectedDate = selectedMonth.atDay(1)
                },
                onNextMonth = {
                    selectedMonth = selectedMonth.plusMonths(1)
                    selectedDate = selectedMonth.atDay(1)
                }
            )
            
            Spacer(Modifier.height(6.dp))
            
            CalendarGrid(
                monthCells = monthCells,
                selectedDate = selectedDate,
                transactionDatesInMonth = transactionDatesInMonth,
                dailyBalances = dailyBalances,
                onDateSelected = { selectedDate = it }
            )

            Spacer(Modifier.height(6.dp))
            
            CurrentBalanceCard(
                currentBalanceTodayMinor = currentBalanceTodayMinor,
                currency = currency
            )
            
            Spacer(Modifier.height(6.dp))
            
            Text("Transactions du ${formatDate(selectedDate, dateFormat)}", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(dayTransactions) { tx ->
                    TransactionCard(
                        transaction = tx,
                        currency = currency,
                        onValidate = { onValidate(tx.id) },
                        onSetStatus = { status -> onSetStatus(tx.id, status) },
                        onEdit = {
                            editingId = tx.id
                            editAmountText = (tx.amountMinor / 100.0).toString()
                            editDateText = tx.date.toString()
                            editApplySeries = tx.isRecurring
                        },
                        onPostpone = { onPostpone(tx.id) },
                        onDelete = {
                            deleteTargetId = tx.id
                            deleteIsRecurring = tx.isRecurring
                            deleteApplySeries = tx.isRecurring
                        }
                    )
                }
            }
        }
        
        FloatingActionButton(
            onClick = {
                addDialogOpen = true
                addAmountText = ""
                addCategoryText = "Divers"
                addType = TransactionType.EXPENSE
                addStatus = TransactionStatus.PLANIFIEE
                addRecurrence = RecurrenceRule.NONE
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("+")
        }
    }

    if (addDialogOpen) {
        AddTransactionDialog(
            selectedDate = selectedDate,
            amountText = addAmountText,
            categoryText = addCategoryText,
            type = addType,
            status = addStatus,
            recurrence = addRecurrence,
            filteredCategories = filteredCategories,
            onAmountChange = { addAmountText = it },
            onCategoryChange = { addCategoryText = it },
            onTypeChange = { addType = it },
            onStatusChange = { addStatus = it },
            onRecurrenceChange = { addRecurrence = it },
            onConfirm = {
                val amount = ((addAmountText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                onAddTransaction(addType, amount, addCategoryText, addStatus, selectedDate, addRecurrence)
                addDialogOpen = false
            },
            onDismiss = { addDialogOpen = false }
        )
    }

    if (editingId != null) {
        EditTransactionDialog(
            amountText = editAmountText,
            dateText = editDateText,
            isRecurring = transactions.firstOrNull { it.id == editingId }?.isRecurring == true,
            applySeries = editApplySeries,
            onAmountChange = { editAmountText = it },
            onDateChange = { editDateText = it },
            onApplySeriesChange = { editApplySeries = it },
            onConfirm = {
                val id = editingId ?: return@EditTransactionDialog
                val amountMinor = ((editAmountText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                val newDate = runCatching { LocalDate.parse(editDateText) }.getOrElse { LocalDate.now() }
                onEditTransaction(id, amountMinor, newDate, editApplySeries)
                editingId = null
            },
            onDismiss = { editingId = null }
        )
    }

    if (deleteTargetId != null) {
        DeleteTransactionDialog(
            isRecurring = deleteIsRecurring,
            applySeries = deleteApplySeries,
            onApplySeriesChange = { deleteApplySeries = it },
            onConfirm = {
                val id = deleteTargetId ?: return@DeleteTransactionDialog
                onDelete(id, deleteIsRecurring && deleteApplySeries)
                deleteTargetId = null
            },
            onDismiss = { deleteTargetId = null }
        )
    }
}

@Composable
private fun MonthNavigator(
    selectedMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(onClick = onPreviousMonth) { Text("<") }
        Text("${selectedMonth.month} ${selectedMonth.year}", fontWeight = FontWeight.Bold)
        Button(onClick = onNextMonth) { Text(">") }
    }
}

@Composable
private fun CalendarGrid(
    monthCells: List<LocalDate?>,
    selectedDate: LocalDate,
    transactionDatesInMonth: Set<LocalDate>,
    dailyBalances: Map<LocalDate, Long>,
    onDateSelected: (LocalDate) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("L", "M", "M", "J", "V", "S", "D").forEach {
            Text(it, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        }
    }
    monthCells.chunked(7).forEach { week ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            week.forEach { date ->
                if (date == null) {
                    Spacer(modifier = Modifier.weight(1f).height(56.dp))
                } else {
                    val isSelected = date == selectedDate
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                            .clickable { onDateSelected(date) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) AppColors.SelectedCardBackground else AppColors.CardBackground
                        )
                    ) {
                        Column(Modifier.padding(4.dp)) {
                            Text("${date.dayOfMonth}", fontWeight = FontWeight.SemiBold)
                            if (date in transactionDatesInMonth) {
                                val dayBalance = dailyBalances[date] ?: 0L
                                if (dayBalance != 0L) {
                                    Text(
                                        text = formatCompactAmount(dayBalance),
                                        color = if (dayBalance >= 0) AppColors.IncomeGreen else AppColors.ExpenseRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun CurrentBalanceCard(
    currentBalanceTodayMinor: Long,
    currency: CurrencyCode
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.InfoCardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Solde actuel", fontWeight = FontWeight.SemiBold)
            Text(
                text = formatAmount(currentBalanceTodayMinor, currency),
                color = if (currentBalanceTodayMinor >= 0L) AppColors.IncomeGreen else AppColors.ExpenseRed,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TransactionCard(
    transaction: TransactionEntity,
    currency: CurrencyCode,
    onValidate: () -> Unit,
    onSetStatus: (TransactionStatus) -> Unit,
    onEdit: () -> Unit,
    onPostpone: () -> Unit,
    onDelete: () -> Unit
) {
    val amountColor = when (transaction.type) {
        TransactionType.INCOME -> AppColors.IncomeGreen
        TransactionType.EXPENSE -> AppColors.ExpenseRed
    }
    val cardAlpha = if (transaction.status == TransactionStatus.PLANIFIEE) 0.65f else 1f
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "${transaction.type} - ${formatAmount(transaction.amountMinor, currency)}",
                color = amountColor,
                fontWeight = FontWeight.Bold
            )
            Text("${transaction.category} / ${transaction.date} / ${transaction.status}")
            if (transaction.status == TransactionStatus.EN_RETARD) {
                Text("Echeance depassee", color = AppColors.OverdueOrange, fontWeight = FontWeight.SemiBold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (transaction.status != TransactionStatus.VALIDEE) {
                    Button(onClick = onValidate) {
                        Text("Valider")
                    }
                }
                if (transaction.status == TransactionStatus.EN_RETARD) {
                    Button(onClick = { onSetStatus(TransactionStatus.PLANIFIEE) }) {
                        Text("Planifier")
                    }
                }
                if (transaction.status == TransactionStatus.PLANIFIEE) {
                    Button(onClick = { onSetStatus(TransactionStatus.EN_RETARD) }) {
                        Text("Retard")
                    }
                }
                Button(onClick = onEdit) {
                    Text("Editer")
                }
                Button(onClick = onPostpone) {
                    Text("+1 mois")
                }
                Button(onClick = onDelete) {
                    Text("Supprimer")
                }
            }
        }
    }
}

private fun buildCalendarCells(month: YearMonth): List<LocalDate?> {
    val days = month.lengthOfMonth()
    val leading = month.atDay(1).dayOfWeek.value - 1
    val cells = mutableListOf<LocalDate?>()
    repeat(leading) { cells += null }
    (1..days).forEach { day -> cells += month.atDay(day) }
    while (cells.size % 7 != 0) cells += null
    return cells
}

private fun computeDailyBalancesForMonth(
    transactions: List<TransactionEntity>,
    month: YearMonth,
    manualOpeningMajor: Double?
): Map<LocalDate, Long> {
    val monthStart = month.atDay(1)
    val monthEnd = month.atEndOfMonth()
    val openingMinor = ((manualOpeningMajor ?: 0.0) * 100).toLong()
    val beforeMonthNet = transactions.filter { it.date < monthStart }.sumOf {
        if (it.type == TransactionType.INCOME) it.amountMinor else -it.amountMinor
    }
    var running = openingMinor + beforeMonthNet
    val byDate = transactions.groupBy { it.date }
    val result = mutableMapOf<LocalDate, Long>()
    var current = monthStart
    while (!current.isAfter(monthEnd)) {
        val dayNet = (byDate[current] ?: emptyList()).sumOf {
            if (it.type == TransactionType.INCOME) it.amountMinor else -it.amountMinor
        }
        running += dayNet
        result[current] = running
        current = current.plusDays(1)
    }
    return result
}

private fun formatCompactAmount(amountMinor: Long): String {
    val major = amountMinor / 100.0
    return "%.0f".format(major)
}

private fun formatAmount(amountMinor: Long, currency: CurrencyCode): String {
    val major = amountMinor / 100.0
    val symbol = when (currency) {
        CurrencyCode.DT -> "DT"
        CurrencyCode.USD -> "$"
        CurrencyCode.EUR -> "EUR"
    }
    return "%.2f %s".format(major, symbol)
}

private fun formatDate(date: LocalDate, format: DateFormatOption): String {
    return date.format(DateTimeFormatter.ofPattern(format.pattern))
}
