package com.abccash.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abccash.app.data.local.entity.CurrencyCode
import com.abccash.app.data.local.entity.TransactionEntity
import com.abccash.app.data.local.entity.TransactionStatus
import com.abccash.app.data.local.entity.TransactionType
import com.abccash.app.ui.DateFormatOption
import com.abccash.app.ui.theme.AppColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun PlanningScreen(
    transactions: List<TransactionEntity>,
    currency: CurrencyCode,
    dateFormat: DateFormatOption
) {
    val grouped = transactions.groupBy { it.date.withDayOfMonth(1) }.toSortedMap()
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Planning", fontWeight = FontWeight.Bold)
                Text("Planifiees: ${transactions.count { it.status == TransactionStatus.PLANIFIEE }}")
                Text("En retard: ${transactions.count { it.status == TransactionStatus.EN_RETARD }}")
                Text("Validees: ${transactions.count { it.status == TransactionStatus.VALIDEE }}")
            }
        }
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            grouped.forEach { (monthStart, monthItems) ->
                item {
                    MonthPlanningCard(
                        monthStart = monthStart,
                        monthItems = monthItems,
                        currency = currency,
                        dateFormat = dateFormat
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthPlanningCard(
    monthStart: LocalDate,
    monthItems: List<TransactionEntity>,
    currency: CurrencyCode,
    dateFormat: DateFormatOption
) {
    val planned = monthItems.filter { it.status != TransactionStatus.VALIDEE }
    val income = planned.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
    val expense = planned.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
    
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Mois: ${formatDate(monthStart, dateFormat)}", fontWeight = FontWeight.SemiBold)
            Text("Echeances: ${planned.size}")
            Text("Entrees prevues: ${formatAmount(income, currency)}", color = AppColors.IncomeGreen)
            Text("Sorties prevues: ${formatAmount(expense, currency)}", color = AppColors.ExpenseRed)
        }
    }
}

private fun formatDate(date: LocalDate, format: DateFormatOption): String {
    return date.format(DateTimeFormatter.ofPattern(format.pattern))
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
