package com.abccash.app.domain.usecase

import com.abccash.app.data.local.dao.TransactionDao
import com.abccash.app.data.local.entity.RecurrenceRule
import com.abccash.app.data.local.entity.TransactionEntity
import java.time.LocalDateTime

class CreateTransactionWithRecurrenceUseCase(
    private val transactionDao: TransactionDao
) {
    suspend operator fun invoke(input: TransactionEntity): List<Long> {
        val now = LocalDateTime.now()
        val base = input.copy(createdAt = now, updatedAt = now)
        val baseId = transactionDao.insert(base)

        if (!base.isRecurring || base.recurrenceRule == RecurrenceRule.NONE) {
            return listOf(baseId)
        }

        val endDate = base.date.plusMonths(12)
        val futureItems = mutableListOf<TransactionEntity>()
        var nextDate = computeNextDate(base.date, base.recurrenceRule)
        while (nextDate <= endDate) {
            futureItems += base.copy(
                id = 0L,
                date = nextDate,
                parentRecurringId = baseId,
                createdAt = now,
                updatedAt = now
            )
            nextDate = computeNextDate(nextDate, base.recurrenceRule)
        }
        return listOf(baseId) + transactionDao.insertAll(futureItems)
    }

    private fun computeNextDate(from: java.time.LocalDate, rule: RecurrenceRule): java.time.LocalDate {
        return when (rule) {
            RecurrenceRule.WEEKLY -> from.plusWeeks(1)
            RecurrenceRule.MONTHLY -> from.plusMonths(1)
            RecurrenceRule.QUARTERLY -> from.plusMonths(3)
            RecurrenceRule.FOUR_MONTHLY -> from.plusMonths(4)
            RecurrenceRule.NONE -> from
        }
    }
}
