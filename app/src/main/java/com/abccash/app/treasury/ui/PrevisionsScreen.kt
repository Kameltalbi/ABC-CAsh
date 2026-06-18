package com.abccash.app.treasury.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.treasury.data.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrevisionsScreen(
    userRole: UserRole,
    permissions: Set<UserPermission>,
    invoices: List<Invoice>,
    expenses: List<Expense>,
    onUpdateInvoice: (String, String, String, Double, LocalDate, (String?) -> Unit) -> Unit,
    onRecordPayment: (String, Double, LocalDate, PaymentMethod, (String?) -> Unit) -> Unit,
    onDeleteInvoice: (String) -> Unit,
    onUpdateExpense: (
        String, String, Double, LocalDate, Boolean,
        ExpenseRecurrence?, LocalDate?, Boolean
    ) -> Unit,
    onDeleteExpense: (String) -> Unit
) {
    val canViewIncome = hasPermission(userRole, permissions, UserPermission.VIEW_INVOICES)
    val canManageExpense = hasPermission(userRole, permissions, UserPermission.MANAGE_EXPENSES)
    val canMarkPaidIncome = userRole == UserRole.ADMIN ||
        hasPermission(userRole, permissions, UserPermission.ADD_PAYMENTS)

    if (!hasPermission(userRole, permissions, UserPermission.VIEW_TREASURY)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Accès refusé", color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
        }
        return
    }

    val formatAmount = rememberFormatMoney()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH) }
    val today = remember { LocalDate.now() }
    val horizonEnd = remember { today.plusMonths(12) }

    val sections = remember(invoices, expenses, canViewIncome, canManageExpense) {
        val filteredInvoices = if (canViewIncome) invoices else emptyList()
        val filteredExpenses = if (canManageExpense) expenses else emptyList()
        EcheanceForecast.groupByMonth(
            EcheanceForecast.buildItems(filteredInvoices, filteredExpenses, today, horizonEnd)
        )
    }

    var invoiceToEdit by remember { mutableStateOf<Invoice?>(null) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var itemToDelete by remember { mutableStateOf<EcheanceItem?>(null) }
    var editError by remember { mutableStateOf<String?>(null) }

    invoiceToEdit?.let { invoice ->
        InvoiceFormDialog(
            title = "Modifier l'encaissement",
            initialInvoice = invoice,
            selectedMonth = YearMonth.from(invoice.dueDate),
            onDismiss = {
                invoiceToEdit = null
                editError = null
            },
            onConfirm = { invoiceNumber, clientName, totalAmount, dueDate, _ ->
                onUpdateInvoice(invoice.id, invoiceNumber, clientName, totalAmount, dueDate) { error ->
                    if (error == null) {
                        invoiceToEdit = null
                        editError = null
                    } else {
                        editError = error
                    }
                }
            },
            errorMessage = editError
        )
    }

    expenseToEdit?.let { expense ->
        ExpenseFormDialog(
            initialExpense = expense,
            selectedMonth = YearMonth.from(expense.date),
            onDismiss = {
                expenseToEdit = null
                editError = null
            },
            onConfirm = { label, amount, date, recurring, recurrence, endDate, paid ->
                onUpdateExpense(
                    expense.id, label, amount, date, recurring, recurrence, endDate, paid
                )
                expenseToEdit = null
            },
            onStopRecurrence = { _ -> expenseToEdit = null }
        )
    }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Supprimer la prévision ?") },
            text = { Text("Supprimer « ${item.label} » (${formatAmount(item.amount)}) ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (item.type) {
                            EcheanceType.INCOME -> item.invoiceId?.let(onDeleteInvoice)
                            EcheanceType.EXPENSE -> item.expenseId?.let(onDeleteExpense)
                        }
                        itemToDelete = null
                    }
                ) {
                    Text("Supprimer", color = Color(0xFFF44336))
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("Annuler") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF9F6))
    ) {
        Text(
            text = "Prévisions",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        if (sections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Aucune prévision", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Les recettes et dépenses à venir apparaîtront ici.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sections.forEach { section ->
                    item {
                        Text(
                            text = section.label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF424242),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(section.items, key = { it.id }) { item ->
                        EcheanceRow(
                            item = item,
                            formattedAmount = formatAmount(item.amount),
                            formattedDate = item.dueDate.format(dateFormatter),
                            isOverdue = item.dueDate.isBefore(today),
                            canMarkPaid = when (item.type) {
                                EcheanceType.INCOME -> canMarkPaidIncome
                                EcheanceType.EXPENSE -> canManageExpense
                            },
                            canEdit = when (item.type) {
                                EcheanceType.INCOME -> userRole == UserRole.ADMIN
                                EcheanceType.EXPENSE -> canManageExpense
                            },
                            onMarkPaid = {
                                when (item.type) {
                                    EcheanceType.INCOME -> item.invoiceId?.let { id ->
                                        invoices.find { it.id == id }?.let { invoice ->
                                            onRecordPayment(
                                                id,
                                                invoice.remainingAmount,
                                                LocalDate.now(),
                                                PaymentMethod.CASH
                                            ) { }
                                        }
                                    }
                                    EcheanceType.EXPENSE -> item.expenseId?.let { id ->
                                        expenses.find { it.id == id }?.let { expense ->
                                            onUpdateExpense(
                                                id,
                                                expense.label,
                                                expense.amount,
                                                item.dueDate,
                                                expense.isRecurring,
                                                expense.recurrence,
                                                expense.recurrenceEndDate,
                                                true
                                            )
                                        }
                                    }
                                }
                            },
                            onEdit = {
                                when (item.type) {
                                    EcheanceType.INCOME -> {
                                        invoiceToEdit = item.invoiceId?.let { id ->
                                            invoices.find { it.id == id }
                                        }
                                    }
                                    EcheanceType.EXPENSE -> {
                                        expenseToEdit = item.expenseId?.let { id ->
                                            expenses.find { it.id == id }
                                        }
                                    }
                                }
                            },
                            onDelete = { itemToDelete = item }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EcheanceRow(
    item: EcheanceItem,
    formattedAmount: String,
    formattedDate: String,
    isOverdue: Boolean,
    canMarkPaid: Boolean,
    canEdit: Boolean,
    onMarkPaid: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isIncome = item.type == EcheanceType.INCOME
    val accent = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
    val iconBg = accent.copy(alpha = 0.12f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A),
                    maxLines = 1
                )
                Text(
                    text = if (isOverdue) "$formattedDate · En retard" else formattedDate,
                    fontSize = 12.sp,
                    color = if (isOverdue) Color(0xFFF44336) else Color(0xFF9E9E9E)
                )
            }
            Text(
                text = formattedAmount,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color(0xFF9E9E9E))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    if (canMarkPaid) {
                        DropdownMenuItem(
                            text = { Text("Marquer comme payé") },
                            onClick = {
                                menuExpanded = false
                                onMarkPaid()
                            }
                        )
                    }
                    if (canEdit) {
                        DropdownMenuItem(
                            text = { Text("Modifier") },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                    }
                    if (canEdit) {
                        DropdownMenuItem(
                            text = { Text("Supprimer", color = Color(0xFFF44336)) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}
