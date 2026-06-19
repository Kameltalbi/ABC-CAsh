package com.abccash.app.treasury.data

import java.time.LocalDate
import java.util.UUID

import androidx.annotation.StringRes
import com.abccash.app.R

enum class ExpenseRecurrence(@StringRes val labelRes: Int, val monthsInterval: Int?) {
    WEEKLY(R.string.recurrence_weekly, null),
    MONTHLY(R.string.recurrence_monthly, 1),
    EVERY_2_MONTHS(R.string.recurrence_every_2_months, 2),
    EVERY_3_MONTHS(R.string.recurrence_every_3_months, 3),
    EVERY_4_MONTHS(R.string.recurrence_every_4_months, 4),
    EVERY_6_MONTHS(R.string.recurrence_every_6_months, 6),
    ANNUAL(R.string.recurrence_annual, 12)
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
    val paymentMethod: PaymentMethod? = null,
    val createdDate: LocalDate = LocalDate.now(),
    val entrepriseId: String = "",
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val categoryLabel: String = ""
)
