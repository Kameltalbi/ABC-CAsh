package com.abccash.app.treasury.repository

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.abccash.app.treasury.data.PilotCalculations
import com.abccash.app.treasury.data.PilotCategory
import com.abccash.app.treasury.data.PilotEntry
import com.abccash.app.treasury.data.PilotEntryType
import com.abccash.app.treasury.data.PilotImportRecord
import com.abccash.app.treasury.data.PilotMoney
import com.abccash.app.treasury.data.PilotMonthlyTarget
import com.abccash.app.treasury.data.PilotRecurrence
import com.abccash.app.treasury.data.PilotRules
import com.abccash.app.treasury.data.PilotText
import com.abccash.app.treasury.importer.PilotCategoryAction
import com.abccash.app.treasury.importer.PilotParsedLine
import com.abccash.app.treasury.local.TreasuryDao
import com.abccash.app.treasury.local.toDomain
import com.abccash.app.treasury.local.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

typealias PilotCodes = com.abccash.app.treasury.data.PilotCodes

data class PilotWrite(
    val id: String? = null,
    val error: String? = null
)

class PilotageRepository(
    private val dao: TreasuryDao,
    private val database: RoomDatabase
) {
    fun observeCategories(entrepriseId: String): Flow<List<PilotCategory>> =
        dao.observePilotCategories(entrepriseId).map { list -> list.map { it.toDomain() } }

    fun observeEntries(entrepriseId: String): Flow<List<PilotEntry>> =
        dao.observePilotEntries(entrepriseId).map { list -> list.map { it.toDomain() } }

    fun observeTargets(entrepriseId: String): Flow<List<PilotMonthlyTarget>> =
        dao.observePilotTargets(entrepriseId).map { list -> list.map { it.toDomain() } }

    suspend fun saveCategory(
        entrepriseId: String,
        type: PilotEntryType,
        name: String,
        categoryId: String? = null
    ): PilotWrite {
        if (!type.needsCategory) return PilotWrite(error = PilotCodes.TYPE)
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return PilotWrite(error = PilotCodes.NAME)
        val key = PilotText.categoryKey(trimmed)
        val categories = currentCategories(entrepriseId)
        val clash = categories.any {
            it.type == type && it.id != categoryId && PilotText.categoryKey(it.name) == key
        }
        if (clash) return PilotWrite(error = PilotCodes.EXISTS)
        val now = LocalDateTime.now()
        return try {
            if (categoryId == null) {
                val created = PilotCategory(
                    entrepriseId = entrepriseId,
                    type = type,
                    name = trimmed,
                    colorIndex = PilotCalculations.nextRubriqueColor(
                        categories.filter { it.type == type }.map { it.colorIndex }
                    ),
                    createdAt = now,
                    updatedAt = now
                )
                dao.upsertPilotCategory(created.toEntity())
                PilotWrite(id = created.id)
            } else {
                val current = categories.find { it.id == categoryId && it.entrepriseId == entrepriseId }
                    ?: return PilotWrite(error = PilotCodes.MISSING)
                dao.upsertPilotCategory(current.copy(name = trimmed, updatedAt = now).toEntity())
                PilotWrite(id = current.id)
            }
        } catch (error: Exception) {
            val code = if (error.message?.contains("UNIQUE", ignoreCase = true) == true) {
                PilotCodes.EXISTS
            } else {
                error.message
            }
            PilotWrite(error = code)
        }
    }

    suspend fun deleteCategory(entrepriseId: String, categoryId: String): String? {
        if (dao.countPilotEntriesForCategory(categoryId, entrepriseId) > 0) return PilotCodes.USED
        dao.deletePilotCategory(categoryId, entrepriseId)
        return null
    }

    suspend fun saveEntry(
        entrepriseId: String,
        entryId: String?,
        type: PilotEntryType,
        date: LocalDate,
        categoryId: String,
        amount: Double,
        note: String,
        recurrenceMonths: Int
    ): String? {
        if (!PilotMoney.isPositive(amount)) return PilotCodes.AMOUNT
        val category = currentCategories(entrepriseId).find { it.id == categoryId }
            ?: return PilotCodes.CATEGORY
        if (category.type != type || category.entrepriseId != entrepriseId) return PilotCodes.CATEGORY
        val now = LocalDateTime.now()
        val current = entryId?.let { id ->
            currentEntries(entrepriseId).find { it.id == id && it.entrepriseId == entrepriseId }
        }
        if (entryId != null && current == null) return PilotCodes.MISSING
        val saved = (current ?: PilotEntry(
            entrepriseId = entrepriseId,
            type = type,
            date = date,
            categoryId = categoryId,
            amount = amount,
            createdAt = now,
            updatedAt = now
        )).copy(
            type = type,
            date = date,
            categoryId = categoryId,
            amount = PilotMoney.decimal(amount).toDouble(),
            note = note.trim(),
            recurring = recurrenceMonths > 0,
            recurrenceMonths = PilotRecurrence.fromMonths(recurrenceMonths).months,
            updatedAt = now
        )
        if (PilotCalculations.isActivityDuplicate(currentEntries(entrepriseId), saved)) {
            return PilotCodes.DUPLICATE
        }
        dao.upsertPilotEntry(saved.toEntity())
        return null
    }

    suspend fun saveMovement(
        entry: PilotEntry,
        strictCash: Boolean = true,
        requireAccount: Boolean = true
    ): String? {
        if (entry.entrepriseId.isBlank()) return PilotCodes.TENANT
        val normalized = PilotRules.withDefaultCollection(entry).let { sale ->
            if (sale.type != PilotEntryType.SALE || !sale.bankAccountId.isNullOrBlank()) sale
            else {
                val suggested = suggestedBankAccountId(sale.entrepriseId)
                if (suggested == null) sale else sale.copy(bankAccountId = suggested)
            }
        }
        val category = normalized.categoryId?.let { id ->
            currentCategories(normalized.entrepriseId).find { it.id == id }
        }
        val source = normalized.bankAccountId?.let { dao.findBankAccountById(it) }
        val destination = normalized.counterAccountId?.let { dao.findBankAccountById(it) }
        val error = PilotRules.validate(
            entry = normalized,
            categoryEntrepriseId = category?.entrepriseId,
            categoryType = category?.type,
            sourceEntrepriseId = source?.entrepriseId,
            destinationEntrepriseId = destination?.entrepriseId,
            strictCash = strictCash,
            requireAccount = requireAccount
        )
        if (error != null) return error
        val now = LocalDateTime.now()
        val existing = currentEntries(normalized.entrepriseId).find { it.id == normalized.id }
        val months = if (normalized.type == PilotEntryType.SALE || normalized.type == PilotEntryType.EXPENSE) {
            PilotRecurrence.fromMonths(normalized.recurrenceMonths).months
        } else {
            0
        }
        val saved = normalized.copy(
            amount = PilotMoney.decimal(normalized.amount).toDouble(),
            note = normalized.note.trim(),
            recurring = months > 0,
            recurrenceMonths = months,
            createdAt = existing?.createdAt ?: normalized.createdAt,
            updatedAt = now
        )
        if (PilotCalculations.isActivityDuplicate(currentEntries(saved.entrepriseId), saved)) {
            return PilotCodes.DUPLICATE
        }
        dao.upsertPilotEntry(saved.toEntity())
        return null
    }

    /**
     * Relie les lignes Pilotage à la trésorerie sans saisie manuelle :
     * - ventes : date d'encaissement = date de saisie si absente + compte par défaut
     * - dépenses : compte par défaut si absent
     * À rappeler dès qu'un compte existe (sinon la courbe reste plate).
     */
    suspend fun applyDefaultSaleCollections(entrepriseId: String): Int {
        val accountId = suggestedBankAccountId(entrepriseId) ?: return 0
        val now = LocalDateTime.now()
        val updated = currentEntries(entrepriseId).mapNotNull { entry ->
            when (entry.type) {
                PilotEntryType.SALE -> {
                    val needsDate = entry.treasuryDate == null
                    val needsAccount = entry.bankAccountId.isNullOrBlank()
                    if (!needsDate && !needsAccount) return@mapNotNull null
                    entry.copy(
                        treasuryDate = entry.treasuryDate ?: entry.date,
                        bankAccountId = entry.bankAccountId ?: accountId,
                        updatedAt = now
                    )
                }
                PilotEntryType.EXPENSE -> {
                    if (!entry.bankAccountId.isNullOrBlank()) return@mapNotNull null
                    entry.copy(bankAccountId = accountId, updatedAt = now)
                }
                else -> null
            }
        }
        if (updated.isEmpty()) return 0
        dao.upsertPilotEntries(updated.map { it.toEntity() })
        return updated.size
    }

    private suspend fun suggestedBankAccountId(entrepriseId: String): String? {
        dao.findDefaultBankAccount(entrepriseId)?.id?.let { return it }
        return dao.getBankAccountsForBackup(entrepriseId).firstOrNull()?.id
    }

    suspend fun deleteEntry(entrepriseId: String, entryId: String) {
        dao.deletePilotEntry(entryId, entrepriseId)
    }

    suspend fun deleteEntries(entrepriseId: String, entryIds: Collection<String>) {
        val ids = entryIds.filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) return
        dao.deletePilotEntries(ids, entrepriseId)
    }

    suspend fun bulkUpdateExpenses(
        entrepriseId: String,
        entryIds: Collection<String>,
        categoryId: String?,
        bankAccountId: String?,
        date: LocalDate?
    ): String? {
        val ids = entryIds.filter { it.isNotBlank() }.toSet()
        if (ids.isEmpty()) return PilotCodes.MISSING
        if (categoryId == null && bankAccountId == null && date == null) return PilotCodes.MISSING
        val category = categoryId?.let { id -> currentCategories(entrepriseId).find { it.id == id } }
        if (categoryId != null && (category == null || category.type != PilotEntryType.EXPENSE)) {
            return PilotCodes.CATEGORY
        }
        if (bankAccountId != null) {
            val account = dao.findBankAccountById(bankAccountId)
            if (account == null || account.entrepriseId != entrepriseId) return PilotCodes.ACCOUNT
        }
        val now = LocalDateTime.now()
        val current = currentEntries(entrepriseId)
        val updated = current.filter { it.id in ids && it.type == PilotEntryType.EXPENSE }.map { entry ->
            entry.copy(
                categoryId = categoryId ?: entry.categoryId,
                bankAccountId = bankAccountId ?: entry.bankAccountId,
                date = date ?: entry.date,
                updatedAt = now
            )
        }
        if (updated.isEmpty()) return PilotCodes.MISSING
        for (entry in updated) {
            val error = PilotRules.validate(
                entry = entry,
                categoryEntrepriseId = entrepriseId,
                categoryType = PilotEntryType.EXPENSE,
                sourceEntrepriseId = entrepriseId,
                strictCash = !entry.bankAccountId.isNullOrBlank(),
                requireAccount = false
            )
            if (error != null) return error
            val others = current.filter { it.id != entry.id } +
                updated.filter { it.id != entry.id }
            if (PilotCalculations.isActivityDuplicate(others, entry)) return PilotCodes.DUPLICATE
        }
        dao.upsertPilotEntries(updated.map { it.toEntity() })
        return null
    }

    suspend fun duplicateEntry(entrepriseId: String, entryId: String): String? {
        val current = currentEntries(entrepriseId).find { it.id == entryId } ?: return PilotCodes.MISSING
        val now = LocalDateTime.now()
        val copy = current.copy(
            id = UUID.randomUUID().toString(),
            importId = null,
            createdAt = now,
            updatedAt = now
        )
        if (PilotCalculations.isActivityDuplicate(currentEntries(entrepriseId), copy)) {
            return PilotCodes.DUPLICATE
        }
        dao.upsertPilotEntry(copy.toEntity())
        return null
    }

    suspend fun copyRecurring(entrepriseId: String, targetMonth: YearMonth): Int {
        val copies = PilotCalculations.recurringToCopy(currentEntries(entrepriseId), entrepriseId, targetMonth)
            .map { PilotCalculations.shiftedRecurringCopy(it, targetMonth) }
        if (copies.isEmpty()) return 0
        dao.upsertPilotEntries(copies.map { it.toEntity() })
        return copies.size
    }

    suspend fun saveTarget(entrepriseId: String, month: YearMonth, amount: Double?) {
        val existing = currentTargets(entrepriseId).find { it.year == month.year && it.month == month.monthValue }
        if (amount == null || !PilotMoney.isPositive(amount)) {
            if (existing != null) dao.deletePilotTarget(existing.id, entrepriseId)
            return
        }
        val now = LocalDateTime.now()
        val target = (existing ?: PilotMonthlyTarget(
            entrepriseId = entrepriseId,
            year = month.year,
            month = month.monthValue,
            salesTarget = amount,
            createdAt = now,
            updatedAt = now
        )).copy(
            salesTarget = PilotMoney.decimal(amount).toDouble(),
            updatedAt = now
        )
        dao.upsertPilotTarget(target.toEntity())
    }

    suspend fun importLines(
        entrepriseId: String,
        type: PilotEntryType,
        filename: String,
        lines: List<PilotParsedLine>,
        ignoredRows: Int,
        actions: Map<String, PilotCategoryAction>
    ): String? {
        val valid = lines.filter { it.isValid && it.date != null && it.amount != null && PilotMoney.isPositive(it.amount) }
        if (valid.isEmpty()) return PilotCodes.MISSING
        database.withTransaction {
            val existingCategories = currentCategories(entrepriseId)
            val categories = existingCategories.filter { it.type == type }.toMutableList()
            val usedColors = categories.map { it.colorIndex }.toMutableSet()
            val now = LocalDateTime.now()
            val import = PilotImportRecord(
                entrepriseId = entrepriseId,
                type = type,
                filename = filename,
                importedRows = valid.size,
                ignoredRows = ignoredRows,
                createdAt = now
            )
            dao.upsertPilotImport(import.toEntity())
            val defaultAccountId = if (type == PilotEntryType.SALE) {
                suggestedBankAccountId(entrepriseId)
            } else {
                null
            }
            val entries = valid.map { line ->
                val category = resolveCategory(categories, usedColors, entrepriseId, type, line.categoryName, actions, now)
                PilotEntry(
                    entrepriseId = entrepriseId,
                    type = type,
                    date = line.date!!,
                    categoryId = category.id,
                    amount = PilotMoney.decimal(line.amount!!).toDouble(),
                    note = line.note.trim(),
                    treasuryDate = if (type == PilotEntryType.SALE) line.date else null,
                    bankAccountId = if (type == PilotEntryType.SALE) defaultAccountId else null,
                    importId = import.id,
                    createdAt = now,
                    updatedAt = now
                )
            }
            dao.upsertPilotEntries(entries.map { it.toEntity() })
        }
        return null
    }

    suspend fun deleteEntrepriseData(entrepriseId: String) {
        dao.deletePilotEntriesForEntreprise(entrepriseId)
        dao.deletePilotImportsForEntreprise(entrepriseId)
        dao.deletePilotTargetsForEntreprise(entrepriseId)
        dao.deletePilotCategoriesForEntreprise(entrepriseId)
    }

    private suspend fun resolveCategory(
        categories: MutableList<PilotCategory>,
        usedColors: MutableSet<Int>,
        entrepriseId: String,
        type: PilotEntryType,
        rawName: String,
        actions: Map<String, PilotCategoryAction>,
        now: LocalDateTime
    ): PilotCategory {
        val key = PilotText.categoryKey(rawName)
        categories.find { PilotText.categoryKey(it.name) == key }?.let { return it }
        val action = actions[key]
        val attached = action?.attachToCategoryId?.let { id -> categories.find { it.id == id } }
        if (action != null && !action.create && attached != null) return attached
        val created = PilotCategory(
            entrepriseId = entrepriseId,
            type = type,
            name = rawName.trim(),
            colorIndex = PilotCalculations.nextRubriqueColor(usedColors),
            createdAt = now,
            updatedAt = now
        )
        dao.upsertPilotCategory(created.toEntity())
        categories += created
        usedColors += created.colorIndex
        return created
    }

    private suspend fun currentCategories(entrepriseId: String): List<PilotCategory> =
        dao.observePilotCategories(entrepriseId).first().map { it.toDomain() }

    private suspend fun currentEntries(entrepriseId: String): List<PilotEntry> =
        dao.observePilotEntries(entrepriseId).first().map { it.toDomain() }

    private suspend fun currentTargets(entrepriseId: String): List<PilotMonthlyTarget> =
        dao.observePilotTargets(entrepriseId).first().map { it.toDomain() }
}
