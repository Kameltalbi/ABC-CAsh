package com.abccash.app.treasury.repository

import com.abccash.app.treasury.data.BankAccount
import com.abccash.app.treasury.data.BankAccountSource
import com.abccash.app.treasury.data.Contact
import com.abccash.app.treasury.data.TaxIdValidationStatus
import com.abccash.app.treasury.data.Entreprise
import com.abccash.app.treasury.data.Expense
import com.abccash.app.treasury.data.Invoice
import com.abccash.app.treasury.data.InvoiceDocumentStatus
import com.abccash.app.treasury.data.InvoiceLineItemCodec
import com.abccash.app.treasury.data.InvoiceNumberGenerator
import com.abccash.app.treasury.data.InvoiceSettings
import com.abccash.app.treasury.data.Payment
import com.abccash.app.treasury.data.Product
import com.abccash.app.treasury.data.ProductKind
import com.abccash.app.treasury.data.Quote
import com.abccash.app.treasury.data.QuoteStatus
import com.abccash.app.treasury.data.affectsBankTreasury
import com.abccash.app.treasury.data.User
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import com.abccash.app.treasury.data.SubscriptionPlan
import com.abccash.app.treasury.data.UserSubscription
import com.abccash.app.treasury.local.BankAccountEntity
import com.abccash.app.treasury.local.ContactEntity
import com.abccash.app.treasury.local.EntrepriseEntity
import com.abccash.app.treasury.local.ExpenseEntity
import com.abccash.app.treasury.local.InvoiceEntity
import com.abccash.app.treasury.local.PaymentEntity
import com.abccash.app.treasury.local.ProductEntity
import com.abccash.app.treasury.local.QuoteEntity
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.abccash.app.treasury.local.TreasuryDao
import com.abccash.app.treasury.local.UserEntity
import com.abccash.app.treasury.security.PasswordHasher
import com.abccash.app.treasury.datastore.UserPreferences
import com.abccash.app.treasury.export.TreasuryBackupData
import com.abccash.app.treasury.export.TreasuryBackupJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Locale

data class InvoiceImportStats(
    val imported: Int,
    val skippedDuplicates: Int
)

