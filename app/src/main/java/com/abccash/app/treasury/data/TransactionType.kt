package com.abccash.app.treasury.data

import androidx.annotation.StringRes
import com.abccash.app.R

enum class TransactionType(
    val route: String,
    @StringRes val titleRes: Int,
    @StringRes val forecastTitleRes: Int
) {
    INCOME("income", R.string.new_invoice, R.string.forecast_income),
    EXPENSE("expense", R.string.new_expense, R.string.forecast_expense);

    companion object {
        fun fromRoute(value: String?): TransactionType? =
            entries.find { it.route == value }

        fun addRoute(type: TransactionType, forecast: Boolean = false): String =
            "add_transaction/${type.route}?forecast=$forecast"
    }
}
