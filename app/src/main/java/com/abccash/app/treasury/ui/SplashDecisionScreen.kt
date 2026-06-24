package com.abccash.app.treasury.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import com.abccash.app.treasury.data.effectivePermissions
import com.abccash.app.treasury.datastore.UserPreferences
import com.abccash.app.treasury.repository.TreasuryRepository
import com.abccash.app.ui.theme.AppColors
import kotlinx.coroutines.delay

@Composable
fun SplashDecisionScreen(
    repository: TreasuryRepository,
    userPreferences: UserPreferences,
    onNavigateToInscription: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToAccountSetup: () -> Unit,
    onNavigateToMainApp: (String, UserRole, String, Set<UserPermission>) -> Unit
) {
    LaunchedEffect(Unit) {
        delay(800)
        try {
            if (!repository.hasAnyUser()) {
                onNavigateToInscription()
                return@LaunchedEffect
            }

            if (repository.needsAccountCredentialsSetup()) {
                onNavigateToAccountSetup()
                return@LaunchedEffect
            }

            if (userPreferences.readLoggedIn()) {
                val userId = userPreferences.readSessionUserId()
                val user = userId?.let { repository.getUserById(it) }
                if (user != null && user.isActive && user.entrepriseId.isNotBlank()) {
                    val permissions = effectivePermissions(user.role, user.permissions)
                    userPreferences.saveUserSession(
                        userId = user.id,
                        email = user.email,
                        nom = user.nom,
                        role = user.role,
                        entrepriseId = user.entrepriseId,
                        permissions = permissions
                    )
                    onNavigateToMainApp(user.id, user.role, user.entrepriseId, permissions)
                    return@LaunchedEffect
                }
                userPreferences.clearUserSession()
            }

            onNavigateToLogin()
        } catch (_: Exception) {
            onNavigateToLogin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_abc_cash_logo),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.fillMaxWidth(0.72f),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.splash_tagline),
                fontSize = 16.sp,
                color = AppColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
        }
    }
}
