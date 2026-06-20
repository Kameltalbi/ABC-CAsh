package com.abccash.app.treasury

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.abccash.app.treasury.remote.TreasurySyncScheduler
import com.abccash.app.treasury.remote.TreasurySyncService
import com.abccash.app.treasury.repository.TreasuryRepository
import com.abccash.app.treasury.datastore.AppSettings
import com.abccash.app.treasury.ui.*
import com.abccash.app.treasury.ui.settings.*
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import com.abccash.app.R
import com.abccash.app.treasury.viewmodel.InscriptionViewModelFactory
import com.abccash.app.treasury.viewmodel.TreasuryViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class Screen(val route: String, @StringRes val titleRes: Int, val icon: ImageVector) {
    object Splash : Screen("splash", R.string.loading, Icons.Default.HourglassEmpty)
    object OnboardingAdmin : Screen("onboarding_admin", R.string.settings, Icons.Default.AdminPanelSettings)
    object Login : Screen("login", R.string.login, Icons.Default.Login)
    object Inscription : Screen("inscription", R.string.create_account, Icons.Default.PersonAdd)
    object Dashboard : Screen("dashboard", R.string.nav_home, Icons.Default.SpaceDashboard)
    object Transactions : Screen("transactions", R.string.nav_transactions, Icons.Default.SwapVert)
    object Treasury : Screen("treasury", R.string.nav_treasury, Icons.Default.TrendingUp)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
    object PaymentEntry : Screen("payment/{invoiceId}", R.string.collections, Icons.Default.Payment)
    object ImportInvoices : Screen("import_invoices", R.string.add, Icons.Default.FileUpload)
    object AddTransaction : Screen("add_transaction/{type}", R.string.transactions, Icons.Default.Add)
    object BankReconciliation : Screen("bank_reconciliation", R.string.bank_account, Icons.Default.AccountBalance)
    object Previsions : Screen("previsions", R.string.nav_forecasts, Icons.Default.Event)
    object AddUser : Screen("add_user", R.string.settings_users, Icons.Default.PersonAdd)
    object Subscription : Screen("subscription", R.string.plan_free, Icons.Default.Payments)
}

private fun plusSectionSelected(currentRoute: String?): Boolean =
    currentRoute == Screen.Previsions.route ||
        currentRoute == Screen.Settings.route ||
        currentRoute == Screen.Subscription.route

@Composable
private fun Screen.adaptiveNavLabel(itemCount: Int): String {
    val slotWidth = LocalConfiguration.current.screenWidthDp / itemCount.coerceAtLeast(1)
    return when (this) {
        Screen.Dashboard -> when {
            slotWidth >= 95 -> stringResource(R.string.nav_home)
            else -> stringResource(R.string.nav_home_short)
        }
        Screen.Transactions -> when {
            slotWidth >= 95 -> stringResource(R.string.nav_transactions)
            else -> stringResource(R.string.nav_transactions_short)
        }
        Screen.Treasury -> when {
            slotWidth >= 95 -> stringResource(R.string.nav_treasury)
            else -> stringResource(R.string.nav_treasury_short)
        }
        Screen.Settings -> when {
            slotWidth >= 95 -> stringResource(R.string.nav_settings)
            else -> stringResource(R.string.nav_settings_short)
        }
        else -> stringResource(titleRes)
    }
}

@Composable
private fun plusNavLabel(itemCount: Int): String {
    val slotWidth = LocalConfiguration.current.screenWidthDp / itemCount.coerceAtLeast(1)
    return if (slotWidth >= 72) stringResource(R.string.nav_plus) else "+"
}

