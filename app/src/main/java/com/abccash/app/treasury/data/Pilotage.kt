package com.abccash.app.treasury.data

import androidx.annotation.StringRes
import com.abccash.app.R
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Locale
import java.util.UUID

enum class PilotRecurrence(val months: Int, @StringRes val labelRes: Int) {
    NONE(0, R.string.pilot_recurrence_none),
    MONTHLY(1, R.string.pilot_recurrence_monthly),
    EVERY_2_MONTHS(2, R.string.pilot_recurrence_2),
    QUARTERLY(3, R.string.pilot_recurrence_quarter),
    EVERY_4_MONTHS(4, R.string.pilot_recurrence_4),
    SEMIANNUAL(6, R.string.pilot_recurrence_semester),
    ANNUAL(12, R.string.pilot_recurrence_year);

    companion object {
        fun fromMonths(months: Int, recurring: Boolean = false): PilotRecurrence {
            val value = if (months > 0) months else if (recurring) 1 else 0
            return entries.find { it.months == value } ?: NONE
        }
    }
}

enum class PilotEntryType {
    SALE,
    EXPENSE,
    OTHER_INFLOW,
    OTHER_OUTFLOW,
    TRANSFER;

    val affectsResult: Boolean
        get() = this == SALE || this == EXPENSE

    val needsCategory: Boolean
        get() = this != TRANSFER

    companion object {
        fun fromStored(raw: String?): PilotEntryType? =
            entries.find { it.name == raw }
    }
}

