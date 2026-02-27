package com.abccash.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.data.local.entity.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

data class CalendarDay(
    val date: LocalDate?,
    val isCurrentMonth: Boolean,
    val transactionAmount: Double = 0.0,
    val hasTransactions: Boolean = false
)

@Composable
fun CalendarScreen(
    openingBalance: Double,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currency: CurrencyCode,
    onAddTransaction: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit
) {
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    
    val calendarDays = remember(selectedMonth, transactions) {
        buildCalendarDays(selectedMonth, transactions)
    }
    
    val dayTransactions = remember(selectedDate, transactions) {
        transactions.filter { it.date == selectedDate }
    }
    
    val selectedDayBalance = remember(selectedDate, transactions, openingBalance) {
        calculateBalanceAtDate(selectedDate, transactions, openingBalance)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Navigation du mois
            MonthNavigationBar(
                selectedMonth = selectedMonth,
                onPreviousMonth = { selectedMonth = selectedMonth.minusMonths(1) },
                onNextMonth = { selectedMonth = selectedMonth.plusMonths(1) }
            )
            
            // Grille du calendrier
            CalendarGrid(
                calendarDays = calendarDays,
                selectedDate = selectedDate,
                onDateClick = { date -> date?.let { selectedDate = it } }
            )
            
            // Carte de solde
            BalanceCard(
                date = selectedDate,
                balance = selectedDayBalance,
                currency = currency
            )
            
            // Transactions du jour
            DayTransactionsSection(
                transactions = dayTransactions,
                onTransactionClick = onTransactionClick
            )
        }
        
        // FAB pour ajouter une transaction
        FloatingActionButton(
            onClick = onAddTransaction,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Ajouter transaction")
        }
    }
}

@Composable
private fun MonthNavigationBar(
    selectedMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                Icons.Filled.ChevronLeft,
                contentDescription = "Mois précédent",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Text(
            text = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH)),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        IconButton(onClick = onNextMonth) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "Mois suivant",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    calendarDays: List<CalendarDay>,
    selectedDate: LocalDate,
    onDateClick: (LocalDate?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        // En-têtes des jours de la semaine
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Grille des jours
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(calendarDays) { day ->
                CalendarDayCell(
                    day = day,
                    isSelected = day.date == selectedDate,
                    isToday = day.date == LocalDate.now(),
                    onClick = { onDateClick(day.date) }
                )
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(enabled = day.date != null) { onClick() }
            .then(
                if (isSelected && day.date != null) {
                    Modifier
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (day.date != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                        isToday -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                
                if (day.hasTransactions && day.isCurrentMonth) {
                    Text(
                        text = String.format("%.0f", kotlin.math.abs(day.transactionAmount)),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = if (day.transactionAmount >= 0)
                            Color(0xFF10B981)
                        else
                            Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(
    date: LocalDate,
    balance: Double,
    currency: CurrencyCode
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Solde au ${date.format(DateTimeFormatter.ofPattern("dd MMMM", Locale.FRENCH))} :",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = String.format("%.2f %s", balance, currency.name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (balance >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
            )
        }
    }
}

@Composable
private fun DayTransactionsSection(
    transactions: List<TransactionEntity>,
    onTransactionClick: (TransactionEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Transactions du jour",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        if (transactions.isEmpty()) {
            Text(
                text = "Aucune transaction",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transactions) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionItem(
    transaction: TransactionEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = transaction.status.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${if (transaction.type == TransactionType.INCOME) "+" else "-"}${String.format("%.2f", transaction.amountMinor / 100.0)} €",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (transaction.type == TransactionType.INCOME)
                    Color(0xFF10B981)
                else
                    Color(0xFFEF4444)
            )
        }
    }
}

private fun buildCalendarDays(
    month: YearMonth,
    transactions: List<TransactionEntity>
): List<CalendarDay> {
    val firstDayOfMonth = month.atDay(1)
    val lastDayOfMonth = month.atEndOfMonth()
    
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value
    val daysInMonth = month.lengthOfMonth()
    
    val days = mutableListOf<CalendarDay>()
    
    // Jours du mois précédent
    val previousMonth = month.minusMonths(1)
    val daysInPreviousMonth = previousMonth.lengthOfMonth()
    for (i in (firstDayOfWeek - 1) downTo 1) {
        val date = previousMonth.atDay(daysInPreviousMonth - i + 1)
        days.add(CalendarDay(date, false))
    }
    
    // Jours du mois actuel
    for (day in 1..daysInMonth) {
        val date = month.atDay(day)
        val dayTransactions = transactions.filter { it.date == date }
        val amount = dayTransactions.sumOf {
            if (it.type == TransactionType.INCOME) it.amountMinor / 100.0
            else -(it.amountMinor / 100.0)
        }
        days.add(
            CalendarDay(
                date = date,
                isCurrentMonth = true,
                transactionAmount = amount,
                hasTransactions = dayTransactions.isNotEmpty()
            )
        )
    }
    
    // Jours du mois suivant
    val remainingCells = 42 - days.size
    val nextMonth = month.plusMonths(1)
    for (day in 1..remainingCells) {
        val date = nextMonth.atDay(day)
        days.add(CalendarDay(date, false))
    }
    
    return days
}

private fun calculateBalanceAtDate(
    date: LocalDate,
    transactions: List<TransactionEntity>,
    openingBalance: Double
): Double {
    return openingBalance + transactions
        .filter { !it.date.isAfter(date) }
        .sumOf {
            if (it.type == TransactionType.INCOME) it.amountMinor / 100.0
            else -(it.amountMinor / 100.0)
        }
}