class TreasuryRepository(
    private val dao: TreasuryDao,
    private val database: RoomDatabase,
    private val userPreferences: UserPreferences
) {

    companion object {
        const val SUBSCRIPTION_LIMIT_REACHED = "SUBSCRIPTION_LIMIT_REACHED"
    }

    suspend fun hasAnyUser(): Boolean = dao.countUsers() > 0

    suspend fun getSoloOwner(): User? = dao.findFirstUser()?.toDomain()

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

    suspend fun isEmailTaken(email: String): Boolean =
        dao.findUserByEmail(normalizeEmail(email)) != null

    suspend fun needsAccountCredentialsSetup(): Boolean {
        val user = dao.findFirstUser() ?: return false
        return user.email.endsWith(".local", ignoreCase = true)
    }

    suspend fun completeAccountSetup(
        email: String,
        telephone: String,
        plainPassword: String
    ): String? {
        if (plainPassword.length < 6) return "Le mot de passe doit contenir au moins 6 caractères"
        val entity = dao.findFirstUser() ?: return "Aucun compte trouvé"
        val normalizedEmail = normalizeEmail(email)
        if (normalizedEmail.isBlank()) return "L'e-mail est obligatoire"
        val existing = dao.findUserByEmail(normalizedEmail)
        if (existing != null && existing.id != entity.id) return "Cet e-mail est déjà utilisé"
        val phone = normalizePhone(telephone)
        if (phone.isNotBlank()) {
            val phoneOwner = dao.findUserByTelephone(phone)
            if (phoneOwner != null && phoneOwner.id != entity.id) return "Ce numéro est déjà utilisé"
        }
        dao.upsertUser(
            entity.copy(
                email = normalizedEmail,
                telephone = phone,
                passwordHash = PasswordHasher.hash(plainPassword)
            )
        )
        return null
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

    fun observeQuotes(entrepriseId: String): Flow<List<Quote>> =
        dao.observeQuotes(entrepriseId).map { entities -> entities.map { it.toDomain() } }

    fun observeExpenses(entrepriseId: String): Flow<List<Expense>> =
        dao.observeExpenses(entrepriseId).map { entities -> entities.map { it.toDomain() } }

    fun observeUsers(entrepriseId: String): Flow<List<User>> =
        dao.observeUsers(entrepriseId).map { entities -> entities.map { it.toDomain() } }

    fun observeEntreprise(entrepriseId: String): Flow<Entreprise?> =
        dao.observeEntreprise(entrepriseId).map { it?.toDomain() }

    fun observeBankAccounts(entrepriseId: String): Flow<List<BankAccount>> =
        dao.observeBankAccounts(entrepriseId).map { entities -> entities.map { it.toDomain() } }

    suspend fun getDefaultBankAccount(entrepriseId: String): BankAccount? =
        dao.findDefaultBankAccount(entrepriseId)?.toDomain()

    suspend fun getBankAccount(accountId: String): BankAccount? =
        dao.findBankAccountById(accountId)?.toDomain()

    suspend fun saveBankAccount(account: BankAccount): String? {
        if (account.name.isBlank()) return "Le nom du compte est requis"
        database.withTransaction {
            if (account.isDefault) {
                dao.clearDefaultBankAccounts(account.entrepriseId)
            }
            dao.upsertBankAccount(account.toEntity())
        }
        return null
    }

    suspend fun deleteBankAccount(accountId: String) {
        dao.deleteBankAccountById(accountId)
    }

    fun observeContacts(entrepriseId: String): Flow<List<Contact>> =
        dao.observeContacts(entrepriseId).map { entities -> entities.map { it.toDomain() } }

    suspend fun getContact(contactId: String): Contact? =
        dao.findContactById(contactId)?.toDomain()

    suspend fun saveContact(contact: Contact): String? {
        if (contact.name.isBlank() && contact.legalName.isBlank()) return "Le nom est requis"
        val resolvedName = contact.name.ifBlank { contact.legalName }
        val resolvedLegal = contact.legalName.ifBlank { resolvedName }
        val billingAddress = contact.billingAddressFormatted
        dao.upsertContact(
            contact.copy(
                name = resolvedName,
                legalName = resolvedLegal,
                address = billingAddress,
                taxIdValidationStatus = TaxIdValidationStatus.UNVERIFIED
            ).toEntity()
        )
        return null
    }

    suspend fun deleteContact(contactId: String) {
        dao.deleteContactById(contactId)
    }

    fun observeProducts(entrepriseId: String): Flow<List<Product>> =
        dao.observeProducts(entrepriseId).map { entities -> entities.map { it.toDomain() } }

    suspend fun saveProduct(product: Product): String? {
        if (product.name.isBlank()) return "Le nom du produit est obligatoire"
        if (product.unitPriceExclTax <= 0) return "Le prix doit être supérieur à 0"
        if (product.categoryLabel.isBlank()) return "La catégorie est obligatoire"
        dao.upsertProduct(product.toEntity())
        return null
    }

    suspend fun deleteProduct(productId: String) {
        dao.deactivateProduct(productId)
    }

    suspend fun resolveBankAccountIdForBankPayment(entrepriseId: String): String? =
        getDefaultBankAccount(entrepriseId)?.id

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
        if (normalizedPhone.isNotBlank()) {
            val phoneTaken = dao.findUserByTelephone(normalizedPhone)
            if (phoneTaken != null && phoneTaken.id != userId) return "Ce téléphone est déjà utilisé"
        }
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
        if (invoice.entrepriseId.isBlank()) return "L'ID entreprise est obligatoire"
        if (!canAddTransaction(invoice.entrepriseId)) return SUBSCRIPTION_LIMIT_REACHED
        if (invoice.clientName.isBlank()) return "Le nom du client est obligatoire"
        if (invoice.totalAmount <= 0) return "Le montant total doit être supérieur à 0"
        if (invoice.paidAmount < 0) return "Le montant payé ne peut pas être négatif"
        if (invoice.totalAmount < invoice.paidAmount) {
            return "Le montant total ne peut pas être inférieur au montant déjà encaissé"
        }
        return when (invoice.documentStatus) {
            InvoiceDocumentStatus.DRAFT -> {
                if (invoice.invoiceNumber.isNotBlank()) {
                    return "Un brouillon ne doit pas avoir de numéro de facture"
                }
                dao.upsertInvoice(invoice.toEntity())
                null
            }
            InvoiceDocumentStatus.VALIDATED -> {
                if (invoice.invoiceNumber.isBlank()) return "Le numéro de facture est obligatoire"
                if (invoiceExists(invoice.entrepriseId, invoice.invoiceNumber)) {
                    return "Ce numéro de facture existe déjà"
                }
                dao.upsertInvoice(invoice.toEntity())
                null
            }
        }
    }

    suspend fun saveInvoiceDraft(invoice: Invoice): String? =
        addInvoice(invoice.copy(documentStatus = InvoiceDocumentStatus.DRAFT, invoiceNumber = ""))

    suspend fun validateInvoice(
        invoiceId: String,
        settings: InvoiceSettings
    ): String? {
        val entity = dao.findInvoiceById(invoiceId) ?: return "Facture introuvable"
        if (entity.documentStatus == InvoiceDocumentStatus.VALIDATED) {
            return "Cette facture est déjà validée"
        }
        val payments = dao.getPaymentsForInvoices(listOf(invoiceId))
        val invoice = entity.toDomain(payments.map { it.toDomain() })
        if (invoice.clientName.isBlank()) return "Le nom du client est obligatoire"
        if (invoice.totalAmount <= 0) return "Le montant total doit être supérieur à 0"

        val number = InvoiceNumberGenerator.nextNumber(
            prefix = settings.prefix,
            year = invoice.createdDate.year,
            existingNumbers = dao.getValidatedInvoiceNumbers(invoice.entrepriseId)
        )
        val validated = invoice.copy(
            invoiceNumber = number,
            documentStatus = InvoiceDocumentStatus.VALIDATED
        )
        if (invoiceExists(validated.entrepriseId, number)) {
            return "Ce numéro de facture existe déjà"
        }
        dao.upsertInvoice(validated.toEntity())
        return null
    }

    suspend fun nextInvoiceNumber(entrepriseId: String, year: Int, settings: InvoiceSettings): String =
        InvoiceNumberGenerator.nextNumber(
            prefix = settings.prefix,
            year = year,
            existingNumbers = dao.getValidatedInvoiceNumbers(entrepriseId)
        )

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
        val existingEntity = dao.findInvoiceById(invoice.id) ?: return "Facture introuvable"
        val existing = existingEntity.toDomain(
            dao.getPaymentsForInvoices(listOf(invoice.id)).map { it.toDomain() }
        )
        if (invoice.clientName.isBlank()) return "Le nom du client est obligatoire"
        if (invoice.totalAmount <= 0) return "Le montant total doit être supérieur à 0"
        if (invoice.totalAmount < invoice.paidAmount) {
            return "Le montant total ne peut pas être inférieur au montant déjà encaissé"
        }

        val invoiceToSave = when (existing.documentStatus) {
            InvoiceDocumentStatus.VALIDATED -> invoice.copy(
                invoiceNumber = existing.invoiceNumber,
                documentStatus = InvoiceDocumentStatus.VALIDATED
            )
            InvoiceDocumentStatus.DRAFT -> invoice.copy(
                invoiceNumber = "",
                documentStatus = InvoiceDocumentStatus.DRAFT
            )
        }

        if (invoiceToSave.documentStatus == InvoiceDocumentStatus.VALIDATED) {
            val duplicate = dao.findInvoiceByNumber(
                invoiceToSave.entrepriseId,
                normalizeInvoiceNumber(invoiceToSave.invoiceNumber)
            )
            if (duplicate != null && duplicate.id != invoiceToSave.id) {
                return "Ce numéro de facture existe déjà"
            }
        }

        dao.upsertInvoice(invoiceToSave.toEntity())
        return null
    }

    suspend fun deleteInvoice(invoiceId: String): String? {
        if (invoiceId.isBlank()) return "L'ID facture est obligatoire"
        if (dao.findInvoiceById(invoiceId) == null) return "Facture introuvable"
        dao.deleteInvoiceById(invoiceId)
        return null
    }

    suspend fun saveQuoteDraft(quote: Quote): String? =
        addQuote(quote.copy(status = QuoteStatus.DRAFT, quoteNumber = ""))

    suspend fun addQuote(quote: Quote): String? {
        if (quote.entrepriseId.isBlank()) return "L'ID entreprise est obligatoire"
        if (quote.clientName.isBlank()) return "Le nom du client est obligatoire"
        if (quote.totalAmount <= 0) return "Le montant total doit être supérieur à 0"
        return when (quote.status) {
            QuoteStatus.DRAFT -> {
                if (quote.quoteNumber.isNotBlank()) {
                    return "Un brouillon ne doit pas avoir de numéro de devis"
                }
                dao.upsertQuote(quote.toEntity())
                null
            }
            QuoteStatus.SENT -> {
                if (quote.quoteNumber.isBlank()) return "Le numéro de devis est obligatoire"
                if (quoteExists(quote.entrepriseId, quote.quoteNumber)) {
                    return "Ce numéro de devis existe déjà"
                }
                dao.upsertQuote(quote.toEntity())
                null
            }
            else -> {
                dao.upsertQuote(quote.toEntity())
                null
            }
        }
    }

    suspend fun validateQuote(quoteId: String, settings: InvoiceSettings): String? {
        val entity = dao.findQuoteById(quoteId) ?: return "Devis introuvable"
        if (entity.status != QuoteStatus.DRAFT) return "Ce devis est déjà validé"
        val quote = entity.toDomain()
        if (quote.clientName.isBlank()) return "Le nom du client est obligatoire"
        if (quote.totalAmount <= 0) return "Le montant total doit être supérieur à 0"

        val number = InvoiceNumberGenerator.nextNumber(
            prefix = settings.quotePrefix,
            year = quote.issueDate.year,
            existingNumbers = dao.getIssuedQuoteNumbers(quote.entrepriseId)
        )
        val validated = quote.copy(quoteNumber = number, status = QuoteStatus.SENT)
        if (quoteExists(validated.entrepriseId, number)) {
            return "Ce numéro de devis existe déjà"
        }
        dao.upsertQuote(validated.toEntity())
        return null
    }

    suspend fun nextQuoteNumber(entrepriseId: String, year: Int, settings: InvoiceSettings): String =
        InvoiceNumberGenerator.nextNumber(
            prefix = settings.quotePrefix,
            year = year,
            existingNumbers = dao.getIssuedQuoteNumbers(entrepriseId)
        )

    suspend fun updateQuote(quote: Quote): String? {
        val existingEntity = dao.findQuoteById(quote.id) ?: return "Devis introuvable"
        val existing = existingEntity.toDomain()
        if (quote.clientName.isBlank()) return "Le nom du client est obligatoire"
        if (quote.totalAmount <= 0) return "Le montant total doit être supérieur à 0"

        val quoteToSave = when (existing.status) {
            QuoteStatus.DRAFT -> quote.copy(quoteNumber = "", status = QuoteStatus.DRAFT)
            QuoteStatus.SENT, QuoteStatus.ACCEPTED, QuoteStatus.REFUSED, QuoteStatus.CONVERTED -> quote.copy(
                quoteNumber = existing.quoteNumber,
                status = existing.status,
                convertedInvoiceId = existing.convertedInvoiceId
            )
        }

        if (quoteToSave.status != QuoteStatus.DRAFT && quoteToSave.quoteNumber.isNotBlank()) {
            val duplicate = dao.findQuoteByNumber(quoteToSave.entrepriseId, quoteToSave.quoteNumber)
            if (duplicate != null && duplicate.id != quoteToSave.id) {
                return "Ce numéro de devis existe déjà"
            }
        }

        dao.upsertQuote(quoteToSave.toEntity())
        return null
    }

    suspend fun updateQuoteStatus(quoteId: String, status: QuoteStatus): String? {
        val entity = dao.findQuoteById(quoteId) ?: return "Devis introuvable"
        if (entity.status == QuoteStatus.DRAFT) return "Validez le devis avant de changer son statut"
        if (entity.status == QuoteStatus.CONVERTED) return "Un devis converti ne peut plus être modifié"
        if (status == QuoteStatus.DRAFT) return "Statut invalide"
        if (status in listOf(QuoteStatus.ACCEPTED, QuoteStatus.REFUSED) && entity.status != QuoteStatus.SENT) {
            return "Seuls les devis envoyés peuvent être acceptés ou refusés"
        }
        dao.upsertQuote(entity.copy(status = status))
        return null
    }

    suspend fun deleteQuote(quoteId: String): String? {
        if (quoteId.isBlank()) return "L'ID devis est obligatoire"
        val entity = dao.findQuoteById(quoteId) ?: return "Devis introuvable"
        if (entity.status != QuoteStatus.DRAFT) {
            return "Seuls les brouillons peuvent être supprimés"
        }
        dao.deleteQuoteById(quoteId)
        return null
    }

    suspend fun convertQuoteToInvoice(quoteId: String, settings: InvoiceSettings): String? {
        val entity = dao.findQuoteById(quoteId) ?: return "Devis introuvable"
        val quote = entity.toDomain()
        if (quote.status != QuoteStatus.ACCEPTED) {
            return "Seuls les devis acceptés peuvent être convertis en facture"
        }
        if (quote.convertedInvoiceId != null) return "Ce devis a déjà été converti"

        val invoiceNumber = InvoiceNumberGenerator.nextNumber(
            prefix = settings.prefix,
            year = LocalDate.now().year,
            existingNumbers = dao.getValidatedInvoiceNumbers(quote.entrepriseId)
        )
        val invoice = Invoice(
            clientName = quote.clientName,
            clientContactId = quote.clientContactId,
            totalAmount = quote.totalAmount,
            dueDate = quote.validUntil,
            entrepriseId = quote.entrepriseId,
            category = quote.category,
            categoryLabel = quote.categoryLabel,
            documentStatus = InvoiceDocumentStatus.VALIDATED,
            invoiceNumber = invoiceNumber,
            amountExclTax = quote.amountExclTax,
            tvaRate = quote.tvaRate,
            otherTaxRate = quote.otherTaxRate,
            otherTaxMode = quote.otherTaxMode,
            otherTaxLabel = quote.otherTaxLabel,
            lineItems = quote.lineItems
        )
        val error = addInvoice(invoice)
        if (error != null) return error
        dao.upsertQuote(
            quote.copy(status = QuoteStatus.CONVERTED, convertedInvoiceId = invoice.id).toEntity()
        )
        return null
    }

    private suspend fun quoteExists(entrepriseId: String, quoteNumber: String): Boolean =
        dao.findQuoteByNumber(entrepriseId, normalizeQuoteNumber(quoteNumber)) != null

    private fun normalizeQuoteNumber(quoteNumber: String): String =
        quoteNumber.trim().uppercase(Locale.ROOT)

    suspend fun addPayment(payment: Payment): String? {
        if (payment.invoiceId.isBlank()) return "L'ID facture est obligatoire"
        if (payment.amount <= 0) return "Le montant du paiement doit être supérieur à 0"
        val entrepriseId = dao.findInvoiceById(payment.invoiceId)?.entrepriseId.orEmpty()
        val paymentToSave = if (payment.affectsBankTreasury() && payment.bankAccountId == null) {
            payment.copy(bankAccountId = resolveBankAccountIdForBankPayment(entrepriseId))
        } else {
            payment
        }
        dao.upsertPayment(paymentToSave.toEntity())
        return null
    }

    suspend fun addExpense(expense: Expense): String? {
        if (expense.label.isBlank()) return "Le libellé de la dépense est obligatoire"
        if (expense.entrepriseId.isBlank()) return "L'ID entreprise est obligatoire"
        if (!canAddTransaction(expense.entrepriseId)) return SUBSCRIPTION_LIMIT_REACHED
        if (expense.amount <= 0) return "Le montant de la dépense doit être supérieur à 0"
        val expenseToSave = if (expense.affectsBankTreasury() && expense.bankAccountId == null) {
            expense.copy(bankAccountId = resolveBankAccountIdForBankPayment(expense.entrepriseId))
        } else {
            expense
        }
        dao.upsertExpense(expenseToSave.toEntity())
        return null
    }

    suspend fun updateExpense(expense: Expense) {
        dao.upsertExpense(expense.toEntity())
    }

    suspend fun deleteExpense(expenseId: String): String? {
        if (expenseId.isBlank()) return "L'ID dépense est obligatoire"
        dao.deleteExpenseById(expenseId)
        return null
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

        return applyBackup(backup)
    }

    suspend fun importInitialBackup(json: String): Result<User> {
        if (hasAnyUser()) {
            return Result.failure(IllegalStateException("L'application est déjà configurée"))
        }
        val backup = runCatching { TreasuryBackupJson.fromJson(json) }
            .getOrElse { return Result.failure(IllegalArgumentException("Fichier invalide: ${it.message}")) }

        val owner = backup.users.firstOrNull { it.role == UserRole.ADMIN }
            ?: backup.users.firstOrNull()
            ?: return Result.failure(IllegalArgumentException("Sauvegarde sans utilisateur"))

        database.withTransaction {
            dao.upsertEntreprise(
                Entreprise(
                    id = backup.entrepriseId,
                    nom = backup.entrepriseNom,
                    email = owner.email,
                    telephone = owner.telephone,
                    adminId = owner.id
                ).toEntity()
            )
            val invoiceIds = backup.invoices.map { it.id }.toSet()
            val orphanPayments = backup.invoices.flatMap { it.payments }
                .filter { it.invoiceId !in invoiceIds }
            if (orphanPayments.isNotEmpty()) {
                throw IllegalArgumentException("Sauvegarde invalide : paiements sans facture associée")
            }
            backup.invoices.forEach { dao.upsertInvoice(it.toEntity()) }
            backup.invoices.flatMap { it.payments }.forEach { dao.upsertPayment(it.toEntity()) }
            backup.expenses.forEach { dao.upsertExpense(it.toEntity()) }
            backup.users.forEach { dao.upsertUser(it.toEntity()) }
        }
        return Result.success(owner)
    }

    private suspend fun applyBackup(backup: TreasuryBackupData): String? {
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

    // Subscription management
    suspend fun getUserSubscription(entrepriseId: String): UserSubscription {
        val currentMonth = YearMonth.now()
        val transactionsThisMonth = countTransactionsThisMonth(entrepriseId, currentMonth)
        val plan = userPreferences.readSubscriptionPlan()
        return UserSubscription(
            plan = plan,
            transactionsThisMonth = transactionsThisMonth
        )
    }

    suspend fun canAddTransaction(entrepriseId: String): Boolean {
        val subscription = getUserSubscription(entrepriseId)
        return !subscription.isTransactionLimitReached
    }

    suspend fun deleteAccountData(entrepriseId: String): String? {
        return try {
            database.withTransaction {
                dao.deletePaymentsForEntreprise(entrepriseId)
                dao.deleteInvoicesForEntreprise(entrepriseId)
                dao.deleteExpensesForEntreprise(entrepriseId)
                dao.deleteQuotesForEntreprise(entrepriseId)
                dao.deleteBankAccountsForEntreprise(entrepriseId)
                dao.deleteContactsForEntreprise(entrepriseId)
                dao.deactivateProductsForEntreprise(entrepriseId)
                dao.deleteUsersForEntreprise(entrepriseId)
                dao.deleteEntrepriseById(entrepriseId)
            }
            null
        } catch (e: Exception) {
            e.message ?: "Erreur lors de la suppression"
        }
    }

    private suspend fun countTransactionsThisMonth(entrepriseId: String, month: YearMonth): Int {
        val invoices = dao.getInvoicesForBackup(entrepriseId)
        val expenses = dao.getExpensesForBackup(entrepriseId)
        
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()
        
        // Count real transactions (invoices + expenses)
        val invoiceCount = invoices.count {
            it.dueDate >= monthStart && it.dueDate <= monthEnd
        }
        val expenseCount = expenses.count {
            it.date >= monthStart && it.date <= monthEnd
        }

        return invoiceCount + expenseCount
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
    clientContactId = clientContactId,
    totalAmount = totalAmount,
    paidAmount = payments.sumOf { it.amount },
    dueDate = dueDate,
    createdDate = createdDate,
    entrepriseId = entrepriseId,
    payments = payments,
    category = category,
    categoryLabel = categoryLabel,
    documentStatus = documentStatus,
    amountExclTax = amountExclTax,
    tvaRate = tvaRate,
    otherTaxRate = otherTaxRate,
    otherTaxMode = otherTaxMode,
    otherTaxLabel = otherTaxLabel,
    lineItems = InvoiceLineItemCodec.decode(lineItemsJson)
)

private fun Invoice.toEntity(): InvoiceEntity = InvoiceEntity(
    id = id,
    invoiceNumber = invoiceNumber,
    clientName = clientName,
    clientContactId = clientContactId,
    totalAmount = totalAmount,
    dueDate = dueDate,
    createdDate = createdDate,
    entrepriseId = entrepriseId,
    category = category,
    categoryLabel = categoryLabel,
    documentStatus = documentStatus,
    amountExclTax = amountExclTax,
    tvaRate = tvaRate,
    otherTaxRate = otherTaxRate,
    otherTaxMode = otherTaxMode,
    otherTaxLabel = otherTaxLabel,
    lineItemsJson = InvoiceLineItemCodec.encode(lineItems)
)

private fun QuoteEntity.toDomain(): Quote = Quote(
    id = id,
    quoteNumber = quoteNumber,
    clientName = clientName,
    clientContactId = clientContactId,
    totalAmount = totalAmount,
    issueDate = issueDate,
    validUntil = validUntil,
    createdDate = createdDate,
    entrepriseId = entrepriseId,
    category = category,
    categoryLabel = categoryLabel,
    status = status,
    amountExclTax = amountExclTax,
    tvaRate = tvaRate,
    otherTaxRate = otherTaxRate,
    otherTaxMode = otherTaxMode,
    otherTaxLabel = otherTaxLabel,
    lineItems = InvoiceLineItemCodec.decode(lineItemsJson),
    convertedInvoiceId = convertedInvoiceId,
    notes = notes
)

private fun Quote.toEntity(): QuoteEntity = QuoteEntity(
    id = id,
    quoteNumber = quoteNumber,
    clientName = clientName,
    clientContactId = clientContactId,
    totalAmount = totalAmount,
    issueDate = issueDate,
    validUntil = validUntil,
    createdDate = createdDate,
    entrepriseId = entrepriseId,
    category = category,
    categoryLabel = categoryLabel,
    status = status,
    amountExclTax = amountExclTax,
    tvaRate = tvaRate,
    otherTaxRate = otherTaxRate,
    otherTaxMode = otherTaxMode,
    otherTaxLabel = otherTaxLabel,
    lineItemsJson = InvoiceLineItemCodec.encode(lineItems),
    convertedInvoiceId = convertedInvoiceId,
    notes = notes
)

private fun ProductEntity.toDomain(): Product = Product(
    id = id,
    entrepriseId = entrepriseId,
    name = name,
    unitPriceExclTax = unitPriceExclTax,
    kind = kind,
    unit = unit,
    category = category,
    categoryLabel = categoryLabel,
    isActive = isActive,
    createdDate = createdDate
)

private fun Product.toEntity(): ProductEntity = ProductEntity(
    id = id,
    entrepriseId = entrepriseId,
    name = name,
    unitPriceExclTax = unitPriceExclTax,
    kind = kind,
    unit = unit,
    category = category,
    categoryLabel = categoryLabel,
    isActive = isActive,
    createdDate = createdDate
)

private fun PaymentEntity.toDomain(): Payment = Payment(
    id = id,
    invoiceId = invoiceId,
    amount = amount,
    date = date,
    method = method,
    note = note,
    bankAccountId = bankAccountId
)

private fun Payment.toEntity(): PaymentEntity = PaymentEntity(
    id = id,
    invoiceId = invoiceId,
    amount = amount,
    date = date,
    method = method,
    note = note,
    bankAccountId = bankAccountId
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
    paymentMethod = paymentMethod,
    bankAccountId = bankAccountId,
    createdDate = createdDate,
    entrepriseId = entrepriseId,
    category = category,
    categoryLabel = categoryLabel,
    supplierContactId = supplierContactId,
    note = note,
    receiptImagePath = receiptImagePath,
    isExpenseNote = isExpenseNote
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
    paymentMethod = paymentMethod,
    bankAccountId = bankAccountId,
    createdDate = createdDate,
    entrepriseId = entrepriseId,
    category = category,
    categoryLabel = categoryLabel,
    supplierContactId = supplierContactId,
    note = note,
    receiptImagePath = receiptImagePath,
    isExpenseNote = isExpenseNote
)

private fun ContactEntity.toDomain(): Contact = Contact(
    id = id,
    entrepriseId = entrepriseId,
    type = type,
    name = name,
    email = email,
    phone = phone,
    address = address,
    notes = notes,
    countryCode = countryCode,
    legalName = legalName,
    taxIdType = taxIdType,
    taxIdValue = taxIdValue,
    taxIdValidationStatus = taxIdValidationStatus,
    addressLine1 = addressLine1,
    addressLine2 = addressLine2,
    postalCode = postalCode,
    city = city,
    createdDate = createdDate
)

private fun Contact.toEntity(): ContactEntity = ContactEntity(
    id = id,
    entrepriseId = entrepriseId,
    type = type,
    name = name,
    email = email,
    phone = phone,
    address = address,
    notes = notes,
    countryCode = countryCode,
    legalName = legalName,
    taxIdType = taxIdType,
    taxIdValue = taxIdValue,
    taxIdValidationStatus = taxIdValidationStatus,
    addressLine1 = addressLine1,
    addressLine2 = addressLine2,
    postalCode = postalCode,
    city = city,
    createdDate = createdDate
)

private fun BankAccountEntity.toDomain(): BankAccount = BankAccount(
    id = id,
    entrepriseId = entrepriseId,
    name = name,
    bankName = bankName,
    ibanLast4 = ibanLast4,
    openingBalance = openingBalance,
    alertLowBalance = alertLowBalance,
    isDefault = isDefault,
    source = source,
    createdDate = createdDate
)

private fun BankAccount.toEntity(): BankAccountEntity = BankAccountEntity(
    id = id,
    entrepriseId = entrepriseId,
    name = name,
    bankName = bankName,
    ibanLast4 = ibanLast4,
    openingBalance = openingBalance,
    alertLowBalance = alertLowBalance,
    isDefault = isDefault,
    source = source,
    createdDate = createdDate
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
