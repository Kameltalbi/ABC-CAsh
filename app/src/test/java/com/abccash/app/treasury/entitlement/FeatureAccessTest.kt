package com.abccash.app.treasury.entitlement

import com.abccash.app.treasury.data.SubscriptionPlan
import com.abccash.app.treasury.data.UserSubscription
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureAccessTest {
    @Test
    fun phaseDeLancementDesactiveLesQuotas() {
        assertTrue(FeatureAccess.isLaunchFreePhase)
        assertFalse(FeatureAccess.enforcesPaidLimits())
        assertFalse(FeatureAccess.allowsBillingPurchases())

        val heavyFree = UserSubscription(
            plan = SubscriptionPlan.FREE,
            transactionsThisMonth = 10_000,
            treasuryAccountsCount = 100
        )
        assertFalse(heavyFree.isTransactionLimitReached)
        assertFalse(heavyFree.isTreasuryAccountLimitReached)
        assertTrue(SubscriptionPlan.FREE.hasOcrScan)
    }

    @Test
    fun capacitesProFuturesRestentDocumentees() {
        FutureProCapability.entries.forEach { capability ->
            assertTrue(
                FeatureAccess.isFutureProCapabilityUnlocked(capability, SubscriptionPlan.FREE)
            )
        }
    }
}
