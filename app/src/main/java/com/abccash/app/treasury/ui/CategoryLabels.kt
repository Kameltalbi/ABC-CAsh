package com.abccash.app.treasury.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.abccash.app.R
import com.abccash.app.treasury.data.CategoryDefaults
import com.abccash.app.treasury.data.CategorySelection
import com.abccash.app.treasury.data.CategorySlice
import com.abccash.app.treasury.data.ExpenseCategory
import com.abccash.app.treasury.data.RevenueCategory
import com.abccash.app.treasury.datastore.AppSettings

@Composable
fun localizedCategoryLabel(slice: CategorySlice): String {
    if (slice.label.isNotBlank()) return slice.label
    slice.revenueCategory?.let { return stringResource(it.labelRes) }
    slice.expenseCategory?.let { return stringResource(it.labelRes) }
    return slice.label
}

@Composable
fun incomeCategoryOptions(customLabels: List<String>): List<String> =
    CategorySelection.incomeOptions(customLabels)

@Composable
fun expenseCategoryOptions(customLabels: List<String>): List<String> =
    CategorySelection.expenseOptions(customLabels)

@Composable
fun EnsureDefaultCategories(
    entrepriseId: String,
    appSettings: AppSettings
) {
    val incomeDefaults = CategoryDefaults.incomeLabelResIds.map { stringResource(it) }
    val expenseDefaults = CategoryDefaults.expenseLabelResIds.map { stringResource(it) }
    LaunchedEffect(entrepriseId, incomeDefaults, expenseDefaults) {
        if (entrepriseId.isNotBlank()) {
            appSettings.ensureDefaultIncomeCategories(entrepriseId, incomeDefaults)
            appSettings.ensureDefaultExpenseCategories(entrepriseId, expenseDefaults)
        }
    }
}

data class FormCategoryOption<T>(
    @androidx.annotation.StringRes val labelRes: Int,
    val value: T
)

@Composable
fun defaultIncomeFormOptions(): List<FormCategoryOption<com.abccash.app.treasury.data.RevenueCategory>> =
    listOf(
        FormCategoryOption(R.string.form_income_service, com.abccash.app.treasury.data.RevenueCategory.SERVICE),
        FormCategoryOption(R.string.form_income_goods, com.abccash.app.treasury.data.RevenueCategory.GOODS),
        FormCategoryOption(R.string.form_income_grants, com.abccash.app.treasury.data.RevenueCategory.OTHER)
    )

@Composable
fun defaultExpenseFormOptions(): List<FormCategoryOption<ExpenseCategory>> =
    listOf(
        FormCategoryOption(R.string.form_expense_taxes, ExpenseCategory.TAXES),
        FormCategoryOption(R.string.form_expense_material, ExpenseCategory.MATERIAL),
        FormCategoryOption(R.string.form_expense_rent, ExpenseCategory.SUBSCRIPTION),
        FormCategoryOption(R.string.form_expense_transport, ExpenseCategory.TRANSPORT),
        FormCategoryOption(R.string.form_expense_meals, ExpenseCategory.MEALS)
    )
