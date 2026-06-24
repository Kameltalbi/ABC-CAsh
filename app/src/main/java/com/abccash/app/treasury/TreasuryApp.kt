package com.abccash.app.treasury

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.abccash.app.treasury.viewmodel.InscriptionViewModelFactory
import com.abccash.app.treasury.viewmodel.LoginViewModelFactory
import com.abccash.app.treasury.viewmodel.TreasuryViewModel
import kotlinx.coroutines.launch
import java.time.YearMonth

sealed class Screen(val route: String, @StringRes val titleRes: Int, val icon: ImageVector) {
    object Splash : Screen("splash", R.string.loading, Icons.Default.HourglassEmpty)
    object Onboarding : Screen("onboarding", R.string.app_name, Icons.Default.Info)
    object Login : Screen("login", R.string.login, Icons.Default.Login)
    object AccountSetup : Screen("account_setup", R.string.account_setup_title, Icons.Default.PersonAdd)
    object Inscription : Screen("inscription", R.string.create_account, Icons.Default.PersonAdd)
    object Dashboard : Screen("dashboard", R.string.nav_home, Icons.Default.SpaceDashboard)
    object Transactions : Screen("transactions", R.string.nav_transactions, Icons.Default.SwapVert)
    object Treasury : Screen("treasury", R.string.nav_treasury, Icons.Default.TrendingUp)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
    object AddTransaction : Screen("add_transaction/{type}", R.string.transactions, Icons.Default.Add)
    object BankReconciliation : Screen("bank_reconciliation", R.string.bank_account, Icons.Default.AccountBalance)
    object BankAccounts : Screen("bank_accounts", R.string.bank_accounts_title, Icons.Default.AccountBalance)
    object BankAccountDetail : Screen("bank_account/{accountId}", R.string.bank_account_detail, Icons.Default.AccountBalance)
    object Previsions : Screen("previsions", R.string.nav_forecasts, Icons.Default.Event)
    object Subscription : Screen("subscription", R.string.plan_free, Icons.Default.Payments)
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
        Screen.Transactions -> if (useShort) {
            stringResource(R.string.nav_transactions_short)
        } else {
            stringResource(R.string.nav_transactions)
        }
        Screen.Treasury -> if (useShort) {
            stringResource(R.string.nav_treasury_short)
        } else {
            stringResource(R.string.nav_treasury)
        }
        Screen.Previsions -> if (useShort) {
            stringResource(R.string.nav_forecasts_short)
        } else {
            stringResource(R.string.nav_forecasts)
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
    viewModel: TreasuryViewModel,
    userPreferences: UserPreferences,
    googleBackupManager: GoogleBackupManager
) {
    val navController = rememberNavController()
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

        composable(Screen.Transactions.route) {
            MainAppScaffold(
                navController = navController,
                viewModel = viewModel,
                userRole = currentUserRole ?: uiState.currentUserRole,
                permissions = currentPermissions.ifEmpty { uiState.permissions },
                appSettings = appSettings,
                userPreferences = userPreferences,
                googleBackupManager = googleBackupManager,
                googleAccountEmail = googleAccountEmail,
                startDestination = Screen.Transactions.route,
                onLogout = { logout() }
            )
        }

        composable(Screen.Treasury.route) {
            MainAppScaffold(
                navController = navController,
                viewModel = viewModel,
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

        composable(Screen.Settings.route) {
            MainAppScaffold(
                navController = navController,
                viewModel = viewModel,
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
                            saveError = when (error) {
                                TreasuryRepository.ACCOUNT_LIMIT_REACHED -> accountLimitError
                                else -> error
                            }
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
                        viewModel.deleteBankAccount(accountId) {
                            navController.popBackStack()
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
                                onResult = { error ->
                                    if (error == null) {
                                        viewModel.setSelectedMonth(YearMonth.from(date))
                                    }
                                    onResult(error)
                                }
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
                                onResult = { error ->
                                    if (error == null) {
                                        viewModel.setSelectedMonth(YearMonth.from(date))
                                    }
                                    onResult(error)
                                }
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

        composable(Screen.Previsions.route) {
            MainAppScaffold(
                navController = navController,
                viewModel = viewModel,
                userRole = currentUserRole ?: uiState.currentUserRole,
                permissions = currentPermissions.ifEmpty { uiState.permissions },
                appSettings = appSettings,
                userPreferences = userPreferences,
                googleBackupManager = googleBackupManager,
                googleAccountEmail = googleAccountEmail,
                startDestination = Screen.Previsions.route,
                onLogout = { logout() }
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val billingManager = remember { BillingManager.getInstance(context) }
    val billingPlan by billingManager.subscriptionPlan.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        billingManager.startConnection()
    }
    LaunchedEffect(billingPlan) {
        viewModel.syncSubscriptionPlan(billingPlan)
    }

    val canViewTreasury = hasPermission(userRole, permissions, UserPermission.VIEW_TREASURY)
    val canViewInvoices = hasPermission(userRole, permissions, UserPermission.VIEW_INVOICES)
    val canManageExpenses = hasPermission(userRole, permissions, UserPermission.MANAGE_EXPENSES)
    val isAdmin = userRole == UserRole.ADMIN

    fun closeDrawer() {
        scope.launch { drawerState.close() }
    }

    fun openDrawer() {
        scope.launch { drawerState.open() }
    }

    fun navigateToMainTab(route: String) {
        closeDrawer()
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

    val drawerItems = buildList {
        add(
            DrawerMenuEntry(
                titleRes = R.string.plus_subscription,
                subtitleRes = R.string.plus_subscription_sub,
                icon = Icons.Default.Payments,
                onClick = { navigateToMainTab(Screen.Subscription.route) }
            )
        )
        add(
            DrawerMenuEntry(
                titleRes = R.string.plus_settings,
                subtitleRes = R.string.plus_settings_sub_full,
                icon = Icons.Default.Settings,
                onClick = { navigateToMainTab(Screen.Settings.route) }
            )
        )
    }

    val companyName = uiState.entreprise?.nom.orEmpty()
    val userName = uiState.users.find { it.id == uiState.currentUserId }?.nom.orEmpty()

    AppLockGate(appSettings = appSettings) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                TreasuryNavigationDrawerContent(
                    companyName = companyName,
                    userName = userName,
                    items = drawerItems,
                    onClose = { closeDrawer() }
                )
            }
        ) {
        Scaffold(
            containerColor = Color.White,
            bottomBar = {
                TreasuryBottomNavigation(
                    navController = navController,
                    userRole = userRole,
                    permissions = permissions,
                    isMenuOpen = drawerState.isOpen,
                    onOpenMenu = {
                        if (drawerState.isOpen) closeDrawer() else openDrawer()
                    }
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
                    val userName = uiState.users
                        .find { it.id == uiState.currentUserId }
                        ?.nom
                        .orEmpty()
                    if (uiState.subscription.plan.hasDashboardAccess) {
                        ModernDashboardScreen(
                            userRole = userRole,
                            permissions = permissions,
                            userName = userName,
                            companyName = uiState.entreprise?.nom.orEmpty(),
                            invoices = uiState.invoices,
                            expenses = uiState.expenses,
                            bankAccounts = uiState.bankAccounts,
                            entrepriseId = uiState.entrepriseId,
                            userPreferences = userPreferences,
                            onNavigateToAddIncome = {
                                navController.navigate(TransactionType.addRoute(TransactionType.INCOME))
                            },
                            onNavigateToAddExpense = {
                                navController.navigate(TransactionType.addRoute(TransactionType.EXPENSE))
                            },
                            onNavigateToSubscription = {
                                navigateToMainTab(Screen.Subscription.route)
                            },
                            onNavigateToBankAccounts = {
                                navController.navigate(SettingsRoutes.OPTIONS_BANK)
                            },
                            onOpenDrawer = { openDrawer() }
                        )
                    } else {
                        SubscriptionFeatureGateScreen(
                            titleRes = R.string.nav_home,
                            messageRes = R.string.subscription_dashboard_gate_message,
                            onUpgrade = { navigateToMainTab(Screen.Subscription.route) },
                            onOpenDrawer = { openDrawer() }
                        )
                    }
                }
                Screen.Subscription.route -> {
                    SubscriptionScreen(
                        currentPlan = uiState.subscription.plan,
                        onBack = { navigateToMainTab(Screen.Dashboard.route) },
                        onSelectPlan = { plan -> viewModel.syncSubscriptionPlan(plan) }
                    )
                }
                Screen.Transactions.route -> {
                    TransactionsScreen(
                        userRole = userRole,
                        permissions = permissions,
                        invoices = uiState.invoices,
                        expenses = uiState.expenses,
                        selectedMonth = uiState.selectedMonth,
                        onMonthChange = viewModel::setSelectedMonth,
                        onNavigateToAddIncome = {
                            navController.navigate(TransactionType.addRoute(TransactionType.INCOME))
                        },
                        onNavigateToAddExpense = {
                            navController.navigate(TransactionType.addRoute(TransactionType.EXPENSE))
                        },
                        onUpdateInvoice = { invoiceId, invoiceNumber, clientName, totalAmount, dueDate, onResult ->
                            viewModel.updateInvoice(invoiceId, invoiceNumber, clientName, totalAmount, dueDate, onResult)
                        },
                        onRecordPayment = { invoiceId, amount, date, method, onResult ->
                            viewModel.recordPayment(invoiceId, amount, date, method, onResult)
                        },
                        onDeleteInvoice = { id, onResult -> viewModel.deleteInvoice(id, onResult) },
                        onDeleteInvoices = viewModel::deleteInvoices,
                        onUpdateExpense = { expenseId, label, amount, date, isRecurring, recurrence, recurrenceEndDate, isPaid, paymentMethod ->
                            viewModel.updateExpense(expenseId, label, amount, date, isRecurring, recurrence, recurrenceEndDate, isPaid, paymentMethod)
                        },
                        onStopRecurrence = viewModel::stopExpenseRecurrence,
                        onDeleteExpense = viewModel::deleteExpense,
                        onDeleteExpenses = viewModel::deleteExpenses,
                        onValidateExpense = viewModel::validateForecastExpense,
                        onOpenDrawer = { openDrawer() }
                    )
                }
                Screen.Treasury.route -> {
                    TreasuryBalanceScreen(
                        userRole = userRole,
                        permissions = permissions,
                        invoices = uiState.invoices,
                        expenses = uiState.expenses,
                        onExportCsv = viewModel::buildCsvExport,
                        onNavigateToBankReconciliation = {
                            navController.navigate(Screen.BankReconciliation.route)
                        },
                        onOpenDrawer = { openDrawer() }
                    )
                }
                Screen.Previsions.route -> {
                    PrevisionsScreen(
                        userRole = userRole,
                        permissions = permissions,
                        invoices = uiState.invoices,
                        expenses = uiState.expenses,
                        selectedMonth = uiState.selectedMonth,
                        onMonthChange = viewModel::setSelectedMonth,
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route)
                        },
                        onUpdateInvoice = viewModel::updateInvoice,
                        onRecordPayment = viewModel::recordPayment,
                        onDeleteInvoice = viewModel::deleteInvoice,
                        onUpdateExpense = { expenseId, label, amount, date, isRecurring, recurrence, recurrenceEndDate, isPaid, paymentMethod ->
                            viewModel.updateExpense(expenseId, label, amount, date, isRecurring, recurrence, recurrenceEndDate, isPaid, paymentMethod)
                        },
                        onValidateForecastExpense = viewModel::validateForecastExpense,
                        onDeleteExpense = viewModel::deleteExpense,
                        onNavigateToAddIncome = {
                            navController.navigate(TransactionType.addRoute(TransactionType.INCOME, forecast = true))
                        },
                        onNavigateToAddExpense = {
                            navController.navigate(TransactionType.addRoute(TransactionType.EXPENSE, forecast = true))
                        },
                        onForecastValidated = { month ->
                            viewModel.setSelectedMonth(month)
                            navController.navigate(Screen.Transactions.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                            }
                        },
                        onOpenDrawer = { openDrawer() }
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
                    val displayFirstName = userName.trim().substringBefore(" ").ifBlank { userName }
                    SettingsScreen(
                        userFirstName = displayFirstName,
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
                        onNavigate = { route -> navController.navigate(route) },
                        onOpenDrawer = { openDrawer() },
                        onAccountDeleted = onLogout
                    )
                }
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
    permissions: Set<UserPermission>,
    isMenuOpen: Boolean,
    onOpenMenu: () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val canViewTreasury = hasPermission(userRole, permissions, UserPermission.VIEW_TREASURY)
    val canViewInvoices = hasPermission(userRole, permissions, UserPermission.VIEW_INVOICES)
    val canManageExpenses = hasPermission(userRole, permissions, UserPermission.MANAGE_EXPENSES)
    val isAdmin = userRole == UserRole.ADMIN
    val canViewPrevisions = canViewTreasury || canViewInvoices || canManageExpenses || isAdmin

    val mainTabs = buildList {
        add(Screen.Dashboard)
        if (canViewInvoices || canManageExpenses) {
            add(Screen.Transactions)
        }
        if (canViewPrevisions) {
            add(Screen.Previsions)
        }
        if (canViewTreasury) {
            add(Screen.Treasury)
        }
    }

    val itemCount = mainTabs.size + 1

    NavigationBar(
        containerColor = Color.White,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        mainTabs.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = stringResource(screen.titleRes)) },
                label = {
                    NavBarLabel(screen.adaptiveNavLabel(itemCount))
                },
                selected = !isMenuOpen && currentRoute?.startsWith(screen.route.split("/")[0]) == true,
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
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = stringResource(R.string.nav_menu)
                )
            },
            label = {
                NavBarLabel(stringResource(R.string.nav_menu))
            },
            selected = isMenuOpen,
            onClick = onOpenMenu,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
    }
}
