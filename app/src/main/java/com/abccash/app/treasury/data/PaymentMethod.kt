package com.abccash.app.treasury.data

import androidx.annotation.StringRes
import com.abccash.app.R

enum class PaymentMethod(@StringRes val labelRes: Int) {
    CHECK(R.string.payment_check),
    TRANSFER(R.string.payment_transfer),
    CASH(R.string.payment_cash),
    BILL_OF_EXCHANGE(R.string.payment_bill)
}
