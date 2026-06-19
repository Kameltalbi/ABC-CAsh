package com.abccash.app.treasury.data

import androidx.annotation.StringRes
import com.abccash.app.R

enum class RevenueCategory(@StringRes val labelRes: Int) {
    SERVICE(R.string.category_service),
    GOODS(R.string.category_goods),
    OTHER(R.string.category_other)
}
