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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
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
import com.abccash.app.treasury.backup.GoogleBackupManager
import com.abccash.app.ui.theme.AppColors
import com.abccash.app.treasury.data.SubscriptionPlan
import com.abccash.app.treasury.data.UserSubscription
import com.abccash.app.treasury.datastore.AppSettings
import com.abccash.app.treasury.datastore.AppSettingsState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
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
    onDeleteAccount: (onResult: (String?) -> Unit) -> Unit,
    onNavigate: (String) -> Unit,
    onOpenDrawer: () -> Unit = {},
    onAccountDeleted: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by appSettings.settingsFlow.collectAsState(initial = AppSettingsState())

    var signedInEmail by remember(googleAccountEmail) {
        mutableStateOf(googleAccountEmail ?: googleBackupManager.getSignedInEmail())
    }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var googleError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var pendingCsv by remember { mutableStateOf<String?>(null) }
    val expandedSections = remember { mutableStateMapOf<String, Boolean>() }

    fun toggleSection(key: String) {
        expandedSections[key] = !(expandedSections[key] ?: false)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isGoogleLoading = false
        runCatching {
            GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
        }.onSuccess { account ->
            signedInEmail = account.email
            onGoogleSignedIn(account.email)
            googleError = null
        }.onFailure {
            googleError = context.getString(R.string.google_sign_in_failed)
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
                if (!isDeleting) showDeleteConfirm = false
            },
            title = { Text(stringResource(R.string.settings_delete_account_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_delete_account_confirm_message))
                    deleteError?.let { Text(it, color = SettingsDanger, fontSize = 13.sp) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleting = true
                        deleteError = null
                        onDeleteAccount { error ->
                            isDeleting = false
                            if (error == null) {
                                showDeleteConfirm = false
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
                    onClick = { showDeleteConfirm = false },
                    enabled = !isDeleting
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
                    SettingsInfoListItem(
                        headline = stringResource(R.string.settings_manage_categories),
                        supporting = stringResource(R.string.settings_income_categories_sub),
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, tint = AppColors.BrandBlue) },
                        onClick = { onNavigate(SettingsRoutes.CATEGORIES_INCOME) }
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
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
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
