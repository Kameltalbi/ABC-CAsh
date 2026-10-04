package com.abccash.app.treasury.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R
import com.abccash.app.locale.AppLanguage
import com.abccash.app.locale.AppLocale
import com.abccash.app.treasury.backup.GoogleBackupManager
import com.abccash.app.treasury.data.UserSubscription
import com.abccash.app.treasury.datastore.AppSettings
import com.abccash.app.treasury.datastore.AppSettingsState
import com.abccash.app.treasury.ui.resolveTreasuryMessage
import com.abccash.app.ui.theme.AppColors
import java.util.Locale

private val PageBg = Color(0xFFF4F6FA)
private val Ink = Color(0xFF0F2744)
private val Muted = Color(0xFF6B7A90)
private val Line = Color(0xFFE6EBF2)
private val Blue = Color(0xFF2563EB)
private val BlueSoft = Color(0xFFE8F0FE)
private val Green = Color(0xFF15803D)
private val GreenSoft = Color(0xFFE7F7ED)
private val Orange = Color(0xFFC2410C)
private val OrangeSoft = Color(0xFFFFF1E8)
private val Purple = Color(0xFF7C3AED)
private val PurpleSoft = Color(0xFFF3E8FF)
private val Amber = Color(0xFFD97706)
private val AmberSoft = Color(0xFFFFF7E8)
private val SettingsDanger = AppColors.ExpenseRed
private val DangerSoft = Color(0xFFFFF1F0)
private val CardShape = RoundedCornerShape(18.dp)

