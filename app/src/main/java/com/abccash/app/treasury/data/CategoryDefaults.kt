package com.abccash.app.treasury.data

import androidx.annotation.StringRes
import com.abccash.app.R

object CategoryDefaults {
    val incomeLabelResIds: List<Int> = listOf(
        R.string.form_income_service,
        R.string.form_income_goods,
        R.string.form_income_grants
    )

    val expenseLabelResIds: List<Int> = listOf(
        R.string.form_expense_taxes,
        R.string.form_expense_material,
        R.string.form_expense_rent,
        R.string.form_expense_transport,
        R.string.form_expense_meals
    )
}
