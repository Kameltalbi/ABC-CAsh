package com.abccash.app.treasury.data

import java.time.LocalDate
import java.util.UUID

enum class ExpenseRecurrence(val label: String, val monthsInterval: Int?) {
    WEEKLY("Weekly", null),
    MONTHLY("Monthly", 1),
    EVERY_2_MONTHS("Every 2 months", 2),
    EVERY_3_MONTHS("Every 3 months", 3),
    EVERY_4_MONTHS("Every 4 months", 4),
    EVERY_6_MONTHS("Every 6 months", 6),
    ANNUAL("Annual", 12)
}

data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val amount: Double,
    val date: LocalDate,
    val isRecurring: Boolean = false,
    val recurrence: ExpenseRecurrence? = null,
    val recurrenceEndDate: LocalDate? = null,
    val isPaid: Boolean = true,
    val createdDate: LocalDate = LocalDate.now(),
    val entrepriseId: String = "",
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val categoryLabel: String = ""
)
