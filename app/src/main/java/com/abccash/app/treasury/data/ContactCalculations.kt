package com.abccash.app.treasury.data

object ContactCalculations {

    fun invoicesForContact(contact: Contact, invoices: List<Invoice>): List<Invoice> =
        invoices.filter { invoice ->
            invoice.clientContactId == contact.id ||
                (invoice.clientContactId == null &&
                    invoice.clientName.equals(contact.name, ignoreCase = true))
        }

    fun expensesForContact(contact: Contact, expenses: List<Expense>): List<Expense> =
        expenses.filter { expense ->
            expense.supplierContactId == contact.id ||
                (expense.supplierContactId == null &&
                    expense.label.equals(contact.name, ignoreCase = true))
        }

    fun summarize(contact: Contact, invoices: List<Invoice>, expenses: List<Expense>): ContactSummary {
        val linkedInvoices = invoicesForContact(contact, invoices)
        val linkedExpenses = expensesForContact(contact, expenses)
        val count = when (contact.type) {
            ContactType.CLIENT -> linkedInvoices.size
            ContactType.SUPPLIER -> linkedExpenses.size
        }
        val total = when (contact.type) {
            ContactType.CLIENT -> linkedInvoices.sumOf { it.totalAmount }
            ContactType.SUPPLIER -> linkedExpenses.sumOf { it.amount }
        }
        return ContactSummary(contact = contact, transactionCount = count, totalAmount = total)
    }
}
