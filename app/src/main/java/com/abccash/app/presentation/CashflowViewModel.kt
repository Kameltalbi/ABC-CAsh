package com.abccash.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abccash.app.data.local.dao.BalanceDao
import com.abccash.app.data.local.dao.CategoryDao
import com.abccash.app.data.local.dao.TransactionDao
import com.abccash.app.data.local.entity.BalanceEntity
import com.abccash.app.data.local.entity.CategoryEntity
import com.abccash.app.data.local.entity.CurrencyCode
import com.abccash.app.data.local.entity.RecurrenceRule
import com.abccash.app.data.local.entity.TransactionEntity
import com.abccash.app.data.local.entity.TransactionStatus
import com.abccash.app.data.local.entity.TransactionType
import com.abccash.app.domain.model.CumulativeForecastPoint
import com.abccash.app.domain.usecase.Build12MonthCumulativeForecastUseCase
import com.abccash.app.domain.usecase.CreateTransactionWithRecurrenceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class CashflowUiState(
    val selectedCurrency: CurrencyCode = CurrencyCode.DT,
    val openingBalanceMinor: Long = 0L,
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val forecast: List<CumulativeForecastPoint> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val dataChangeVersion: Long = 0L
)

class CashflowViewModel(
    private val transactionDao: TransactionDao,
    private val balanceDao: BalanceDao,
    private val categoryDao: CategoryDao,
    private val createTransactionUseCase: CreateTransactionWithRecurrenceUseCase,
    private val buildForecastUseCase: Build12MonthCumulativeForecastUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CashflowUiState())
    val uiState: StateFlow<CashflowUiState> = _uiState.asStateFlow()

    init {
        refreshAll()
    }

    fun setCurrency(currency: CurrencyCode) {
        _uiState.update { it.copy(selectedCurrency = currency) }
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching {
                val today = LocalDate.now().toString()
                transactionDao.markOverdue(today = today, updatedAt = LocalDateTime.now().toString())
                ensureDefaultCategories()

                val currencyName = _uiState.value.selectedCurrency.name
                val opening = balanceDao.getByCurrency(currencyName)?.amountMinor ?: 0L
                val txList = transactionDao.getByCurrency(currencyName)
                val categories = categoryDao.getAll()
                val forecast = buildForecastUseCase(currencyName)
                Quad(opening, txList, categories, forecast)
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        openingBalanceMinor = result.first,
                        transactions = result.second,
                        categories = result.third,
                        forecast = result.fourth,
                        loading = false
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(loading = false, error = err.message) }
            }
        }
    }

    fun saveOpeningBalance(amountMinor: Long) {
        android.util.Log.d("CashflowViewModel", "saveOpeningBalance called with: $amountMinor")
        viewModelScope.launch {
            val balance = BalanceEntity(
                currency = _uiState.value.selectedCurrency,
                amountMinor = amountMinor,
                updatedAt = LocalDateTime.now()
            )
            android.util.Log.d("CashflowViewModel", "Saving balance: $balance")
            balanceDao.upsert(balance)
            android.util.Log.d("CashflowViewModel", "Balance saved, refreshing...")
            refreshAll()
            markDataChanged()
            android.util.Log.d("CashflowViewModel", "Refresh complete, new balance: ${_uiState.value.openingBalanceMinor}")
        }
    }

    fun addQuickTransaction(
        type: TransactionType,
        amountMinor: Long,
        category: String,
        status: TransactionStatus,
        date: LocalDate,
        recurrenceRule: RecurrenceRule
    ) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                type = type,
                amountMinor = amountMinor,
                currency = _uiState.value.selectedCurrency,
                date = date,
                category = category,
                status = status,
                isRecurring = recurrenceRule != RecurrenceRule.NONE,
                recurrenceRule = recurrenceRule
            )
            createTransactionUseCase(tx)
            refreshAll()
            markDataChanged()
        }
    }

    fun validateTransaction(id: Long) {
        viewModelScope.launch {
            transactionDao.validateTransaction(id, LocalDateTime.now().toString())
            refreshAll()
            markDataChanged()
        }
    }

    fun setTransactionStatus(id: Long, status: TransactionStatus) {
        viewModelScope.launch {
            val current = _uiState.value.transactions.firstOrNull { it.id == id } ?: return@launch
            transactionDao.update(
                current.copy(
                    status = status,
                    updatedAt = LocalDateTime.now()
                )
            )
            refreshAll()
            markDataChanged()
        }
    }

    fun postponeTransactionOneMonth(id: Long) {
        viewModelScope.launch {
            val current = _uiState.value.transactions.firstOrNull { it.id == id } ?: return@launch
            if (current.isRecurring) {
                editTransaction(
                    id = id,
                    amountMinor = current.amountMinor,
                    newDate = current.date.plusMonths(1),
                    applyToSeries = true
                )
                return@launch
            }
            transactionDao.update(current.copy(date = current.date.plusMonths(1), status = TransactionStatus.PLANIFIEE, updatedAt = LocalDateTime.now()))
            refreshAll()
            markDataChanged()
        }
    }

    fun deleteTransaction(id: Long, applyToSeries: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.transactions.firstOrNull { it.id == id } ?: return@launch
            if (applyToSeries && current.isRecurring) {
                val seriesRootId = current.parentRecurringId ?: current.id
                transactionDao.deleteSeriesByRootId(seriesRootId)
            } else {
                transactionDao.deleteById(id)
            }
            refreshAll()
            markDataChanged()
        }
    }

    fun editTransaction(
        id: Long,
        amountMinor: Long,
        newDate: LocalDate,
        applyToSeries: Boolean
    ) {
        viewModelScope.launch {
            val current = _uiState.value.transactions.firstOrNull { it.id == id } ?: return@launch
            val updatedAt = LocalDateTime.now().toString()
            if (applyToSeries && current.isRecurring) {
                val seriesRootId = current.parentRecurringId ?: current.id
                val deltaDays = ChronoUnit.DAYS.between(current.date, newDate).toInt()
                transactionDao.editRecurringSeriesAmountAndDateShift(
                    seriesRootId = seriesRootId,
                    amountMinor = amountMinor,
                    deltaDays = deltaDays,
                    updatedAt = updatedAt
                )
            } else {
                transactionDao.update(
                    current.copy(
                        amountMinor = amountMinor,
                        date = newDate,
                        updatedAt = LocalDateTime.now()
                    )
                )
            }
            refreshAll()
            markDataChanged()
        }
    }

    fun addCategory(name: String, type: TransactionType) {
        viewModelScope.launch {
            val cleaned = name.trim()
            if (cleaned.isBlank()) return@launch
            categoryDao.insert(CategoryEntity(name = cleaned, type = type))
            refreshAll()
            markDataChanged()
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            categoryDao.deleteById(id)
            refreshAll()
            markDataChanged()
        }
    }

    fun exportBackup(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                val root = JSONObject()
                val balances = balanceDao.getAll()
                val categories = categoryDao.getAll()
                val transactions = transactionDao.getAll()

                root.put("version", 1)
                root.put("exportedAt", LocalDateTime.now().toString())

                val balancesJson = JSONArray()
                balances.forEach { b ->
                    balancesJson.put(
                        JSONObject()
                            .put("id", b.id)
                            .put("currency", b.currency.name)
                            .put("amountMinor", b.amountMinor)
                            .put("updatedAt", b.updatedAt.toString())
                    )
                }
                root.put("balances", balancesJson)

                val categoriesJson = JSONArray()
                categories.forEach { c ->
                    categoriesJson.put(
                        JSONObject()
                            .put("id", c.id)
                            .put("name", c.name)
                            .put("type", c.type.name)
                            .put("createdAt", c.createdAt.toString())
                    )
                }
                root.put("categories", categoriesJson)

                val transactionsJson = JSONArray()
                transactions.forEach { t ->
                    transactionsJson.put(
                        JSONObject()
                            .put("id", t.id)
                            .put("type", t.type.name)
                            .put("amountMinor", t.amountMinor)
                            .put("currency", t.currency.name)
                            .put("date", t.date.toString())
                            .put("category", t.category)
                            .put("status", t.status.name)
                            .put("isRecurring", t.isRecurring)
                            .put("recurrenceRule", t.recurrenceRule.name)
                            .put("parentRecurringId", t.parentRecurringId)
                            .put("note", t.note)
                            .put("createdAt", t.createdAt.toString())
                            .put("updatedAt", t.updatedAt.toString())
                    )
                }
                root.put("transactions", transactionsJson)
                root.toString(2)
            }.onSuccess(onSuccess).onFailure { onError(it.message ?: "Erreur export") }
        }
    }

    fun importBackup(
        json: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                val root = JSONObject(json)
                val balances = root.optJSONArray("balances") ?: JSONArray()
                val categories = root.optJSONArray("categories") ?: JSONArray()
                val transactions = root.optJSONArray("transactions") ?: JSONArray()

                val balanceEntities = buildList {
                    for (i in 0 until balances.length()) {
                        val b = balances.getJSONObject(i)
                        add(
                            BalanceEntity(
                                id = b.optLong("id"),
                                currency = CurrencyCode.valueOf(b.getString("currency")),
                                amountMinor = b.getLong("amountMinor"),
                                updatedAt = LocalDateTime.parse(b.getString("updatedAt"))
                            )
                        )
                    }
                }
                val categoryEntities = buildList {
                    for (i in 0 until categories.length()) {
                        val c = categories.getJSONObject(i)
                        add(
                            CategoryEntity(
                                id = c.optLong("id"),
                                name = c.getString("name"),
                                type = TransactionType.valueOf(c.getString("type")),
                                createdAt = LocalDateTime.parse(c.getString("createdAt"))
                            )
                        )
                    }
                }
                val transactionEntities = buildList {
                    for (i in 0 until transactions.length()) {
                        val t = transactions.getJSONObject(i)
                        add(
                            TransactionEntity(
                                id = t.optLong("id"),
                                type = TransactionType.valueOf(t.getString("type")),
                                amountMinor = t.getLong("amountMinor"),
                                currency = CurrencyCode.valueOf(t.getString("currency")),
                                date = LocalDate.parse(t.getString("date")),
                                category = t.getString("category"),
                                status = TransactionStatus.valueOf(t.getString("status")),
                                isRecurring = t.optBoolean("isRecurring", false),
                                recurrenceRule = RecurrenceRule.valueOf(t.optString("recurrenceRule", RecurrenceRule.NONE.name)),
                                parentRecurringId = if (t.isNull("parentRecurringId")) null else t.optLong("parentRecurringId"),
                                note = if (t.isNull("note")) null else t.optString("note"),
                                createdAt = LocalDateTime.parse(t.getString("createdAt")),
                                updatedAt = LocalDateTime.parse(t.getString("updatedAt"))
                            )
                        )
                    }
                }

                transactionDao.deleteAll()
                categoryDao.deleteAll()
                balanceDao.deleteAll()
                if (balanceEntities.isNotEmpty()) balanceDao.insertAll(balanceEntities)
                if (categoryEntities.isNotEmpty()) categoryDao.insertAll(categoryEntities)
                if (transactionEntities.isNotEmpty()) transactionDao.insertAll(transactionEntities)
            }.onSuccess {
                refreshAll()
                markDataChanged()
                onSuccess()
            }.onFailure { onError(it.message ?: "Erreur import") }
        }
    }

    private fun markDataChanged() {
        _uiState.update { it.copy(dataChangeVersion = it.dataChangeVersion + 1) }
    }

    private suspend fun ensureDefaultCategories() {
        val existing = categoryDao.getAll()
        if (existing.any { it.name.equals("Salaire", ignoreCase = true) && it.type == TransactionType.INCOME }) {
            categoryDao.deleteByNameAndType("Salaire", TransactionType.INCOME.name)
        }

        val defaultBusinessCategories = listOf(
            CategoryEntity(name = "Ventes", type = TransactionType.INCOME),
            CategoryEntity(name = "Prestations", type = TransactionType.INCOME),
            CategoryEntity(name = "Encaissements clients", type = TransactionType.INCOME),
            CategoryEntity(name = "Produits financiers", type = TransactionType.INCOME),
            CategoryEntity(name = "Loyer", type = TransactionType.EXPENSE),
            CategoryEntity(name = "Factures", type = TransactionType.EXPENSE),
            CategoryEntity(name = "Salaires et charges", type = TransactionType.EXPENSE),
            CategoryEntity(name = "Transport", type = TransactionType.EXPENSE)
        )
        defaultBusinessCategories.forEach { categoryDao.insert(it) }
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
