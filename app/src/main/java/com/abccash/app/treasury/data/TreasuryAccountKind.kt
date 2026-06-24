package com.abccash.app.treasury.data

import androidx.annotation.StringRes
import com.abccash.app.R

enum class TreasuryAccountKind(@StringRes val labelRes: Int) {
    BANK(R.string.treasury_account_kind_bank),
    CASH(R.string.treasury_account_kind_cash)
}
