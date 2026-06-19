package com.abccash.server.service

import com.abccash.server.db.Entreprises
import com.abccash.server.db.Expenses
import com.abccash.server.db.Invoices
import com.abccash.server.db.Payments
import com.abccash.server.db.Users
import com.abccash.server.model.EntrepriseDto
import com.abccash.server.model.ExpenseDto
import com.abccash.server.model.InvoiceDto
import com.abccash.server.model.PaymentDto
import com.abccash.server.model.SyncPullResponse
import com.abccash.server.model.SyncPushRequest
import com.abccash.server.model.UserDto
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class SyncService {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun pull(entrepriseId: String): SyncPullResponse? = transaction {
        val entrepriseRow = Entreprises.selectAll()
            .where { Entreprises.id eq entrepriseId }
            .singleOrNull() ?: return@transaction null

        val users = Users.selectAll()
            .where { Users.entrepriseId eq entrepriseId }
            .map { row ->
                UserDto(
                    id = row[Users.id],
                    nom = row[Users.nom],
                    email = row[Users.email],
                    telephone = row[Users.telephone],
                    role = row[Users.role],
                    permissions = row[Users.permissions].split(',').filter { it.isNotBlank() },
                    entrepriseId = row[Users.entrepriseId]
                )
            }

        val invoiceRows = Invoices.selectAll()
            .where { Invoices.entrepriseId eq entrepriseId }
            .toList()
        val invoiceIds = invoiceRows.map { it[Invoices.id] }
        val paymentRows = if (invoiceIds.isEmpty()) {
            emptyList()
        } else {
            Payments.selectAll().where { Payments.invoiceId inList invoiceIds }.toList()
        }

        val invoices = invoiceRows.map { row ->
            val payments = paymentRows
                .filter { it[Payments.invoiceId] == row[Invoices.id] }
                .map { p ->
                    PaymentDto(
                        id = p[Payments.id],
                        invoiceId = p[Payments.invoiceId],
                        amount = p[Payments.amount],
                        date = p[Payments.date].format(dateFormatter),
                        method = p[Payments.method],
                        note = p[Payments.note]
                    )
                }
            InvoiceDto(
                id = row[Invoices.id],
                invoiceNumber = row[Invoices.invoiceNumber],
                clientName = row[Invoices.clientName],
                totalAmount = row[Invoices.totalAmount],
                dueDate = row[Invoices.dueDate].format(dateFormatter),
                createdDate = row[Invoices.createdDate].format(dateFormatter),
                entrepriseId = row[Invoices.entrepriseId],
                category = row[Invoices.category],
                categoryLabel = row[Invoices.categoryLabel],
                payments = payments
            )
        }

        val expenses = Expenses.selectAll()
            .where { Expenses.entrepriseId eq entrepriseId }
            .map { row ->
                ExpenseDto(
                    id = row[Expenses.id],
                    label = row[Expenses.label],
                    amount = row[Expenses.amount],
                    date = row[Expenses.date].format(dateFormatter),
                    isRecurring = row[Expenses.isRecurring],
                    recurrence = row[Expenses.recurrence],
                    recurrenceEndDate = row[Expenses.recurrenceEndDate]?.format(dateFormatter),
                    isPaid = row[Expenses.isPaid],
                    paymentMethod = row[Expenses.paymentMethod],
                    createdDate = row[Expenses.createdDate].format(dateFormatter),
                    entrepriseId = row[Expenses.entrepriseId],
                    category = row[Expenses.category],
                    categoryLabel = row[Expenses.categoryLabel]
                )
            }

        SyncPullResponse(
            entreprise = EntrepriseDto(
                id = entrepriseRow[Entreprises.id],
                nom = entrepriseRow[Entreprises.nom],
                email = entrepriseRow[Entreprises.email],
                telephone = entrepriseRow[Entreprises.telephone],
                adresse = entrepriseRow[Entreprises.adresse]
            ),
            users = users,
            invoices = invoices,
            expenses = expenses,
            serverTime = Instant.now().toString()
        )
    }

    fun push(entrepriseId: String, request: SyncPushRequest): String? = transaction {
        for (invoice in request.invoices) {
            if (invoice.entrepriseId != entrepriseId) return@transaction "Invoice belongs to another company"
            upsertInvoice(invoice)
        }
        for (expense in request.expenses) {
            if (expense.entrepriseId != entrepriseId) return@transaction "Expense belongs to another company"
            upsertExpense(expense)
        }
        null
    }

    private fun upsertInvoice(invoice: InvoiceDto) {
        val now = Instant.now()
        val exists = Invoices.selectAll().where { Invoices.id eq invoice.id }.count() > 0
        if (exists) {
            Invoices.update({ Invoices.id eq invoice.id }) {
                it[invoiceNumber] = invoice.invoiceNumber
                it[clientName] = invoice.clientName
                it[totalAmount] = invoice.totalAmount
                it[dueDate] = LocalDate.parse(invoice.dueDate)
                it[createdDate] = LocalDate.parse(invoice.createdDate)
                it[category] = invoice.category
                it[categoryLabel] = invoice.categoryLabel
                it[updatedAt] = now
            }
        } else {
            Invoices.insert {
                it[id] = invoice.id
                it[entrepriseId] = invoice.entrepriseId
                it[invoiceNumber] = invoice.invoiceNumber
                it[clientName] = invoice.clientName
                it[totalAmount] = invoice.totalAmount
                it[dueDate] = LocalDate.parse(invoice.dueDate)
                it[createdDate] = LocalDate.parse(invoice.createdDate)
                it[category] = invoice.category
                it[categoryLabel] = invoice.categoryLabel
                it[updatedAt] = now
            }
        }
        for (payment in invoice.payments) {
            val paymentExists = Payments.selectAll().where { Payments.id eq payment.id }.count() > 0
            if (paymentExists) {
                Payments.update({ Payments.id eq payment.id }) {
                    it[invoiceId] = payment.invoiceId
                    it[amount] = payment.amount
                    it[date] = LocalDate.parse(payment.date)
                    it[method] = payment.method
                    it[note] = payment.note
                    it[updatedAt] = now
                }
            } else {
                Payments.insert {
                    it[id] = payment.id
                    it[invoiceId] = payment.invoiceId
                    it[amount] = payment.amount
                    it[date] = LocalDate.parse(payment.date)
                    it[method] = payment.method
                    it[note] = payment.note
                    it[updatedAt] = now
                }
            }
        }
    }

    private fun upsertExpense(expense: ExpenseDto) {
        val now = Instant.now()
        val exists = Expenses.selectAll().where { Expenses.id eq expense.id }.count() > 0
        if (exists) {
            Expenses.update({ Expenses.id eq expense.id }) {
                it[label] = expense.label
                it[amount] = expense.amount
                it[date] = LocalDate.parse(expense.date)
                it[isRecurring] = expense.isRecurring
                it[recurrence] = expense.recurrence
                it[recurrenceEndDate] = expense.recurrenceEndDate?.let(LocalDate::parse)
                it[isPaid] = expense.isPaid
                it[paymentMethod] = expense.paymentMethod
                it[createdDate] = LocalDate.parse(expense.createdDate)
                it[category] = expense.category
                it[categoryLabel] = expense.categoryLabel
                it[updatedAt] = now
            }
        } else {
            Expenses.insert {
                it[id] = expense.id
                it[entrepriseId] = expense.entrepriseId
                it[label] = expense.label
                it[amount] = expense.amount
                it[date] = LocalDate.parse(expense.date)
                it[isRecurring] = expense.isRecurring
                it[recurrence] = expense.recurrence
                it[recurrenceEndDate] = expense.recurrenceEndDate?.let(LocalDate::parse)
                it[isPaid] = expense.isPaid
                it[paymentMethod] = expense.paymentMethod
                it[createdDate] = LocalDate.parse(expense.createdDate)
                it[category] = expense.category
                it[categoryLabel] = expense.categoryLabel
                it[updatedAt] = now
            }
        }
    }
}
