package com.abccash.app.treasury.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abccash.app.treasury.data.PilotCategory
import com.abccash.app.treasury.data.PilotEntry
import com.abccash.app.treasury.data.PilotEntryType
import com.abccash.app.treasury.data.PilotMonthlyTarget
import com.abccash.app.treasury.data.PilotText
import java.time.LocalDateTime
import java.util.UUID
import com.abccash.app.treasury.importer.PilotCategoryAction
import com.abccash.app.treasury.importer.PilotColumn
import com.abccash.app.treasury.importer.PilotColumnMapping
import com.abccash.app.treasury.importer.PilotDuplicateMode
import com.abccash.app.treasury.importer.PilotImportAnalysis
import com.abccash.app.treasury.importer.PilotImportParser
import com.abccash.app.treasury.importer.PilotImportPreview
import com.abccash.app.treasury.importer.PilotParsedLine
import com.abccash.app.treasury.importer.PilotTable
import com.abccash.app.treasury.repository.PilotageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.YearMonth

enum class PilotImportStep {
    MAPPING,
    PREVIEW
}

data class PilotImportSession(
    val type: PilotEntryType,
    val filename: String,
    val table: PilotTable,
    val mapping: PilotColumnMapping,
    val step: PilotImportStep,
    val duplicateMode: PilotDuplicateMode = PilotDuplicateMode.IGNORE,
    val reviewedIndexes: Set<Int> = emptySet(),
    val categoryActions: Map<String, PilotCategoryAction> = emptyMap(),
    val error: String? = null
) {
    val lines: List<PilotParsedLine>
        get() = if (mapping.isComplete) PilotImportParser.parseLines(table, mapping) else emptyList()
}

data class PilotageUiState(
    val entrepriseId: String = "",
    val month: YearMonth = YearMonth.now(),
    val categories: List<PilotCategory> = emptyList(),
    val entries: List<PilotEntry> = emptyList(),
    val targets: List<PilotMonthlyTarget> = emptyList(),
    val ready: Boolean = false,
    val importSession: PilotImportSession? = null,
    val feedback: String? = null,
    val pendingEntryId: String? = null
)

