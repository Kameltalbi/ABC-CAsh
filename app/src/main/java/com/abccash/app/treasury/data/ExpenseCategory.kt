package com.abccash.app.treasury.data

import androidx.annotation.StringRes
import com.abccash.app.R

enum class ExpenseCategory(@StringRes val labelRes: Int) {
    MATERIAL(R.string.category_material),
    TRANSPORT(R.string.category_transport),
    MEALS(R.string.category_meals),
    TAXES(R.string.category_taxes),
    SUBSCRIPTION(R.string.category_subscription),
    OTHER(R.string.category_other)
}
