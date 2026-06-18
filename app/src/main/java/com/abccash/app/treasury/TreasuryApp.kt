package com.abccash.app.treasury

import androidx.compose.foundation.layout.Box
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.abccash.app.treasury.data.TransactionType
import com.abccash.app.treasury.data.User
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import com.abccash.app.treasury.data.effectivePermissions
import com.abccash.app.treasury.data.hasPermission
import com.abccash.app.treasury.datastore.UserPreferences
import com.abccash.app.treasury.repository.TreasuryRepository
import com.abccash.app.treasury.datastore.AppSettings
import com.abccash.app.treasury.ui.*
import com.abccash.app.treasury.ui.settings.*
import com.abccash.app.treasury.viewmodel.InscriptionViewModelFactory
import com.abccash.app.treasury.viewmodel.TreasuryViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Splash : Screen("splash", "Chargement", Icons.Default.HourglassEmpty)
    object OnboardingAdmin : Screen("onboarding_admin", "Onboarding", Icons.Default.AdminPanelSettings)
    object Login : Screen("login", "Connexion", Icons.Default.Login)
    object Inscription : Screen("inscription", "Inscription", Icons.Default.PersonAdd)
    object Dashboard : Screen("dashboard", "Accueil", Icons.Default.SpaceDashboard)
    object Transactions : Screen("transactions", "Transactions", Icons.Default.SwapVert)
    object Treasury : Screen("treasury", "Trésorerie", Icons.Default.TrendingUp)
    object Settings : Screen("settings", "Paramètres", Icons.Default.Settings)
    object PaymentEntry : Screen("payment/{invoiceId}", "Paiement", Icons.Default.Payment)
    object ImportInvoices : Screen("import_invoices", "Import", Icons.Default.FileUpload)
    object AddTransaction : Screen("add_transaction/{type}", "Transaction", Icons.Default.Add)
    object BankReconciliation : Screen("bank_reconciliation", "Compte bancaire", Icons.Default.AccountBalance)
    object Previsions : Screen("previsions", "Prévisions", Icons.Default.Event)
    object AddUser : Screen("add_user", "Nouvel utilisateur", Icons.Default.PersonAdd)
}

private fun plusSectionSelected(currentRoute: String?): Boolean =
    currentRoute == Screen.Previsions.route ||
        currentRoute == Screen.Settings.route ||
        currentRoute?.startsWith("settings/") == true

@Composable
private fun Screen.adaptiveNavLabel(itemCount: Int): String {
    val slotWidth = LocalConfiguration.current.screenWidthDp / itemCount.coerceAtLeast(1)
    return when (this) {
        Screen.Dashboard -> when {
            slotWidth >= 95 -> title
            slotWidth >= 72 -> "Accueil"
            else -> "Acc."
        }
        Screen.Transactions -> when {
            slotWidth >= 95 -> title
            else -> "Trans."
        }
        Screen.Treasury -> when {
            slotWidth >= 95 -> title
            slotWidth >= 78 -> "Trésorerie"
            else -> "Trésor."
        }
        Screen.Settings -> when {
            slotWidth >= 95 -> title
            else -> "Param."
        }
        else -> title
    }
}

@Composable
private fun plusNavLabel(itemCount: Int): String {
    val slotWidth = LocalConfiguration.current.screenWidthDp / itemCount.coerceAtLeast(1)
    return if (slotWidth >= 72) "Plus" else "+"
}

