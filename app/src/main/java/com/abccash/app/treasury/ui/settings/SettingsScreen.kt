package com.abccash.app.treasury.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
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
import com.abccash.app.R
import com.abccash.app.locale.AppLanguage
import com.abccash.app.locale.AppLocale
import com.abccash.app.treasury.backup.GoogleBackupManager
import com.abccash.app.ui.theme.AppColors
import com.abccash.app.treasury.data.SubscriptionPlan
import com.abccash.app.treasury.data.UserSubscription
import com.abccash.app.treasury.datastore.AppSettings
import com.abccash.app.treasury.datastore.AppSettingsState
import com.abccash.app.treasury.ui.googleSignInErrorMessage
import com.abccash.app.treasury.ui.resolveTreasuryMessage
import kotlinx.coroutines.launch

// Palette alignée ABC Cash — bleu principal, pas de violet.
private val SettingsBackground = Color.White
private val SettingsTextPrimary = AppColors.TextPrimary
private val SettingsMuted = AppColors.TextSecondary
private val SettingsDanger = AppColors.ExpenseRed
private val SettingsProgressTrack = AppColors.Border

/**
 * Architecture UX — Note de frais :
 * Aucun module « Note de frais » séparé. Les frais pro (repas, déplacements…) sont des
 * dépenses standard avec pièce jointe photo, stockée sur le Google Drive connecté.
 *
 * Architecture données — Onboarding :
 * Le prénom et le nom de société affichés ici proviennent de l'inscription initiale
 * (écran AccountSetup / Inscription, 2 étapes au premier lancement).
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    onUpgradeSubscription: () -> Unit,
    onExportCsv: (Int) -> String?,
    onDeleteAccount: (deleteDriveBackup: Boolean, onResult: (String?) -> Unit) -> Unit,
    onDeleteAllTransactions: (onResult: (String?) -> Unit) -> Unit = { it(null) },
    onDeleteTransactionsForMonth: (month: java.time.YearMonth, onResult: (String?) -> Unit) -> Unit = { _, cb -> cb(null) },
    onNavigate: (String) -> Unit,
    onOpenDrawer: () -> Unit = {},
    onAccountDeleted: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by appSettings.settingsFlow.collectAsState(initial = AppSettingsState())
    val currentLanguage = remember(settings.appLanguageTag) {
        AppLanguage.fromTag(settings.appLanguageTag)
    }

    var signedInEmail by remember(googleAccountEmail) {
        mutableStateOf(googleAccountEmail ?: googleBackupManager.getSignedInEmail())
    }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var googleError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteDriveBackup by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var showDeleteAllTxConfirm by remember { mutableStateOf(false) }
    var isDeletingAllTx by remember { mutableStateOf(false) }
    var deleteAllTxError by remember { mutableStateOf<String?>(null) }
    var deleteTxWholeScope by remember { mutableStateOf(true) }
    var deleteTxMonth by remember { mutableStateOf(java.time.YearMonth.now()) }
    var pendingCsv by remember { mutableStateOf<String?>(null) }
    val expandedSections = remember {
        mutableStateMapOf(
            "categories" to true,
            "app" to true
        )
    }

    fun toggleSection(key: String) {
        expandedSections[key] = !(expandedSections[key] ?: false)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isGoogleLoading = false
        if (result.resultCode != android.app.Activity.RESULT_OK) {
            if (result.resultCode != android.app.Activity.RESULT_CANCELED) {
                googleError = context.getString(R.string.google_sign_in_failed)
            }
            return@rememberLauncherForActivityResult
        }
        googleBackupManager.handleSignInResult(result.data)
            .onSuccess { account ->
                signedInEmail = account.email
                onGoogleSignedIn(account.email)
                googleError = null
            }
            .onFailure { error ->
                googleError = googleSignInErrorMessage(context, error)
            }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        val csv = pendingCsv
        if (uri != null && csv != null) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(csv.toByteArray(Charsets.UTF_8))
            }
        }
        pendingCsv = null
    }

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
                                color = SettingsTextPrimary
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
                            color = SettingsTextPrimary
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
                            color = SettingsTextPrimary
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
                                    Icons.Default.KeyboardArrowLeft,
                                    contentDescription = stringResource(R.string.previous_month)
                                )
                            }
                            Text(
                                text = AppLocale.monthYear(deleteTxMonth),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SettingsTextPrimary
                            )
                            IconButton(
                                onClick = { deleteTxMonth = deleteTxMonth.plusMonths(1) },
                                enabled = !isDeletingAllTx
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowRight,
                                    contentDescription = stringResource(R.string.next_month)
                                )
                            }
                        }
                    }
                    Text(
                        text = stringResource(R.string.settings_delete_transactions_warning),
                        fontSize = 12.sp,
                        color = AppColors.TextSecondary
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = SettingsTextPrimary
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CollapsibleSettingsSection(
                    title = stringResource(R.string.settings_screen_section_profile),
                    expanded = expandedSections["profile"] ?: false,
                    onToggle = { toggleSection("profile") }
                ) {
                    SettingsInfoListItem(
                        headline = stringResource(R.string.settings_user_first_name),
                        supporting = userFirstName.ifBlank { "—" },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = AppColors.BrandBlue) },
                        onClick = { onNavigate(SettingsRoutes.PROFILE_USER) }
                    )
                    HorizontalDivider(color = SettingsProgressTrack)
                    SettingsInfoListItem(
                        headline = stringResource(R.string.settings_company_name),
                        supporting = companyName.ifBlank { "—" },
                        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = AppColors.BrandBlue) },
                        onClick = { onNavigate(SettingsRoutes.PROFILE_COMPANY) }
                    )
                    HorizontalDivider(color = SettingsProgressTrack)
                    ListItem(
                        headlineContent = {
                            Text(
                                stringResource(R.string.settings_default_currency_fixed),
                                color = SettingsMuted,
                                fontSize = 14.sp
                            )
                        }
                    )
                    HorizontalDivider(color = SettingsProgressTrack)
                }
            }

            item {
                CollapsibleSettingsSection(
                    title = stringResource(R.string.settings_section_categories),
                    expanded = expandedSections["categories"] ?: true,
                    onToggle = { toggleSection("categories") }
                ) {
                    SettingsInfoListItem(
                        headline = stringResource(R.string.settings_income_categories),
                        supporting = stringResource(R.string.settings_manage_categories),
                        leadingIcon = {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = AppColors.BrandBlue)
                        },
                        onClick = { onNavigate(SettingsRoutes.CATEGORIES_INCOME) }
                    )
                    HorizontalDivider(color = SettingsProgressTrack)
                    SettingsInfoListItem(
                        headline = stringResource(R.string.settings_expense_categories),
                        supporting = stringResource(R.string.settings_manage_categories),
                        leadingIcon = {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = AppColors.BrandBlue)
                        },
                        onClick = { onNavigate(SettingsRoutes.CATEGORIES_EXPENSE) }
                    )
                }
            }

            item {
                CollapsibleSettingsSection(
                    title = stringResource(R.string.settings_section_app),
                    expanded = expandedSections["app"] ?: true,
                    onToggle = { toggleSection("app") }
                ) {
                    SettingsInfoListItem(
                        headline = stringResource(R.string.settings_bank_accounts),
                        supporting = stringResource(R.string.settings_bank_accounts_sub),
                        leadingIcon = {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = AppColors.BrandBlue
                            )
                        },
                        onClick = { onNavigate(SettingsRoutes.OPTIONS_BANK) }
                    )
                    SettingsInfoListItem(
                        headline = stringResource(R.string.settings_language),
                        supporting = stringResource(currentLanguage.labelRes),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = null,
                                tint = AppColors.BrandBlue
                            )
                        },
                        onClick = { onNavigate(SettingsRoutes.OPTIONS_LANGUAGE) }
                    )
                    SettingsInfoListItem(
                        headline = stringResource(R.string.settings_notifications),
                        supporting = if (settings.notificationsEnabled) {
                            stringResource(R.string.settings_notifications_on)
                        } else {
                            stringResource(R.string.settings_notifications_off)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = AppColors.BrandBlue
                            )
                        },
                        onClick = { onNavigate(SettingsRoutes.OPTIONS_NOTIFICATIONS) }
                    )
                }
            }

            item {
                SubscriptionSectionCard(
                    subscription = subscription,
                    onUpgrade = onUpgradeSubscription,
                    expanded = expandedSections["subscription"] ?: false,
                    onToggle = { toggleSection("subscription") }
                )
            }

            item {
                CollapsibleSettingsSection(
                    title = stringResource(R.string.settings_section_backup),
                    expanded = expandedSections["backup"] ?: false,
                    onToggle = { toggleSection("backup") }
                ) {
                    Text(
                        text = stringResource(R.string.settings_backup_cloud_desc),
                        fontSize = 13.sp,
                        color = SettingsMuted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    HorizontalDivider(color = SettingsProgressTrack)
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = signedInEmail?.let {
                                stringResource(R.string.settings_google_connected, it)
                            } ?: stringResource(R.string.settings_google_not_connected),
                            fontSize = 13.sp,
                            color = if (signedInEmail != null) Color(0xFF15803D) else SettingsMuted,
                            fontWeight = FontWeight.Medium
                        )
                        googleError?.let {
                            Text(it, color = SettingsDanger, fontSize = 12.sp)
                        }
                        if (signedInEmail == null) {
                            Button(
                                onClick = {
                                    isGoogleLoading = true
                                    googleSignInLauncher.launch(googleBackupManager.getSignInIntent())
                                },
                                enabled = !isGoogleLoading,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                )
                            ) {
                                if (isGoogleLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Text(
                                        "G  ${stringResource(R.string.settings_connect_google_drive)}",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        googleBackupManager.signOut()
                                        signedInEmail = null
                                        onGoogleSignedOut()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(stringResource(R.string.google_sign_out))
                            }
                        }
                    }
                }
            }

            item {
                CollapsibleSettingsSection(
                    title = stringResource(R.string.settings_section_security_export),
                    expanded = expandedSections["security"] ?: false,
                    onToggle = { toggleSection("security") }
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                stringResource(R.string.settings_biometric_lock),
                                color = SettingsTextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = settings.biometricEnabled,
                                onCheckedChange = { enabled ->
                                    scope.launch { appSettings.setBiometricEnabled(enabled) }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = SettingsProgressTrack
                                )
                            )
                        }
                    )
                    HorizontalDivider(color = SettingsProgressTrack)
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.settings_export_csv_hint),
                            fontSize = 12.sp,
                            color = SettingsMuted,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Button(
                            onClick = {
                                val csv = onExportCsv(java.time.YearMonth.now().year) ?: return@Button
                                pendingCsv = csv
                                csvExportLauncher.launch("abc-cash-export.csv")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                stringResource(R.string.settings_export_csv_excel),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            item {
                TextButton(
                    onClick = { showDeleteAllTxConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        stringResource(R.string.settings_delete_all_transactions),
                        color = SettingsDanger,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            item {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        stringResource(R.string.settings_delete_account),
                        color = SettingsDanger,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsibleSettingsSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SettingsTextPrimary
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AppColors.BrandBlue
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    HorizontalDivider(color = SettingsProgressTrack)
                    content()
                }
            }
        }
    }
}

@Composable
private fun SubscriptionSectionCard(
    subscription: UserSubscription,
    onUpgrade: () -> Unit,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val plan = subscription.plan
    val limit = plan.transactionsPerMonth ?: 30
    val used = subscription.transactionsThisMonth.coerceAtMost(limit)
    val progress = if (limit > 0) used.toFloat() / limit else 0f
    val isUnlimited = plan.unlimited

    CollapsibleSettingsSection(
        title = stringResource(R.string.settings_section_subscription),
        expanded = expanded,
        onToggle = onToggle
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!isUnlimited) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AppColors.BrandBlueLight
                ) {
                    Text(
                        text = stringResource(R.string.settings_free_plan_badge),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.BrandBlueDark
                    )
                }
                Text(
                    text = stringResource(R.string.settings_monthly_usage, used, limit),
                    fontSize = 14.sp,
                    color = SettingsTextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.subscription_monthly_reset_hint),
                    fontSize = 12.sp,
                    color = SettingsMuted
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = SettingsProgressTrack,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            } else {
                Text(
                    text = stringResource(R.string.plan_unlimited),
                    fontSize = 14.sp,
                    color = Color(0xFF15803D),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onUpgrade,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                enabled = !isUnlimited
            ) {
                Text(
                    stringResource(R.string.settings_upgrade_unlimited),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SettingsInfoListItem(
    headline: String,
    supporting: String,
    leadingIcon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(headline, color = SettingsMuted, fontSize = 12.sp)
        },
        supportingContent = {
            Text(
                supporting,
                color = SettingsTextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
        },
        leadingContent = leadingIcon,
        modifier = Modifier
            .fillMaxWidth()
            .then(Modifier.clickable(onClick = onClick)),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
