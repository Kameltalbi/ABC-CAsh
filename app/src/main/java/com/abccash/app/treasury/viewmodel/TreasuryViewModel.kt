package com.abccash.app.treasury.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abccash.app.treasury.data.*
import com.abccash.app.treasury.data.BalanceCorrection
import com.abccash.app.treasury.data.BalanceCorrectionType
import com.abccash.app.treasury.backup.GoogleBackupManager
import com.abccash.app.treasury.datastore.UserPreferences
import com.abccash.app.treasury.export.TreasuryCsvExporter
import com.abccash.app.treasury.importer.BankStatementEntry
import com.abccash.app.treasury.repository.TreasuryRepository
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import java.time.format.DateTimeFormatter

data class TreasuryUiState(
    val currentUserId: String? = null,
    val currentUserRole: UserRole = UserRole.STAFF,
    val permissions: Set<UserPermission> = emptySet(),
    val entrepriseId: String? = null,
    val entreprise: Entreprise? = null,
    val users: List<User> = emptyList(),
    val invoices: List<Invoice> = emptyList(),
    val quotes: List<Quote> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val bankAccounts: List<BankAccount> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val selectedMonth: YearMonth = YearMonth.now(),
    val importFeedback: ImportFeedback? = null,
    val backupFeedback: String? = null,
    val subscription: UserSubscription = UserSubscription()
)

