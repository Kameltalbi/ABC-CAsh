package com.abccash.app.treasury.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
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
import com.abccash.app.treasury.ui.DrawerMenuIconButton

object SettingsRoutes {
    const val HUB = "settings"
    const val PROFILE_USER = "settings/profile/user"
    const val PROFILE_COMPANY = "settings/profile/company"
    const val CATEGORIES_INCOME = "settings/categories/income"
    const val CATEGORIES_EXPENSE = "settings/categories/expense"
    const val OPTIONS_CURRENCY = "settings/options/currency"
    const val OPTIONS_NOTIFICATIONS = "settings/options/notifications"
    const val OPTIONS_SECURITY = "settings/options/security"
    const val OPTIONS_LANGUAGE = "settings/options/language"
    const val OPTIONS_BACKUP = "settings/options/backup"
    const val OPTIONS_BANK = "settings/options/bank"
}

private data class SettingsHubSection(
    @androidx.annotation.StringRes val titleRes: Int,
    val entries: List<SettingsMenuEntry>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHubScreen(
    onNavigate: (String) -> Unit,
    onOpenDrawer: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val sections = listOf(
        SettingsHubSection(
            titleRes = R.string.settings_section_profile,
            entries = listOf(
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
        ),
        SettingsHubSection(
            titleRes = R.string.settings_section_categories,
            entries = listOf(
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
        ),
        SettingsHubSection(
            titleRes = R.string.settings_section_app,
            entries = listOf(
                SettingsMenuEntry(
                    title = stringResource(R.string.settings_bank_accounts),
                    subtitle = stringResource(R.string.settings_bank_accounts_sub),
                    icon = Icons.Default.AccountBalance,
                    route = SettingsRoutes.OPTIONS_BANK
                ),
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
                    title = stringResource(R.string.settings_language),
                    subtitle = stringResource(R.string.settings_language_sub),
                    icon = Icons.Default.Language,
                    route = SettingsRoutes.OPTIONS_LANGUAGE
                ),
                SettingsMenuEntry(
                    title = stringResource(R.string.backup_restore),
                    subtitle = stringResource(R.string.settings_backup_sub),
                    icon = Icons.Default.Backup,
                    route = SettingsRoutes.OPTIONS_BACKUP
                )
            )
        )
    )

    val expandedSections = remember { mutableStateMapOf<Int, Boolean>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawerMenuIconButton(onClick = onOpenDrawer)
            Text(
                text = stringResource(R.string.settings),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sections.forEach { section ->
                item(key = "accordion-${section.titleRes}") {
                    val expanded = expandedSections[section.titleRes] ?: false
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedSections[section.titleRes] = !expanded
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(section.titleRes),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF2E3F50)
                                )
                                Icon(
                                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                                                  else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color(0xFF2E3F50)
                                )
                            }
                            AnimatedVisibility(
                                visible = expanded,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Column {
                                    HorizontalDivider(color = Color(0xFFE2E8F0))
                                    section.entries.forEachIndexed { index, entry ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onNavigate(entry.route) }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                entry.icon,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(entry.title, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color(0xFF1A1A1A))
                                                Text(entry.subtitle, fontSize = 12.sp, color = Color.Gray)
                                            }
                                            Icon(
                                                Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = Color.LightGray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        if (index < section.entries.lastIndex) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(start = 48.dp),
                                                color = Color(0xFFE2E8F0)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336))
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.logout), fontWeight = FontWeight.SemiBold)
        }
    }
}
