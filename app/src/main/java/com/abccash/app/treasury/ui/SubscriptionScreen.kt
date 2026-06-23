package com.abccash.app.treasury.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    currentPlan: SubscriptionPlan,
    onBack: () -> Unit,
    onSelectPlan: (SubscriptionPlan) -> Unit
) {
    val context = LocalContext.current
    val billingManager = remember { BillingManager.getInstance(context) }
    val subscriptionPlan by billingManager.subscriptionPlan.collectAsStateWithLifecycle()
    val isConnected by billingManager.isConnected.collectAsStateWithLifecycle()
    val isPurchasing by billingManager.isPurchasing.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        billingManager.startConnection()
    }

    DisposableEffect(Unit) {
        onDispose {
            billingManager.endConnection()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.subscription_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
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
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = stringResource(R.string.subscription_unlock_features),
                fontSize = 14.sp,
                color = Color.Gray
            )

            if (!isConnected) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))
                ) {
                    Text(
                        text = stringResource(R.string.subscription_play_connecting),
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFF856404)
                    )
                }
            }
            
            SubscriptionPlanCard(
                plan = SubscriptionPlan.FREE,
                currentPlan = subscriptionPlan,
                onSelectPlan = onSelectPlan,
                isPopular = false,
                isEnabled = true
            )
            
            SubscriptionPlanCard(
                plan = SubscriptionPlan.STARTER,
                currentPlan = subscriptionPlan,
                onSelectPlan = { plan ->
                    // Handle purchase in a coroutine
                },
                isPopular = false,
                isEnabled = isConnected && !isPurchasing
            )
            
            SubscriptionPlanCard(
                plan = SubscriptionPlan.PRO,
                currentPlan = subscriptionPlan,
                onSelectPlan = { plan ->
                    // Handle purchase in a coroutine
                },
                isPopular = true,
                isEnabled = isConnected && !isPurchasing
            )
        }
    }
}

@Composable
private fun SubscriptionPlanCard(
    plan: SubscriptionPlan,
    currentPlan: SubscriptionPlan,
    onSelectPlan: (SubscriptionPlan) -> Unit,
    isPopular: Boolean,
    isEnabled: Boolean = true
) {
    val isSelected = currentPlan == plan
    val backgroundColor = if (isSelected) {
        Color(0xFF22C55E)
    } else {
        Color.White
    }
    val textColor = if (isSelected) Color.White else Color.Black
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp)
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
                    text = when (plan) {
                        SubscriptionPlan.FREE -> stringResource(R.string.plan_free)
                        SubscriptionPlan.STARTER -> stringResource(R.string.plan_starter)
                        SubscriptionPlan.PRO -> stringResource(R.string.plan_pro)
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                if (isPopular) {
                    Text(
                        text = "Populaire",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFBBF24),
                        modifier = Modifier
                            .background(Color(0xFFFEF3C7), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            
            Text(
                text = "$${plan.priceUsd}/mois",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            
            if (plan.hasTransactionLimit) {
                Text(
                    text = "${plan.transactionsPerMonth} transactions/mois",
                    fontSize = 14.sp,
                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray
                )
            } else {
                Text(
                    text = "Transactions illimitées",
                    fontSize = 14.sp,
                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray
                )
            }
            
            PlanFeatures(plan, textColor)
            
            Button(
                onClick = { onSelectPlan(plan) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color.White else Color(0xFF22C55E),
                    contentColor = if (isSelected) Color(0xFF22C55E) else Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSelected && isEnabled
            ) {
                Text(
                    text = if (isSelected) "Plan actuel" else "Choisir ce plan",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PlanFeatures(plan: SubscriptionPlan, textColor: Color) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (plan) {
            SubscriptionPlan.FREE -> {
                FeatureItem("Accès à toutes les fonctionnalités", textColor)
                FeatureItem("Export CSV", textColor)
                FeatureItem("${plan.transactionsPerMonth} transactions/mois", textColor)
            }
            SubscriptionPlan.STARTER -> {
                FeatureItem("Tout le plan Free", textColor)
                FeatureItem("Transactions illimitées", textColor)
                FeatureItem("Export CSV", textColor)
                FeatureItem("Support email", textColor)
            }
            SubscriptionPlan.PRO -> {
                FeatureItem("Tout le plan Starter", textColor)
                FeatureItem(stringResource(R.string.subscription_pro_bank_feature), textColor)
                FeatureItem(stringResource(R.string.google_backup_title), textColor)
                FeatureItem("Support prioritaire", textColor)
            }
        }
    }
}

@Composable
private fun FeatureItem(text: String, textColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = textColor
        )
    }
}
