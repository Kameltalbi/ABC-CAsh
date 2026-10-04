package com.abccash.app.treasury.data

import java.time.LocalDate

object PilotCodes {
    const val AMOUNT = "AMOUNT"
    const val CATEGORY = "CATEGORY"
    const val NAME = "NAME"
    const val EXISTS = "EXISTS"
    const val USED = "USED"
    const val MISSING = "MISSING"
    const val ACCOUNT = "ACCOUNT"
    const val TRANSFER = "TRANSFER"
    const val TYPE = "TYPE"
    const val TENANT = "TENANT"
    const val DATE = "DATE"
    const val DUPLICATE = "DUPLICATE"
}

enum class CashSourceType {
    PILOTAGE,
    LEGACY_PAYMENT,
    LEGACY_EXPENSE,
    BALANCE_CORRECTION,
    OPENING_BALANCE
}

data class CashFlowItem(
    val sourceType: CashSourceType,
    val sourceId: String,
    val entrepriseId: String,
    val date: LocalDate,
    val signedAmount: Double,
    val bankAccountId: String,
    val label: String
)

object PilotRules {
    /** Date d'encaissement effective : saisie explicite, sinon date de la vente. */
    fun effectiveCollectionDate(entry: PilotEntry): LocalDate =
        entry.treasuryDate ?: entry.date

    fun withDefaultCollection(entry: PilotEntry): PilotEntry {
        if (entry.type != PilotEntryType.SALE) return entry
        if (entry.treasuryDate != null) return entry
        return entry.copy(treasuryDate = entry.date)
    }

    fun validate(
        entry: PilotEntry,
        categoryEntrepriseId: String? = null,
        categoryType: PilotEntryType? = null,
        sourceEntrepriseId: String? = null,
        destinationEntrepriseId: String? = null,
        strictCash: Boolean = false,
        requireAccount: Boolean = true
    ): String? {
        if (!PilotMoney.isPositive(entry.amount)) return PilotCodes.AMOUNT
        if (entry.type != PilotEntryType.TRANSFER && entry.counterAccountId != null) return PilotCodes.TYPE
        if (entry.type.needsCategory) {
            if (entry.categoryId.isNullOrBlank() || categoryType == null) return PilotCodes.CATEGORY
            if (categoryType != entry.type) return PilotCodes.CATEGORY
            if (categoryEntrepriseId != entry.entrepriseId) return PilotCodes.TENANT
        }
        val sourceError = accountTenant(entry.bankAccountId, sourceEntrepriseId, entry.entrepriseId)
        if (sourceError != null) return sourceError
        val destinationError = accountTenant(entry.counterAccountId, destinationEntrepriseId, entry.entrepriseId)
        if (destinationError != null) return destinationError
        return when (entry.type) {
            PilotEntryType.SALE -> saleCash(entry, strictCash, requireAccount)
            PilotEntryType.EXPENSE ->
                if (strictCash && requireAccount && entry.bankAccountId.isNullOrBlank()) PilotCodes.ACCOUNT else null
            PilotEntryType.OTHER_INFLOW,
            PilotEntryType.OTHER_OUTFLOW -> if (entry.bankAccountId.isNullOrBlank()) PilotCodes.ACCOUNT else null
            PilotEntryType.TRANSFER -> transfer(entry)
        }
    }

    fun requiresStrictCash(
        existing: PilotEntry?,
        type: PilotEntryType,
        treasuryDate: LocalDate?,
        bankAccountId: String?
    ): Boolean = when (type) {
        PilotEntryType.SALE -> existing == null ||
            treasuryDate != null ||
            !bankAccountId.isNullOrBlank() ||
            existing.treasuryDate != null ||
            !existing.bankAccountId.isNullOrBlank()
        PilotEntryType.EXPENSE -> existing == null ||
            !bankAccountId.isNullOrBlank() ||
            !existing.bankAccountId.isNullOrBlank()
        else -> true
    }

    fun suggestedAccount(accounts: List<BankAccount>, entrepriseId: String): BankAccount? {
        val own = accounts.filter { it.entrepriseId == entrepriseId }
        return if (own.size == 1) own.first() else own.firstOrNull { it.isDefault }
    }

    private fun accountTenant(accountId: String?, accountEntrepriseId: String?, entrepriseId: String): String? {
        if (accountId.isNullOrBlank()) return null
        if (accountEntrepriseId == null || accountEntrepriseId != entrepriseId) return PilotCodes.TENANT
        return null
    }