@Composable
fun TreasuryApp(
    repository: TreasuryRepository,
    viewModel: TreasuryViewModel,
    syncService: TreasurySyncService,
    userPreferences: UserPreferences
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
            }
            TreasurySyncScheduler.schedule(context)
            viewModel.syncSilently(immediate = true)
        }
    }

    fun logout() {
        coroutineScope.launch {
            TreasurySyncScheduler.cancel(context)
            userPreferences.clearUserSession()
            viewModel.clearSession()
            isAuthenticated = false
            currentUserRole = null
            currentPermissions = emptySet()
            navController.navigate(Screen.Login.route) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    AppCurrencyProvider(appSettings = appSettings) {
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner, isAuthenticated) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME && isAuthenticated) {
                    viewModel.syncSilently(immediate = true)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route
        ) {
        composable(Screen.Splash.route) {
            SplashDecisionScreen(
                repository = repository,
                userPreferences = userPreferences,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToInscription = {
                    navController.navigate(Screen.Inscription.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToMainApp = { userId, userRole, entrepriseId, permissions ->
                    isAuthenticated = true
                    currentUserRole = userRole
                    currentPermissions = permissions
                    viewModel.setSession(entrepriseId, userRole, permissions, userId)
                    TreasurySyncScheduler.schedule(context)
                    viewModel.syncSilently(immediate = true)
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onSessionInvalid = {
                    viewModel.clearSession()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.OnboardingAdmin.route) {
            OnboardingAdminScreen(
                onContinue = {
                    coroutineScope.launch {
                        userPreferences.setOnboardingAdminVu(true)
                        val entrepriseId = userPreferences.currentEntrepriseId.first().orEmpty()
                        val isAdmin = userPreferences.isAdmin.first()
                        val role = if (isAdmin) UserRole.ADMIN else UserRole.STAFF
                        val permissions = userPreferences.currentPermissions.first()
                            .let { effectivePermissions(role, it) }
                        isAuthenticated = true
                        currentUserRole = role
                        currentPermissions = permissions
                        viewModel.setSession(
                            entrepriseId,
                            role,
                            permissions,
                            userPreferences.currentUserId.first().orEmpty()
                        )
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.OnboardingAdmin.route) { inclusive = true }
                        }
                    }
                },
                onLogout = ::logout
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                repository = repository,
                syncService = syncService,
                onLoginSuccess = { user -> enterMainApp(user) }
            )
        }

        composable(Screen.Inscription.route) {
            var inscriptionAllowed by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(Unit) {
                inscriptionAllowed = !repository.hasAnyUser()
                if (inscriptionAllowed == false) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Inscription.route) { inclusive = true }
                    }
                }
            }
            if (inscriptionAllowed == true) {
                InscriptionScreen(
                    onBack = {
                        navController.navigate(Screen.Splash.route) {
                            popUpTo(Screen.Inscription.route) { inclusive = true }
                        }
                    },
                    onInscriptionSuccess = { user -> enterMainApp(user) },
                    viewModel = viewModel(factory = InscriptionViewModelFactory(repository, syncService))
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
                startDestination = Screen.Dashboard.route,
                onLogout = ::logout
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
                startDestination = Screen.Transactions.route,
                onLogout = ::logout
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
                startDestination = Screen.Treasury.route,
                onLogout = ::logout
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
                startDestination = Screen.Settings.route,
                onLogout = ::logout
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
                startDestination = Screen.Subscription.route,
                onLogout = ::logout
            )
        }

        composable(SettingsRoutes.USERS) {
            val role = currentUserRole ?: uiState.currentUserRole
            val permissions = currentPermissions.ifEmpty { uiState.permissions }
            SettingsUsersScreen(
                userRole = role,
                permissions = permissions,
                currentUserId = uiState.currentUserId,
                users = uiState.users,
                onBack = { navController.popBackStack() },
                onNavigateToAddUser = { navController.navigate(Screen.AddUser.route) },
                onDeleteUser = viewModel::deleteUser,
                onChangePassword = viewModel::changePassword,
                onResetPassword = viewModel::resetUserPassword,
                onExportBackup = viewModel::exportBackup,
                onRestoreBackup = viewModel::restoreBackup,
                backupFeedback = uiState.backupFeedback,
                onClearBackupFeedback = viewModel::clearBackupFeedback
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

        composable(SettingsRoutes.OPTIONS_SYNC) {
            SettingsSyncScreen(
                syncService = syncService,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PaymentEntry.route) { backStackEntry ->
            val invoiceId = backStackEntry.arguments?.getString("invoiceId")
            val invoice = invoiceId?.let { viewModel.getInvoice(it) }
            val role = currentUserRole ?: uiState.currentUserRole
            val permissions = currentPermissions.ifEmpty { uiState.permissions }
            val canPay = hasPermission(role, permissions, UserPermission.ADD_PAYMENTS)

            when {
                invoice != null && canPay -> {
                    PaymentEntryScreen(
                        invoice = invoice,
                        onBack = { navController.popBackStack() },
                        onSavePayment = { amount, date, method ->
                            val saved = viewModel.addPayment(invoice.id, amount, date, method)
                            if (saved) {
                                navController.popBackStack()
                            }
                            saved
                        }
                    )
                }
                else -> {
                    AccessDeniedScreen(
                        message = if (invoice == null) {
                            stringResource(R.string.invoice_not_found)
                        } else {
                            stringResource(R.string.no_payment_permission)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        composable(Screen.ImportInvoices.route) {
            val role = currentUserRole ?: uiState.currentUserRole
            if (role == UserRole.ADMIN) {
                InvoiceImportScreen(
                    onBack = { navController.popBackStack() },
                    onImportInvoices = { invoices ->
                        viewModel.importInvoices(invoices)
                        navController.popBackStack()
                    }
                )
            } else {
                AccessDeniedScreen(
                    message = stringResource(R.string.admin_import_only),
                    onBack = { navController.popBackStack() }
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
            when (type) {
                TransactionType.INCOME -> if (role == UserRole.ADMIN) {
                    NewTransactionScreen(
                        type = TransactionType.INCOME,
                        forecastMode = forecast,
                        selectedMonth = uiState.selectedMonth,
                        customIncomeCategories = customIncome,
                        customExpenseCategories = customExpense,
                        onBack = { navController.popBackStack() },
                        onSaveIncome = { client, amount, date, category, categoryLabel, markAsCollected, paymentMethod, onResult ->
                            viewModel.addIncomeTransaction(
                                client, amount, date, category, categoryLabel, markAsCollected, paymentMethod, onResult
                            )
                        },
                        onSaveExpense = { _, _, _, _, _, _, _, _, _, _, onResult -> onResult(null) }
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
                        onBack = { navController.popBackStack() },
                        onSaveIncome = { _, _, _, _, _, _, _, onResult -> onResult(null) },
                        onSaveExpense = { label, amount, date, category, categoryLabel, isRecurring, recurrence, recurrenceEndDate, isPaid, paymentMethod, onResult ->
                            viewModel.addExpenseTransaction(
                                label, amount, date, category, categoryLabel,
                                isRecurring, recurrence, recurrenceEndDate,
                                isPaid, paymentMethod, onResult
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
                startDestination = Screen.Previsions.route,
                onLogout = ::logout
            )
        }

        composable(Screen.AddUser.route) {
            val role = currentUserRole ?: uiState.currentUserRole
            val permissions = currentPermissions.ifEmpty { uiState.permissions }
            val canManage = hasPermission(role, permissions, UserPermission.MANAGE_USERS)

            if (canManage && role == UserRole.ADMIN) {
                NewUserScreen(
                    onBack = { navController.popBackStack() },
                    onSave = { name, email, phone, password, userRole, userPermissions, onResult ->
                        viewModel.addUser(name, email, phone, password, userRole, userPermissions, onResult)
                    }
                )
            } else {
                AccessDeniedScreen(
                    message = stringResource(R.string.admin_users_only),
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
    }
}

@Composable
private fun MainAppScaffold(
    navController: NavHostController,
    viewModel: TreasuryViewModel,
    userRole: UserRole,
    permissions: Set<UserPermission>,
    appSettings: AppSettings,
    userPreferences: UserPreferences,
    startDestination: String,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPlusMenu by remember { mutableStateOf(false) }

    val canViewTreasury = hasPermission(userRole, permissions, UserPermission.VIEW_TREASURY)
    val canViewInvoices = hasPermission(userRole, permissions, UserPermission.VIEW_INVOICES)
    val canManageExpenses = hasPermission(userRole, permissions, UserPermission.MANAGE_EXPENSES)
    val isAdmin = userRole == UserRole.ADMIN

    fun navigateToMainTab(route: String) {
        showPlusMenu = false
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

    val plusMenuItems = buildList {
        if (canViewTreasury || canViewInvoices || canManageExpenses || isAdmin) {
            add(
                PlusMenuEntry(
                    titleRes = R.string.plus_forecasts,
                    subtitleRes = R.string.plus_forecasts_sub,
                    icon = Icons.Default.Event,
                    onClick = { navigateToMainTab(Screen.Previsions.route) }
                )
            )
        }
        add(
            PlusMenuEntry(
                titleRes = R.string.plus_subscription,
                subtitleRes = R.string.plus_subscription_sub,
                icon = Icons.Default.Payments,
                onClick = { navigateToMainTab(Screen.Subscription.route) }
            )
        )
        add(
            PlusMenuEntry(
                titleRes = R.string.plus_bank_connection,
                subtitleRes = R.string.plus_bank_connection_sub,
                icon = Icons.Default.AccountBalance,
                onClick = { navigateToMainTab(Screen.Subscription.route) }
            )
        )
        add(
            PlusMenuEntry(
                titleRes = R.string.plus_settings,
                subtitleRes = R.string.plus_settings_sub_full,
                icon = Icons.Default.Settings,
                onClick = { navigateToMainTab(Screen.Settings.route) }
            )
        )
    }

    AppLockGate(appSettings = appSettings) {
        PlusMenuBottomSheet(
            visible = showPlusMenu,
            items = plusMenuItems,
            onDismiss = { showPlusMenu = false }
        )
        Scaffold(
            bottomBar = {
                TreasuryBottomNavigation(
                    navController = navController,
                    userRole = userRole,
                    permissions = permissions,
                    onPlusClick = { showPlusMenu = true }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (startDestination) {
                Screen.Dashboard.route -> {
                    val userName = uiState.users
                        .find { it.id == uiState.currentUserId }
                        ?.nom
                        .orEmpty()
                    ModernDashboardScreen(
                        userRole = userRole,
                        permissions = permissions,
                        userName = userName,
                        companyName = uiState.entreprise?.nom.orEmpty(),
                        invoices = uiState.invoices,
                        expenses = uiState.expenses,
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
                        }
                    )
                }
                Screen.Subscription.route -> {
                    SubscriptionScreen(
                        currentPlan = com.abccash.app.treasury.data.SubscriptionPlan.FREE,
                        onBack = { navigateToMainTab(Screen.Dashboard.route) },
                        onSelectPlan = {
                            // TODO: Implement subscription upgrade logic via BillingManager
                        }
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
                        importFeedback = uiState.importFeedback,
                        onClearImportFeedback = viewModel::clearImportFeedback,
                        onNavigateToImport = {
                            navController.navigate(Screen.ImportInvoices.route)
                        },
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
                        onDeleteInvoice = viewModel::deleteInvoice,
                        onUpdateExpense = { expenseId, label, amount, date, isRecurring, recurrence, recurrenceEndDate, isPaid ->
                            viewModel.updateExpense(expenseId, label, amount, date, isRecurring, recurrence, recurrenceEndDate, isPaid)
                        },
                        onStopRecurrence = viewModel::stopExpenseRecurrence,
                        onDeleteExpense = viewModel::deleteExpense,
                        onValidateExpense = viewModel::validateForecastExpense
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
                        }
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
                        onUpdateExpense = viewModel::updateExpense,
                        onValidateForecastExpense = viewModel::validateForecastExpense,
                        onDeleteExpense = viewModel::deleteExpense,
                        onNavigateToAddIncome = {
                            navController.navigate(
                                TransactionType.addRoute(TransactionType.INCOME, forecast = true)
                            )
                        },
                        onNavigateToAddExpense = {
                            navController.navigate(
                                TransactionType.addRoute(TransactionType.EXPENSE, forecast = true)
                            )
                        },
                        onForecastValidated = { month ->
                            viewModel.setSelectedMonth(month)
                            navController.navigate(Screen.Transactions.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                            }
                        }
                    )
                }
                Screen.Settings.route -> {
                    SettingsHubScreen(
                        userRole = userRole,
                        permissions = permissions,
                        onNavigate = { route -> navController.navigate(route) },
                        onLogout = onLogout
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
    permissions: Set<UserPermission>,
    onPlusClick: () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val canViewTreasury = hasPermission(userRole, permissions, UserPermission.VIEW_TREASURY)

    val mainTabs = buildList {
        add(Screen.Dashboard)
        if (hasPermission(userRole, permissions, UserPermission.VIEW_INVOICES) ||
            hasPermission(userRole, permissions, UserPermission.MANAGE_EXPENSES)
        ) {
            add(Screen.Transactions)
        }
        if (canViewTreasury) {
            add(Screen.Treasury)
        }
    }

    NavigationBar(
        containerColor = Color.White,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        val itemCount = mainTabs.size + 1
        mainTabs.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = stringResource(screen.titleRes)) },
                label = {
                    Text(
                        text = screen.adaptiveNavLabel(itemCount),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                selected = currentRoute?.startsWith(screen.route.split("/")[0]) == true,
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
            icon = { Icon(Icons.Default.MoreHoriz, contentDescription = stringResource(R.string.nav_plus)) },
            label = {
                Text(
                    text = plusNavLabel(itemCount),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall
                )
            },
            selected = plusSectionSelected(currentRoute),
            onClick = onPlusClick,
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
