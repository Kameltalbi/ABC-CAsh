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
    ): String = exportYear(invoices, expenses, selectedMonth.year)

    fun exportYear(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        year: Int
    ): String {
        val yearInvoices = invoices.filter { YearMonth.from(it.dueDate).year == year }
        val yearExpenses = (1..12)
            .flatMap { monthNumber ->
                val month = YearMonth.of(year, monthNumber)
                expenses.filter { it.appliesToMonth(month) }
            }
            .distinctBy { it.id }

        return buildString {
            appendLine("# ABC Cash export - année $year")
            appendLine()
            appendLine("## FACTURES")
            appendLine("numero;client;montant_total;encaisse;reste;echeance;statut")
            yearInvoices.forEach { invoice ->
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
            invoices.flatMap { invoice ->
                invoice.payments
                    .filter { YearMonth.from(it.date).year == year }
                    .map { payment -> invoice to payment }
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
            yearExpenses.forEach { expense ->
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
            val collected = com.abccash.app.treasury.data.TreasuryCalculations.yearlyCollections(invoices, year)
            val totalExpenses = com.abccash.app.treasury.data.TreasuryCalculations.yearlyPaidExpenses(expenses, year)
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