data class PilotCategory(
    val id: String = UUID.randomUUID().toString(),
    val entrepriseId: String,
    val type: PilotEntryType,
    val name: String,
    val active: Boolean = true,
    val colorIndex: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

data class PilotEntry(
    val id: String = UUID.randomUUID().toString(),
    val entrepriseId: String,
    val type: PilotEntryType,
    val date: LocalDate,
    val categoryId: String? = null,
    val amount: Double,
    val note: String = "",
    val recurring: Boolean = false,
    val recurrenceMonths: Int = 0,
    val treasuryDate: LocalDate? = null,
    val bankAccountId: String? = null,
    val counterAccountId: String? = null,
    val importId: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

data class PilotMonthlyTarget(
    val id: String = UUID.randomUUID().toString(),
    val entrepriseId: String,
    val year: Int,
    val month: Int,
    val salesTarget: Double,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

data class PilotImportRecord(
    val id: String = UUID.randomUUID().toString(),
    val entrepriseId: String,
    val type: PilotEntryType,
    val filename: String,
    val importedRows: Int,
    val ignoredRows: Int,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

data class PilotMonthFigures(
    val month: YearMonth,
    val sales: Double,
    val charges: Double,
    val result: Double,
    val coverage: Double?,
    val zeroPoint: Double
)

data class PilotYearFigures(
    val year: Int,
    val months: List<PilotMonthFigures>,
    val sales: Double,
    val charges: Double,
    val result: Double,
    val coverage: Double?
)

data class PilotCategoryShare(
    val categoryId: String,
    val name: String,
    val amount: Double,
    val share: Double?
)

enum class PilotCoverageState {
    NO_CHARGES,
    BELOW,
    AT_ZERO,
    ABOVE
}

object PilotMoney {
    private const val SCALE = 4

    fun decimal(value: Double): BigDecimal =
        BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP)

    fun sum(values: Iterable<Double>): Double =
        values.fold(BigDecimal.ZERO.setScale(SCALE)) { acc, value ->
            acc.add(decimal(value))
        }.setScale(SCALE, RoundingMode.HALF_UP).toDouble()

    fun minus(left: Double, right: Double): Double =
        decimal(left).subtract(decimal(right)).setScale(SCALE, RoundingMode.HALF_UP).toDouble()

    fun ratio(numerator: Double, denominator: Double): Double? {
        val den = decimal(denominator)
        if (den.compareTo(BigDecimal.ZERO) == 0) return null
        return decimal(numerator)
            .divide(den, SCALE + 6, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun isPositive(amount: Double): Boolean =
        decimal(amount).compareTo(BigDecimal.ZERO) > 0

    fun same(left: Double, right: Double): Boolean =
        decimal(left).compareTo(decimal(right)) == 0

    fun percent(ratio: Double): Double =
        BigDecimal.valueOf(ratio)
            .movePointRight(2)
            .setScale(1, RoundingMode.HALF_UP)
            .toDouble()

    fun parse(raw: String): Double? {
        var text = raw.trim()
            .replace("\u00A0", "")
            .replace(" ", "")
        if (text.isEmpty()) return null
        text = text.replace(Regex("(?i)(tnd|dt|eur|usd|dh|€|\\$)"), "")
        text = text.replace(Regex("[^0-9,.-]"), "")
        if (text.isEmpty() || text == "-" || text == "." || text == ",") return null
        val lastComma = text.lastIndexOf(',')
        val lastDot = text.lastIndexOf('.')
        text = when {
            lastComma >= 0 && lastDot >= 0 && lastComma > lastDot ->
                text.replace(".", "").replace(",", ".")
            lastComma >= 0 && lastDot < 0 -> text.replace(",", ".")
            else -> text.replace(",", "")
        }
        val parsed = text.toBigDecimalOrNull() ?: return null
        return parsed.setScale(SCALE, RoundingMode.HALF_UP).toDouble()
    }
}

object PilotText {
    fun categoryKey(name: String): String {
        val folded = name.trim().lowercase(Locale.ROOT)
            .replace('é', 'e').replace('è', 'e').replace('ê', 'e').replace('ë', 'e')
            .replace('à', 'a').replace('â', 'a').replace('ä', 'a')
            .replace('ù', 'u').replace('û', 'u').replace('ü', 'u')
            .replace('ô', 'o').replace('ö', 'o')
            .replace('î', 'i').replace('ï', 'i')
            .replace('ç', 'c')
        return folded.replace(Regex("\\s+"), " ")
    }
}

object PilotCalculations {
    fun monthFigures(
        entries: List<PilotEntry>,
        entrepriseId: String,
        month: YearMonth
    ): PilotMonthFigures {
        val monthEntries = entries.filter {
            it.entrepriseId == entrepriseId && YearMonth.from(it.date) == month
        }
        val sales = PilotMoney.sum(monthEntries.filter { it.type == PilotEntryType.SALE }.map { it.amount })
        val charges = PilotMoney.sum(monthEntries.filter { it.type == PilotEntryType.EXPENSE }.map { it.amount })
        return figures(month, sales, charges)
    }

    fun yearFigures(
        entries: List<PilotEntry>,
        entrepriseId: String,
        year: Int
    ): PilotYearFigures {
        val months = (1..12).map { month ->
            monthFigures(entries, entrepriseId, YearMonth.of(year, month))
        }
        val sales = PilotMoney.sum(months.map { it.sales })
        val charges = PilotMoney.sum(months.map { it.charges })
        val annual = figures(YearMonth.of(year, 1), sales, charges)
        return PilotYearFigures(
            year = year,
            months = months,
            sales = annual.sales,
            charges = annual.charges,
            result = annual.result,
            coverage = annual.coverage
        )
    }

    fun salesByCategory(
        entries: List<PilotEntry>,
        categories: List<PilotCategory>,
        entrepriseId: String,
        month: YearMonth
    ): List<PilotCategoryShare> {
        val names = categories.associate { it.id to it.name }
        val sales = entries.filter {
            it.entrepriseId == entrepriseId &&
                it.type == PilotEntryType.SALE &&
                YearMonth.from(it.date) == month
        }
        val total = PilotMoney.sum(sales.map { it.amount })
        return sales.mapNotNull { it.categoryId }.distinct().let { ids ->
            ids.map { categoryId ->
                val lines = sales.filter { it.categoryId == categoryId }
                val amount = PilotMoney.sum(lines.map { it.amount })
                PilotCategoryShare(
                    categoryId = categoryId,
                    name = names[categoryId] ?: categoryId,
                    amount = amount,
                    share = PilotMoney.ratio(amount, total)
                )
            }
        }.sortedByDescending { it.amount }
    }

    fun categoryEvolution(
        entries: List<PilotEntry>,
        entrepriseId: String,
        categoryId: String,
        endMonth: YearMonth,
        monthsBack: Int = 6
    ): List<Pair<YearMonth, Double>> {
        return (monthsBack - 1 downTo 0).map { offset ->
            val month = endMonth.minusMonths(offset.toLong())
            val amount = PilotMoney.sum(
                entries.filter {
                    it.entrepriseId == entrepriseId &&
                        it.type == PilotEntryType.SALE &&
                        it.categoryId == categoryId &&
                        YearMonth.from(it.date) == month
                }.map { it.amount }
            )
            month to amount
        }
    }

    fun coverageState(figures: PilotMonthFigures): PilotCoverageState {
        val coverage = figures.coverage ?: return PilotCoverageState.NO_CHARGES
        val percent = PilotMoney.percent(coverage)
        return when {
            percent < 100.0 -> PilotCoverageState.BELOW
            percent == 100.0 -> PilotCoverageState.AT_ZERO
            else -> PilotCoverageState.ABOVE
        }
    }

    fun gapToZero(figures: PilotMonthFigures): Double =
        PilotMoney.minus(figures.sales, figures.charges)

    fun targetProgress(sales: Double, target: Double): Double? =
        PilotMoney.ratio(sales, target)

    fun targetRemaining(sales: Double, target: Double): Double {
        val remaining = PilotMoney.minus(target, sales)
        return if (remaining < 0) 0.0 else remaining
    }

    fun isCategoryUsed(entries: List<PilotEntry>, categoryId: String): Boolean =
        entries.any { it.categoryId == categoryId }

    fun nextRubriqueColor(used: Collection<Int>): Int {
        val taken = used.filter { it >= 0 }.toSet()
        var index = 0
        while (index in taken) index++
        return index
    }

    fun rubriqueColorIndexes(categories: List<PilotCategory>): Map<String, Int> {
        val result = linkedMapOf<String, Int>()
        PilotEntryType.entries.forEach { type ->
            val used = mutableSetOf<Int>()
            categories.filter { it.type == type }
                .sortedWith(compareBy({ it.createdAt }, { it.id }))
                .forEach { category ->
                    var index = category.colorIndex
                    if (index < 0 || index in used) {
                        index = 0
                        while (index in used) index++
                    }
                    used += index
                    result[category.id] = index
                }
        }
        return result
    }

    /**
     * Même type + date + rubrique + montant = doublon d'activité (vente/dépense).
     * Empêche de compter plusieurs fois la même charge dans le mois.
     */
    fun isActivityDuplicate(entries: List<PilotEntry>, candidate: PilotEntry): Boolean {
        if (candidate.type != PilotEntryType.SALE && candidate.type != PilotEntryType.EXPENSE) return false
        val categoryId = candidate.categoryId ?: return false
        return entries.any { existing ->
            existing.id != candidate.id &&
                existing.entrepriseId == candidate.entrepriseId &&
                existing.type == candidate.type &&
                existing.date == candidate.date &&
                existing.categoryId == categoryId &&
                PilotMoney.same(existing.amount, candidate.amount)
        }
    }

    fun recurringToCopy(
        entries: List<PilotEntry>,
        entrepriseId: String,
        targetMonth: YearMonth
    ): List<PilotEntry> {
        val due = entries.filter { source ->
            val interval = source.recurrenceEveryMonths()
            source.entrepriseId == entrepriseId &&
                interval > 0 &&
                YearMonth.from(source.date) == targetMonth.minusMonths(interval.toLong())
        }
        val current = entries.filter {
            it.entrepriseId == entrepriseId &&
                YearMonth.from(it.date) == targetMonth
        }
        return due.filter { source ->
            current.none { existing ->
                existing.type == source.type &&
                    existing.categoryId == source.categoryId &&
                    PilotMoney.same(existing.amount, source.amount)
            }
        }
    }

    fun PilotEntry.recurrenceEveryMonths(): Int = when {
        recurrenceMonths > 0 -> recurrenceMonths
        recurring -> 1
        else -> 0
    }

    fun shiftedRecurringCopy(source: PilotEntry, targetMonth: YearMonth): PilotEntry {
        val day = minOf(source.date.dayOfMonth, targetMonth.lengthOfMonth())
        val now = LocalDateTime.now()
        return source.copy(
            id = UUID.randomUUID().toString(),
            date = targetMonth.atDay(day),
            importId = null,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun figures(month: YearMonth, sales: Double, charges: Double): PilotMonthFigures {
        return PilotMonthFigures(
            month = month,
            sales = sales,
            charges = charges,
            result = PilotMoney.minus(sales, charges),
            coverage = PilotMoney.ratio(sales, charges),
            zeroPoint = charges
        )
    }
}
