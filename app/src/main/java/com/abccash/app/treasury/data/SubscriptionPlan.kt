package com.abccash.app.treasury.data

enum class SubscriptionPlan(val id: String, val nameRes: Int, val priceUsd: Double, val transactionsPerMonth: Int?) {
    FREE("free", 0, 0.0, 30),
    STARTER("starter", 0, 4.99, null),
    PRO("pro", 0, 9.99, null);

    val isFree: Boolean get() = this == FREE
    val hasTransactionLimit: Boolean get() = transactionsPerMonth != null
    val unlimited: Boolean get() = transactionsPerMonth == null
}

data class UserSubscription(
    val plan: SubscriptionPlan = SubscriptionPlan.FREE,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val transactionsThisMonth: Int = 0,
    val monthResetDate: Long = System.currentTimeMillis()
) {
    val isActive: Boolean get() = endDate == null || endDate > System.currentTimeMillis()
    val remainingTransactions: Int get() = 
        if (plan.hasTransactionLimit) {
            plan.transactionsPerMonth!! - transactionsThisMonth
        } else {
            Int.MAX_VALUE
        }
    
    val isTransactionLimitReached: Boolean get() = 
        plan.hasTransactionLimit && transactionsThisMonth >= plan.transactionsPerMonth!!
}
