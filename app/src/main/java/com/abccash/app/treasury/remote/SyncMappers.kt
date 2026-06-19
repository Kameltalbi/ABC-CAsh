package com.abccash.app.treasury.remote

import com.abccash.app.treasury.data.Entreprise
import com.abccash.app.treasury.data.Expense
import com.abccash.app.treasury.data.ExpenseCategory
import com.abccash.app.treasury.data.ExpenseRecurrence
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.data.Payment
import com.abccash.app.treasury.data.PaymentMethod
import com.abccash.app.treasury.data.RevenueCategory
import com.abccash.app.treasury.data.User
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import java.time.LocalDate
import java.time.LocalDateTime

private val dateFormatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE

fun EntrepriseDto.toDomain(): Entreprise = Entreprise(
    id = id,
    nom = nom,
    email = email,
    telephone = telephone,
    adresse = adresse,
    dateCreation = LocalDateTime.now()
)

fun UserDto.toDomain(passwordHash: String): User = User(
    id = id,
    nom = nom,
    email = email,
    telephone = telephone,
    passwordHash = passwordHash,
    role = runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.STAFF),
    permissions = permissions.mapNotNull {
        runCatching { UserPermission.valueOf(it) }.getOrNull()
    }.toSet(),
    entrepriseId = entrepriseId,
    dateInscription = LocalDateTime.now(),
    isActive = true
)

fun InvoiceDto.toDomain(): Invoice {
    val parsedPayments = payments.map { it.toDomain() }
    return Invoice(
        id = id,
        invoiceNumber = invoiceNumber,
        clientName = clientName,
        totalAmount = totalAmount,
        paidAmount = parsedPayments.sumOf { it.amount },
        dueDate = LocalDate.parse(dueDate),
        createdDate = LocalDate.parse(createdDate),
        entrepriseId = entrepriseId,
        payments = parsedPayments,
        category = runCatching { RevenueCategory.valueOf(category) }.getOrDefault(RevenueCategory.OTHER),
        categoryLabel = categoryLabel
    )
}

fun PaymentDto.toDomain(): Payment = Payment(
    id = id,
    invoiceId = invoiceId,
    amount = amount,
    date = LocalDate.parse(date),
    method = runCatching { PaymentMethod.valueOf(method) }.getOrDefault(PaymentMethod.TRANSFER),
    note = note
)

fun ExpenseDto.toDomain(): Expense = Expense(
    id = id,
    label = label,
    amount = amount,
    date = LocalDate.parse(date),
    isRecurring = isRecurring,
    recurrence = recurrence?.let { runCatching { ExpenseRecurrence.valueOf(it) }.getOrNull() },
    recurrenceEndDate = recurrenceEndDate?.let(LocalDate::parse),
    isPaid = isPaid,
    paymentMethod = paymentMethod?.let { runCatching { PaymentMethod.valueOf(it) }.getOrNull() },
    createdDate = LocalDate.parse(createdDate),
    entrepriseId = entrepriseId,
    category = runCatching { ExpenseCategory.valueOf(category) }.getOrDefault(ExpenseCategory.OTHER),
    categoryLabel = categoryLabel
)

fun Invoice.toDto(): InvoiceDto = InvoiceDto(
    id = id,
    invoiceNumber = invoiceNumber,
    clientName = clientName,
    totalAmount = totalAmount,
    dueDate = dueDate.format(dateFormatter),
    createdDate = createdDate.format(dateFormatter),
    entrepriseId = entrepriseId,
    category = category.name,
    categoryLabel = categoryLabel,
    payments = payments.map { it.toDto() }
)

fun Payment.toDto(): PaymentDto = PaymentDto(
    id = id,
    invoiceId = invoiceId,
    amount = amount,
    date = date.format(dateFormatter),
    method = method.name,
    note = note
)

fun Expense.toDto(): ExpenseDto = ExpenseDto(
    id = id,
    label = label,
    amount = amount,
    date = date.format(dateFormatter),
    isRecurring = isRecurring,
    recurrence = recurrence?.name,
    recurrenceEndDate = recurrenceEndDate?.format(dateFormatter),
    isPaid = isPaid,
    paymentMethod = paymentMethod?.name,
    createdDate = createdDate.format(dateFormatter),
    entrepriseId = entrepriseId,
    category = category.name,
    categoryLabel = categoryLabel
)