    private fun saleCash(entry: PilotEntry, strictCash: Boolean, requireAccount: Boolean): String? {
        val hasDate = entry.treasuryDate != null
        val hasAccount = !entry.bankAccountId.isNullOrBlank()
        if (!strictCash) {
            if (hasDate != hasAccount) return if (!hasDate) PilotCodes.DATE else PilotCodes.ACCOUNT
            return null
        }
        if (!hasDate) return PilotCodes.DATE
        if (requireAccount && !hasAccount) return PilotCodes.ACCOUNT
        return null
    }

    private fun transfer(entry: PilotEntry): String? {
        if (entry.bankAccountId.isNullOrBlank() || entry.counterAccountId.isNullOrBlank()) return PilotCodes.ACCOUNT
        if (entry.bankAccountId == entry.counterAccountId) return PilotCodes.TRANSFER
        return null
    }
}

object PilotCash {
    fun items(entry: PilotEntry, label: String = entry.note): List<CashFlowItem> {
        if (!PilotMoney.isPositive(entry.amount)) return emptyList()
        val amount = PilotMoney.decimal(entry.amount).toDouble()
        return when (entry.type) {
            PilotEntryType.SALE -> {
                // Sans date d'encaissement : on prend la date de saisie (modifiable à l'édition).
                val date = entry.treasuryDate ?: entry.date
                val account = entry.bankAccountId ?: return emptyList()
                listOf(item(entry, date, account, amount, label))
            }
            PilotEntryType.EXPENSE -> {
                val account = entry.bankAccountId ?: return emptyList()
                listOf(item(entry, entry.date, account, -amount, label))
            }
            PilotEntryType.OTHER_INFLOW -> {
                val account = entry.bankAccountId ?: return emptyList()
                listOf(item(entry, entry.date, account, amount, label))
            }
            PilotEntryType.OTHER_OUTFLOW -> {
                val account = entry.bankAccountId ?: return emptyList()
                listOf(item(entry, entry.date, account, -amount, label))
            }
            PilotEntryType.TRANSFER -> {
                val source = entry.bankAccountId
                val destination = entry.counterAccountId
                if (source.isNullOrBlank() || destination.isNullOrBlank() || source == destination) emptyList()
                else listOf(
                    item(entry, entry.date, source, -amount, label, "#out"),
                    item(entry, entry.date, destination, amount, label, "#in")
                )
            }
        }
    }

    fun items(entries: List<PilotEntry>): List<CashFlowItem> = entries.flatMap { items(it) }

    fun signedOn(items: List<CashFlowItem>, bankAccountId: String): Double =
        PilotMoney.sum(items.filter { it.bankAccountId == bankAccountId }.map { it.signedAmount })

    fun legacyPayment(
        entrepriseId: String,
        paymentId: String,
        date: LocalDate,
        amount: Double,
        bankAccountId: String,
        label: String
    ): CashFlowItem = outside(CashSourceType.LEGACY_PAYMENT, paymentId, entrepriseId, date, amount, bankAccountId, label)

    fun legacyExpense(
        entrepriseId: String,
        expenseId: String,
        date: LocalDate,
        amount: Double,
        bankAccountId: String,
        label: String
    ): CashFlowItem = outside(CashSourceType.LEGACY_EXPENSE, expenseId, entrepriseId, date, -amount, bankAccountId, label)

    fun openingBalance(
        entrepriseId: String,
        accountId: String,
        date: LocalDate,
        amount: Double,
        label: String
    ): CashFlowItem = outside(CashSourceType.OPENING_BALANCE, accountId, entrepriseId, date, amount, accountId, label)

    fun balanceCorrection(
        entrepriseId: String,
        correctionId: String,
        date: LocalDate,
        signedDelta: Double,
        bankAccountId: String,
        label: String
    ): CashFlowItem = outside(
        CashSourceType.BALANCE_CORRECTION,
        correctionId,
        entrepriseId,
        date,
        signedDelta,
        bankAccountId,
        label
    )

    private fun item(
        entry: PilotEntry,
        date: LocalDate,
        bankAccountId: String,
        signedAmount: Double,
        label: String,
        suffix: String = ""
    ) = CashFlowItem(
        sourceType = CashSourceType.PILOTAGE,
        sourceId = entry.id + suffix,
        entrepriseId = entry.entrepriseId,
        date = date,
        signedAmount = signedAmount,
        bankAccountId = bankAccountId,
        label = label
    )

    private fun outside(
        sourceType: CashSourceType,
        sourceId: String,
        entrepriseId: String,
        date: LocalDate,
        signedAmount: Double,
        bankAccountId: String,
        label: String
    ) = CashFlowItem(
        sourceType = sourceType,
        sourceId = sourceId,
        entrepriseId = entrepriseId,
        date = date,
        signedAmount = PilotMoney.decimal(signedAmount).toDouble(),
        bankAccountId = bankAccountId,
        label = label
    )
}