class PilotageViewModel(
    private val repository: PilotageRepository
) : ViewModel() {
    private val _state = MutableStateFlow(PilotageUiState())
    val state: StateFlow<PilotageUiState> = _state.asStateFlow()
    private var observeJob: Job? = null

    fun bind(entrepriseId: String) {
        if (entrepriseId.isBlank() || _state.value.entrepriseId == entrepriseId && observeJob?.isActive == true) return
        observeJob?.cancel()
        _state.update { it.copy(entrepriseId = entrepriseId, ready = false, importSession = null) }
        viewModelScope.launch {
            // Relie les lignes déjà saisies par l'utilisateur (pas d'import auto de données étrangères).
            repository.applyDefaultSaleCollections(entrepriseId)
        }
        observeJob = viewModelScope.launch {
            combine(
                repository.observeCategories(entrepriseId),
                repository.observeEntries(entrepriseId),
                repository.observeTargets(entrepriseId)
            ) { categories, entries, targets ->
                Triple(categories, entries, targets)
            }.collect { (categories, entries, targets) ->
                _state.update {
                    it.copy(
                        categories = categories,
                        entries = entries,
                        targets = targets,
                        ready = true
                    )
                }
            }
        }
    }

    fun setMonth(month: YearMonth) {
        _state.update { it.copy(month = month) }
    }

    /** À appeler quand les comptes bancaires deviennent disponibles. */
    fun applyCashDefaultsIfNeeded() {
        val entrepriseId = _state.value.entrepriseId
        if (entrepriseId.isBlank()) return
        viewModelScope.launch {
            repository.applyDefaultSaleCollections(entrepriseId)
        }
    }

    fun openEntry(entryId: String) {
        val entry = _state.value.entries.find { it.id == entryId }
        _state.update {
            it.copy(
                pendingEntryId = entryId,
                month = entry?.let { found -> YearMonth.from(found.date) } ?: it.month
            )
        }
    }

    fun consumePendingEntry() {
        _state.update { it.copy(pendingEntryId = null) }
    }

    fun clearFeedback() {
        _state.update { it.copy(feedback = null) }
    }

    fun saveEntry(
        entryId: String?,
        type: PilotEntryType,
        date: LocalDate,
        categoryId: String?,
        newCategoryName: String?,
        amount: Double,
        note: String,
        recurrenceMonths: Int,
        onResult: (String?) -> Unit
    ) {
        val entrepriseId = _state.value.entrepriseId
        if (entrepriseId.isBlank()) {
            onResult(com.abccash.app.treasury.repository.PilotCodes.MISSING)
            return
        }
        viewModelScope.launch {
            val resolvedId = when {
                !categoryId.isNullOrBlank() -> categoryId
                !newCategoryName.isNullOrBlank() -> {
                    val created = repository.saveCategory(entrepriseId, type, newCategoryName)
                    if (created.error != null) {
                        onResult(created.error)
                        return@launch
                    }
                    created.id
                }
                else -> null
            }
            if (resolvedId == null) {
                onResult(com.abccash.app.treasury.repository.PilotCodes.CATEGORY)
                return@launch
            }
            onResult(
                repository.saveEntry(
                    entrepriseId = entrepriseId,
                    entryId = entryId,
                    type = type,
                    date = date,
                    categoryId = resolvedId,
                    amount = amount,
                    note = note,
                    recurrenceMonths = recurrenceMonths
                )
            )
        }
    }

    fun saveMovement(
        entryId: String?,
        type: PilotEntryType,
        date: LocalDate,
        categoryId: String?,
        amount: Double,
        note: String,
        recurrenceMonths: Int,
        treasuryDate: LocalDate?,
        bankAccountId: String?,
        counterAccountId: String?,
        strictCash: Boolean,
        requireAccount: Boolean,
        onResult: (String?) -> Unit
    ) {
        val entrepriseId = _state.value.entrepriseId
        if (entrepriseId.isBlank()) {
            onResult(com.abccash.app.treasury.repository.PilotCodes.MISSING)
            return
        }
        viewModelScope.launch {
            val current = entryId?.let { id -> _state.value.entries.find { it.id == id } }
            if (entryId != null && current == null) {
                onResult(com.abccash.app.treasury.repository.PilotCodes.MISSING)
                return@launch
            }
            val now = LocalDateTime.now()
            onResult(
                repository.saveMovement(
                    PilotEntry(
                        id = current?.id ?: UUID.randomUUID().toString(),
                        entrepriseId = entrepriseId,
                        type = type,
                        date = date,
                        categoryId = categoryId,
                        amount = amount,
                        note = note,
                        recurrenceMonths = recurrenceMonths,
                        treasuryDate = treasuryDate,
                        bankAccountId = bankAccountId,
                        counterAccountId = counterAccountId,
                        importId = current?.importId,
                        createdAt = current?.createdAt ?: now,
                        updatedAt = now
                    ),
                    strictCash = strictCash,
                    requireAccount = requireAccount
                )
            )
        }
    }

    fun deleteEntry(entryId: String) {
        val entrepriseId = _state.value.entrepriseId
        if (entrepriseId.isBlank()) return
        viewModelScope.launch { repository.deleteEntry(entrepriseId, entryId) }
    }

    fun deleteEntries(entryIds: Collection<String>) {
        val entrepriseId = _state.value.entrepriseId
        if (entrepriseId.isBlank()) return
        viewModelScope.launch { repository.deleteEntries(entrepriseId, entryIds) }
    }

    fun bulkUpdateExpenses(
        entryIds: Collection<String>,
        categoryId: String?,
        bankAccountId: String?,
        date: LocalDate?,
        onResult: (String?) -> Unit
    ) {
        val entrepriseId = _state.value.entrepriseId
        if (entrepriseId.isBlank()) {
            onResult(com.abccash.app.treasury.repository.PilotCodes.MISSING)
            return
        }
        viewModelScope.launch {
            onResult(
                repository.bulkUpdateExpenses(
                    entrepriseId = entrepriseId,
                    entryIds = entryIds,
                    categoryId = categoryId,
                    bankAccountId = bankAccountId,
                    date = date
                )
            )
        }
    }

    fun duplicateEntry(entryId: String, onResult: (String?) -> Unit) {
        val entrepriseId = _state.value.entrepriseId
        if (entrepriseId.isBlank()) return
        viewModelScope.launch { onResult(repository.duplicateEntry(entrepriseId, entryId)) }
    }

    fun saveCategory(
        type: PilotEntryType,
        name: String,
        categoryId: String?,
        onResult: (id: String?, error: String?) -> Unit
    ) {
        val entrepriseId = _state.value.entrepriseId
        if (entrepriseId.isBlank()) {
            onResult(null, com.abccash.app.treasury.repository.PilotCodes.MISSING)
            return
        }
        viewModelScope.launch {
            val write = repository.saveCategory(entrepriseId, type, name, categoryId)
            onResult(write.id, write.error)
        }
    }

    fun deleteCategory(categoryId: String, onResult: (String?) -> Unit) {
        val entrepriseId = _state.value.entrepriseId
        if (entrepriseId.isBlank()) return
        viewModelScope.launch { onResult(repository.deleteCategory(entrepriseId, categoryId)) }
    }

    fun copyRecurring(onCopied: (Int) -> Unit) {
        val entrepriseId = _state.value.entrepriseId
        if (entrepriseId.isBlank()) return
        val month = _state.value.month
        viewModelScope.launch { onCopied(repository.copyRecurring(entrepriseId, month)) }
    }

    fun saveTarget(amount: Double?) {
        val entrepriseId = _state.value.entrepriseId
        if (entrepriseId.isBlank()) return
        val month = _state.value.month
        viewModelScope.launch { repository.saveTarget(entrepriseId, month, amount) }
    }

    fun startImport(type: PilotEntryType, filename: String, bytes: ByteArray, mimeType: String?) {
        val table = PilotImportParser.read(filename, ByteArrayInputStream(bytes), mimeType)
        if (table.errorMessage != null) {
            _state.update { it.copy(feedback = table.errorMessage) }
            return
        }
        val mapping = PilotImportParser.suggestMapping(table.headers)
        _state.update {
            it.copy(
                importSession = PilotImportSession(
                    type = type,
                    filename = filename,
                    table = table,
                    mapping = mapping,
                    step = if (mapping.isComplete) PilotImportStep.PREVIEW else PilotImportStep.MAPPING
                )
            )
        }
    }

    fun updateMapping(column: PilotColumn, index: Int?) {
        updateSession { session ->
            session.copy(mapping = session.mapping.with(column, index), error = null)
        }
    }

    fun clearMappedColumn(index: Int) {
        updateSession { session ->
            val mapping = session.mapping
            session.copy(
                mapping = mapping.copy(
                    date = if (mapping.date == index) null else mapping.date,
                    category = if (mapping.category == index) null else mapping.category,
                    amount = if (mapping.amount == index) null else mapping.amount,
                    note = if (mapping.note == index) null else mapping.note
                ),
                error = null
            )
        }
    }

    fun confirmMapping() {
        updateSession { session ->
            if (!session.mapping.isComplete) session.copy(error = "MAPPING")
            else session.copy(step = PilotImportStep.PREVIEW, error = null)
        }
    }

    fun backToMapping() {
        updateSession { it.copy(step = PilotImportStep.MAPPING, error = null) }
    }

    fun setDuplicateMode(mode: PilotDuplicateMode) {
        updateSession { it.copy(duplicateMode = mode) }
    }

    fun toggleReviewed(sourceIndex: Int, include: Boolean) {
        updateSession { session ->
            val next = session.reviewedIndexes.toMutableSet()
            if (include) next += sourceIndex else next -= sourceIndex
            session.copy(reviewedIndexes = next, duplicateMode = PilotDuplicateMode.REVIEW)
        }
    }

    fun setCategoryAction(categoryName: String, action: PilotCategoryAction) {
        updateSession { session ->
            session.copy(
                categoryActions = session.categoryActions + (PilotText.categoryKey(categoryName) to action)
            )
        }
    }

    fun cancelImport() {
        _state.update { it.copy(importSession = null) }
    }

    fun preview(): PilotImportPreview? {
        val session = _state.value.importSession ?: return null
        if (!session.mapping.isComplete) return null
        return PilotImportAnalysis.preview(
            lines = session.lines,
            entries = _state.value.entries,
            categories = _state.value.categories,
            entrepriseId = _state.value.entrepriseId,
            type = session.type,
            duplicateMode = session.duplicateMode,
            reviewedIndexes = session.reviewedIndexes
        )
    }

    fun commitImport(onResult: (String?) -> Unit) {
        val session = _state.value.importSession ?: return
        val preview = preview() ?: return
        val entrepriseId = _state.value.entrepriseId
        if (preview.linesToImport.isEmpty()) {
            onResult(com.abccash.app.treasury.repository.PilotCodes.MISSING)
            return
        }
        viewModelScope.launch {
            val error = repository.importLines(
                entrepriseId = entrepriseId,
                type = session.type,
                filename = session.filename,
                lines = preview.linesToImport,
                ignoredRows = preview.invalidCount + (preview.validLines.size - preview.linesToImport.size),
                actions = session.categoryActions
            )
            if (error == null) _state.update { it.copy(importSession = null) }
            onResult(error)
        }
    }

    private fun updateSession(transform: (PilotImportSession) -> PilotImportSession) {
        _state.update { state ->
            val session = state.importSession ?: return@update state
            state.copy(importSession = transform(session))
        }
    }
}

class PilotageViewModelFactory(
    private val repository: PilotageRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PilotageViewModel::class.java)) {
            return PilotageViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
