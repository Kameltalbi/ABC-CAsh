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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.abccash.app.treasury.data.User
import com.abccash.app.treasury.data.UserPermission
import com.abccash.app.treasury.data.UserRole
import com.abccash.app.treasury.data.effectivePermissions
import com.abccash.app.treasury.data.hasPermission
import com.abccash.app.treasury.datastore.UserPreferences
import com.abccash.app.treasury.repository.TreasuryRepository
import com.abccash.app.treasury.ui.*
import com.abccash.app.treasury.viewmodel.InscriptionViewModelFactory
import com.abccash.app.treasury.viewmodel.TreasuryViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Splash : Screen("splash", "Chargement", Icons.Default.HourglassEmpty)
    object OnboardingAdmin : Screen("onboarding_admin", "Onboarding", Icons.Default.AdminPanelSettings)
    object Login : Screen("login", "Connexion", Icons.Default.Login)
    object Inscription : Screen("inscription", "Inscription", Icons.Default.PersonAdd)
    object Invoices : Screen("invoices", "Encaissements", Icons.AutoMirrored.Filled.ReceiptLong)
    object Expenses : Screen("expenses", "Dépenses", Icons.Default.AccountBalance)
    object Treasury : Screen("treasury", "Trésorerie", Icons.Default.TrendingUp)
    object AdminUsers : Screen("admin_users", "Admin", Icons.Default.SupervisorAccount)
    object PaymentEntry : Screen("payment/{invoiceId}", "Paiement", Icons.Default.Payment)
    object ImportInvoices : Screen("import_invoices", "Import", Icons.Default.FileUpload)
    object AddExpense : Screen("add_expense", "Nouvelle dépense", Icons.Default.Add)
    object AddUser : Screen("add_user", "Nouvel utilisateur", Icons.Default.PersonAdd)
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
            navController.navigate(Screen.Invoices.route) {
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
                    navController.navigate(Screen.Invoices.route) {
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
                        navController.navigate(Screen.Invoices.route) {
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

        composable(Screen.Invoices.route) {
            MainAppScaffold(
                navController = navController,
                viewModel = viewModel,
                userRole = currentUserRole ?: uiState.currentUserRole,
                permissions = currentPermissions.ifEmpty { uiState.permissions },
                startDestination = Screen.Invoices.route,
                onLogout = ::logout
            )
        }

        composable(Screen.Expenses.route) {
            MainAppScaffold(
                navController = navController,
                viewModel = viewModel,
                userRole = currentUserRole ?: uiState.currentUserRole,
                permissions = currentPermissions.ifEmpty { uiState.permissions },
                startDestination = Screen.Expenses.route,
                onLogout = ::logout
            )
        }

        composable(Screen.Treasury.route) {
            MainAppScaffold(
                navController = navController,
                viewModel = viewModel,
                userRole = currentUserRole ?: uiState.currentUserRole,
                permissions = currentPermissions.ifEmpty { uiState.permissions },
                startDestination = Screen.Treasury.route,
                onLogout = ::logout
            )
        }

        composable(Screen.AdminUsers.route) {
            MainAppScaffold(
                navController = navController,
                viewModel = viewModel,
                userRole = currentUserRole ?: uiState.currentUserRole,
                permissions = currentPermissions.ifEmpty { uiState.permissions },
                startDestination = Screen.AdminUsers.route,
                onLogout = ::logout
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

        composable(Screen.AddExpense.route) {
            val role = currentUserRole ?: uiState.currentUserRole
            val permissions = currentPermissions.ifEmpty { uiState.permissions }
            val canManage = hasPermission(role, permissions, UserPermission.MANAGE_EXPENSES)

            if (canManage) {
                NewExpenseScreen(
                    selectedMonth = uiState.selectedMonth,
                    onBack = { navController.popBackStack() },
                    onSave = { label, amount, date, isRecurring, recurrence, recurrenceEndDate, isPaid ->
                        viewModel.addExpense(label, amount, date, isRecurring, recurrence, recurrenceEndDate, isPaid)
                        navController.popBackStack()
                    }
                )
            } else {
                AccessDeniedScreen(
                    message = "Vous n'avez pas la permission de gérer les dépenses",
                    onBack = { navController.popBackStack() }
                )
            }
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

@Composable
private fun MainAppScaffold(
    navController: NavHostController,
    viewModel: TreasuryViewModel,
    userRole: UserRole,
    permissions: Set<UserPermission>,
    startDestination: String,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                Screen.Invoices.route -> {
                    InvoicesListScreen(
                        userRole = userRole,
                        permissions = permissions,
                        invoices = uiState.invoices,
                        selectedMonth = uiState.selectedMonth,
                        onMonthChange = viewModel::setSelectedMonth,
                        importFeedback = uiState.importFeedback,
                        onClearImportFeedback = viewModel::clearImportFeedback,
                        onInvoiceClick = { invoiceId ->
                            navController.navigate("payment/$invoiceId")
                        },
                        onNavigateToImport = {
                            navController.navigate(Screen.ImportInvoices.route)
                        },
                        onAddInvoice = { invoiceNumber, clientName, totalAmount, dueDate, onResult ->
                            viewModel.addInvoice(invoiceNumber, clientName, totalAmount, dueDate, onResult)
                        },
                        onUpdateInvoice = { invoiceId, invoiceNumber, clientName, totalAmount, dueDate, onResult ->
                            viewModel.updateInvoice(invoiceId, invoiceNumber, clientName, totalAmount, dueDate, onResult)
                        },
                        onDeleteInvoice = viewModel::deleteInvoice,
                        onDeleteInvoices = viewModel::deleteInvoices
                    )
                }
                Screen.Expenses.route -> {
                    ExpensesManagementScreen(
                        userRole = userRole,
                        permissions = permissions,
                        expenses = uiState.expenses,
                        selectedMonth = uiState.selectedMonth,
                        onMonthChange = viewModel::setSelectedMonth,
                        onNavigateToAddExpense = {
                            navController.navigate(Screen.AddExpense.route)
                        },
                        onUpdateExpense = { expenseId, label, amount, date, isRecurring, recurrence, recurrenceEndDate, isPaid ->
                            viewModel.updateExpense(expenseId, label, amount, date, isRecurring, recurrence, recurrenceEndDate, isPaid)
                        },
                        onStopRecurrence = { expenseId, endDate ->
                            viewModel.stopExpenseRecurrence(expenseId, endDate)
                        },
                        onDeleteExpense = { expenseId ->
                            viewModel.deleteExpense(expenseId)
                        },
                        onDeleteExpenses = viewModel::deleteExpenses
                    )
                }
                Screen.Treasury.route -> {
                    TreasuryBalanceScreen(
                        userRole = userRole,
                        permissions = permissions,
                        selectedMonth = uiState.selectedMonth,
                        totalCollected = viewModel.getMonthlyCollections(uiState.selectedMonth),
                        totalExpenses = viewModel.getMonthlyExpenses(uiState.selectedMonth),
                        forecastedBalance = viewModel.getForecastedBalance(uiState.selectedMonth),
                        invoices = uiState.invoices,
                        expenses = uiState.expenses,
                        onMonthChange = { yearMonth ->
                            viewModel.setSelectedMonth(yearMonth)
                        },
                        onExportCsv = viewModel::buildCsvExport
                    )
                }
                Screen.AdminUsers.route -> {
                    AdminUsersScreen(
                        userRole = userRole,
                        permissions = permissions,
                        currentUserId = uiState.currentUserId,
                        users = uiState.users,
                        onNavigateToAddUser = {
                            navController.navigate(Screen.AddUser.route)
                        },
                        onDeleteUser = viewModel::deleteUser,
                        onChangePassword = viewModel::changePassword,
                        onResetPassword = viewModel::resetUserPassword,
                        onExportBackup = viewModel::exportBackup,
                        onRestoreBackup = viewModel::restoreBackup,
                        backupFeedback = uiState.backupFeedback,
                        onClearBackupFeedback = viewModel::clearBackupFeedback,
                        onLogout = onLogout
                    )
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

    val items = buildList {
        if (hasPermission(userRole, permissions, UserPermission.VIEW_INVOICES)) {
            add(Screen.Invoices)
        }
        if (hasPermission(userRole, permissions, UserPermission.MANAGE_EXPENSES)) {
            add(Screen.Expenses)
        }
        if (hasPermission(userRole, permissions, UserPermission.VIEW_TREASURY)) {
            add(Screen.Treasury)
        }
        if (hasPermission(userRole, permissions, UserPermission.MANAGE_USERS)) {
            add(Screen.AdminUsers)
        }
    }

    NavigationBar(
        containerColor = Color.White,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
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
    }
}
