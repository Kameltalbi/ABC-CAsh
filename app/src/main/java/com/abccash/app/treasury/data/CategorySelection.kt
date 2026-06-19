package com.abccash.app.treasury.data

data class CategorySelection(
    val revenueCategory: RevenueCategory? = null,
    val expenseCategory: ExpenseCategory? = null,
    val customLabel: String? = null
) {
    val displayLabel: String
        get() = customLabel?.takeIf { it.isNotBlank() }
            ?: revenueCategory?.name
            ?: expenseCategory?.name
            ?: ""

    companion object {
        fun resolveIncome(
            label: String,
            customLabels: List<String>
        ): CategorySelection {
            val trimmed = label.trim()
            if (trimmed.isBlank()) {
                return CategorySelection(revenueCategory = RevenueCategory.OTHER)
            }
            return CategorySelection(
                revenueCategory = RevenueCategory.OTHER,
                customLabel = trimmed
            )
        }

        fun resolveExpense(
            label: String,
            customLabels: List<String>
        ): CategorySelection {
            val trimmed = label.trim()
            if (trimmed.isBlank()) {
                return CategorySelection(expenseCategory = ExpenseCategory.OTHER)
            }
            return CategorySelection(
                expenseCategory = ExpenseCategory.OTHER,
                customLabel = trimmed
            )
        }

        fun incomeOptions(customLabels: List<String>): List<String> = customLabels

        fun expenseOptions(customLabels: List<String>): List<String> = customLabels

        fun displayIncome(category: RevenueCategory, customLabel: String?): CategorySliceKey =
            if (customLabel.isNullOrBlank()) {
                CategorySliceKey(revenueCategory = category)
            } else {
                CategorySliceKey(label = customLabel)
            }

        fun displayExpense(category: ExpenseCategory, customLabel: String?): CategorySliceKey =
            if (customLabel.isNullOrBlank()) {
                CategorySliceKey(expenseCategory = category)
            } else {
                CategorySliceKey(label = customLabel)
            }
    }
}

data class CategorySliceKey(
    val label: String = "",
    val revenueCategory: RevenueCategory? = null,
    val expenseCategory: ExpenseCategory? = null
)
