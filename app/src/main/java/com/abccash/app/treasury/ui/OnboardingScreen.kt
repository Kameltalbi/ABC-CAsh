package com.abccash.app.treasury.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R
import com.abccash.app.ui.theme.AppColors

private data class OnboardingPage(
    val icon: ImageVector,
    val iconBg: Color,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Default.TrendingUp,
            iconBg = AppColors.BrandBlue,
            titleRes = R.string.onboarding_tour_welcome_title,
            descriptionRes = R.string.onboarding_tour_welcome_desc
        ),
        OnboardingPage(
            icon = Icons.Default.AddCircle,
            iconBg = AppColors.BrandBlue,
            titleRes = R.string.onboarding_tour_entry_title,
            descriptionRes = R.string.onboarding_tour_entry_desc
        ),
        OnboardingPage(
            icon = Icons.Default.EventNote,
            iconBg = AppColors.Warning,
            titleRes = R.string.onboarding_tour_forecasts_title,
            descriptionRes = R.string.onboarding_tour_forecasts_desc
        ),
        OnboardingPage(
            icon = Icons.Default.SwapVert,
            iconBg = AppColors.Success,
            titleRes = R.string.onboarding_tour_transactions_title,
            descriptionRes = R.string.onboarding_tour_transactions_desc
        ),
        OnboardingPage(
            icon = Icons.Default.AccountBalance,
            iconBg = AppColors.BrandBlue,
            titleRes = R.string.onboarding_tour_treasury_title,
            descriptionRes = R.string.onboarding_tour_treasury_desc
        )
    )

    var currentPage by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.5f))

            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    (slideInHorizontally(tween(300)) { it } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)))
                },
                label = "onboarding_content"
            ) { pageIndex ->
                val page = pages[pageIndex]
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(page.iconBg.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(page.iconBg.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = page.icon,
                                contentDescription = null,
                                tint = page.iconBg,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    Text(
                        text = stringResource(page.titleRes),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(page.descriptionRes),
                        fontSize = 16.sp,
                        color = AppColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (index == currentPage) 24.dp else 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (index == currentPage) MaterialTheme.colorScheme.primary
                                else AppColors.Border
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = {
                    if (currentPage < pages.lastIndex) currentPage++
                    else onFinish()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (currentPage < pages.lastIndex) {
                        stringResource(R.string.onboarding_next)
                    } else {
                        stringResource(R.string.onboarding_start)
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (currentPage < pages.lastIndex) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onFinish) {
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        color = AppColors.TextTertiary,
                        fontSize = 14.sp
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
