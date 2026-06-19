package com.abccash.app.treasury.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abccash.app.treasury.data.*
import com.abccash.app.treasury.export.TreasuryCsvExporter
import com.abccash.app.treasury.repository.TreasuryRepository
import com.abccash.app.treasury.remote.TreasurySyncService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class TreasuryUiState(
    val currentUserId: String? = null,
    val currentUserRole: UserRole = UserRole.STAFF,
    val permissions: Set<UserPermission> = emptySet(),
    val entrepriseId: String? = null,
    val entreprise: Entreprise? = null,
    val users: List<User> = emptyList(),
    val invoices: List<Invoice> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val selectedMonth: YearMonth = YearMonth.now(),
    val importFeedback: String? = null,
    val backupFeedback: String? = null
)

class TreasuryViewModel(
    private val repository: TreasuryRepository,
    private val syncService: TreasurySyncService
) : ViewModel() {
    private val _entrepriseId = MutableStateFlow<String?>(null)
    private val _uiState = MutableStateFlow(TreasuryUiState())
    val uiState: StateFlow<TreasuryUiState> = _uiState.asStateFlow()

    init {
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
    }

    fun clearSession() {
        _entrepriseId.value = null
        _uiState.value = TreasuryUiState()
    }

    fun setUserRole(role: UserRole) {
        _uiState.update { it.copy(currentUserRole = role) }
    }

    private fun requireEntrepriseId(): String? = _uiState.value.entrepriseId

    fun addInvoice(
        invoiceNumber: String,
        clientName: String,
        totalAmount: Double,
        dueDate: LocalDate,
        category: RevenueCategory = RevenueCategory.OTHER,
        markAsCollected: Boolean = false,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        onResult: (String?) -> Unit = {}
    ) {
        val entrepriseId = requireEntrepriseId()
        if (entrepriseId == null) {
            onResult("Session expirée, reconnectez-vous")
            return
        }
        viewModelScope.launch {
            val number = invoiceNumber.ifBlank {
                "ENC-${dueDate.format(DateTimeFormatter.BASIC_ISO_DATE)}-" +
                    (System.currentTimeMillis() % 10_000).toString().padStart(4, '0')
            }
            val invoice = Invoice(
                invoiceNumber = number,
                clientName = clientName,
                totalAmount = totalAmount,
                dueDate = dueDate,
                entrepriseId = entrepriseId,
                category = category
            )
            val error = repository.addInvoice(invoice)
            if (error == null && markAsCollected) {
                repository.addPayment(
                    Payment(
                        invoiceId = invoice.id,
                        amount = totalAmount,
                        date = dueDate,
                        method = paymentMethod
                    )
                )
            }
            onResult(error)
        }
    }

    fun addIncomeTransaction(
        clientName: String,
        amount: Double,
        date: LocalDate,
        category: RevenueCategory,
        categoryLabel: String = "",
        markAsCollected: Boolean,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        onResult: (String?) -> Unit
    ) {
        val entrepriseId = requireEntrepriseId()
        if (entrepriseId == null) {
            onResult("Session expirée, reconnectez-vous")
            return
        }
        viewModelScope.launch {
            val number = "ENC-${date.format(DateTimeFormatter.BASIC_ISO_DATE)}-" +
                (System.currentTimeMillis() % 10_000).toString().padStart(4, '0')
            val invoice = Invoice(
                invoiceNumber = number,
                clientName = clientName,
                totalAmount = amount,
                dueDate = date,
                entrepriseId = entrepriseId,
                category = category,
                categoryLabel = categoryLabel
            )
            val error = repository.addInvoice(invoice)
            if (error == null && markAsCollected) {
                repository.addPayment(
                    Payment(
                        invoiceId = invoice.id,
                        amount = amount,
                        date = date,
                        method = paymentMethod
                    )
                )
            }
            onResult(error)
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
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        onResult: (String?) -> Unit
    ) {
        val entrepriseId = requireEntrepriseId()
        if (entrepriseId == null) {
            onResult("Session expirée, reconnectez-vous")
            return
        }
        viewModelScope.launch {
            repository.addExpense(
                Expense(
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
                    categoryLabel = categoryLabel
                )
            )
            onResult(null)
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
            onResult(repository.updateUserProfile(userId, nom, email, telephone))
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
            onResult("Session expirée, reconnectez-vous")
            return
        }
        viewModelScope.launch {
            onResult(repository.updateEntrepriseProfile(entrepriseId, nom, email, telephone, adresse))
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
            onResult("Encaissement introuvable")
            return
        }
        if (amount <= 0 || amount > invoice.remainingAmount) {
            onResult("Montant invalide (max ${invoice.remainingAmount})")
            return
        }
        viewModelScope.launch {
            repository.addPayment(
                Payment(
                    invoiceId = invoiceId,
                    amount = amount,
                    date = date,
                    method = method
                )
            )
            onResult(null)
        }
    }

    fun importInvoices(invoices: List<Invoice>) {
        val entrepriseId = requireEntrepriseId() ?: return
        viewModelScope.launch {
            val stats = repository.importInvoices(entrepriseId, invoices)
            val message = buildString {
                append("${stats.imported} facture(s) importée(s)")
                if (stats.skippedDuplicates > 0) {
                    append(", ${stats.skippedDuplicates} doublon(s) ignoré(s)")
                }
            }
            _uiState.update {
                it.copy(importFeedback = message)
            }
        }
    }

    fun clearImportFeedback() {
        _uiState.update { it.copy(importFeedback = null) }
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
            onResult("Facture introuvable")
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
        }
    }

    fun deleteInvoice(invoiceId: String) {
        viewModelScope.launch {
            repository.deleteInvoice(invoiceId)
        }
    }

    fun deleteInvoices(invoiceIds: Collection<String>) {
        if (invoiceIds.isEmpty()) return
        viewModelScope.launch {
            invoiceIds.forEach { repository.deleteInvoice(it) }
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
            repository.addPayment(
                Payment(
                    invoiceId = invoiceId,
                    amount = amount,
                    date = date,
                    method = method
                )
            )
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
            repository.addExpense(
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
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
        }
    }

    fun deleteExpenses(expenseIds: Collection<String>) {
        if (expenseIds.isEmpty()) return
        viewModelScope.launch {
            expenseIds.forEach { repository.deleteExpense(it) }
        }
    }

    fun validateForecastExpense(
        expenseId: String,
        paymentDate: LocalDate,
        paymentMethod: PaymentMethod,
        onResult: (String?) -> Unit = {}
    ) {
        val existing = _uiState.value.expenses.find { it.id == expenseId }
        if (existing == null) {
            onResult("Dépense introuvable")
            return
        }
        viewModelScope.launch {
            repository.updateExpense(
                existing.copy(
                    isPaid = true,
                    paymentMethod = paymentMethod,
                    date = if (existing.isRecurring) existing.date else paymentDate
                )
            )
            onResult(null)
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
        isPaid: Boolean
    ) {
        val existing = _uiState.value.expenses.find { it.id == expenseId } ?: return
        viewModelScope.launch {
            repository.updateExpense(
                existing.copy(
                    label = label,
                    amount = amount,
                    date = date,
                    isRecurring = isRecurring,
                    recurrence = if (isRecurring) recurrence else null,
                    recurrenceEndDate = if (isRecurring) recurrenceEndDate else null,
                    isPaid = isPaid,
                    paymentMethod = when {
                        isPaid -> existing.paymentMethod ?: PaymentMethod.CASH
                        else -> null
                    }
                )
            )
        }
    }

    fun stopExpenseRecurrence(expenseId: String, endDate: LocalDate) {
        val existing = _uiState.value.expenses.find { it.id == expenseId } ?: return
        if (!existing.isRecurring) return
        viewModelScope.launch {
            repository.updateExpense(existing.copy(recurrenceEndDate = endDate))
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
            onResult("Session expirée, reconnectez-vous")
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
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            repository.deleteUser(userId)
        }
    }

    fun changePassword(
        userId: String,
        currentPassword: String,
        newPassword: String,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            onResult(repository.changePassword(userId, currentPassword, newPassword))
        }
    }

    fun resetUserPassword(userId: String, newPassword: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            onResult(repository.resetUserPassword(userId, newPassword))
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
            onResult("Session expirée")
            return
        }
        viewModelScope.launch {
            val error = repository.restoreBackup(entrepriseId, json)
            if (error == null) {
                _uiState.update { it.copy(backupFeedback = "Sauvegarde restaurée avec succès") }
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
            onResult("Session expirée, reconnectez-vous")
            return
        }
        val gap = bankBalance - calculatedBalance
        if (!createAdjustments || kotlin.math.abs(gap) <= 0.001) {
            onResult(null)
            return
        }
        if (gap > 0 && userRole != UserRole.ADMIN) {
            onResult("Seul l'administrateur peut créer un ajustement d'encaissement")
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
                repository.addInvoice(invoice).also { invoiceError ->
                    if (invoiceError == null) {
                        repository.addPayment(
                            Payment(
                                invoiceId = invoice.id,
                                amount = gap,
                                date = today,
                                method = PaymentMethod.CASH,
                                note = "Ajustement automatique solde bancaire"
                            )
                        )
                    }
                }
            } else {
                repository.addExpense(
                    Expense(
                        label = TreasuryAdjustmentLabels.EXPENSE,
                        amount = kotlin.math.abs(gap),
                        date = today,
                        isPaid = true,
                        entrepriseId = entrepriseId
                    )
                )
                null
            }
            onResult(error)
        }
    }

    fun syncNow(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val entrepriseId = _entrepriseId.value
            if (entrepriseId.isNullOrBlank()) {
                onResult("Session inactive")
                return@launch
            }
            onResult(syncService.syncNow(entrepriseId))
        }
    }
}

class TreasuryViewModelFactory(
    private val repository: TreasuryRepository,
    private val syncService: TreasurySyncService
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TreasuryViewModel::class.java)) {
            return TreasuryViewModel(repository, syncService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
