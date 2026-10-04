package com.abccash.app.treasury.data

import androidx.annotation.StringRes
import com.abccash.app.R
import com.abccash.app.treasury.entitlement.FeatureAccess

/**
 * Plans commerciaux.
 *
 * Pendant [FeatureAccess.isLaunchFreePhase], les quotas Free historiques ne sont pas appliqués
 * (voir [UserSubscription]). Les valeurs Free ci-dessous décrivent le futur freemium uniquement.
 */
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

    /** OCR : libre en phase de lancement ; sinon réservé au Pro (futur). */
    val hasOcrScan: Boolean get() = FeatureAccess.allowsOcr(this)

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
        if (!FeatureAccess.enforcesPaidLimits() || !plan.hasTransactionLimit) {
            Int.MAX_VALUE
        } else {
            plan.transactionsPerMonth!! - transactionsThisMonth
        }

    val remainingTreasuryAccounts: Int get() =
        if (!FeatureAccess.enforcesPaidLimits()) {
            Int.MAX_VALUE
        } else {
            (plan.treasuryAccountsLimit - treasuryAccountsCount).coerceAtLeast(0)
        }

    val isTransactionLimitReached: Boolean get() =
        FeatureAccess.enforcesPaidLimits() &&
            plan.hasTransactionLimit &&
            transactionsThisMonth >= plan.transactionsPerMonth!!

    val isTreasuryAccountLimitReached: Boolean get() =
        FeatureAccess.enforcesPaidLimits() &&
            treasuryAccountsCount >= plan.treasuryAccountsLimit
}
