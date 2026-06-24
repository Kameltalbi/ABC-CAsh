package com.abccash.app.treasury.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abccash.app.R
import com.abccash.app.treasury.billing.BillingManager
import com.abccash.app.treasury.data.SubscriptionPlan
import com.abccash.app.ui.theme.AppColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    currentPlan: SubscriptionPlan,
    onBack: () -> Unit,
    onSelectPlan: (SubscriptionPlan) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val billingManager = remember { BillingManager.getInstance(context) }
    val subscriptionPlan by billingManager.subscriptionPlan.collectAsStateWithLifecycle()
    val isConnected by billingManager.isConnected.collectAsStateWithLifecycle()
    val isPurchasing by billingManager.isPurchasing.collectAsStateWithLifecycle()
    val activePlan = if (subscriptionPlan != SubscriptionPlan.FREE) subscriptionPlan else currentPlan

    LaunchedEffect(Unit) {
        billingManager.startConnection()
    }

    DisposableEffect(Unit) {
        onDispose { billingManager.endConnection() }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.subscription_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.subscription_choose_plan),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Text(
                text = stringResource(R.string.subscription_two_plans_desc),
                fontSize = 14.sp,
                color = AppColors.TextSecondary,
                lineHeight = 20.sp
            )

            if (!isConnected) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppColors.WarningBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.subscription_play_connecting),
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFF856404),
                        fontSize = 13.sp
                    )
                }
            }

            SubscriptionPlanCard(
                plan = SubscriptionPlan.FREE,
                currentPlan = activePlan,
                onSelectPlan = onSelectPlan,
                isEnabled = true,
                isPurchasing = false
            )

            SubscriptionPlanCard(
                plan = SubscriptionPlan.PRO,
                currentPlan = activePlan,
                onSelectPlan = { plan ->
                    val activity = context as? Activity
                    if (activity != null) {
                        scope.launch {
                            val started = billingManager.launchBillingFlow(activity, plan)
                            if (started) onSelectPlan(plan)
                        }
                    }
                },
                isEnabled = isConnected && !isPurchasing,
                isPurchasing = isPurchasing,
                highlighted = true
            )
        }
    }
}

@Composable
private fun SubscriptionPlanCard(
    plan: SubscriptionPlan,
    currentPlan: SubscriptionPlan,
    onSelectPlan: (SubscriptionPlan) -> Unit,
    isEnabled: Boolean,
    isPurchasing: Boolean,
    highlighted: Boolean = false
) {
    val isSelected = currentPlan == plan
    val accent = AppColors.BrandBlue
    val cardBackground = when {
        isSelected -> accent
        highlighted -> Color.White
        else -> Color.White
    }
    val textColor = if (isSelected) Color.White else AppColors.TextPrimary
    val mutedColor = if (isSelected) Color.White.copy(alpha = 0.85f) else AppColors.TextSecondary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = if (highlighted) 4.dp else 1.dp),
        border = if (highlighted && !isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, accent.copy(alpha = 0.35f))
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(plan.nameRes),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                if (highlighted && !isSelected) {
                    Text(
                        text = stringResource(R.string.subscription_recommended),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                        modifier = Modifier
                            .background(accent.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Text(
                text = if (plan.isFree) {
                    stringResource(R.string.subscription_price_free)
                } else {
                    stringResource(R.string.subscription_price_monthly, plan.priceUsd)
                },
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Text(
                text = if (plan.hasTransactionLimit) {
                    stringResource(R.string.subscription_items_per_month, plan.transactionsPerMonth!!)
                } else {
                    stringResource(R.string.subscription_unlimited_items)
                },
                fontSize = 14.sp,
                color = mutedColor
            )

            PlanFeatures(plan = plan, textColor = textColor, mutedColor = mutedColor)

            Button(
                onClick = { onSelectPlan(plan) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color.White else accent,
                    contentColor = if (isSelected) accent else Color.White,
                    disabledContainerColor = accent.copy(alpha = 0.4f),
                    disabledContentColor = Color.White.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSelected && isEnabled && !isPurchasing
            ) {
                if (isPurchasing && !plan.isFree) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = if (isSelected) {
                            stringResource(R.string.subscription_current_plan)
                        } else if (plan.isFree) {
                            stringResource(R.string.subscription_stay_free)
                        } else {
                            stringResource(R.string.settings_upgrade_unlimited)
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanFeatures(
    plan: SubscriptionPlan,
    textColor: Color,
    mutedColor: Color
) {
    val features = when (plan) {
        SubscriptionPlan.FREE -> listOf(
            PlanFeature(R.string.subscription_free_feature_limit, true),
            PlanFeature(R.string.subscription_free_feature_no_dashboard, false),
            PlanFeature(R.string.subscription_free_feature_accounts, true),
            PlanFeature(R.string.subscription_free_feature_receipts, true),
            PlanFeature(R.string.subscription_free_feature_no_ocr, false),
            PlanFeature(R.string.subscription_free_feature_forecasts, true),
            PlanFeature(R.string.subscription_free_feature_backup, true),
            PlanFeature(R.string.subscription_free_feature_biometric, true),
            PlanFeature(R.string.subscription_free_feature_csv, true)
        )
        SubscriptionPlan.PRO -> listOf(
            PlanFeature(R.string.subscription_pro_feature_unlimited, true),
            PlanFeature(R.string.subscription_pro_feature_dashboard, true),
            PlanFeature(R.string.subscription_pro_feature_accounts, true),
            PlanFeature(R.string.subscription_pro_feature_ocr, true),
            PlanFeature(R.string.subscription_pro_feature_forecasts, true),
            PlanFeature(R.string.subscription_pro_feature_backup, true),
            PlanFeature(R.string.subscription_pro_feature_biometric, true),
            PlanFeature(R.string.subscription_pro_feature_csv, true)
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        features.forEach { feature ->
            FeatureItem(
                text = stringResource(feature.textRes),
                included = feature.included,
                textColor = textColor,
                mutedColor = mutedColor
            )
        }
    }
}

private data class PlanFeature(
    val textRes: Int,
    val included: Boolean
)

@Composable
private fun FeatureItem(
    text: String,
    included: Boolean,
    textColor: Color,
    mutedColor: Color
) {
    val iconColor = when {
        included -> textColor
        else -> mutedColor.copy(alpha = 0.7f)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            if (included) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = if (included) textColor else mutedColor,
            lineHeight = 18.sp
        )
    }
}
