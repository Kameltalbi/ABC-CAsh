package com.abccash.app.treasury.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abccash.app.treasury.data.*
import com.abccash.app.treasury.export.TreasuryCsvExporter
import com.abccash.app.treasury.repository.TreasuryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class TreasuryUiState(
    val currentUserId: String? = null,
    val currentUserRole: UserRole = UserRole.STAFF,
    val permissions: Set<UserPermission> = emptySet(),
    val entrepriseId: String? = null,
    val users: List<User> = emptyList(),
    val invoices: List<Invoice> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val selectedMonth: YearMonth = YearMonth.now(),
    val importFeedback: String? = null,
    val backupFeedback: String? = null
)

class TreasuryViewModel(private val repository: TreasuryRepository) : ViewModel() {
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
        dueDate: LocalDate
    ) {
        val entrepriseId = requireEntrepriseId() ?: return
        viewModelScope.launch {
            repository.addInvoice(
                Invoice(
                    invoiceNumber = invoiceNumber,
                    clientName = clientName,
                    totalAmount = totalAmount,
                    dueDate = dueDate,
                    entrepriseId = entrepriseId
                )
            )
        }
    }

    fun importInvoices(invoices: List<Invoice>) {
        val entrepriseId = requireEntrepriseId() ?: return
        viewModelScope.launch {
            invoices.forEach { invoice ->
                repository.addInvoice(invoice.copy(entrepriseId = entrepriseId))
            }
            _uiState.update {
                it.copy(importFeedback = "${invoices.size} facture(s) importée(s)")
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
        dueDate: LocalDate
    ): Boolean {
        val existing = getInvoice(invoiceId) ?: return false
        if (totalAmount < existing.paidAmount) return false
        viewModelScope.launch {
            repository.updateInvoice(
                existing.copy(
                    invoiceNumber = invoiceNumber,
                    clientName = clientName,
                    totalAmount = totalAmount,
                    dueDate = dueDate
                )
            )
        }
        return true
    }

    fun deleteInvoice(invoiceId: String) {
        viewModelScope.launch {
            repository.deleteInvoice(invoiceId)
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
                    isPaid = isPaid
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
        permissions: Set<UserPermission>
    ) {
        val entrepriseId = requireEntrepriseId() ?: return
        viewModelScope.launch {
            repository.addUser(
                User(
                    nom = nom,
                    email = email.trim(),
                    telephone = telephone.trim(),
                    passwordHash = password,
                    role = role,
                    permissions = permissions,
                    entrepriseId = entrepriseId
                )
            )
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

    fun buildCsvExport(): String? {
        val state = _uiState.value
        if (state.entrepriseId == null) return null
        return TreasuryCsvExporter.export(
            invoices = state.invoices,
            expenses = state.expenses,
            selectedMonth = state.selectedMonth
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
        return _uiState.value.invoices
            .flatMap { it.payments }
            .filter { YearMonth.from(it.date) == yearMonth }
            .sumOf { it.amount }
    }

    fun getMonthlyExpenses(yearMonth: YearMonth): Double {
        return _uiState.value.expenses
            .filter { it.appliesToMonth(yearMonth) }
            .sumOf { it.amount }
    }

    fun getMonthlyBalance(yearMonth: YearMonth): Double {
        return getMonthlyCollections(yearMonth) - getMonthlyExpenses(yearMonth)
    }

    fun getForecastedBalance(yearMonth: YearMonth): Double {
        val currentBalance = getMonthlyBalance(yearMonth)
        val pendingInvoices = _uiState.value.invoices
            .filter { it.status != InvoiceStatus.PAID && YearMonth.from(it.dueDate) == yearMonth }
            .sumOf { it.remainingAmount }
        val upcomingExpenses = _uiState.value.expenses
            .filter { !it.isPaid && it.appliesToMonth(yearMonth) }
            .sumOf { it.amount }
        return currentBalance + pendingInvoices - upcomingExpenses
    }
}

class TreasuryViewModelFactory(private val repository: TreasuryRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TreasuryViewModel::class.java)) {
            return TreasuryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
