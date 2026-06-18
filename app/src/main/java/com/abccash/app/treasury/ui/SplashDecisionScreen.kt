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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import com.abccash.app.treasury.data.effectivePermissions
import com.abccash.app.treasury.datastore.UserPreferences
import com.abccash.app.treasury.repository.TreasuryRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@Composable
fun SplashDecisionScreen(
    repository: TreasuryRepository,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToInscription: () -> Unit,
    onNavigateToMainApp: (String, UserRole, String, Set<UserPermission>) -> Unit
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }

    LaunchedEffect(Unit) {
        delay(1200)

        val isLoggedIn = userPreferences.isLoggedIn.first()
        if (isLoggedIn) {
            val isAdmin = userPreferences.isAdmin.first()
            val userId = userPreferences.currentUserId.first().orEmpty()
            val entrepriseId = userPreferences.currentEntrepriseId.first().orEmpty()
            val onboardingVu = userPreferences.onboardingAdminVu.first()
            val role = if (isAdmin) UserRole.ADMIN else UserRole.STAFF
            val storedPermissions = userPreferences.currentPermissions.first()
            val permissions = repository.getUserById(userId)?.permissions
                ?.let { effectivePermissions(role, it) }
                ?: effectivePermissions(role, storedPermissions)

            if (isAdmin && !onboardingVu) {
                onNavigateToOnboarding()
            } else {
                onNavigateToMainApp(userId, role, entrepriseId, permissions)
            }
        } else if (repository.hasAnyUser()) {
            onNavigateToLogin()
        } else {
            onNavigateToInscription()
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
                contentDescription = "ABC Cash",
                modifier = Modifier.fillMaxWidth(0.72f),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Gestion de trésorerie", fontSize = 16.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
        }
    }
}