class TreasuryViewModel(
    private val repository: TreasuryRepository,
    private val googleBackupManager: GoogleBackupManager,
    private val userPreferences: UserPreferences
) : ViewModel() {
    private val _entrepriseId = MutableStateFlow<String?>(null)
    private val _uiState = MutableStateFlow(TreasuryUiState())
    val uiState: StateFlow<TreasuryUiState> = _uiState.asStateFlow()
    private var autoBackupJob: Job? = null

    init {
        initDataObservers()
    }

    fun bankAccountSummaries(): List<BankAccountSummary> {
        val state = _uiState.value
        return BankAccountCalculations.summarize(state.bankAccounts, state.invoices, state.expenses)
    }

    fun bankAccountMovements(accountId: String): List<BankAccountMovement> {
        val state = _uiState.value
        val account = state.bankAccounts.firstOrNull { it.id == accountId } ?: return emptyList()
        val defaultBankId = BankAccountCalculations.defaultAccountId(state.bankAccounts, TreasuryAccountKind.BANK)
        val defaultCashId = BankAccountCalculations.defaultAccountId(state.bankAccounts, TreasuryAccountKind.CASH)
        return BankAccountCalculations.movements(account, state.invoices, state.expenses, defaultBankId, defaultCashId)
    }

    fun bankAccountBalance(accountId: String): Double {
        val state = _uiState.value
        val account = state.bankAccounts.firstOrNull { it.id == accountId } ?: return 0.0
        val defaultBankId = BankAccountCalculations.defaultAccountId(state.bankAccounts, TreasuryAccountKind.BANK)
        val defaultCashId = BankAccountCalculations.defaultAccountId(state.bankAccounts, TreasuryAccountKind.CASH)
        return BankAccountCalculations.balance(account, state.invoices, state.expenses, defaultBankId, defaultCashId)
    }

    fun observeTreasuryInitialized(entrepriseId: String) =
        repository.observeTreasuryInitialized(entrepriseId)

    fun observeBalanceCorrections(entrepriseId: String) =
        repository.observeBalanceCorrections(entrepriseId)

    fun initTreasury(
        entrepriseId: String,
        bankAccountId: String,
        initialBalance: Double,
        balanceDate: java.time.LocalDate,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val state = _uiState.value
            val userId = state.currentUserId ?: ""
            val userName = state.users.firstOrNull { it.id == userId }?.nom
                ?: state.entreprise?.nom ?: ""
            val error = repository.initTreasury(
                entrepriseId = entrepriseId,
                bankAccountId = bankAccountId,
                initialBalance = initialBalance,
                balanceDate = balanceDate,
                userId = userId,
                userName = userName
            )
            onResult(error)
        }
    }

    fun saveBalanceCorrection(
        entrepriseId: String,
        bankAccountId: String,
        oldBalance: Double,
        newBalance: Double,
        correctionDate: java.time.LocalDate,
        motif: String,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val state = _uiState.value
            val userId = state.currentUserId ?: ""
            val userName = state.users.firstOrNull { it.id == userId }?.nom
                ?: state.entreprise?.nom ?: ""
            val correction = BalanceCorrection(
                entrepriseId = entrepriseId,
                bankAccountId = bankAccountId,
                type = BalanceCorrectionType.CORRECTION,
                oldBalance = oldBalance,
                newBalance = newBalance,
                correctionDate = correctionDate,
                motif = motif,
                userId = userId,
                userName = userName
            )
            val error = repository.saveBalanceCorrection(correction)
            onResult(error)
            if (error == null) scheduleGoogleBackup()
        }
    }

    fun saveBankAccount(account: BankAccount, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val error = repository.saveBankAccount(account)
            onResult(error)
            if (error == null) {
                refreshSubscription()
                scheduleGoogleBackup()
            }
        }
    }

    fun deleteBankAccount(accountId: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            repository.deleteBankAccount(accountId)
            onResult(null)
            scheduleGoogleBackup()
        }
    }

    fun getBankAccount(accountId: String): BankAccount? =
        _uiState.value.bankAccounts.firstOrNull { it.id == accountId }

    private fun initDataObservers() {
        viewModelScope.launch {
            _entrepriseId.flatMapLatest { id ->
                if (id == null) {
                    kotlinx.coroutines.flow.flowOf(emptyList())
                } else {
                    repository.observeInvoices(id)
                }
            }.collect { invoices ->
                _uiState.update { it.copy(invoices = invoices) }
            }
        }
        viewModelScope.launch {
            _entrepriseId.flatMapLatest { id ->
                if (id == null) {
                    kotlinx.coroutines.flow.flowOf(emptyList())
                } else {
                    repository.observeQuotes(id)
                }
            }.collect { quotes ->
                _uiState.update { it.copy(quotes = quotes) }
            }
        }
        viewModelScope.launch {
            _entrepriseId.flatMapLatest { id ->
                if (id == null) {
                    kotlinx.coroutines.flow.flowOf(emptyList())
                } else {
                    repository.observeExpenses(id)
                }
            }.collect { expenses ->
                _uiState.update { it.copy(expenses = expenses) }
            }
        }
        viewModelScope.launch {
            _entrepriseId.flatMapLatest { id ->
                if (id == null) {
                    kotlinx.coroutines.flow.flowOf(emptyList())
                } else {
                    repository.observeUsers(id)
                }
            }.collect { users ->
                _uiState.update { it.copy(users = users) }
            }
        }
        viewModelScope.launch {
            _entrepriseId.flatMapLatest { id ->
                if (id == null) {
                    kotlinx.coroutines.flow.flowOf(null)
                } else {
                    repository.observeEntreprise(id)
                }
            }.collect { entreprise ->
                _uiState.update { it.copy(entreprise = entreprise) }
            }
        }
        viewModelScope.launch {
            _entrepriseId.flatMapLatest { id ->
                if (id == null) {
                    kotlinx.coroutines.flow.flowOf(emptyList())
                } else {
                    repository.observeBankAccounts(id)
                }
            }.collect { accounts ->
                _uiState.update { it.copy(bankAccounts = accounts) }
            }
        }
        viewModelScope.launch {
            _entrepriseId.flatMapLatest { id ->
                if (id == null) {
                    kotlinx.coroutines.flow.flowOf(emptyList())
                } else {
                    repository.observeContacts(id)
                }
            }.collect { contacts ->
                _uiState.update { it.copy(contacts = contacts) }
            }
        }
    }

    fun contactSummaries(type: ContactType): List<ContactSummary> {
        val state = _uiState.value
        return state.contacts
            .filter { it.type == type }
            .map { ContactCalculations.summarize(it, state.invoices, state.expenses) }
    }

    fun getContact(contactId: String): Contact? =
        _uiState.value.contacts.firstOrNull { it.id == contactId }

    fun contactInvoices(contactId: String): List<Invoice> {
        val contact = getContact(contactId) ?: return emptyList()
        return ContactCalculations.invoicesForContact(contact, _uiState.value.invoices)
    }

    fun contactExpenses(contactId: String): List<Expense> {
        val contact = getContact(contactId) ?: return emptyList()
        return ContactCalculations.expensesForContact(contact, _uiState.value.expenses)
    }

    fun expenseNotes(): List<Expense> =
        _uiState.value.expenses.filter { it.isExpenseNote }

    fun saveContact(contact: Contact, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val error = repository.saveContact(contact)
            onResult(error)
            if (error == null) scheduleGoogleBackup()
        }
    }

    fun deleteContact(contactId: String, onResult: () -> Unit) {
        viewModelScope.launch {
            repository.deleteContact(contactId)
            onResult()
            scheduleGoogleBackup()
        }
    }

    fun setSession(
        entrepriseId: String,
        role: UserRole,
        permissions: Set<UserPermission>,
        userId: String? = null
    ) {
        _entrepriseId.value = entrepriseId
        _uiState.update {
            it.copy(
                entrepriseId = entrepriseId,
                currentUserRole = role,
                permissions = permissions,
                currentUserId = userId
            )
        }
        refreshSubscription()
    }

    fun syncSubscriptionPlan(plan: SubscriptionPlan) {
        viewModelScope.launch {
            userPreferences.saveSubscriptionPlan(plan)
            refreshSubscription()
        }
    }

    fun clearSession() {
        autoBackupJob?.cancel()
        _entrepriseId.value = null
        _uiState.value = TreasuryUiState()
    }

    fun refreshSubscription() {
        viewModelScope.launch {
            val entrepriseId = _entrepriseId.value ?: return@launch
            val subscription = repository.getUserSubscription(entrepriseId)
            _uiState.update { it.copy(subscription = subscription) }
        }
    }

    fun deleteAccountAndData(deleteDriveBackup: Boolean, onResult: (String?) -> Unit) {
        val entrepriseId = requireEntrepriseId()
        if (entrepriseId == null) {
            onResult(TreasuryMessage.SESSION_EXPIRED)
            return
        }
        viewModelScope.launch {
            if (deleteDriveBackup && googleBackupManager.isSignedIn()) {
                googleBackupManager.deleteBackup()
                    .onFailure { error ->
                        onResult(error.message ?: TreasuryMessage.GOOGLE_DELETE_BACKUP_FAILED)
                        return@launch
                    }
            }
            val error = repository.deleteAccountData(entrepriseId)
            if (error == null) {
                googleBackupManager.signOut()
                userPreferences.clearGoogleAccount()
                userPreferences.clearSubscriptionPlan()
                clearSession()
            }
            onResult(error)
        }
    }

    /** Sauvegarde automatique sur Google Drive si l'utilisateur est connecté. */
    private fun scheduleGoogleBackup() {
        val entrepriseId = _entrepriseId.value ?: return
        if (!googleBackupManager.isSignedIn()) return
        autoBackupJob?.cancel()
        autoBackupJob = viewModelScope.launch {
            delay(1_500)
            val json = repository.exportBackup(entrepriseId) ?: return@launch
            googleBackupManager.uploadBackup(json).onSuccess {
                userPreferences.setGoogleLastBackupAt(Instant.now().toString())
            }
        }
    }

    fun backupToGoogle(onResult: (String?) -> Unit) {
        val entrepriseId = requireEntrepriseId()
        if (entrepriseId == null) {
            onResult(TreasuryMessage.SESSION_INACTIVE)
            return
        }
        if (!googleBackupManager.isSignedIn()) {
            onResult(TreasuryMessage.CONNECT_GOOGLE)
            return
        }
        viewModelScope.launch {
            val json = repository.exportBackup(entrepriseId)
            if (json == null) {
                onResult(TreasuryMessage.EXPORT_DATA_FAILED)
                return@launch
            }
            googleBackupManager.uploadBackup(json)
                .onSuccess {
                    userPreferences.setGoogleLastBackupAt(Instant.now().toString())
                    _uiState.update { it.copy(backupFeedback = TreasuryMessage.GOOGLE_BACKUP_SUCCESS) }
                    onResult(null)
                }
                .onFailure { onResult(it.message ?: TreasuryMessage.GOOGLE_DRIVE_ERROR) }
        }
    }

    fun restoreFromGoogle(onResult: (String?) -> Unit) {
        if (!googleBackupManager.isSignedIn()) {
            onResult(TreasuryMessage.CONNECT_GOOGLE)
            return
        }
        viewModelScope.launch {
            val jsonResult = googleBackupManager.downloadBackup()
            val json = jsonResult.getOrElse {
                onResult(it.message ?: TreasuryMessage.GOOGLE_DRIVE_ERROR)
                return@launch
            }
            if (json.isNullOrBlank()) {
                onResult(TreasuryMessage.GOOGLE_NO_BACKUP)
                return@launch
            }
            val entrepriseId = requireEntrepriseId()
            if (entrepriseId == null) {
                onResult(TreasuryMessage.SESSION_INACTIVE)
                return@launch
            }
            val error = repository.restoreBackup(entrepriseId, json)
            if (error == null) {
                _uiState.update { it.copy(backupFeedback = TreasuryMessage.GOOGLE_DATA_RESTORED) }
            }
            onResult(error)
        }
    }

    fun restoreInitialFromGoogle(onResult: (User?, String?) -> Unit) {
        if (!googleBackupManager.isSignedIn()) {
            onResult(null, TreasuryMessage.CONNECT_GOOGLE)
            return
        }
        viewModelScope.launch {
            val jsonResult = googleBackupManager.downloadBackup()
            val json = jsonResult.getOrElse {
                onResult(null, it.message ?: TreasuryMessage.GOOGLE_DRIVE_ERROR)
                return@launch
            }
            if (json.isNullOrBlank()) {
                onResult(null, TreasuryMessage.GOOGLE_NO_BACKUP)
                return@launch
            }
            repository.importInitialBackup(json)
                .onSuccess { user ->
                    googleBackupManager.getSignedInEmail()?.let { userPreferences.saveGoogleAccount(it) }
                    onResult(user, null)
                }
                .onFailure { onResult(null, it.message ?: TreasuryMessage.RESTORE_IMPOSSIBLE) }
        }
    }

    fun onGoogleSignedIn(email: String?) {
        viewModelScope.launch {
            userPreferences.saveGoogleAccount(email)
            scheduleGoogleBackup()
        }
    }

    fun onGoogleSignedOut() {
        viewModelScope.launch {
            googleBackupManager.signOut()
            userPreferences.clearGoogleAccount()
        }
    }

    fun googleSignedInEmail(): String? = googleBackupManager.getSignedInEmail()

    fun setUserRole(role: UserRole) {
        _uiState.update { it.copy(currentUserRole = role) }
    }

    private fun requireEntrepriseId(): String? = _uiState.value.entrepriseId

    fun saveInvoiceDraft(
        clientName: String,
        amountExclTax: Double,
        dueDate: LocalDate,
        category: RevenueCategory = RevenueCategory.OTHER,
        categoryLabel: String = "",
        clientContactId: String? = null,
        lineItems: List<InvoiceLineItem> = emptyList(),
        markAsCollected: Boolean = false,
        paymentMethod: PaymentMethod = PaymentMethod.CREDIT_CARD,
        onResult: (String?) -> Unit = {}
    ) {
        val entrepriseId = requireEntrepriseId()
        if (entrepriseId == null) {
            onResult(TreasuryMessage.SESSION_EXPIRED_RECONNECT)
            return
        }
        viewModelScope.launch {
            val settings = userPreferences.observeInvoiceSettings(entrepriseId).first()
            val invoice = buildInvoice(
                entrepriseId = entrepriseId,
                clientName = clientName,
                clientContactId = clientContactId,
                amountExclTax = amountExclTax,
                dueDate = dueDate,
                category = category,
                categoryLabel = categoryLabel,
                settings = settings,
                documentStatus = InvoiceDocumentStatus.DRAFT,
                lineItems = lineItems
            )
            val error = repository.saveInvoiceDraft(invoice)
            if (error == null && markAsCollected) {
                onResult(applyFullPayment(invoice, dueDate, paymentMethod))
                return@launch
            }
            onResult(error)
            if (error == null) scheduleGoogleBackup()
        }
    }

    fun validateNewInvoice(
        clientName: String,
        amountExclTax: Double,
        dueDate: LocalDate,
        category: RevenueCategory = RevenueCategory.OTHER,
        categoryLabel: String = "",
        clientContactId: String? = null,
        lineItems: List<InvoiceLineItem> = emptyList(),
        markAsCollected: Boolean = false,
        paymentMethod: PaymentMethod = PaymentMethod.CREDIT_CARD,
        onResult: (String?) -> Unit = {}
    ) {
        val entrepriseId = requireEntrepriseId()
        if (entrepriseId == null) {
            onResult(TreasuryMessage.SESSION_EXPIRED_RECONNECT)
            return
        }
        viewModelScope.launch {
            val settings = userPreferences.observeInvoiceSettings(entrepriseId).first()
            val number = repository.nextInvoiceNumber(entrepriseId, dueDate.year, settings)
            val invoice = buildInvoice(
                entrepriseId = entrepriseId,
                clientName = clientName,
                clientContactId = clientContactId,
                amountExclTax = amountExclTax,
                dueDate = dueDate,
                category = category,
                categoryLabel = categoryLabel,
                settings = settings,
                documentStatus = InvoiceDocumentStatus.VALIDATED,
                invoiceNumber = number,
                lineItems = lineItems
            )
            val error = repository.addInvoice(invoice)
            if (error == null && markAsCollected) {
                onResult(applyFullPayment(invoice, dueDate, paymentMethod))
                return@launch
            }
            onResult(error)
            if (error == null) scheduleGoogleBackup()
        }
    }

    fun validateExistingInvoice(invoiceId: String, onResult: (String?) -> Unit = {}) {
        val entrepriseId = requireEntrepriseId()
        if (entrepriseId == null) {
            onResult(TreasuryMessage.SESSION_EXPIRED_RECONNECT)
            return
        }
        viewModelScope.launch {
            val settings = userPreferences.observeInvoiceSettings(entrepriseId).first()
            val error = repository.validateInvoice(invoiceId, settings)
            onResult(error)
            if (error == null) scheduleGoogleBackup()
        }
    }

    fun updateInvoiceForm(
        invoiceId: String,
        clientName: String,
        clientContactId: String?,
        lineItems: List<InvoiceLineItem>,
        dueDate: LocalDate,
        category: RevenueCategory = RevenueCategory.OTHER,
        categoryLabel: String = "",
        validate: Boolean = false,
        onResult: (String?) -> Unit = {}
    ) {
        val existing = getInvoice(invoiceId)
        if (existing == null) {
            onResult(TreasuryMessage.INVOICE_NOT_FOUND)
            return
        }
        val entrepriseId = requireEntrepriseId()
        if (entrepriseId == null) {
            onResult(TreasuryMessage.SESSION_EXPIRED_RECONNECT)
            return
        }
        viewModelScope.launch {
            val settings = userPreferences.observeInvoiceSettings(entrepriseId).first()
            val amountExclTax = InvoiceLineItemCodec.totalExclTax(lineItems)
            val error = when {
                existing.isDraft && validate -> {
                    val draft = buildInvoice(
                        entrepriseId = entrepriseId,
                        clientName = clientName,
                        clientContactId = clientContactId,
                        amountExclTax = amountExclTax,
                        dueDate = dueDate,
                        category = category,
                        categoryLabel = categoryLabel,
                        settings = settings,
                        documentStatus = InvoiceDocumentStatus.DRAFT,
                        existingId = invoiceId,
                        lineItems = lineItems
                    )
                    repository.updateInvoice(draft)
                        ?: repository.validateInvoice(invoiceId, settings)
                }
                existing.isDraft -> {
                    val draft = buildInvoice(
                        entrepriseId = entrepriseId,
                        clientName = clientName,
                        clientContactId = clientContactId,
                        amountExclTax = amountExclTax,
                        dueDate = dueDate,
                        category = category,
                        categoryLabel = categoryLabel,
                        settings = settings,
                        documentStatus = InvoiceDocumentStatus.DRAFT,
                        existingId = invoiceId,
                        lineItems = lineItems
                    )
                    repository.updateInvoice(draft)
                }
                else -> {
                    val validated = buildInvoice(
                        entrepriseId = entrepriseId,
                        clientName = clientName,
                        clientContactId = clientContactId,
                        amountExclTax = amountExclTax,
                        dueDate = dueDate,
                        category = category,
                        categoryLabel = categoryLabel,
                        settings = settings,
                        documentStatus = InvoiceDocumentStatus.VALIDATED,
                        invoiceNumber = existing.invoiceNumber,
                        existingId = invoiceId,
                        lineItems = lineItems
                    )
                    repository.updateInvoice(validated)
                }
            }
            onResult(error)
            if (error == null) scheduleGoogleBackup()
        }
    }

    fun saveInvoiceSettings(entrepriseId: String, settings: InvoiceSettings, onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            userPreferences.saveInvoiceSettings(entrepriseId, settings)
            onResult(null)
        }
    }

    private suspend fun applyFullPayment(
        invoice: Invoice,
        date: LocalDate,
        method: PaymentMethod
    ): String? {
        return repository.addPayment(
            Payment(
                invoiceId = invoice.id,
                amount = invoice.totalAmount,
                date = date,
                method = method
            )
        )
    }

    private fun buildInvoice(
        entrepriseId: String,
        clientName: String,
        clientContactId: String?,
        amountExclTax: Double,
        dueDate: LocalDate,
        category: RevenueCategory,
        categoryLabel: String,
        settings: InvoiceSettings,
        documentStatus: InvoiceDocumentStatus,
        invoiceNumber: String = "",
        existingId: String? = null,
        lineItems: List<InvoiceLineItem> = emptyList()
    ): Invoice {
        val ht = if (lineItems.isNotEmpty()) {
            InvoiceLineItemCodec.totalExclTax(lineItems)
        } else {
            amountExclTax
        }
        val tax = InvoiceTaxCalculations.fromAmountExclTax(ht, settings)
        return Invoice(
            id = existingId ?: java.util.UUID.randomUUID().toString(),
            invoiceNumber = invoiceNumber,
            clientName = clientName,
            clientContactId = clientContactId,
            totalAmount = tax.totalInclTax,
            dueDate = dueDate,
            entrepriseId = entrepriseId,
            category = category,
            categoryLabel = categoryLabel,
            documentStatus = documentStatus,
            amountExclTax = tax.amountExclTax,
            tvaRate = tax.tvaRate,
            otherTaxRate = tax.otherTaxRate,
            otherTaxMode = tax.otherTaxMode,
            otherTaxLabel = tax.otherTaxLabel,
            lineItems = lineItems
        )
    }

    private fun buildQuote(
        entrepriseId: String,
        clientName: String,
        clientContactId: String?,
        amountExclTax: Double,
        issueDate: LocalDate,
        validUntil: LocalDate,
        category: RevenueCategory,
        categoryLabel: String,
        settings: InvoiceSettings,
        status: QuoteStatus,
        quoteNumber: String = "",
        existingId: String? = null,
        lineItems: List<InvoiceLineItem> = emptyList(),
        notes: String = ""
    ): Quote {
        val ht = if (lineItems.isNotEmpty()) {
            InvoiceLineItemCodec.totalExclTax(lineItems)
        } else {
            amountExclTax
        }
        val tax = InvoiceTaxCalculations.fromAmountExclTax(ht, settings)
        return Quote(
            id = existingId ?: java.util.UUID.randomUUID().toString(),
            quoteNumber = quoteNumber,
            clientName = clientName,
            clientContactId = clientContactId,
            totalAmount = tax.totalInclTax,
            issueDate = issueDate,
            validUntil = validUntil,
            entrepriseId = entrepriseId,
            category = category,
            categoryLabel = categoryLabel,
            status = status,
            amountExclTax = tax.amountExclTax,
            tvaRate = tax.tvaRate,
            otherTaxRate = tax.otherTaxRate,
            otherTaxMode = tax.otherTaxMode,
            otherTaxLabel = tax.otherTaxLabel,
            lineItems = lineItems,
            notes = notes
        )
    }

    fun saveQuoteDraft(
        clientName: String,
        amountExclTax: Double,
        issueDate: LocalDate,
        validUntil: LocalDate,
        category: RevenueCategory = RevenueCategory.OTHER,
        categoryLabel: String = "",
        clientContactId: String? = null,
        lineItems: List<InvoiceLineItem> = emptyList(),
        notes: String = "",
        onResult: (String?) -> Unit = {}
    ) {
        val entrepriseId = requireEntrepriseId()
        if (entrepriseId == null) {
            onResult(TreasuryMessage.SESSION_EXPIRED_RECONNECT)
            return
        }
        viewModelScope.launch {
            val settings = userPreferences.observeInvoiceSettings(entrepriseId).first()
            val quote = buildQuote(
                entrepriseId = entrepriseId,
                clientName = clientName,
                clientContactId = clientContactId,
                amountExclTax = amountExclTax,
                issueDate = issueDate,
                validUntil = validUntil,
                category = category,
                categoryLabel = categoryLabel,
                settings = settings,
                status = QuoteStatus.DRAFT,
                lineItems = lineItems,
                notes = notes
            )
            val error = repository.saveQuoteDraft(quote)
            onResult(error)
            if (error == null) scheduleGoogleBackup()
        }
    }

    fun validateNewQuote(
        clientName: String,
        amountExclTax: Double,
        issueDate: LocalDate,
        validUntil: LocalDate,
        category: RevenueCategory = RevenueCategory.OTHER,
        categoryLabel: String = "",
        clientContactId: String? = null,
        lineItems: List<InvoiceLineItem> = emptyList(),
        notes: String = "",
        onResult: (String?) -> Unit = {}
    ) {
        val entrepriseId = requireEntrepriseId()
        if (entrepriseId == null) {
            onResult(TreasuryMessage.SESSION_EXPIRED_RECONNECT)
            return
        }
        viewModelScope.launch {
            val settings = userPreferences.observeInvoiceSettings(entrepriseId).first()
            val number = repository.nextQuoteNumber(entrepriseId, issueDate.year, settings)
            val quote = buildQuote(
                entrepriseId = entrepriseId,
                clientName = clientName,
                clientContactId = clientContactId,
                amountExclTax = amountExclTax,
                issueDate = issueDate,
                validUntil = validUntil,
                category = category,
                categoryLabel = categoryLabel,
                settings = settings,
                status = QuoteStatus.SENT,
                quoteNumber = number,
                lineItems = lineItems,
                notes = notes
            )
            val error = repository.addQuote(quote)
            onResult(error)
            if (error == null) scheduleGoogleBackup()
        }
    }

    fun validateExistingQuote(quoteId: String, onResult: (String?) -> Unit = {}) {
        val entrepriseId = requireEntrepriseId()
        if (entrepriseId == null) {
            onResult(TreasuryMessage.SESSION_EXPIRED_RECONNECT)
            return
        }
        viewModelScope.launch {
            val settings = userPreferences.observeInvoiceSettings(entrepriseId).first()
            val error = repository.validateQuote(quoteId, settings)
            onResult(error)
            if (error == null) scheduleGoogleBackup()
        }
    }

    fun acceptQuote(quoteId: String, onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            val error = repository.updateQuoteStatus(quoteId, QuoteStatus.ACCEPTED)
            onResult(error)
            if (error == null) scheduleGoogleBackup()
        }
    }

    fun refuseQuote(quoteId: String, onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            val error = repository.updateQuoteStatus(quoteId, QuoteStatus.REFUSED)
            onResult(error)
            if (error == null) scheduleGoogleBackup()
        }
    }

    fun convertQuoteToInvoice(quoteId: String, onResult: (String?) -> Unit = {}) {
        val entrepriseId = requireEntrepriseId()
        if (entrepriseId == null) {
            onResult(TreasuryMessage.SESSION_EXPIRED_RECONNECT)
            return
        }
        viewModelScope.launch {
            val settings = userPreferences.observeInvoiceSettings(entrepriseId).first()
            val error = repository.convertQuoteToInvoice(quoteId, settings)
            onResult(error)
            if (error == null) scheduleGoogleBackup()
        }
    }

    fun deleteQuote(quoteId: String, onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            val error = repository.deleteQuote(quoteId)
            onResult(error)
            if (error == null) scheduleGoogleBackup()
        }
    }

    fun addInvoice(
        invoiceNumber: String,
        clientName: String,
        totalAmount: Double,
        dueDate: LocalDate,
        category: RevenueCategory = RevenueCategory.OTHER,
        categoryLabel: String = "",
        clientContactId: String? = null,
        markAsCollected: Boolean = false,
        paymentMethod: PaymentMethod = PaymentMethod.CREDIT_CARD,
        onResult: (String?) -> Unit = {}
    ) {
        val entrepriseId = requireEntrepriseId()
        if (entrepriseId == null) {
            onResult(TreasuryMessage.SESSION_EXPIRED_RECONNECT)
            return
        }
        viewModelScope.launch {
            val settings = userPreferences.observeInvoiceSettings(entrepriseId).first()
            val amountExclTax = InvoiceTaxCalculations.amountExclTaxFromTotal(totalAmount, settings)
            val number = repository.nextInvoiceNumber(entrepriseId, dueDate.year, settings)
            val invoice = buildInvoice(
                entrepriseId = entrepriseId,
                clientName = clientName,
                clientContactId = clientContactId,
                amountExclTax = amountExclTax,
                dueDate = dueDate,
                category = category,
                categoryLabel = categoryLabel,
                settings = settings,
                documentStatus = InvoiceDocumentStatus.VALIDATED,
                invoiceNumber = number
            )
            val error = repository.addInvoice(invoice)
            if (error == null && markAsCollected) {
                onResult(applyFullPayment(invoice, dueDate, paymentMethod))
                return@launch
            }
            onResult(error)
            if (error == null) scheduleGoogleBackup()
        }
    }

    fun suggestNextInvoiceNumber(year: Int, onResult: (String) -> Unit) {
        val entrepriseId = requireEntrepriseId()
        if (entrepriseId == null) {
            onResult("")
            return
        }
        viewModelScope.launch {
            val settings = userPreferences.observeInvoiceSettings(entrepriseId).first()
            onResult(repository.nextInvoiceNumber(entrepriseId, year, settings))
        }
    }

    fun addIncomeTransaction(
        clientName: String,
        amount: Double,
        date: LocalDate,
        category: RevenueCategory,
        categoryLabel: String = "",
        markAsCollected: Boolean,
        paymentMethod: PaymentMethod = PaymentMethod.CREDIT_CARD,
        clientContactId: String? = null,
        invoiceNumber: String = "",
        onResult: (String?) -> Unit
    ) {
        val entrepriseId = requireEntrepriseId()
        if (entrepriseId == null) {
            onResult(TreasuryMessage.SESSION_EXPIRED_RECONNECT)
            return
        }
        viewModelScope.launch {
            val settings = userPreferences.observeInvoiceSettings(entrepriseId).first()
            val number = invoiceNumber.trim().ifBlank {
                repository.nextInvoiceNumber(entrepriseId, date.year, settings)
            }
            val invoice = Invoice(
                invoiceNumber = number,
                clientName = clientName,
                clientContactId = clientContactId,
                totalAmount = amount,
                dueDate = date,
                entrepriseId = entrepriseId,
                category = category,
                categoryLabel = categoryLabel
            )
            val error = repository.addInvoice(invoice)
            if (error == null && markAsCollected) {
                val paymentError = repository.addPayment(
                    Payment(
                        invoiceId = invoice.id,
                        amount = amount,
                        date = date,
                        method = paymentMethod
                    )
                )
                if (paymentError != null) {
                    onResult(paymentError)
                    return@launch
                }
            }
            onResult(error)
            if (error == null) {
                scheduleGoogleBackup()
                refreshSubscription()
            }
        }
    }

    fun addExpenseTransaction(
        label: String,
        amount: Double,
        date: LocalDate,
        category: ExpenseCategory,
        categoryLabel: String = "",
        isRecurring: Boolean = false,
        recurrence: ExpenseRecurrence? = null,
        recurrenceEndDate: LocalDate? = null,
        isPaid: Boolean = true,
        paymentMethod: PaymentMethod = PaymentMethod.CREDIT_CARD,
        note: String = "",
        supplierContactId: String? = null,
        receiptImagePath: String? = null,
        isExpenseNote: Boolean = false,
        expenseId: String? = null,
        onResult: (String?) -> Unit
    ) {
        val entrepriseId = requireEntrepriseId()
        if (entrepriseId == null) {
            onResult(TreasuryMessage.SESSION_EXPIRED_RECONNECT)
            return
        }
        viewModelScope.launch {
            val error = repository.addExpense(
                Expense(
                    id = expenseId ?: java.util.UUID.randomUUID().toString(),
                    label = label,
                    amount = amount,
                    date = date,
                    isRecurring = isRecurring,
                    recurrence = if (isRecurring) recurrence else null,
                    recurrenceEndDate = if (isRecurring) recurrenceEndDate else null,
                    isPaid = isPaid,
                    paymentMethod = if (isPaid) paymentMethod else null,
                    entrepriseId = entrepriseId,
                    category = category,
                    categoryLabel = categoryLabel,
                    supplierContactId = supplierContactId,
                    note = note,
                    receiptImagePath = receiptImagePath,
                    isExpenseNote = isExpenseNote
                )
            )
            onResult(error)
            if (error == null) {
                scheduleGoogleBackup()
                refreshSubscription()
            }
        }
    }

    fun deleteExpenseNote(expenseId: String, onResult: () -> Unit) {
        viewModelScope.launch {
            val expense = _uiState.value.expenses.firstOrNull { it.id == expenseId }
            repository.deleteExpense(expenseId)
            expense?.receiptImagePath?.let { com.abccash.app.treasury.export.ReceiptImageStorage.deleteReceipt(it) }
            onResult()
            scheduleGoogleBackup()
        }
    }

    fun updateUserProfile(
        userId: String,
        nom: String,
        email: String,
        telephone: String,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val error = repository.updateUserProfile(userId, nom, email, telephone)
            if (error != null) {
                onResult(error)
                return@launch
            }
            scheduleGoogleBackup()
            onResult(null)
        }
    }

    fun updateEntrepriseProfile(
        nom: String,
        email: String,
        telephone: String,
        adresse: String,
        onResult: (String?) -> Unit
    ) {
        val entrepriseId = requireEntrepriseId() ?: run {
            onResult(TreasuryMessage.SESSION_EXPIRED_RECONNECT)
            return
        }
        viewModelScope.launch {
            val error = repository.updateEntrepriseProfile(entrepriseId, nom, email, telephone, adresse)
            if (error != null) {
                onResult(error)
                return@launch
            }
            scheduleGoogleBackup()
            onResult(null)
        }
    }

    fun recordPayment(
        invoiceId: String,
        amount: Double,
        date: LocalDate,
        method: PaymentMethod,
        onResult: (String?) -> Unit = {}
    ) {
        val invoice = getInvoice(invoiceId)
        if (invoice == null) {
            onResult(TreasuryMessage.PAYMENT_NOT_FOUND)
            return
        }
        if (amount <= 0 || amount > invoice.remainingAmount) {
            onResult(TreasuryMessage.invalidPaymentAmount(invoice.remainingAmount))
            return
        }
        viewModelScope.launch {
            val error = repository.addPayment(
                Payment(
                    invoiceId = invoiceId,
                    amount = amount,
                    date = date,
                    method = method
                )
            )
            onResult(error)
            if (error == null) scheduleGoogleBackup()
        }
    }

    fun importInvoices(invoices: List<Invoice>) {
        val entrepriseId = requireEntrepriseId() ?: return
        viewModelScope.launch {
            val stats = repository.importInvoices(entrepriseId, invoices)
            _uiState.update {
                it.copy(importFeedback = ImportFeedback(stats.imported, stats.skippedDuplicates))
            }
            scheduleGoogleBackup()
        }
    }

    fun clearImportFeedback() {
        _uiState.update { it.copy(importFeedback = null) }
    }

    fun deleteAllTransactions(onResult: (String?) -> Unit) {
        val entrepriseId = requireEntrepriseId()
        if (entrepriseId == null) {
            onResult(TreasuryMessage.SESSION_EXPIRED_RECONNECT)
            return
        }
        viewModelScope.launch {
            val error = repository.deleteAllTransactions(entrepriseId)
            onResult(error)
            if (error == null) {
                scheduleGoogleBackup()
                refreshSubscription()
            }
        }
    }

    /** Deletes the transactions shown for a given month (income + expenses), keeping the account. */
    fun deleteTransactionsForMonth(month: YearMonth, onResult: (String?) -> Unit) {
        if (requireEntrepriseId() == null) {
            onResult(TreasuryMessage.SESSION_EXPIRED_RECONNECT)
            return
        }
        viewModelScope.launch {
            val state = _uiState.value
            val invoiceIds = state.invoices.filter { it.transactionDateIn(month) }.map { it.id }
            val expenseIds = state.expenses
                .filter { it.isPaid && YearMonth.from(it.date) == month }
                .map { it.id }
            invoiceIds.forEach { repository.deleteInvoice(it) }
            expenseIds.forEach { repository.deleteExpense(it) }
            onResult(null)
            scheduleGoogleBackup()
            refreshSubscription()
        }
    }

    /**
     * Imports settled bank statement operations: credits become collected income (invoices),
     * debits become paid expenses. Categories are left as default so the user can adjust later.
     * Duplicate operations (same date, amount and label) are skipped.
     */
    fun importBankStatement(
        entries: List<BankStatementEntry>,
        onResult: (imported: Int, skipped: Int) -> Unit
    ) {
        val entrepriseId = requireEntrepriseId()
        if (entrepriseId == null) {
            onResult(0, 0)
            return
        }
        viewModelScope.launch {
            val settings = userPreferences.observeInvoiceSettings(entrepriseId).first()
            val current = _uiState.value
            val incomeSignatures = current.invoices
                .map { transactionSignature(it.dueDate, it.totalAmount, it.clientName) }
                .toMutableSet()
            val expenseSignatures = current.expenses
                .map { transactionSignature(it.date, it.amount, it.label) }
                .toMutableSet()

            var imported = 0
            var skipped = 0

            for (entry in entries) {
                val signature = transactionSignature(entry.date, entry.amount, entry.label)
                if (entry.isCredit) {
                    if (!incomeSignatures.add(signature)) {
                        skipped++
                        continue
                    }
                    val number = repository.nextInvoiceNumber(entrepriseId, entry.date.year, settings)
                    val invoice = Invoice(
                        invoiceNumber = number,
                        clientName = entry.label,
                        totalAmount = entry.amount,
                        dueDate = entry.date,
                        entrepriseId = entrepriseId,
                        category = RevenueCategory.OTHER
                    )
                    val error = repository.addInvoice(invoice, enforceLimit = false)
                    if (error == null) {
                        repository.addPayment(
                            Payment(
                                invoiceId = invoice.id,
                                amount = entry.amount,
                                date = entry.date,
                                method = PaymentMethod.TRANSFER
                            )
                        )
                        imported++
                    } else {
                        skipped++
                    }
                } else {
                    if (!expenseSignatures.add(signature)) {
                        skipped++
                        continue
                    }
                    val error = repository.addExpense(
                        Expense(
                            label = entry.label,
                            amount = entry.amount,
                            date = entry.date,
                            isPaid = true,
                            paymentMethod = PaymentMethod.TRANSFER,
                            entrepriseId = entrepriseId,
                            category = ExpenseCategory.OTHER
                        ),
                        enforceLimit = false
                    )
                    if (error == null) imported++ else skipped++
                }
            }

            onResult(imported, skipped)
            if (imported > 0) {
                scheduleGoogleBackup()
                refreshSubscription()
            }
        }
    }

    private fun transactionSignature(date: LocalDate, amount: Double, label: String): String {
        val normalizedAmount = Math.round(amount * 1000.0)
        val normalizedLabel = label.trim().lowercase(Locale.ROOT)
        return "$date|$normalizedAmount|$normalizedLabel"
    }

    fun updateInvoice(
        invoiceId: String,
        invoiceNumber: String,
        clientName: String,
        totalAmount: Double,
        dueDate: LocalDate,
        onResult: (String?) -> Unit = {}
    ) {
        val existing = getInvoice(invoiceId)
        if (existing == null) {
            onResult(TreasuryMessage.INVOICE_NOT_FOUND)
            return
        }
        viewModelScope.launch {
            val error = repository.updateInvoice(
                existing.copy(
                    invoiceNumber = invoiceNumber,
                    clientName = clientName,
                    totalAmount = totalAmount,
                    dueDate = dueDate
                )
            )
            onResult(error)
            if (error == null) scheduleGoogleBackup()
        }
    }

    fun deleteInvoice(invoiceId: String, onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            val error = repository.deleteInvoice(invoiceId)
            onResult(error)
            if (error == null) scheduleGoogleBackup()
        }
    }

    fun deleteInvoices(invoiceIds: Collection<String>) {
        if (invoiceIds.isEmpty()) return
        viewModelScope.launch {
            invoiceIds.forEach { repository.deleteInvoice(it) }
            scheduleGoogleBackup()
        }
    }

    fun addPayment(
        invoiceId: String,
        amount: Double,
        date: LocalDate,
        method: PaymentMethod
    ): Boolean {
        val invoice = getInvoice(invoiceId) ?: return false
        if (amount <= 0 || amount > invoice.remainingAmount) return false
        viewModelScope.launch {
            val error = repository.addPayment(
                Payment(
                    invoiceId = invoiceId,
                    amount = amount,
                    date = date,
                    method = method
                )
            )
            if (error == null) scheduleGoogleBackup()
        }
        return true
    }

    fun addExpense(
        label: String,
        amount: Double,
        date: LocalDate,
        isRecurring: Boolean,
        recurrence: ExpenseRecurrence?,
        recurrenceEndDate: LocalDate?,
        isPaid: Boolean
    ) {
        val entrepriseId = requireEntrepriseId() ?: return
        viewModelScope.launch {
            val error = repository.addExpense(
                Expense(
                    label = label,
                    amount = amount,
                    date = date,
                    isRecurring = isRecurring,
                    recurrence = if (isRecurring) recurrence else null,
                    recurrenceEndDate = if (isRecurring) recurrenceEndDate else null,
                    isPaid = isPaid,
                    entrepriseId = entrepriseId
                )
            )
            if (error == null) scheduleGoogleBackup()
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            val error = repository.deleteExpense(expenseId)
            if (error == null) scheduleGoogleBackup()
        }
    }

    fun deleteExpenses(expenseIds: Collection<String>) {
        if (expenseIds.isEmpty()) return
        viewModelScope.launch {
            expenseIds.forEach { repository.deleteExpense(it) }
            scheduleGoogleBackup()
        }
    }

    fun validateForecastExpense(
        expenseId: String,
        paymentDate: LocalDate,
        paymentMethod: PaymentMethod,
        occurrenceDueDate: LocalDate = paymentDate,
        onResult: (String?) -> Unit = {}
    ) {
        val existing = _uiState.value.expenses.find { it.id == expenseId }
        if (existing == null) {
            onResult(TreasuryMessage.EXPENSE_NOT_FOUND)
            return
        }
        viewModelScope.launch {
            if (existing.isRecurring) {
                repository.addExpense(
                    existing.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        isRecurring = false,
                        recurrence = null,
                        recurrenceEndDate = null,
                        isPaid = true,
                        date = paymentDate,
                        paymentMethod = paymentMethod
                    )
                )
                val nextDue = existing.nextOccurrenceAfter(occurrenceDueDate)
                if (nextDue == null) {
                    repository.deleteExpense(expenseId)
                } else {
                    repository.updateExpense(
                        existing.copy(
                            date = nextDue,
                            isPaid = false,
                            paymentMethod = null
                        )
                    )
                }
            } else {
                repository.updateExpense(
                    existing.copy(
                        isPaid = true,
                        paymentMethod = paymentMethod,
                        date = paymentDate
                    )
                )
            }
            onResult(null)
            scheduleGoogleBackup()
        }
    }

    fun updateExpense(
        expenseId: String,
        label: String,
        amount: Double,
        date: LocalDate,
        isRecurring: Boolean,
        recurrence: ExpenseRecurrence?,
        recurrenceEndDate: LocalDate?,
        isPaid: Boolean,
        paymentMethod: PaymentMethod?,
        category: ExpenseCategory,
        categoryLabel: String
    ) {
        val existing = _uiState.value.expenses.find { it.id == expenseId } ?: return
        viewModelScope.launch {
            repository.updateExpense(
                existing.copy(
                    label = label,
                    amount = amount,
                    date = date,
                    category = category,
                    categoryLabel = categoryLabel,
                    isRecurring = isRecurring,
                    paymentMethod = paymentMethod,
                    recurrence = if (isRecurring) recurrence else null,
                    recurrenceEndDate = if (isRecurring) recurrenceEndDate else null,
                    isPaid = isPaid
                )
            )
            scheduleGoogleBackup()
        }
    }

    fun stopExpenseRecurrence(expenseId: String, endDate: LocalDate) {
        val existing = _uiState.value.expenses.find { it.id == expenseId } ?: return
        if (!existing.isRecurring) return
        viewModelScope.launch {
            repository.updateExpense(existing.copy(recurrenceEndDate = endDate))
            scheduleGoogleBackup()
        }
    }

    fun addUser(
        nom: String,
        email: String,
        telephone: String,
        password: String,
        role: UserRole,
        permissions: Set<UserPermission>,
        onResult: (String?) -> Unit = {}
    ) {
        val entrepriseId = requireEntrepriseId()
        if (entrepriseId == null) {
            onResult(TreasuryMessage.SESSION_EXPIRED_RECONNECT)
            return
        }
        viewModelScope.launch {
            val error = repository.addUser(
                User(
                    nom = nom.trim(),
                    email = email,
                    telephone = telephone,
                    passwordHash = password,
                    role = role,
                    permissions = permissions,
                    entrepriseId = entrepriseId
                )
            )
            onResult(error)
            if (error == null) scheduleGoogleBackup()
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            repository.deleteUser(userId)
            scheduleGoogleBackup()
        }
    }

    fun changePassword(
        userId: String,
        currentPassword: String,
        newPassword: String,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val error = repository.changePassword(userId, currentPassword, newPassword)
            onResult(error)
            if (error == null) scheduleGoogleBackup()
        }
    }

    fun resetUserPassword(userId: String, newPassword: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val error = repository.resetUserPassword(userId, newPassword)
            onResult(error)
            if (error == null) scheduleGoogleBackup()
        }
    }

    fun buildCsvExport(year: Int = YearMonth.now().year): String? {
        val state = _uiState.value
        if (state.entrepriseId == null) return null
        return TreasuryCsvExporter.exportYear(
            invoices = state.invoices,
            expenses = state.expenses,
            year = year
        )
    }

    fun exportBackup(onResult: (String?) -> Unit) {
        val entrepriseId = requireEntrepriseId()
        if (entrepriseId == null) {
            onResult(null)
            return
        }
        viewModelScope.launch {
            onResult(repository.exportBackup(entrepriseId))
        }
    }

    fun restoreBackup(json: String, onResult: (String?) -> Unit) {
        val entrepriseId = requireEntrepriseId()
        if (entrepriseId == null) {
            onResult(TreasuryMessage.SESSION_EXPIRED)
            return
        }
        viewModelScope.launch {
            val error = repository.restoreBackup(entrepriseId, json)
            if (error == null) {
                _uiState.update { it.copy(backupFeedback = TreasuryMessage.BACKUP_RESTORED_SUCCESS) }
                scheduleGoogleBackup()
            }
            onResult(error)
        }
    }

    fun clearBackupFeedback() {
        _uiState.update { it.copy(backupFeedback = null) }
    }

    fun setSelectedMonth(yearMonth: YearMonth) {
        _uiState.update { it.copy(selectedMonth = yearMonth) }
    }

    fun getInvoice(invoiceId: String): Invoice? {
        return _uiState.value.invoices.find { it.id == invoiceId }
    }

    fun getQuote(quoteId: String): Quote? =
        _uiState.value.quotes.find { it.id == quoteId }

    fun getMonthlyCollections(yearMonth: YearMonth): Double {
        return TreasuryCalculations.monthlyCollections(_uiState.value.invoices, yearMonth)
    }

    fun getMonthlyExpenses(yearMonth: YearMonth): Double {
        return TreasuryCalculations.monthlyPaidExpenses(_uiState.value.expenses, yearMonth)
    }

    fun getMonthlyBalance(yearMonth: YearMonth): Double {
        return TreasuryCalculations.monthlyBalance(
            getMonthlyCollections(yearMonth),
            getMonthlyExpenses(yearMonth)
        )
    }

    fun getForecastedBalance(yearMonth: YearMonth): Double {
        return TreasuryCalculations.forecastedBalance(
            invoices = _uiState.value.invoices,
            expenses = _uiState.value.expenses,
            month = yearMonth
        )
    }

    fun getYearlyBalance(year: Int): Double =
        TreasuryCalculations.yearlyBalance(_uiState.value.invoices, _uiState.value.expenses, year)

    /**
     * Aligne la trésorerie calculée sur le solde bancaire réel.
     * - Banque > appli → encaissement d'ajustement
     * - Banque < appli → dépense d'ajustement
     */
    fun reconcileTreasuryWithBank(
        bankBalance: Double,
        calculatedBalance: Double,
        createAdjustments: Boolean,
        userRole: UserRole,
        onResult: (String?) -> Unit
    ) {
        val entrepriseId = requireEntrepriseId()
        if (entrepriseId == null) {
            onResult(TreasuryMessage.SESSION_EXPIRED_RECONNECT)
            return
        }
        val gap = bankBalance - calculatedBalance
        if (!createAdjustments || kotlin.math.abs(gap) <= 0.001) {
            onResult(null)
            return
        }
        if (gap > 0 && userRole != UserRole.ADMIN) {
            onResult(TreasuryMessage.ADMIN_ONLY_COLLECTION_ADJUSTMENT)
            return
        }
        viewModelScope.launch {
            val today = LocalDate.now()
            val error = if (gap > 0) {
                val invoice = Invoice(
                    invoiceNumber = TreasuryAdjustmentLabels.invoiceNumber(today),
                    clientName = TreasuryAdjustmentLabels.INVOICE_CLIENT,
                    totalAmount = gap,
                    dueDate = today,
                    entrepriseId = entrepriseId
                )
                val invoiceError = repository.addInvoice(invoice)
                if (invoiceError == null) {
                    val paymentError = repository.addPayment(
                        Payment(
                            invoiceId = invoice.id,
                            amount = gap,
                            date = today,
                            method = PaymentMethod.TRANSFER,
                            note = "Ajustement automatique solde bancaire"
                        )
                    )
                    paymentError
                } else {
                    invoiceError
                }
            } else {
                repository.addExpense(
                    Expense(
                        label = TreasuryAdjustmentLabels.EXPENSE,
                        amount = kotlin.math.abs(gap),
                        date = today,
                        isPaid = true,
                        paymentMethod = PaymentMethod.TRANSFER,
                        entrepriseId = entrepriseId
                    )
                )
            }
            onResult(error)
            if (error == null) scheduleGoogleBackup()
        }
    }
}

class TreasuryViewModelFactory(
    private val repository: TreasuryRepository,
    private val googleBackupManager: GoogleBackupManager,
    private val userPreferences: UserPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TreasuryViewModel::class.java)) {
            return TreasuryViewModel(
                repository,
                googleBackupManager,
                userPreferences
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
