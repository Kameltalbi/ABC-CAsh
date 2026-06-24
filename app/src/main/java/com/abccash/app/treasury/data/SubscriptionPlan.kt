package com.abccash.app.treasury.data

import androidx.annotation.StringRes
import com.abccash.app.R

enum class SubscriptionPlan(
    val id: String,
    @StringRes val nameRes: Int,
    val priceUsd: Double,
    val transactionsPerMonth: Int?,
    val treasuryAccountsLimit: Int
) {
    FREE("free", R.string.plan_free, 0.0, 30, 2),
    PRO("pro", R.string.plan_pro, 4.99, null, 5);

    val isFree: Boolean get() = this == FREE
    val hasTransactionLimit: Boolean get() = transactionsPerMonth != null
    val unlimited: Boolean get() = transactionsPerMonth == null

    companion object {
        fun fromId(id: String?): SubscriptionPlan =
            entries.firstOrNull { it.id == id } ?: FREE
    }
}

data class UserSubscription(
    val plan: SubscriptionPlan = SubscriptionPlan.FREE,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val transactionsThisMonth: Int = 0,
    val treasuryAccountsCount: Int = 0,
    val monthResetDate: Long = System.currentTimeMillis()
) {
    val isActive: Boolean get() = endDate == null || endDate > System.currentTimeMillis()
    val remainingTransactions: Int get() =
        if (plan.hasTransactionLimit) {
            plan.transactionsPerMonth!! - transactionsThisMonth
        } else {
            Int.MAX_VALUE
        }

    val remainingTreasuryAccounts: Int get() =
        (plan.treasuryAccountsLimit - treasuryAccountsCount).coerceAtLeast(0)

    val isTransactionLimitReached: Boolean get() =
        plan.hasTransactionLimit && transactionsThisMonth >= plan.transactionsPerMonth!!

    val isTreasuryAccountLimitReached: Boolean get() =
        treasuryAccountsCount >= plan.treasuryAccountsLimit
}