@Composable
fun TreasuryApp(
    repository: TreasuryRepository,
    viewModel: TreasuryViewModel
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
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
        }
    }

    fun logout() {
        coroutineScope.launch {
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
            val currentUser = uiState.users.find { it.id == uiState.currentUserId }
            SettingsUserProfileScreen(
                currentUser = currentUser,
                onBack = { navController.popBackStack() },
                onSave = { nom, email, telephone, onResult ->
                    val userId = uiState.currentUserId
                    if (userId == null) {
                        onResult("Session expirée")
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
                            "Facture introuvable"
                        } else {
                            "Vous n'avez pas la permission d'enregistrer un paiement"
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
                    message = "Seul l'administrateur peut importer des encaissements",
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.AddTransaction.route) { backStackEntry ->
            val type = TransactionType.fromRoute(backStackEntry.arguments?.getString("type"))
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
                        selectedMonth = uiState.selectedMonth,
                        customIncomeCategories = customIncome,
                        customExpenseCategories = customExpense,
                        onBack = { navController.popBackStack() },
                        onSaveIncome = { client, amount, date, category, categoryLabel, markAsCollected, onResult ->
                            viewModel.addIncomeTransaction(
                                client, amount, date, category, categoryLabel, markAsCollected, onResult
                            )
                        },
                        onSaveExpense = { _, _, _, _, _, onResult -> onResult(null) }
                    )
                } else {
                    AccessDeniedScreen(
                        message = "Seul l'administrateur peut créer un encaissement",
                        onBack = { navController.popBackStack() }
                    )
                }
                TransactionType.EXPENSE -> if (hasPermission(role, permissions, UserPermission.MANAGE_EXPENSES)) {
                    NewTransactionScreen(
                        type = TransactionType.EXPENSE,
                        selectedMonth = uiState.selectedMonth,
                        customIncomeCategories = customIncome,
                        customExpenseCategories = customExpense,
                        onBack = { navController.popBackStack() },
                        onSaveIncome = { _, _, _, _, _, _, onResult -> onResult(null) },
                        onSaveExpense = { label, amount, date, category, categoryLabel, onResult ->
                            viewModel.addExpenseTransaction(
                                label, amount, date, category, categoryLabel, onResult
                            )
                        }
                    )
                } else {
                    AccessDeniedScreen(
                        message = "Vous n'avez pas la permission de gérer les dépenses",
                        onBack = { navController.popBackStack() }
                    )
                }
                null -> AccessDeniedScreen(
                    message = "Type de transaction invalide",
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
                    message = "Vous n'avez pas la permission de modifier le solde bancaire",
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
                    message = "Seul l'administrateur peut créer des utilisateurs",
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

    AppLockGate(appSettings = appSettings) {
        Scaffold(
            bottomBar = {
                TreasuryBottomNavigation(
                    navController = navController,
                    userRole = userRole,
                    permissions = permissions
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (startDestination) {
                Screen.Dashboard.route -> {
                    val userName = uiState.users
                        .find { it.id == uiState.currentUserId }
                        ?.nom
                        .orEmpty()
                    InnovativeDashboardScreen(
                        userRole = userRole,
                        permissions = permissions,
                        userName = userName,
                        companyName = uiState.entreprise?.nom.orEmpty(),
                        invoices = uiState.invoices,
                        expenses = uiState.expenses,
                        entrepriseId = uiState.entrepriseId,
                        userPreferences = userPreferences,
                        onNavigateToAddIncome = {
                            navController.navigate("add_transaction/${TransactionType.INCOME.route}")
                        },
                        onNavigateToAddExpense = {
                            navController.navigate("add_transaction/${TransactionType.EXPENSE.route}")
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
                            navController.navigate("add_transaction/${TransactionType.INCOME.route}")
                        },
                        onNavigateToAddExpense = {
                            navController.navigate("add_transaction/${TransactionType.EXPENSE.route}")
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
                        onDeleteExpense = viewModel::deleteExpense
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
                        onUpdateInvoice = viewModel::updateInvoice,
                        onRecordPayment = viewModel::recordPayment,
                        onDeleteInvoice = viewModel::deleteInvoice,
                        onUpdateExpense = viewModel::updateExpense,
                        onDeleteExpense = viewModel::deleteExpense
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
    permissions: Set<UserPermission>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var showPlusMenu by remember { mutableStateOf(false) }
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

    PlusMenuBottomSheet(
        visible = showPlusMenu,
        showPrevisions = canViewTreasury,
        onDismiss = { showPlusMenu = false },
        onPrevisions = {
            showPlusMenu = false
            navController.navigate(Screen.Previsions.route) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        onSettings = {
            showPlusMenu = false
            navController.navigate(Screen.Settings.route) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    )

    NavigationBar(
        containerColor = Color.White,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        val itemCount = mainTabs.size + 1
        mainTabs.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
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
            icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "Plus") },
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
            onClick = {
                if (plusSectionSelected(currentRoute)) {
                    showPlusMenu = true
                } else {
                    val destination = when {
                        canViewTreasury -> Screen.Previsions.route
                        else -> Screen.Settings.route
                    }
                    navController.navigate(destination) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
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
}
