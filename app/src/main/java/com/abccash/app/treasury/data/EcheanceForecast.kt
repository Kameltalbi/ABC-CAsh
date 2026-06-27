package com.abccash.app.treasury.data

import com.abccash.app.locale.AppLocale
import java.time.LocalDate
import java.time.YearMonth

enum class EcheanceType {
    INCOME,
    EXPENSE
}

data class EcheanceItem(
    val id: String,
    val type: EcheanceType,
    val label: String,
    val amount: Double,
    val dueDate: LocalDate,
    val invoiceId: String? = null,
    val expenseId: String? = null,
    val isRecurring: Boolean = false
)

data class EcheanceMonthSection(
    val month: YearMonth,
    val items: List<EcheanceItem>
) {
    val label: String
        get() = AppLocale.monthYear(month)
}

object EcheanceForecast {

    fun buildItemsForMonth(
        month: YearMonth,
        invoices: List<Invoice>,
        expenses: List<Expense>
    ): List<EcheanceItem> {
        val income = invoices
            .filter { it.status != InvoiceStatus.PAID && it.remainingAmount > 0 }
            .filter { YearMonth.from(it.dueDate) == month }
            .map { invoice ->
                EcheanceItem(
                    id = "inv_${invoice.id}",
                    type = EcheanceType.INCOME,
                    label = invoice.clientName,
                    amount = invoice.remainingAmount,
                    dueDate = invoice.dueDate,
                    invoiceId = invoice.id
                )
            }

        val expenseItems = expenses
            .filter { !it.isPaid }
            .filter { it.appliesToMonth(month) }
            .mapNotNull { expense ->
                val dueDate = expense.occurrenceDateIn(month) ?: return@mapNotNull null
                EcheanceItem(
                    id = if (expense.isRecurring) "exp_${expense.id}_$dueDate" else "exp_${expense.id}",
                    type = EcheanceType.EXPENSE,
                    label = expense.label,
                    amount = expense.amount,
                    dueDate = dueDate,
                    expenseId = expense.id,
                    isRecurring = expense.isRecurring
                )
            }

        return (income + expenseItems).sortedBy { it.dueDate }
    }

    fun buildItems(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        from: LocalDate = LocalDate.now(),
        to: LocalDate = LocalDate.now().plusMonths(12)
    ): List<EcheanceItem> {
        val income = buildIncomeItems(invoices, from, to)
        val expenseItems = buildExpenseItems(expenses, from, to)
        return (income + expenseItems).sortedBy { it.dueDate }
    }

    fun groupByMonth(items: List<EcheanceItem>): List<EcheanceMonthSection> =
        items.groupBy { YearMonth.from(it.dueDate) }
            .toSortedMap()
            .map { (month, sectionItems) ->
                EcheanceMonthSection(month = month, items = sectionItems.sortedBy { it.dueDate })
            }

    private fun isWithinRange(date: LocalDate, from: LocalDate, to: LocalDate): Boolean =
        !date.isBefore(from) && !date.isAfter(to)

    private fun buildIncomeItems(
        invoices: List<Invoice>,
        from: LocalDate,
        to: LocalDate
    ): List<EcheanceItem> =
        invoices
            .filter { it.status != InvoiceStatus.PAID && it.remainingAmount > 0 }
            .filter { isWithinRange(it.dueDate, from, to) }
            .map { invoice ->
                EcheanceItem(
                    id = "inv_${invoice.id}",
                    type = EcheanceType.INCOME,
                    label = invoice.clientName,
                    amount = invoice.remainingAmount,
                    dueDate = invoice.dueDate,
                    invoiceId = invoice.id
                )
            }

    private fun buildExpenseItems(
        expenses: List<Expense>,
        from: LocalDate,
        to: LocalDate
    ): List<EcheanceItem> {
        val items = mutableListOf<EcheanceItem>()
        val horizonStart = YearMonth.from(from)
        val horizonEnd = YearMonth.from(to)

        expenses.filter { !it.isPaid }.forEach { expense ->
            if (!expense.isRecurring) {
                if (isWithinRange(expense.date, from, to)) {
                    items.add(
                        EcheanceItem(
                            id = "exp_${expense.id}",
                            type = EcheanceType.EXPENSE,
                            label = expense.label,
                            amount = expense.amount,
                            dueDate = expense.date,
                            expenseId = expense.id,
                            isRecurring = false
                        )
                    )
                }
            } else {
                var month = maxOf(horizonStart, YearMonth.from(expense.date))
                while (!month.isAfter(horizonEnd)) {
                    if (expense.appliesToMonth(month)) {
                        val occurrence = expense.occurrenceDateIn(month)
                        if (occurrence != null && isWithinRange(occurrence, from, to)) {
                            items.add(
                                EcheanceItem(
                                    id = "exp_${expense.id}_$occurrence",
                                    type = EcheanceType.EXPENSE,
                                    label = expense.label,
                                    amount = expense.amount,
                                    dueDate = occurrence,
                                    expenseId = expense.id,
                                    isRecurring = true
                                )
                            )
                        }
                    }
                    month = month.plusMonths(1)
                }
            }
        }
        return items
    }

    /** Unpaid income/expense due dates strictly before [today]. */
    fun countOverdue(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        today: LocalDate = LocalDate.now()
    ): Int {
        val lookback = today.minusYears(3)
        return buildItems(invoices, expenses, from = lookback, to = today)
            .count { it.dueDate.isBefore(today) }
    }
}
