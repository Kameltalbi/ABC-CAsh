package com.abccash.app.treasury.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.treasury.data.Expense
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.data.PaymentMethod
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import com.abccash.app.treasury.data.forMonth
import com.abccash.app.treasury.data.hasPermission
import com.abccash.app.treasury.data.occurrenceDateIn
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class TransactionRow(
    val date: LocalDate,
    val invoice: Invoice? = null,
    val expense: Expense? = null
) {
    val isIncome: Boolean get() = invoice != null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    userRole: UserRole,
    permissions: Set<UserPermission>,
    invoices: List<Invoice>,
    expenses: List<Expense>,
    selectedMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    importFeedback: String? = null,
    onClearImportFeedback: () -> Unit = {},
    onNavigateToImport: () -> Unit,
    onNavigateToAddIncome: () -> Unit,
    onNavigateToAddExpense: () -> Unit,
    onUpdateInvoice: (String, String, String, Double, LocalDate, (String?) -> Unit) -> Unit,
    onRecordPayment: (String, Double, LocalDate, PaymentMethod, (String?) -> Unit) -> Unit,
    onDeleteInvoice: (String) -> Unit,
    onUpdateExpense: (String, String, Double, LocalDate, Boolean, com.abccash.app.treasury.data.ExpenseRecurrence?, LocalDate?, Boolean) -> Unit,
    onStopRecurrence: (String, LocalDate) -> Unit,
    onDeleteExpense: (String) -> Unit
) {
    val canViewIncome = hasPermission(userRole, permissions, UserPermission.VIEW_INVOICES)
    val canManageExpense = hasPermission(userRole, permissions, UserPermission.MANAGE_EXPENSES)
    val isAdmin = userRole == UserRole.ADMIN
    val canAddPayment = hasPermission(userRole, permissions, UserPermission.ADD_PAYMENTS)
    val canAdd = isAdmin || canManageExpense

    if (!canViewIncome && !canManageExpense) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Accès refusé", color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
        }
        return
    }

    var showTypeSheet by remember { mutableStateOf(false) }
    var invoiceToEdit by remember { mutableStateOf<Invoice?>(null) }
    var invoiceToDelete by remember { mutableStateOf<Invoice?>(null) }
    var invoiceForPartialPayment by remember { mutableStateOf<Invoice?>(null) }
    var invoiceToMarkPaid by remember { mutableStateOf<Invoice?>(null) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var paymentError by remember { mutableStateOf<String?>(null) }
    var editError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(importFeedback) {
        if (importFeedback != null) {
            delay(4000)
            onClearImportFeedback()
        }
    }

    val monthLabel = remember(selectedMonth) {
        selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH))
            .replaceFirstChar { it.uppercase() }
    }

    val rows = remember(invoices, expenses, selectedMonth, canViewIncome, canManageExpense) {
        val incomeRows = if (canViewIncome) {
            invoices.filter { YearMonth.from(it.dueDate) == selectedMonth }
                .map { TransactionRow(date = it.dueDate, invoice = it) }
        } else emptyList()
        val expenseRows = if (canManageExpense) {
            expenses.forMonth(selectedMonth).map { expense ->
                TransactionRow(
                    date = expense.occurrenceDateIn(selectedMonth) ?: expense.date,
                    expense = expense
                )
            }
        } else emptyList()
        (incomeRows + expenseRows).sortedByDescending { it.date }
    }

    Scaffold(
        floatingActionButton = {
            if (canAdd) {
                FloatingActionButton(
                    onClick = { showTypeSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nouvelle transaction", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Transactions", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(monthLabel, fontSize = 13.sp, color = Color.Gray)
                }
                if (isAdmin) {
                    IconButton(onClick = onNavigateToImport) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Importer")
                    }
                }
            }

            MonthSelectorRow(selectedMonth = selectedMonth, onMonthChange = onMonthChange)

            importFeedback?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    color = Color(0xFF4CAF50)
                )
            }

            if (rows.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aucune transaction ce mois-ci", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rows, key = {
                        when {
                            it.invoice != null -> "inv-${it.invoice.id}"
                            it.expense != null -> "exp-${it.expense.id}"
                            else -> it.date.toString()
                        }
                    }) { row ->
                        when {
                            row.invoice != null -> InvoiceCard(
                                invoice = row.invoice,
                                isAdmin = isAdmin,
                                canAddPayment = canAddPayment,
                                onMarkPaid = { invoiceToMarkPaid = row.invoice },
                                onPartialPayment = {
                                    paymentError = null
                                    invoiceForPartialPayment = row.invoice
                                },
                                onEdit = { invoiceToEdit = row.invoice },
                                onDelete = { invoiceToDelete = row.invoice }
                            )
                            row.expense != null -> ExpenseItem(
                                expense = row.expense,
                                displayMonth = selectedMonth,
                                canManage = canManageExpense,
                                onEdit = { expenseToEdit = row.expense },
                                onDelete = { expenseToDelete = row.expense }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTypeSheet) {
        TransactionTypeChoiceSheet(
            canAddIncome = isAdmin,
            canAddExpense = canManageExpense,
            onDismiss = { showTypeSheet = false },
            onSelectIncome = {
                showTypeSheet = false
                onNavigateToAddIncome()
            },
            onSelectExpense = {
                showTypeSheet = false
                onNavigateToAddExpense()
            }
        )
    }

    invoiceToMarkPaid?.let { invoice ->
        AlertDialog(
            onDismissRequest = { invoiceToMarkPaid = null },
            title = { Text("Marquer comme soldé") },
            text = { Text("Enregistrer le paiement total de ${formatMoney(invoice.remainingAmount)} ?") },
            confirmButton = {
                TextButton(onClick = {
                    onRecordPayment(
                        invoice.id,
                        invoice.remainingAmount,
                        LocalDate.now(),
                        PaymentMethod.CASH
                    ) { error ->
                        if (error == null) invoiceToMarkPaid = null else paymentError = error
                    }
                }) { Text("Confirmer") }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToMarkPaid = null }) { Text("Annuler") }
            }
        )
    }

    invoiceForPartialPayment?.let { invoice ->
        PartialPaymentDialog(
            invoice = invoice,
            errorMessage = paymentError,
            onDismiss = {
                invoiceForPartialPayment = null
                paymentError = null
            },
            onConfirm = { amount, date, method ->
                onRecordPayment(invoice.id, amount, date, method) { error ->
                    if (error == null) {
                        invoiceForPartialPayment = null
                        paymentError = null
                    } else {
                        paymentError = error
                    }
                }
            }
        )
    }

    invoiceToEdit?.let { invoice ->
        InvoiceFormDialog(
            title = "Modifier encaissement",
            initialInvoice = invoice,
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

    invoiceToDelete?.let { invoice ->
        AlertDialog(
            onDismissRequest = { invoiceToDelete = null },
            title = { Text("Supprimer l'encaissement") },
            text = { Text("Supprimer ${invoice.clientName} ?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteInvoice(invoice.id)
                    invoiceToDelete = null
                }) { Text("Supprimer", color = Color(0xFFF44336)) }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToDelete = null }) { Text("Annuler") }
            }
        )
    }

    expenseToEdit?.let { expense ->
        ExpenseFormDialog(
            initialExpense = expense,
            selectedMonth = selectedMonth,
            onDismiss = { expenseToEdit = null },
            onConfirm = { label, amount, date, isRecurring, recurrence, recurrenceEndDate, isPaid ->
                onUpdateExpense(expense.id, label, amount, date, isRecurring, recurrence, recurrenceEndDate, isPaid)
                expenseToEdit = null
            },
            onStopRecurrence = { endDate ->
                onStopRecurrence(expense.id, endDate)
                expenseToEdit = null
            }
        )
    }

    expenseToDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Supprimer la dépense") },
            text = { Text("Supprimer ${expense.label} ?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteExpense(expense.id)
                    expenseToDelete = null
                }) { Text("Supprimer", color = Color(0xFFF44336)) }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) { Text("Annuler") }
            }
        )
    }
}
