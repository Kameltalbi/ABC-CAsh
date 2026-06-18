package com.abccash.app.treasury.data

data class CategorySelection(
    val revenueCategory: RevenueCategory? = null,
    val expenseCategory: ExpenseCategory? = null,
    val customLabel: String? = null
) {
    val displayLabel: String
        get() = customLabel?.takeIf { it.isNotBlank() }
            ?: revenueCategory?.label
            ?: expenseCategory?.label
            ?: ""

    companion object {
        fun resolveIncome(label: String, customLabels: List<String>): CategorySelection {
            RevenueCategory.entries.find { it.label == label }?.let {
                return CategorySelection(revenueCategory = it)
            }
            if (customLabels.any { it.equals(label, ignoreCase = true) }) {
                return CategorySelection(
                    revenueCategory = RevenueCategory.OTHER,
                    customLabel = label
                )
            }
            return CategorySelection(revenueCategory = RevenueCategory.OTHER)
        }

        fun resolveExpense(label: String, customLabels: List<String>): CategorySelection {
            ExpenseCategory.entries.find { it.label == label }?.let {
                return CategorySelection(expenseCategory = it)
            }
            if (customLabels.any { it.equals(label, ignoreCase = true) }) {
                return CategorySelection(
                    expenseCategory = ExpenseCategory.OTHER,
                    customLabel = label
                )
            }
            return CategorySelection(expenseCategory = ExpenseCategory.OTHER)
        }

        fun incomeOptions(customLabels: List<String>): List<String> =
            RevenueCategory.entries.map { it.label } + customLabels

        fun expenseOptions(customLabels: List<String>): List<String> =
            ExpenseCategory.entries.map { it.label } + customLabels

        fun displayIncome(category: RevenueCategory, customLabel: String?): String =
            customLabel?.takeIf { it.isNotBlank() } ?: category.label

        fun displayExpense(category: ExpenseCategory, customLabel: String?): String =
            customLabel?.takeIf { it.isNotBlank() } ?: category.label
    }
}