@Suppress("UNUSED_PARAMETER")
@Composable
fun SettingsScreen(
    userFirstName: String,
    companyName: String,
    subscription: UserSubscription,
    appSettings: AppSettings,
    googleBackupManager: GoogleBackupManager,
    googleAccountEmail: String?,
    onGoogleSignedIn: (String?) -> Unit,
    onGoogleSignedOut: () -> Unit,
    onUpgradeSubscription: () -> Unit = {},
    onExportCsv: (Int) -> String?,
    onDeleteAccount: (deleteDriveBackup: Boolean, onResult: (String?) -> Unit) -> Unit,
    onDeleteAllTransactions: (onResult: (String?) -> Unit) -> Unit = { it(null) },
    onDeleteTransactionsForMonth: (month: java.time.YearMonth, onResult: (String?) -> Unit) -> Unit = { _, cb -> cb(null) },
    onNavigate: (String) -> Unit,
    onAccountDeleted: () -> Unit = {}
) {
    val context = LocalContext.current
    val settings by appSettings.settingsFlow.collectAsState(initial = AppSettingsState())
    val currentLanguage = remember(settings.appLanguageTag) {
        AppLanguage.fromTag(settings.appLanguageTag)
    }
    val signedInEmail = googleAccountEmail ?: googleBackupManager.getSignedInEmail()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteDriveBackup by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var showDeleteAllTxConfirm by remember { mutableStateOf(false) }
    var isDeletingAllTx by remember { mutableStateOf(false) }
    var deleteAllTxError by remember { mutableStateOf<String?>(null) }
    var deleteTxWholeScope by remember { mutableStateOf(true) }
    var deleteTxMonth by remember { mutableStateOf(java.time.YearMonth.now()) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) {
                    showDeleteConfirm = false
                    deleteDriveBackup = false
                }
            },
            title = { Text(stringResource(R.string.settings_delete_account_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_delete_account_confirm_message))
                    if (signedInEmail != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = deleteDriveBackup,
                                onCheckedChange = { deleteDriveBackup = it },
                                enabled = !isDeleting
                            )
                            Text(
                                text = stringResource(R.string.settings_delete_drive_backup),
                                fontSize = 14.sp,
                                color = Ink
                            )
                        }
                    }
                    deleteError?.let { Text(it, color = SettingsDanger, fontSize = 13.sp) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleting = true
                        deleteError = null
                        onDeleteAccount(deleteDriveBackup) { error ->
                            isDeleting = false
                            if (error == null) {
                                showDeleteConfirm = false
                                deleteDriveBackup = false
                                onAccountDeleted()
                            } else {
                                deleteError = error
                            }
                        }
                    },
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.delete), color = SettingsDanger)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        deleteDriveBackup = false
                    },
                    enabled = !isDeleting
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDeleteAllTxConfirm) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeletingAllTx) {
                    showDeleteAllTxConfirm = false
                    deleteAllTxError = null
                }
            },
            title = { Text(stringResource(R.string.settings_delete_transactions_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isDeletingAllTx) { deleteTxWholeScope = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = deleteTxWholeScope,
                            onClick = { deleteTxWholeScope = true },
                            enabled = !isDeletingAllTx
                        )
                        Text(
                            text = stringResource(R.string.settings_delete_scope_all),
                            fontSize = 14.sp,
                            color = Ink
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isDeletingAllTx) { deleteTxWholeScope = false },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !deleteTxWholeScope,
                            onClick = { deleteTxWholeScope = false },
                            enabled = !isDeletingAllTx
                        )
                        Text(
                            text = stringResource(R.string.settings_delete_scope_month),
                            fontSize = 14.sp,
                            color = Ink
                        )
                    }
                    if (!deleteTxWholeScope) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { deleteTxMonth = deleteTxMonth.minusMonths(1) },
                                enabled = !isDeletingAllTx
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = stringResource(R.string.previous_month)
                                )
                            }
                            Text(
                                text = AppLocale.monthYear(deleteTxMonth),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Ink
                            )
                            IconButton(
                                onClick = { deleteTxMonth = deleteTxMonth.plusMonths(1) },
                                enabled = !isDeletingAllTx
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = stringResource(R.string.next_month)
                                )
                            }
                        }
                    }
                    Text(
                        text = stringResource(R.string.settings_delete_transactions_warning),
                        fontSize = 12.sp,
                        color = Muted
                    )
                    deleteAllTxError?.let { Text(it, color = SettingsDanger, fontSize = 13.sp) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeletingAllTx = true
                        deleteAllTxError = null
                        val callback: (String?) -> Unit = { error ->
                            isDeletingAllTx = false
                            if (error == null) {
                                showDeleteAllTxConfirm = false
                            } else {
                                deleteAllTxError = context.resolveTreasuryMessage(error) ?: error
                            }
                        }
                        if (deleteTxWholeScope) {
                            onDeleteAllTransactions(callback)
                        } else {
                            onDeleteTransactionsForMonth(deleteTxMonth, callback)
                        }
                    },
                    enabled = !isDeletingAllTx
                ) {
                    if (isDeletingAllTx) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.delete), color = SettingsDanger)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAllTxConfirm = false },
                    enabled = !isDeletingAllTx
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    val languageLabel = when {
        currentLanguage == AppLanguage.FRENCH -> stringResource(R.string.language_fr_default)
        currentLanguage == AppLanguage.SYSTEM &&
            Locale.getDefault().language.equals("fr", ignoreCase = true) ->
            stringResource(R.string.language_fr_default)
        else -> stringResource(currentLanguage.labelRes)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        Text(
            text = stringResource(R.string.nav_plus),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Ink
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                CompanyCard(
                    userName = userFirstName,
                    companyName = companyName,
                    onClick = { onNavigate(SettingsRoutes.PROFILE_USER) }
                )
            }
            item {
                SettingsGroupCard(
                    title = stringResource(R.string.pilot_categories),
                    titleIcon = Icons.Default.LocalOffer,
                    titleTint = Green,
                    titleBg = GreenSoft
                ) {
                    SettingsRow(
                        title = stringResource(R.string.settings_sales_categories),
                        subtitle = stringResource(R.string.settings_sales_categories_sub),
                        icon = Icons.Default.TrendingUp,
                        iconTint = Green,
                        iconBg = GreenSoft,
                        onClick = { onNavigate(SettingsRoutes.CATEGORIES_INCOME) }
                    )
                    HorizontalDivider(color = Line, modifier = Modifier.padding(start = 52.dp))
                    SettingsRow(
                        title = stringResource(R.string.settings_expense_categories),
                        subtitle = stringResource(R.string.settings_expense_categories_hub_sub),
                        icon = Icons.Default.ShoppingCart,
                        iconTint = Orange,
                        iconBg = OrangeSoft,
                        onClick = { onNavigate(SettingsRoutes.CATEGORIES_EXPENSE) }
                    )
                }
            }
            item {
                SettingsGroupCard(
                    title = stringResource(R.string.settings_section_accounts),
                    titleIcon = Icons.Default.AccountBalance,
                    titleTint = Purple,
                    titleBg = PurpleSoft
                ) {
                    SettingsRow(
                        title = stringResource(R.string.settings_bank_accounts),
                        subtitle = stringResource(R.string.settings_bank_accounts_hub_sub),
                        icon = Icons.Default.AccountBalance,
                        iconTint = Purple,
                        iconBg = PurpleSoft,
                        onClick = { onNavigate(SettingsRoutes.OPTIONS_BANK) }
                    )
                }
            }
            item {
                SettingsGroupCard(
                    title = stringResource(R.string.settings_section_app),
                    titleIcon = Icons.Default.Settings,
                    titleTint = Amber,
                    titleBg = AmberSoft
                ) {
                    SettingsRow(
                        title = stringResource(R.string.settings_language),
                        subtitle = languageLabel,
                        icon = Icons.Default.Language,
                        iconTint = Blue,
                        iconBg = BlueSoft,
                        onClick = { onNavigate(SettingsRoutes.OPTIONS_LANGUAGE) }
                    )
                    HorizontalDivider(color = Line, modifier = Modifier.padding(start = 52.dp))
                    SettingsRow(
                        title = stringResource(R.string.settings_notifications),
                        subtitle = if (settings.notificationsEnabled) {
                            stringResource(R.string.settings_notifications_on)
                        } else {
                            stringResource(R.string.settings_notifications_off)
                        },
                        icon = Icons.Default.Notifications,
                        iconTint = Blue,
                        iconBg = BlueSoft,
                        onClick = { onNavigate(SettingsRoutes.OPTIONS_NOTIFICATIONS) }
                    )
                    HorizontalDivider(color = Line, modifier = Modifier.padding(start = 52.dp))
                    SettingsRow(
                        title = stringResource(R.string.settings_appearance),
                        subtitle = stringResource(R.string.settings_appearance_sub),
                        icon = Icons.Default.PhoneAndroid,
                        iconTint = Blue,
                        iconBg = BlueSoft,
                        onClick = null
                    )
                }
            }
            item {
                LaunchPhaseInfoCard()
            }
            item {
                SettingsLinkCard(
                    title = stringResource(R.string.settings_section_backup),
                    subtitle = stringResource(R.string.settings_backup_row_sub),
                    icon = Icons.Default.CloudUpload,
                    iconTint = Blue,
                    iconBg = BlueSoft,
                    onClick = { onNavigate(SettingsRoutes.OPTIONS_BACKUP) }
                )
            }
            item {
                SettingsLinkCard(
                    title = stringResource(R.string.settings_section_security_export),
                    subtitle = stringResource(R.string.settings_security_row_sub),
                    icon = Icons.Default.VerifiedUser,
                    iconTint = Blue,
                    iconBg = BlueSoft,
                    onClick = { onNavigate(SettingsRoutes.OPTIONS_SECURITY) }
                )
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DangerSoft),
                    shape = CardShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        SettingsRow(
                            title = stringResource(R.string.settings_delete_all_transactions),
                            subtitle = stringResource(R.string.settings_delete_all_transactions_sub),
                            icon = Icons.Default.DeleteForever,
                            iconTint = SettingsDanger,
                            iconBg = Color.White,
                            titleColor = SettingsDanger,
                            onClick = { showDeleteAllTxConfirm = true }
                        )
                        HorizontalDivider(color = Color(0xFFFFD0C8), modifier = Modifier.padding(start = 52.dp))
                        SettingsRow(
                            title = stringResource(R.string.settings_delete_account),
                            subtitle = stringResource(R.string.settings_delete_account_sub),
                            icon = Icons.Default.PersonOff,
                            iconTint = SettingsDanger,
                            iconBg = Color.White,
                            titleColor = SettingsDanger,
                            onClick = { showDeleteConfirm = true }
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun CompanyCard(
    userName: String,
    companyName: String,
    onClick: () -> Unit
) {
    val initials = remember(userName, companyName) {
        val source = userName.ifBlank { companyName }
        source.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "A" }
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SoftIcon(Icons.Default.Storefront, Blue, BlueSoft)
                Text(
                    text = stringResource(R.string.settings_section_company),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f),
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Muted,
                    modifier = Modifier.size(20.dp)
                )
            }
            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(BlueSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initials, color = Blue, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(stringResource(R.string.settings_user_first_name), color = Muted, fontSize = 11.sp)
                    Text(
                        text = userName.ifBlank { "—" },
                        color = Ink,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.settings_company_name),
                        color = Muted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = companyName.ifBlank { "—" },
                        color = Ink,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SoftIcon(Icons.Default.Savings, Amber, AmberSoft, size = 26.dp, iconSize = 14.dp)
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(stringResource(R.string.settings_main_currency), color = Muted, fontSize = 11.sp)
                            Text(
                                text = stringResource(R.string.settings_currency_tnd),
                                color = Ink,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    titleIcon: ImageVector,
    titleTint: Color,
    titleBg: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                SoftIcon(titleIcon, titleTint, titleBg)
                Text(
                    text = title,
                    modifier = Modifier.padding(start = 8.dp),
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            content()
        }
    }
}

@Composable
private fun LaunchPhaseInfoCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        SettingsRow(
            title = stringResource(R.string.settings_launch_free_title),
            subtitle = stringResource(R.string.settings_launch_free_message),
            icon = Icons.Default.WorkspacePremium,
            iconTint = Amber,
            iconBg = AmberSoft,
            onClick = null,
            showChevron = false
        )
    }
}

@Composable
private fun SettingsLinkCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        SettingsRow(
            title = title,
            subtitle = subtitle,
            icon = icon,
            iconTint = iconTint,
            iconBg = iconBg,
            onClick = null,
            showChevron = true
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    onClick: (() -> Unit)?,
    titleColor: Color = Ink,
    showChevron: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SoftIcon(icon, iconTint, iconBg)
        Column(
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f)
        ) {
            Text(
                text = title,
                color = titleColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = Muted,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
        if (showChevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SoftIcon(
    icon: ImageVector,
    tint: Color,
    bg: Color,
    size: androidx.compose.ui.unit.Dp = 32.dp,
    iconSize: androidx.compose.ui.unit.Dp = 17.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}
