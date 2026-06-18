package com.abccash.app.treasury.repository

import com.abccash.app.treasury.data.Entreprise
import com.abccash.app.treasury.data.Expense
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.data.Payment
import com.abccash.app.treasury.data.User
import com.abccash.app.treasury.data.UserRole
import com.abccash.app.treasury.local.EntrepriseEntity
import com.abccash.app.treasury.local.ExpenseEntity
import com.abccash.app.treasury.local.InvoiceEntity
import com.abccash.app.treasury.local.PaymentEntity
import com.abccash.app.treasury.local.TreasuryDao
import com.abccash.app.treasury.local.UserEntity
import com.abccash.app.treasury.security.PasswordHasher
import com.abccash.app.treasury.export.TreasuryBackupData
import com.abccash.app.treasury.export.TreasuryBackupJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class TreasuryRepository(private val dao: TreasuryDao) {

    suspend fun hasAnyUser(): Boolean = dao.countUsers() > 0

    suspend fun registerAdmin(entreprise: Entreprise, user: User): User {
        dao.upsertEntreprise(entreprise.toEntity())
        val admin = user.copy(
            role = UserRole.ADMIN,
            passwordHash = PasswordHasher.hash(user.passwordHash),
            entrepriseId = entreprise.id
        )
        dao.upsertUser(admin.toEntity())
        dao.upsertEntreprise(entreprise.copy(adminId = admin.id).toEntity())
        return admin
    }

    suspend fun login(email: String, password: String): User? {
        val entity = dao.findUserByEmail(email.trim()) ?: return null
        if (!entity.isActive) return null
        return if (PasswordHasher.verify(password, entity.passwordHash)) {
            if (PasswordHasher.needsUpgrade(entity.passwordHash)) {
                dao.upsertUser(entity.copy(passwordHash = PasswordHasher.hash(password)))
            }
            entity.toDomain()
        } else {
            null
        }
    }

    suspend fun isEmailTaken(email: String): Boolean =
        dao.findUserByEmail(email.trim()) != null

    suspend fun isTelephoneTaken(telephone: String): Boolean =
        dao.findUserByTelephone(telephone.trim()) != null

    fun observeInvoices(entrepriseId: String): Flow<List<Invoice>> = combine(
        dao.observeInvoices(entrepriseId),
        dao.observePayments()
    ) { invoiceEntities, paymentEntities ->
        invoiceEntities.map { invoice ->
            val payments = paymentEntities
                .filter { it.invoiceId == invoice.id }
                .map { it.toDomain() }
            invoice.toDomain(payments)
        }
    }

    fun observeExpenses(entrepriseId: String): Flow<List<Expense>> =
        dao.observeExpenses(entrepriseId).map { entities -> entities.map { it.toDomain() } }

    fun observeUsers(entrepriseId: String): Flow<List<User>> =
        dao.observeUsers(entrepriseId).map { entities -> entities.map { it.toDomain() } }

    suspend fun addInvoice(invoice: Invoice) {
        dao.upsertInvoice(invoice.toEntity())
    }

    suspend fun updateInvoice(invoice: Invoice) {
        dao.upsertInvoice(invoice.toEntity())
    }

    suspend fun deleteInvoice(invoiceId: String) {
        dao.deleteInvoiceById(invoiceId)
    }

    suspend fun addPayment(payment: Payment) {
        dao.upsertPayment(payment.toEntity())
    }

    suspend fun addExpense(expense: Expense) {
        dao.upsertExpense(expense.toEntity())
    }

    suspend fun updateExpense(expense: Expense) {
        dao.upsertExpense(expense.toEntity())
    }

    suspend fun deleteExpense(expenseId: String) {
        dao.deleteExpenseById(expenseId)
    }

    suspend fun addUser(user: User) {
        dao.upsertUser(
            user.copy(passwordHash = PasswordHasher.hash(user.passwordHash)).toEntity()
        )
    }

    suspend fun deleteUser(userId: String) {
        dao.deleteUserById(userId)
    }

    suspend fun getUserById(userId: String): User? =
        dao.findUserById(userId)?.toDomain()

    suspend fun changePassword(userId: String, currentPassword: String, newPassword: String): String? {
        val user = dao.findUserById(userId) ?: return "Utilisateur introuvable"
        if (!PasswordHasher.verify(currentPassword, user.passwordHash)) {
            return "Mot de passe actuel incorrect"
        }
        if (newPassword.length < 6) {
            return "Le nouveau mot de passe doit contenir au moins 6 caractères"
        }
        dao.upsertUser(user.copy(passwordHash = PasswordHasher.hash(newPassword)))
        return null
    }

    suspend fun resetUserPassword(userId: String, newPassword: String): String? {
        val user = dao.findUserById(userId) ?: return "Utilisateur introuvable"
        if (newPassword.length < 6) {
            return "Le mot de passe doit contenir au moins 6 caractères"
        }
        dao.upsertUser(user.copy(passwordHash = PasswordHasher.hash(newPassword)))
        return null
    }

    suspend fun exportBackup(entrepriseId: String): String? {
        val entreprise = dao.findEntrepriseById(entrepriseId) ?: return null
        val invoiceEntities = dao.getInvoicesForBackup(entrepriseId)
        val invoiceIds = invoiceEntities.map { it.id }
        val paymentEntities = if (invoiceIds.isEmpty()) {
            emptyList()
        } else {
            dao.getPaymentsForInvoices(invoiceIds)
        }
        val invoices = invoiceEntities.map { invoice ->
            val payments = paymentEntities
                .filter { it.invoiceId == invoice.id }
                .map { it.toDomain() }
            invoice.toDomain(payments)
        }
        val expenses = dao.getExpensesForBackup(entrepriseId).map { it.toDomain() }
        val users = dao.getUsersForBackup(entrepriseId).map { it.toDomain() }

        val backup = TreasuryBackupData(
            version = TreasuryBackupJson.CURRENT_VERSION,
            exportedAt = LocalDateTime.now(),
            entrepriseId = entreprise.id,
            entrepriseNom = entreprise.nom,
            invoices = invoices,
            expenses = expenses,
            users = users
        )
        return TreasuryBackupJson.toJson(backup)
    }

    suspend fun restoreBackup(entrepriseId: String, json: String): String? {
        val backup = runCatching { TreasuryBackupJson.fromJson(json) }
            .getOrElse { return "Fichier de sauvegarde invalide: ${it.message}" }

        if (backup.entrepriseId != entrepriseId) {
            return "Cette sauvegarde appartient à une autre entreprise"
        }

        backup.invoices.forEach { dao.upsertInvoice(it.toEntity()) }
        backup.invoices.flatMap { it.payments }.forEach { dao.upsertPayment(it.toEntity()) }
        backup.expenses.forEach { dao.upsertExpense(it.toEntity()) }
        backup.users.forEach { dao.upsertUser(it.toEntity()) }
        return null
    }
}

