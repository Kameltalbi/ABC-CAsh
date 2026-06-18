package com.abccash.app.treasury.repository

import com.abccash.app.treasury.data.Entreprise
import com.abccash.app.treasury.data.Expense
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.data.Payment
import com.abccash.app.treasury.data.User
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import com.abccash.app.treasury.local.EntrepriseEntity
import com.abccash.app.treasury.local.ExpenseEntity
import com.abccash.app.treasury.local.InvoiceEntity
import com.abccash.app.treasury.local.PaymentEntity
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.abccash.app.treasury.local.TreasuryDao
import com.abccash.app.treasury.local.UserEntity
import com.abccash.app.treasury.security.PasswordHasher
import com.abccash.app.treasury.export.TreasuryBackupData
import com.abccash.app.treasury.export.TreasuryBackupJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.util.Locale

data class InvoiceImportStats(
    val imported: Int,
    val skippedDuplicates: Int
)

class TreasuryRepository(
    private val dao: TreasuryDao,
    private val database: RoomDatabase
) {

    suspend fun hasAnyUser(): Boolean = dao.countUsers() > 0

    suspend fun registerAdmin(entreprise: Entreprise, user: User, plainPassword: String): User {
        dao.upsertEntreprise(entreprise.toEntity())
        val admin = user.copy(
            email = normalizeEmail(user.email),
            telephone = normalizePhone(user.telephone),
            role = UserRole.ADMIN,
            permissions = UserPermission.entries.toSet(),
            passwordHash = PasswordHasher.hash(plainPassword),
            entrepriseId = entreprise.id
        )
        dao.upsertUser(admin.toEntity())
        dao.upsertEntreprise(entreprise.copy(adminId = admin.id).toEntity())
        return admin
    }

    suspend fun login(email: String, password: String): User? {
        val entity = dao.findUserByEmail(normalizeEmail(email)) ?: return null
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
        dao.findUserByEmail(normalizeEmail(email)) != null

    suspend fun isTelephoneTaken(telephone: String): Boolean =
        dao.findUserByTelephone(normalizePhone(telephone)) != null

    fun observeInvoices(entrepriseId: String): Flow<List<Invoice>> = combine(
        dao.observeInvoices(entrepriseId),
        dao.observePayments(entrepriseId)
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

    fun observeEntreprise(entrepriseId: String): Flow<Entreprise?> =
        dao.observeEntreprise(entrepriseId).map { it?.toDomain() }

    suspend fun updateUserProfile(
        userId: String,
        nom: String,
        email: String,
        telephone: String
    ): String? {
        val user = dao.findUserById(userId) ?: return "Utilisateur introuvable"
        val normalizedEmail = normalizeEmail(email)
        val normalizedPhone = normalizePhone(telephone)
        if (nom.isBlank()) return "Le nom est obligatoire"
        if (normalizedEmail.isBlank()) return "L'email est obligatoire"
        val emailTaken = dao.findUserByEmail(normalizedEmail)
        if (emailTaken != null && emailTaken.id != userId) return "Cet email est déjà utilisé"
        val phoneTaken = dao.findUserByTelephone(normalizedPhone)
        if (phoneTaken != null && phoneTaken.id != userId) return "Ce téléphone est déjà utilisé"
        dao.upsertUser(
            user.copy(
                nom = nom.trim(),
                email = normalizedEmail,
                telephone = normalizedPhone
            )
        )
        return null
    }

    suspend fun updateEntrepriseProfile(
        entrepriseId: String,
        nom: String,
        email: String,
        telephone: String,
        adresse: String
    ): String? {
        val entity = dao.findEntrepriseById(entrepriseId) ?: return "Entreprise introuvable"
        if (nom.isBlank()) return "Le nom de l'entreprise est obligatoire"
        dao.upsertEntreprise(
            entity.copy(
                nom = nom.trim(),
                email = email.trim(),
                telephone = normalizePhone(telephone),
                adresse = adresse.trim()
            )
        )
        return null
    }

    suspend fun addInvoice(invoice: Invoice): String? {
        if (invoice.invoiceNumber.isBlank()) return "Le numéro de facture est obligatoire"
        if (invoiceExists(invoice.entrepriseId, invoice.invoiceNumber)) {
            return "Ce numéro de facture existe déjà"
        }
        dao.upsertInvoice(invoice.toEntity())
        return null
    }

    suspend fun importInvoices(entrepriseId: String, invoices: List<Invoice>): InvoiceImportStats {
        val seenInFile = mutableSetOf<String>()
        var imported = 0
        var skippedDuplicates = 0

        for (invoice in invoices) {
            val normalizedNumber = normalizeInvoiceNumber(invoice.invoiceNumber)
            if (normalizedNumber.isBlank()) continue

            if (!seenInFile.add(normalizedNumber)) {
                skippedDuplicates++
                continue
            }
            if (invoiceExists(entrepriseId, normalizedNumber)) {
                skippedDuplicates++
                continue
            }

            dao.upsertInvoice(invoice.copy(entrepriseId = entrepriseId).toEntity())
            imported++
        }

        return InvoiceImportStats(imported = imported, skippedDuplicates = skippedDuplicates)
    }

    private suspend fun invoiceExists(entrepriseId: String, invoiceNumber: String): Boolean {
        return dao.findInvoiceByNumber(entrepriseId, normalizeInvoiceNumber(invoiceNumber)) != null
    }

    private fun normalizeInvoiceNumber(invoiceNumber: String): String {
        return invoiceNumber.trim().uppercase(Locale.ROOT)
    }

    suspend fun updateInvoice(invoice: Invoice): String? {
        if (invoice.invoiceNumber.isBlank()) return "Le numéro de facture est obligatoire"
        val existing = dao.findInvoiceByNumber(invoice.entrepriseId, normalizeInvoiceNumber(invoice.invoiceNumber))
        if (existing != null && existing.id != invoice.id) {
            return "Ce numéro de facture existe déjà"
        }
        if (invoice.totalAmount < invoice.paidAmount) {
            return "Le montant total ne peut pas être inférieur au montant déjà encaissé"
        }
        dao.upsertInvoice(invoice.toEntity())
        return null
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

    suspend fun addUser(user: User): String? {
        if (user.nom.isBlank()) return "Le nom est obligatoire"
        if (user.email.isBlank()) return "L'email est obligatoire"
        if (user.telephone.isBlank()) return "Le téléphone est obligatoire"
        if (user.passwordHash.length < 6) return "Le mot de passe doit contenir au moins 6 caractères"
        if (isEmailTaken(user.email)) return "Cet email est déjà utilisé"
        if (isTelephoneTaken(user.telephone)) return "Ce téléphone est déjà utilisé"
        dao.upsertUser(
            user.copy(
                email = normalizeEmail(user.email),
                telephone = normalizePhone(user.telephone),
                passwordHash = PasswordHasher.hash(user.passwordHash)
            ).toEntity()
        )
        return null
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

        val invoiceIds = backup.invoices.map { it.id }.toSet()
        val orphanPayments = backup.invoices.flatMap { it.payments }
            .filter { it.invoiceId !in invoiceIds }
        if (orphanPayments.isNotEmpty()) {
            return "Sauvegarde invalide : paiements sans facture associée"
        }

        database.withTransaction {
            backup.invoices.forEach { dao.upsertInvoice(it.toEntity()) }
            backup.invoices.flatMap { it.payments }.forEach { dao.upsertPayment(it.toEntity()) }
            backup.expenses.forEach { dao.upsertExpense(it.toEntity()) }
            backup.users.forEach { dao.upsertUser(it.toEntity()) }
        }
        return null
    }
}

private fun normalizeEmail(email: String): String =
    email.trim().lowercase(Locale.ROOT)

private fun normalizePhone(telephone: String): String =
    telephone.replace("\\s".toRegex(), "")

private fun Entreprise.toEntity(): EntrepriseEntity = EntrepriseEntity(
    id = id,
    nom = nom,
    email = email,
    telephone = telephone,
    adresse = adresse,
    dateCreation = dateCreation,
    adminId = adminId
)

private fun EntrepriseEntity.toDomain(): Entreprise = Entreprise(
    id = id,
    nom = nom,
    email = email,
    telephone = telephone,
    adresse = adresse,
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
    payments = payments,
    category = category,
    categoryLabel = categoryLabel
)

private fun Invoice.toEntity(): InvoiceEntity = InvoiceEntity(
    id = id,
    invoiceNumber = invoiceNumber,
    clientName = clientName,
    totalAmount = totalAmount,
    dueDate = dueDate,
    createdDate = createdDate,
    entrepriseId = entrepriseId,
    category = category,
    categoryLabel = categoryLabel
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
    entrepriseId = entrepriseId,
    category = category,
    categoryLabel = categoryLabel
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
    entrepriseId = entrepriseId,
    category = category,
    categoryLabel = categoryLabel
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
