package com.abccash.app.treasury.export

import com.abccash.app.treasury.data.Expense
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.data.appliesToMonth
import java.time.YearMonth
import java.time.format.DateTimeFormatter

object TreasuryCsvExporter {

    fun export(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        selectedMonth: YearMonth
    ): String {
        val monthInvoices = invoices.filter { YearMonth.from(it.dueDate) == selectedMonth }
        val monthExpenses = expenses.filter { it.appliesToMonth(selectedMonth) }

        return buildString {
            appendLine("# ABC Cash export - ${selectedMonth.format(DateTimeFormatter.ofPattern("MM/yyyy"))}")
            appendLine()
            appendLine("## FACTURES")
            appendLine("numero;client;montant_total;encaisse;reste;echeance;statut")
            monthInvoices.forEach { invoice ->
                appendLine(
                    listOf(
                        csvCell(invoice.invoiceNumber),
                        csvCell(invoice.clientName),
                        invoice.totalAmount,
                        invoice.paidAmount,
                        invoice.remainingAmount,
                        invoice.dueDate,
                        invoice.status.name
                    ).joinToString(";")
                )
            }
            appendLine()
            appendLine("## PAIEMENTS")
            appendLine("facture;client;montant;date;methode")
            monthInvoices.flatMap { invoice ->
                invoice.payments.map { payment -> invoice to payment }
            }.forEach { (invoice, payment) ->
                appendLine(
                    listOf(
                        csvCell(invoice.invoiceNumber),
                        csvCell(invoice.clientName),
                        payment.amount,
                        payment.date,
                        payment.method.name
                    ).joinToString(";")
                )
            }
            appendLine()
            appendLine("## DEPENSES")
            appendLine("libelle;montant;date;recurrente;recurrence;payee")
            monthExpenses.forEach { expense ->
                appendLine(
                    listOf(
                        csvCell(expense.label),
                        expense.amount,
                        expense.date,
                        expense.isRecurring,
                        expense.recurrence?.name.orEmpty(),
                        expense.isPaid
                    ).joinToString(";")
                )
            }
            appendLine()
            appendLine("## SYNTHESE")
            val collected = monthInvoices.flatMap { it.payments }
                .filter { YearMonth.from(it.date) == selectedMonth }
                .sumOf { it.amount }
            val totalExpenses = monthExpenses.sumOf { it.amount }
            appendLine("encaissements;$collected")
            appendLine("depenses;$totalExpenses")
            appendLine("solde;${collected - totalExpenses}")
        }
    }

    private fun csvCell(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(';') || escaped.contains('"') || escaped.contains('\n')) {
            "\"$escaped\""
        } else {
            escaped
        }
    }
}
