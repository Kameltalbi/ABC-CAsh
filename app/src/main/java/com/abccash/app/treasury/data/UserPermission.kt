package com.abccash.app.treasury.data

import androidx.annotation.StringRes
import com.abccash.app.R

enum class UserPermission(@StringRes val labelRes: Int) {
    VIEW_INVOICES(R.string.permission_view_invoices),
    ADD_PAYMENTS(R.string.permission_add_payments),
    MANAGE_EXPENSES(R.string.permission_manage_expenses),
    VIEW_TREASURY(R.string.permission_view_treasury),
    MANAGE_USERS(R.string.permission_manage_users)
}
