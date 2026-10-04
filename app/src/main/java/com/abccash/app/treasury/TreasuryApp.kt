package com.abccash.app.treasury

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.abccash.app.treasury.data.TransactionType
import com.abccash.app.treasury.data.User
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import com.abccash.app.treasury.data.effectivePermissions
import com.abccash.app.treasury.data.hasPermission
import com.abccash.app.treasury.datastore.UserPreferences
import com.abccash.app.treasury.backup.GoogleBackupManager
import com.abccash.app.treasury.billing.BillingManager
import com.abccash.app.treasury.repository.TreasuryRepository
import com.abccash.app.treasury.datastore.AppSettings
import com.abccash.app.treasury.ui.*
import com.abccash.app.treasury.ui.settings.*
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import com.abccash.app.R
import com.abccash.app.ui.theme.AppColors
import com.abccash.app.treasury.repository.PilotageRepository
import com.abccash.app.treasury.viewmodel.InscriptionViewModelFactory
import com.abccash.app.treasury.viewmodel.LoginViewModelFactory
import com.abccash.app.treasury.viewmodel.PilotageViewModel
import com.abccash.app.treasury.viewmodel.PilotageViewModelFactory
import com.abccash.app.treasury.viewmodel.TreasuryViewModel
import kotlinx.coroutines.launch
import java.time.YearMonth

sealed class Screen(val route: String, @StringRes val titleRes: Int, val icon: ImageVector) {
    object Splash : Screen("splash", R.string.loading, Icons.Default.HourglassEmpty)
    object Onboarding : Screen("onboarding", R.string.app_name, Icons.Default.Info)
    object Login : Screen("login", R.string.login, Icons.Default.Login)
    object AccountSetup : Screen("account_setup", R.string.account_setup_title, Icons.Default.PersonAdd)
    object Inscription : Screen("inscription", R.string.create_account, Icons.Default.PersonAdd)
    object Dashboard : Screen("dashboard", R.string.nav_home, Icons.Default.Home)
    object Treasury : Screen("treasury", R.string.nav_treasury, Icons.Default.TrendingUp)
    object Pilotage : Screen("pilotage", R.string.nav_pilotage, Icons.Default.BarChart)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
    object AddTransaction : Screen("add_transaction/{type}", R.string.transactions, Icons.Default.Add)
    object BankReconciliation : Screen("bank_reconciliation", R.string.bank_account, Icons.Default.AccountBalance)
    object BankAccounts : Screen("bank_accounts", R.string.bank_accounts_title, Icons.Default.AccountBalance)
    object BankAccountDetail : Screen("bank_account/{accountId}", R.string.bank_account_detail, Icons.Default.AccountBalance)
    object Subscription : Screen("subscription", R.string.plan_free, Icons.Default.Payments)
    object TreasuryCorrectionHistory : Screen("treasury_correction_history", R.string.treasury_history_title, Icons.Default.AccountBalance)
}

@Composable
private fun NavBarLabel(text: String) {
    Text(
        text = text,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        fontSize = 10.sp,
        lineHeight = 11.sp,
        style = MaterialTheme.typography.labelSmall
    )
}

@Composable
private fun Screen.adaptiveNavLabel(itemCount: Int): String {
    val slotWidth = LocalConfiguration.current.screenWidthDp / itemCount.coerceAtLeast(1)
    val useShort = slotWidth < 78
    return when (this) {
        Screen.Dashboard -> if (useShort) {
            stringResource(R.string.nav_home_short)
        } else {
            stringResource(R.string.nav_home)
        }
        Screen.Treasury -> if (useShort) {
            stringResource(R.string.nav_treasury_short)
        } else {
            stringResource(R.string.nav_treasury)
        }
        Screen.Pilotage -> if (useShort) {
            stringResource(R.string.nav_pilotage_short)
        } else {
            stringResource(R.string.nav_pilotage)
        }
        Screen.Settings -> if (useShort) {
            stringResource(R.string.nav_settings_short)
        } else {
            stringResource(R.string.nav_settings)
        }
        else -> stringResource(titleRes)
    }
}

