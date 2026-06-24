package com.abccash.app.treasury.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.abccash.app.treasury.data.SubscriptionPlan
import com.abccash.app.treasury.datastore.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingManager(
    private val context: Context
) : PurchasesUpdatedListener, BillingClientStateListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val userPreferences = UserPreferences(context)

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val _subscriptionPlan = MutableStateFlow<SubscriptionPlan>(SubscriptionPlan.FREE)
    val subscriptionPlan: StateFlow<SubscriptionPlan> = _subscriptionPlan.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    fun startConnection() {
        scope.launch {
            val savedPlan = userPreferences.readSubscriptionPlan()
            if (savedPlan != SubscriptionPlan.FREE) {
                _subscriptionPlan.value = savedPlan
            }
        }
        billingClient.startConnection(this)
    }

    fun endConnection() {
        billingClient.endConnection()
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _isConnected.value = true
            queryExistingPurchases()
        } else {
            _isConnected.value = false
        }
    }

    override fun onBillingServiceDisconnected() {
        _isConnected.value = false
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases)
        }
        _isPurchasing.value = false
    }

    private fun queryExistingPurchases() {
        val queryPurchaseParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(queryPurchaseParams) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases)
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        var activePlan = SubscriptionPlan.FREE
        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }
                val productId = purchase.products.firstOrNull()
                if (productId == "pro_subscription") {
                    activePlan = SubscriptionPlan.PRO
                }
            }
        }
        persistPlan(activePlan)
    }

    private fun persistPlan(plan: SubscriptionPlan) {
        _subscriptionPlan.value = plan
        scope.launch { userPreferences.saveSubscriptionPlan(plan) }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                // Handle error
            }
        }
    }

    suspend fun launchBillingFlow(activity: Activity, plan: SubscriptionPlan): Boolean {
        if (!_isConnected.value) return false

        val productId = when (plan) {
            SubscriptionPlan.PRO -> "pro_subscription"
            else -> return false
        }

        return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val productDetails = productDetailsList.firstOrNull()
                    if (productDetails != null) {
                        val billingFlowParams = BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(
                                listOf(
                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                        .setProductDetails(productDetails)
                                        .build()
                                )
                            )
                            .build()

                        _isPurchasing.value = true
                        val flowResult = billingClient.launchBillingFlow(activity, billingFlowParams)
                        continuation.resume(flowResult.responseCode == BillingClient.BillingResponseCode.OK, onCancellation = null)
                    } else {
                        continuation.resume(false, onCancellation = null)
                    }
                } else {
                    continuation.resume(false, onCancellation = null)
                }
            }
        }
    }

    companion object {
        @Volatile
        private var instance: BillingManager? = null

        fun getInstance(context: Context): BillingManager {
            return instance ?: synchronized(this) {
                instance ?: BillingManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
