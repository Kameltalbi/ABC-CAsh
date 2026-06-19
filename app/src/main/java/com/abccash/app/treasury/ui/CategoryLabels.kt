package com.abccash.app.treasury.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.abccash.app.treasury.data.CategorySelection
import com.abccash.app.treasury.data.CategorySlice
import com.abccash.app.treasury.data.ExpenseCategory
import com.abccash.app.treasury.data.RevenueCategory

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