private fun Entreprise.toEntity(): EntrepriseEntity = EntrepriseEntity(
    id = id,
    nom = nom,
    dateCreation = dateCreation,
    adminId = adminId
)

private fun InvoiceEntity.toDomain(payments: List<Payment>): Invoice = Invoice(
    id = id,
    invoiceNumber = invoiceNumber,
    clientName = clientName,
    totalAmount = totalAmount,
    paidAmount = payments.sumOf { it.amount },
    dueDate = dueDate,
    createdDate = createdDate,
    entrepriseId = entrepriseId,
    payments = payments
)

private fun Invoice.toEntity(): InvoiceEntity = InvoiceEntity(
    id = id,
    invoiceNumber = invoiceNumber,
    clientName = clientName,
    totalAmount = totalAmount,
    dueDate = dueDate,
    createdDate = createdDate,
    entrepriseId = entrepriseId
)

private fun PaymentEntity.toDomain(): Payment = Payment(
    id = id,
    invoiceId = invoiceId,
    amount = amount,
    date = date,
    method = method,
    note = note
)

private fun Payment.toEntity(): PaymentEntity = PaymentEntity(
    id = id,
    invoiceId = invoiceId,
    amount = amount,
    date = date,
    method = method,
    note = note
)

private fun ExpenseEntity.toDomain(): Expense = Expense(
    id = id,
    label = label,
    amount = amount,
    date = date,
    isRecurring = isRecurring,
    recurrence = recurrence,
    recurrenceEndDate = recurrenceEndDate,
    isPaid = isPaid,
    createdDate = createdDate,
    entrepriseId = entrepriseId
)

private fun Expense.toEntity(): ExpenseEntity = ExpenseEntity(
    id = id,
    label = label,
    amount = amount,
    date = date,
    isRecurring = isRecurring,
    recurrence = recurrence,
    recurrenceEndDate = recurrenceEndDate,
    isPaid = isPaid,
    createdDate = createdDate,
    entrepriseId = entrepriseId
)

private fun UserEntity.toDomain(): User = User(
    id = id,
    nom = nom,
    email = email,
    telephone = telephone,
    passwordHash = passwordHash,
    role = role,
    permissions = permissions,
    entrepriseId = entrepriseId,
    dateInscription = dateInscription,
    isActive = isActive
)

private fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    nom = nom,
    email = email,
    telephone = telephone,
    passwordHash = passwordHash,
    role = role,
    permissions = permissions,
    entrepriseId = entrepriseId,
    dateInscription = dateInscription,
    isActive = isActive
)
