package com.abccash.app.treasury.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import com.abccash.app.treasury.data.hasPermission

object SettingsRoutes {
    const val HUB = "settings"
    const val USERS = "settings/users"
    const val PROFILE_USER = "settings/profile/user"
    const val PROFILE_COMPANY = "settings/profile/company"
    const val CATEGORIES_INCOME = "settings/categories/income"
    const val CATEGORIES_EXPENSE = "settings/categories/expense"
    const val OPTIONS_CURRENCY = "settings/options/currency"
    const val OPTIONS_NOTIFICATIONS = "settings/options/notifications"
    const val OPTIONS_SECURITY = "settings/options/security"
    const val OPTIONS_LANGUAGE = "settings/options/language"
    const val OPTIONS_SYNC = "settings/options/sync"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHubScreen(
    userRole: UserRole,
    permissions: Set<UserPermission>,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val canManageUsers = hasPermission(userRole, permissions, UserPermission.MANAGE_USERS)
    val tabs = buildList {
        if (canManageUsers) add(SettingsSection.MANAGEMENT)
        add(SettingsSection.PROFILE)
        add(SettingsSection.CATEGORIES)
        add(SettingsSection.OPTIONS)
    }
    var selectedTab by remember(tabs) { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            TextButton(onClick = onLogout) {
                Text(stringResource(R.string.logout), color = Color(0xFFF44336))
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { index, section ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(stringResource(section.titleRes), fontSize = 13.sp) }
                )
            }
        }

        val entries = when (tabs.getOrNull(selectedTab)) {
            SettingsSection.MANAGEMENT -> listOf(
                SettingsMenuEntry(
                    title = stringResource(R.string.settings_users),
                    subtitle = stringResource(R.string.settings_users_sub),
                    icon = Icons.Default.People,
                    route = SettingsRoutes.USERS
                )
            )
            SettingsSection.PROFILE -> listOf(
                SettingsMenuEntry(
                    title = stringResource(R.string.settings_my_profile),
                    subtitle = stringResource(R.string.settings_my_profile_sub),
                    icon = Icons.Default.Person,
                    route = SettingsRoutes.PROFILE_USER
                ),
                SettingsMenuEntry(
                    title = stringResource(R.string.settings_company),
                    subtitle = stringResource(R.string.settings_company_sub),
                    icon = Icons.Default.Business,
                    route = SettingsRoutes.PROFILE_COMPANY
                )
            )
            SettingsSection.CATEGORIES -> listOf(
                SettingsMenuEntry(
                    title = stringResource(R.string.settings_income_categories),
                    subtitle = stringResource(R.string.settings_income_categories_sub),
                    icon = Icons.Default.TrendingUp,
                    route = SettingsRoutes.CATEGORIES_INCOME
                ),
                SettingsMenuEntry(
                    title = stringResource(R.string.settings_expense_categories),
                    subtitle = stringResource(R.string.settings_expense_categories_sub),
                    icon = Icons.Default.ShoppingCart,
                    route = SettingsRoutes.CATEGORIES_EXPENSE
                )
            )
            SettingsSection.OPTIONS -> listOf(
                SettingsMenuEntry(
                    title = stringResource(R.string.settings_currency),
                    subtitle = stringResource(R.string.settings_currency_sub),
                    icon = Icons.Default.AttachMoney,
                    route = SettingsRoutes.OPTIONS_CURRENCY
                ),
                SettingsMenuEntry(
                    title = stringResource(R.string.settings_notifications),
                    subtitle = stringResource(R.string.settings_notifications_sub),
                    icon = Icons.Default.Notifications,
                    route = SettingsRoutes.OPTIONS_NOTIFICATIONS
                ),
                SettingsMenuEntry(
                    title = stringResource(R.string.settings_security),
                    subtitle = stringResource(R.string.settings_security_sub),
                    icon = Icons.Default.Fingerprint,
                    route = SettingsRoutes.OPTIONS_SECURITY
                ),
                SettingsMenuEntry(
                    title = stringResource(R.string.settings_sync),
                    subtitle = stringResource(R.string.settings_sync_sub),
                    icon = Icons.Default.CloudSync,
                    route = SettingsRoutes.OPTIONS_SYNC
                ),
                SettingsMenuEntry(
                    title = stringResource(R.string.settings_language),
                    subtitle = stringResource(R.string.settings_language_sub),
                    icon = Icons.Default.Language,
                    route = SettingsRoutes.OPTIONS_LANGUAGE
                )
            )
            null -> emptyList()
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(entries) { entry ->
                SettingsMenuCard(entry = entry, onClick = { onNavigate(entry.route) })
            }
        }
    }
}