@Composable
fun TreasuryApp(
    repository: TreasuryRepository,
    pilotageRepository: PilotageRepository,
    viewModel: TreasuryViewModel,
    userPreferences: UserPreferences,
    googleBackupManager: GoogleBackupManager
) {
    val navController = rememberNavController()
    val pilotageViewModel: PilotageViewModel = viewModel(factory = PilotageViewModelFactory(pilotageRepository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val googleAccountEmail by userPreferences.googleAccountEmail.collectAsStateWithLifecycle(initialValue = null)
    val context = LocalContext.current
    val appSettings = remember { AppSettings(context) }
    val coroutineScope = rememberCoroutineScope()

    var isAuthenticated by remember { mutableStateOf(false) }
    var currentUserRole by remember { mutableStateOf<UserRole?>(null) }
    var currentPermissions by remember { mutableStateOf<Set<UserPermission>>(emptySet()) }

    fun enterMainApp(user: User) {
        val permissions = effectivePermissions(user.role, user.permissions)
        coroutineScope.launch {
            userPreferences.saveUserSession(
                userId = user.id,
                email = user.email,
                nom = user.nom,
                role = user.role,
                entrepriseId = user.entrepriseId,
                permissions = permissions
            )
            isAuthenticated = true
            currentUserRole = user.role
            currentPermissions = permissions
            viewModel.setSession(user.entrepriseId, user.role, permissions, user.id)
            navController.navigate(Screen.Dashboard.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    fun logout() {
        coroutineScope.launch {
            userPreferences.clearUserSession()
            viewModel.clearSession()
            isAuthenticated = false
            currentUserRole = null
            currentPermissions = emptySet()
            val currentRoute = navController.currentDestination?.route
            navController.navigate(Screen.Login.route) {
                if (currentRoute != null) {
                    popUpTo(currentRoute) { inclusive = true }
                }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(uiState.entrepriseId) {
        val entrepriseId = uiState.entrepriseId ?: return@LaunchedEffect
        // Aucune donnée d'exemple / fichier embarqué : le nouvel utilisateur démarre vide.
        pilotageViewModel.bind(entrepriseId)
    }

    // Si les comptes arrivent après l'import des ventes, rattacher date d'encaissement + compte.
    LaunchedEffect(uiState.entrepriseId, uiState.bankAccounts.map { it.id }) {
        if (uiState.entrepriseId.isNullOrBlank()) return@LaunchedEffect
        if (uiState.bankAccounts.none { it.entrepriseId == uiState.entrepriseId }) return@LaunchedEffect
        pilotageViewModel.applyCashDefaultsIfNeeded()
    }

    AppCurrencyProvider(appSettings = appSettings) {
        EnsureDefaultCategories(
            entrepriseId = uiState.entrepriseId.orEmpty(),
            appSettings = appSettings
        )
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route
        ) {
        composable(Screen.Splash.route) {
            SplashDecisionScreen(
                repository = repository,
                userPreferences = userPreferences,
                onNavigateToInscription = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToAccountSetup = {
                    navController.navigate(Screen.AccountSetup.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToMainApp = { userId, userRole, entrepriseId, permissions ->
                    isAuthenticated = true
                    currentUserRole = userRole
                    currentPermissions = permissions
                    viewModel.setSession(entrepriseId, userRole, permissions, userId)
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Screen.Inscription.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { user -> enterMainApp(user) },
                viewModel = viewModel(factory = LoginViewModelFactory(repository))
            )
        }

        composable(Screen.AccountSetup.route) {
            AccountSetupScreen(
                repository = repository,
                onSetupComplete = { user -> enterMainApp(user) }
            )
        }

        composable(Screen.Inscription.route) {
            var canRegister by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(Unit) {
                canRegister = !repository.hasAnyUser()
                if (canRegister == false) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Inscription.route) { inclusive = true }
                    }
                }
            }
            if (canRegister == true) {
                InscriptionScreen(
                    onInscriptionSuccess = { user -> enterMainApp(user) },
                    googleBackupManager = googleBackupManager,
                    onGoogleConnected = viewModel::onGoogleSignedIn,
                    onRestoreFromGoogle = viewModel::restoreInitialFromGoogle,
                    viewModel = viewModel(factory = InscriptionViewModelFactory(repository))
                )
            }
        }

        composable(Screen.Dashboard.route) {
            MainAppScaffold(
                navController = navController,
                viewModel = viewModel,
                pilotageViewModel = pilotageViewModel,
                userRole = currentUserRole ?: uiState.currentUserRole,
                permissions = currentPermissions.ifEmpty { uiState.permissions },
                appSettings = appSettings,
                userPreferences = userPreferences,
                googleBackupManager = googleBackupManager,
                googleAccountEmail = googleAccountEmail,
                startDestination = Screen.Dashboard.route,
                onLogout = { logout() }
            )
        }

        composable(Screen.Treasury.route) {
            MainAppScaffold(
                navController = navController,
                viewModel = viewModel,
                pilotageViewModel = pilotageViewModel,
                userRole = currentUserRole ?: uiState.currentUserRole,
                permissions = currentPermissions.ifEmpty { uiState.permissions },
                appSettings = appSettings,
                userPreferences = userPreferences,
                googleBackupManager = googleBackupManager,
                googleAccountEmail = googleAccountEmail,
                startDestination = Screen.Treasury.route,
                onLogout = { logout() }
            )
        }

        composable(Screen.Pilotage.route) {
            MainAppScaffold(
                navController = navController,
                viewModel = viewModel,
                pilotageViewModel = pilotageViewModel,
                userRole = currentUserRole ?: uiState.currentUserRole,
                permissions = currentPermissions.ifEmpty { uiState.permissions },
                appSettings = appSettings,
                userPreferences = userPreferences,
                googleBackupManager = googleBackupManager,
                googleAccountEmail = googleAccountEmail,
                startDestination = Screen.Pilotage.route,
                onLogout = { logout() }
            )
        }

        composable(Screen.Settings.route) {
            MainAppScaffold(
                navController = navController,
                viewModel = viewModel,
                pilotageViewModel = pilotageViewModel,
                userRole = currentUserRole ?: uiState.currentUserRole,
                permissions = currentPermissions.ifEmpty { uiState.permissions },
                appSettings = appSettings,
                userPreferences = userPreferences,
                googleBackupManager = googleBackupManager,
                googleAccountEmail = googleAccountEmail,
                startDestination = Screen.Settings.route,
                onLogout = { logout() }
            )
        }

        composable(Screen.Subscription.route) {
            MainAppScaffold(
                navController = navController,
                viewModel = viewModel,
                pilotageViewModel = pilotageViewModel,
                userRole = currentUserRole ?: uiState.currentUserRole,
                permissions = currentPermissions.ifEmpty { uiState.permissions },
                appSettings = appSettings,
                userPreferences = userPreferences,
                googleBackupManager = googleBackupManager,
                googleAccountEmail = googleAccountEmail,
                startDestination = Screen.Subscription.route,
                onLogout = { logout() }
            )
        }

        composable(SettingsRoutes.PROFILE_USER) {
            val sessionExpiredMessage = stringResource(R.string.session_expired)
            val currentUser = uiState.users.find { it.id == uiState.currentUserId }
            SettingsUserProfileScreen(
                currentUser = currentUser,
                onBack = { navController.popBackStack() },
                onSave = { nom, email, telephone, onResult ->
                    val userId = uiState.currentUserId
                    if (userId == null) {
                        onResult(sessionExpiredMessage)
                    } else {
                        viewModel.updateUserProfile(userId, nom, email, telephone, onResult)
                    }
                },
                onSessionUpdated = { nom, email ->
                    coroutineScope.launch { userPreferences.updateProfileSession(nom, email) }
                }
            )
        }

        composable(SettingsRoutes.PROFILE_COMPANY) {
            val role = currentUserRole ?: uiState.currentUserRole
            SettingsCompanyProfileScreen(
                entreprise = uiState.entreprise,
                canEdit = role == UserRole.ADMIN,
                onBack = { navController.popBackStack() },
                onSave = viewModel::updateEntrepriseProfile
            )
        }

        composable(SettingsRoutes.CATEGORIES_INCOME) {
            SettingsIncomeCategoriesScreen(
                entrepriseId = uiState.entrepriseId.orEmpty(),
                appSettings = appSettings,
                onBack = { navController.popBackStack() }
            )
        }

        composable(SettingsRoutes.CATEGORIES_EXPENSE) {
            SettingsExpenseCategoriesScreen(
                entrepriseId = uiState.entrepriseId.orEmpty(),
                appSettings = appSettings,
                onBack = { navController.popBackStack() }
            )
        }

        composable(SettingsRoutes.OPTIONS_CURRENCY) {
            SettingsCurrencyScreen(
                appSettings = appSettings,
                onBack = { navController.popBackStack() }
            )
        }

        composable(SettingsRoutes.OPTIONS_NOTIFICATIONS) {
            SettingsNotificationsScreen(
                appSettings = appSettings,
                onBack = { navController.popBackStack() }
            )
        }

        composable(SettingsRoutes.OPTIONS_SECURITY) {
            SettingsSecurityScreen(
                appSettings = appSettings,
                onBack = { navController.popBackStack() }
            )
        }

        composable(SettingsRoutes.OPTIONS_LANGUAGE) {
            SettingsLanguageScreen(
                appSettings = appSettings,
                onBack = { navController.popBackStack() }
            )
        }

        composable(SettingsRoutes.OPTIONS_BACKUP) {
            SettingsBackupScreen(
                googleBackupManager = googleBackupManager,
                googleAccountEmail = googleAccountEmail ?: viewModel.googleSignedInEmail(),
                onBack = { navController.popBackStack() },
                onGoogleSignedIn = viewModel::onGoogleSignedIn,
                onGoogleSignedOut = viewModel::onGoogleSignedOut,
                onBackupToGoogle = viewModel::backupToGoogle,
                onRestoreFromGoogle = viewModel::restoreFromGoogle,
                onExportBackup = viewModel::exportBackup,
                onRestoreBackup = viewModel::restoreBackup,
                backupFeedback = uiState.backupFeedback,
                onClearBackupFeedback = viewModel::clearBackupFeedback
            )
        }

        composable(SettingsRoutes.OPTIONS_BANK) {
            val context = LocalContext.current
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            var showAddSheet by remember { mutableStateOf(false) }
            var editingAccount by remember { mutableStateOf<com.abccash.app.treasury.data.BankAccount?>(null) }
            var saveError by remember { mutableStateOf<String?>(null) }
            val subscription = uiState.subscription
            val accountsLimit = subscription.plan.treasuryAccountsLimit
            val accountsUsed = subscription.treasuryAccountsCount
            val canAddAccount = !subscription.isTreasuryAccountLimitReached
            val accountLimitError = stringResource(R.string.treasury_accounts_limit_reached)
            val summaries = remember(uiState.bankAccounts, uiState.invoices, uiState.expenses) {
                viewModel.bankAccountSummaries()
            }

            BankAccountsListScreen(
                summaries = summaries,
                accountsUsed = accountsUsed,
                accountsLimit = accountsLimit,
                canAddAccount = canAddAccount,
                onBack = { navController.popBackStack() },
                onAddAccount = {
                    if (canAddAccount) {
                        saveError = null
                        showAddSheet = true
                    } else {
                        saveError = accountLimitError
                    }
                },
                onOpenAccount = { accountId ->
                    navController.navigate("bank_account/$accountId")
                },
                onEditAccount = { account ->
                    saveError = null
                    editingAccount = account
                },
                onDeleteAccount = { account ->
                    viewModel.deleteBankAccount(account.id) { error ->
                        if (error != null) {
                            Toast.makeText(
                                context,
                                context.resolveTreasuryMessage(error) ?: error,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                onOpenManualReconciliation = {
                    navController.navigate(Screen.BankReconciliation.route)
                }
            )

            BankAccountFormSheet(
                visible = showAddSheet || editingAccount != null,
                initialAccount = editingAccount,
                entrepriseId = uiState.entrepriseId.orEmpty(),
                errorMessage = saveError,
                onDismiss = {
                    showAddSheet = false
                    editingAccount = null
                    saveError = null
                },
                onSave = { account ->
                    viewModel.saveBankAccount(account) { error ->
                        if (error == null) {
                            showAddSheet = false
                            editingAccount = null
                            saveError = null
                        } else {
                            saveError = context.resolveTreasuryMessage(error) ?: error
                        }
                    }
                }
            )
        }

        composable(
            route = Screen.BankAccountDetail.route,
            arguments = listOf(navArgument("accountId") { type = NavType.StringType })
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId").orEmpty()
            val context = LocalContext.current
            val account = viewModel.getBankAccount(accountId)
            var showEditSheet by remember { mutableStateOf(false) }
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val balance = remember(uiState, accountId) { viewModel.bankAccountBalance(accountId) }
            val movements = remember(uiState, accountId) { viewModel.bankAccountMovements(accountId) }

            if (account == null) {
                AccessDeniedScreen(
                    message = stringResource(R.string.bank_account_not_found),
                    onBack = { navController.popBackStack() }
                )
            } else {
                BankAccountDetailScreen(
                    account = account,
                    balance = balance,
                    movements = movements,
                    hasLowBalanceAlert = account.alertLowBalance?.let { balance < it } == true,
                    onBack = { navController.popBackStack() },
                    onEdit = { showEditSheet = true },
                    onDelete = {
                        viewModel.deleteBankAccount(accountId) { error ->
                            if (error == null) {
                                navController.popBackStack()
                            } else {
                                Toast.makeText(
                                    context,
                                    context.resolveTreasuryMessage(error) ?: error,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                )
                BankAccountFormSheet(
                    visible = showEditSheet,
                    initialAccount = account,
                    entrepriseId = uiState.entrepriseId.orEmpty(),
                    onDismiss = { showEditSheet = false },
                    onSave = { updated ->
                        viewModel.saveBankAccount(updated) { error ->
                            if (error == null) showEditSheet = false
                        }
                    }
                )
            }
        }

        composable(
            route = "add_transaction/{type}?forecast={forecast}",
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("forecast") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val type = TransactionType.fromRoute(backStackEntry.arguments?.getString("type"))
            val forecast = backStackEntry.arguments?.getBoolean("forecast") ?: false
            val role = currentUserRole ?: uiState.currentUserRole
            val permissions = currentPermissions.ifEmpty { uiState.permissions }
            val entrepriseId = uiState.entrepriseId.orEmpty()
            val customIncome by appSettings.customIncomeCategories(entrepriseId)
                .collectAsStateWithLifecycle(initialValue = emptyList())
            val customExpense by appSettings.customExpenseCategories(entrepriseId)
                .collectAsStateWithLifecycle(initialValue = emptyList())
            val hasOcrScan = uiState.subscription.plan.hasOcrScan
            when (type) {
                TransactionType.INCOME -> if (role == UserRole.ADMIN) {
                    NewTransactionScreen(
                        type = TransactionType.INCOME,
                        forecastMode = forecast,
                        selectedMonth = uiState.selectedMonth,
                        customIncomeCategories = customIncome,
                        customExpenseCategories = customExpense,
                        hasOcrScan = hasOcrScan,
                        onRequestSuggestedInvoiceNumber = { year, onResult ->
                            viewModel.suggestNextInvoiceNumber(year, onResult)
                        },
                        onBack = { navController.popBackStack() },
                        onSaveIncome = { client, _, invNumber, amount, date, category, categoryLabel, markAsCollected, paymentMethod, onResult ->
                            viewModel.addIncomeTransaction(
                                client, amount, date, category, categoryLabel, markAsCollected, paymentMethod,
                                clientContactId = null,
                                invoiceNumber = invNumber,
                                onResult = onResult
                            )
                        },
                        onSaveExpense = { _, _, _, _, _, _, _, _, _, _, _, _, onResult -> onResult(null) }
                    )
                } else {
                    AccessDeniedScreen(
                        message = stringResource(R.string.admin_income_only),
                        onBack = { navController.popBackStack() }
                    )
                }
                TransactionType.EXPENSE -> if (hasPermission(role, permissions, UserPermission.MANAGE_EXPENSES)) {
                    NewTransactionScreen(
                        type = TransactionType.EXPENSE,
                        forecastMode = forecast,
                        selectedMonth = uiState.selectedMonth,
                        customIncomeCategories = customIncome,
                        customExpenseCategories = customExpense,
                        hasOcrScan = hasOcrScan,
                        onBack = { navController.popBackStack() },
                        onSaveIncome = { _, _, _, _, _, _, _, _, _, onResult -> onResult(null) },
                        onSaveExpense = { label, amount, date, category, categoryLabel, isRecurring, recurrence, recurrenceEndDate, isPaid, paymentMethod, note, receiptImagePath, onResult ->
                            viewModel.addExpenseTransaction(
                                label, amount, date, category, categoryLabel,
                                isRecurring, recurrence, recurrenceEndDate,
                                isPaid, paymentMethod, note = note,
                                receiptImagePath = receiptImagePath,
                                onResult = onResult
                            )
                        }
                    )
                } else {
                    AccessDeniedScreen(
                        message = stringResource(R.string.no_expense_permission),
                        onBack = { navController.popBackStack() }
                    )
                }
                null -> AccessDeniedScreen(
                    message = stringResource(R.string.invalid_transaction_type),
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.BankReconciliation.route) {
            val role = currentUserRole ?: uiState.currentUserRole
            val permissions = currentPermissions.ifEmpty { uiState.permissions }
            val canManageBank = role == UserRole.ADMIN ||
                hasPermission(role, permissions, UserPermission.MANAGE_EXPENSES)
            if (canManageBank) {
                BankReconciliationScreen(
                    entrepriseId = uiState.entrepriseId.orEmpty(),
                    userRole = role,
                    invoices = uiState.invoices,
                    expenses = uiState.expenses,
                    onBack = { navController.popBackStack() },
                    onReconcileTreasury = viewModel::reconcileTreasuryWithBank
                )
            } else {
                AccessDeniedScreen(
                    message = stringResource(R.string.no_bank_permission),
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.TreasuryCorrectionHistory.route) {
            val entrepriseId = uiState.entrepriseId.orEmpty()
            val corrections by viewModel
                .observeBalanceCorrections(entrepriseId)
                .collectAsStateWithLifecycle(initialValue = emptyList())
            TreasuryCorrectionHistoryScreen(
                corrections = corrections,
                formatAmount = { amount ->
                    val fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale.FRANCE)
                    fmt.minimumFractionDigits = 3
                    fmt.maximumFractionDigits = 3
                    fmt.format(amount)
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppScaffold(
    navController: NavHostController,
    viewModel: TreasuryViewModel,
    pilotageViewModel: PilotageViewModel,
    userRole: UserRole,
    permissions: Set<UserPermission>,
    appSettings: AppSettings,
    userPreferences: UserPreferences,
    googleBackupManager: GoogleBackupManager,
    googleAccountEmail: String?,
    startDestination: String,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val billingManager = remember { BillingManager.getInstance(context) }
    val billingPlan by billingManager.subscriptionPlan.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        billingManager.startConnection()
    }
    LaunchedEffect(billingPlan) {
        viewModel.syncSubscriptionPlan(billingPlan)
    }

    fun navigateToMainTab(route: String) {
        if (navController.currentDestination?.route != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val companyName = uiState.entreprise?.nom.orEmpty()
    val userName = uiState.users.find { it.id == uiState.currentUserId }?.nom.orEmpty()

    AppLockGate(appSettings = appSettings) {
        Scaffold(
            containerColor = Color.White,
            bottomBar = {
                TreasuryBottomNavigation(
                    navController = navController,
                    userRole = userRole,
                    permissions = permissions
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(paddingValues)
            ) {
                when (startDestination) {
                Screen.Dashboard.route -> {
                    val entrepriseId = uiState.entrepriseId.orEmpty()
                    val pilotState by pilotageViewModel.state.collectAsStateWithLifecycle()
                    val corrections by viewModel
                        .observeBalanceCorrections(entrepriseId)
                        .collectAsStateWithLifecycle(initialValue = emptyList())
                    DashboardYearScreen(
                        entrepriseId = entrepriseId,
                        accounts = uiState.bankAccounts,
                        invoices = uiState.invoices,
                        expenses = uiState.expenses,
                        pilotEntries = pilotState.entries,
                        corrections = corrections,
                        onOpenAccounts = {
                            navController.navigate(SettingsRoutes.OPTIONS_BANK)
                        },
                        onOpenPilotage = {
                            navigateToMainTab(Screen.Pilotage.route)
                        }
                    )
                }
                Screen.Subscription.route -> {
                    SubscriptionScreen(
                        currentPlan = uiState.subscription.plan,
                        onBack = { navigateToMainTab(Screen.Settings.route) },
                        onSelectPlan = { plan -> viewModel.syncSubscriptionPlan(plan) }
                    )
                }
                Screen.Treasury.route -> {
                    val entrepriseId = uiState.entrepriseId.orEmpty()
                    val pilotState by pilotageViewModel.state.collectAsStateWithLifecycle()
                    val corrections by viewModel
                        .observeBalanceCorrections(entrepriseId)
                        .collectAsStateWithLifecycle(initialValue = emptyList())
                    TreasuryMonthScreen(
                        entrepriseId = entrepriseId,
                        accounts = uiState.bankAccounts,
                        invoices = uiState.invoices,
                        expenses = uiState.expenses,
                        pilotEntries = pilotState.entries,
                        corrections = corrections,
                        onOpenPilot = { entryId ->
                            pilotageViewModel.openEntry(entryId)
                            navigateToMainTab(Screen.Pilotage.route)
                        }
                    )
                }
                Screen.Pilotage.route -> {
                    PilotageScreen(
                        viewModel = pilotageViewModel,
                        accounts = uiState.bankAccounts,
                        onOpenAccounts = {
                            navController.navigate(SettingsRoutes.OPTIONS_BANK)
                        }
                    )
                }
                Screen.Settings.route -> {
                    LaunchedEffect(
                        uiState.entrepriseId,
                        uiState.invoices.size,
                        uiState.expenses.size
                    ) {
                        viewModel.refreshSubscription()
                    }
                    SettingsScreen(
                        userFirstName = userName.ifBlank { companyName },
                        companyName = companyName,
                        subscription = uiState.subscription,
                        appSettings = appSettings,
                        googleBackupManager = googleBackupManager,
                        googleAccountEmail = googleAccountEmail,
                        onGoogleSignedIn = viewModel::onGoogleSignedIn,
                        onGoogleSignedOut = viewModel::onGoogleSignedOut,
                        onUpgradeSubscription = { navigateToMainTab(Screen.Subscription.route) },
                        onExportCsv = viewModel::buildCsvExport,
                        onDeleteAccount = viewModel::deleteAccountAndData,
                        onDeleteAllTransactions = viewModel::deleteAllTransactions,
                        onDeleteTransactionsForMonth = viewModel::deleteTransactionsForMonth,
                        onNavigate = { route -> navController.navigate(route) },
                        onAccountDeleted = onLogout
                    )
                }
            }
        }
        }
    }
}

@Composable
fun TreasuryBottomNavigation(
    navController: NavHostController,
    userRole: UserRole,
    permissions: Set<UserPermission>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val canViewTreasury = hasPermission(userRole, permissions, UserPermission.VIEW_TREASURY)
    val canViewInvoices = hasPermission(userRole, permissions, UserPermission.VIEW_INVOICES)
    val canManageExpenses = hasPermission(userRole, permissions, UserPermission.MANAGE_EXPENSES)
    val isAdmin = userRole == UserRole.ADMIN
    val canViewPilotage = isAdmin || canViewTreasury || canManageExpenses || canViewInvoices
    val mainTabs = buildList {
        add(Screen.Dashboard)
        if (canViewPilotage) {
            add(Screen.Pilotage)
        }
        if (canViewTreasury) {
            add(Screen.Treasury)
        }
    }
    val plusDestinations = setOf(
        Screen.Settings.route,
        Screen.Subscription.route,
        Screen.BankAccounts.route,
        Screen.BankReconciliation.route,
        Screen.TreasuryCorrectionHistory.route
    )
    val plusSelected = currentRoute in plusDestinations ||
        currentRoute?.startsWith("settings/") == true ||
        currentRoute?.startsWith("bank_account/") == true
    val itemCount = mainTabs.size + 1

    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
        unselectedIconColor = AppColors.TextSecondary,
        unselectedTextColor = AppColors.TextSecondary
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppColors.InfoCardBackground,
        shadowElevation = 10.dp,
        tonalElevation = 2.dp
    ) {
        Column {
            HorizontalDivider(
                color = AppColors.Border,
                thickness = 1.dp
            )
            NavigationBar(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                tonalElevation = 0.dp
            ) {
                mainTabs.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = stringResource(screen.titleRes)) },
                        label = {
                            NavBarLabel(screen.adaptiveNavLabel(itemCount))
                        },
                        selected = !plusSelected && currentRoute?.startsWith(screen.route.split("/")[0]) == true,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = navItemColors
                    )
                }
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.MoreHoriz,
                            contentDescription = stringResource(R.string.nav_plus)
                        )
                    },
                    label = { NavBarLabel(stringResource(R.string.nav_plus)) },
                    selected = plusSelected,
                    onClick = {
                        if (currentRoute != Screen.Settings.route) {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    colors = navItemColors
                )
            }
        }
    }
}
