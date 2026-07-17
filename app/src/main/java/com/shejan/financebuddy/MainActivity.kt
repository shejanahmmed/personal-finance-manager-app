package com.shejan.financebuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import com.shejan.financebuddy.R
import com.shejan.financebuddy.data.PreferencesManager
import com.shejan.financebuddy.data.db.FinanceDatabase
import com.shejan.financebuddy.data.db.AccountEntity
import com.shejan.financebuddy.data.db.TransactionEntity
import com.shejan.financebuddy.data.db.BudgetEntity
import com.shejan.financebuddy.ui.budget.BudgetScreen
import com.shejan.financebuddy.ui.goals.GoalsScreen
import com.shejan.financebuddy.ui.home.HomeScreen
import com.shejan.financebuddy.ui.home.TransactionListScreen
import com.shejan.financebuddy.ui.onboarding.OnboardingScreenRoot
import com.shejan.financebuddy.ui.pending.PendingTransactionsScreen
import com.shejan.financebuddy.ui.pending.PendingTransactionsViewModel
import com.shejan.financebuddy.ui.theme.AccentBlue
import com.shejan.financebuddy.ui.theme.AccentTeal
import com.shejan.financebuddy.ui.theme.BackgroundDark
import com.shejan.financebuddy.ui.theme.CardDark
import com.shejan.financebuddy.ui.theme.CardDarker
import com.shejan.financebuddy.ui.theme.DividerColor
import com.shejan.financebuddy.ui.theme.ExpenseRed
import com.shejan.financebuddy.ui.theme.FinanceBuddyTheme
import com.shejan.financebuddy.ui.theme.SurfaceDark
import com.shejan.financebuddy.ui.theme.TextMuted
import com.shejan.financebuddy.ui.theme.TextPrimary
import com.shejan.financebuddy.ui.theme.TextSecondary
import com.shejan.financebuddy.ui.settings.SettingsScreen
import com.shejan.financebuddy.ui.accounts.BankAccountsScreen
import com.shejan.financebuddy.ui.payees.PayeesScreen
import com.shejan.financebuddy.ui.payees.PayeeDetailScreen
import com.shejan.financebuddy.data.db.PayeeEntity
import com.shejan.financebuddy.data.db.PayeeAccountEntity
import com.shejan.financebuddy.sms.SmsPermissionHandler
import com.shejan.financebuddy.ui.profile.EditProfileDialog
import com.shejan.financebuddy.data.db.LoanEntity
import com.shejan.financebuddy.ui.loans.LoansScreen
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.filled.Person
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : ComponentActivity() {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var database: FinanceDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferencesManager = PreferencesManager(applicationContext)
        database = FinanceDatabase.getDatabase(applicationContext)

        setContent {
            val themeMode by preferencesManager.themeMode.collectAsState(initial = "SYSTEM")
            val darkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }
            FinanceBuddyTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        preferencesManager = preferencesManager,
                        database           = database
                    )
                }
            }
        }
    }
}

// ─── App Navigation Host ──────────────────────────────────────

@Composable
fun AppNavigation(
    preferencesManager: PreferencesManager,
    database: FinanceDatabase
) {
    val onboardingCompleted by preferencesManager.isOnboardingCompleted.collectAsState(initial = null)

    // Lift database flows to parent level to prevent reload screen flashing on transitions
    val accounts by database.accountDao().getAllAccounts().collectAsState(initial = emptyList())
    val allTransactions by database.transactionDao().getAllTransactions().collectAsState(initial = emptyList())
    val payees by database.payeeDao().getAllPayees().collectAsState(initial = emptyList())
    val payeeAccounts by database.payeeDao().getAllPayeeAccounts().collectAsState(initial = emptyList())

    if (onboardingCompleted == null) {
        // Render a dark screen matching the splash screen while loading preference state
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
        )
    } else {
        val navController = rememberNavController()
        val startDestination = if (onboardingCompleted == true) "main_dashboard" else "onboarding"

        NavHost(
            navController    = navController,
            startDestination = startDestination,
            modifier         = Modifier.fillMaxSize().background(BackgroundDark),
            enterTransition = { fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220), initialOffsetX = { it }) },
            exitTransition = { fadeOut(animationSpec = tween(220)) + slideOutHorizontally(animationSpec = tween(220), targetOffsetX = { -it }) },
            popEnterTransition = { fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220), initialOffsetX = { -it }) },
            popExitTransition = { fadeOut(animationSpec = tween(220)) + slideOutHorizontally(animationSpec = tween(220), targetOffsetX = { it }) }
        ) {
            composable("onboarding") {
                OnboardingScreenRoot(
                    onFinished = {
                        CoroutineScope(Dispatchers.IO).launch {
                            preferencesManager.setOnboardingCompleted()
                        }
                        navController.navigate("main_dashboard") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }

            composable("main_dashboard") {
                MainDashboardContainer(
                    preferencesManager = preferencesManager,
                    database      = database,
                    accounts      = accounts,
                    allTransactions = allTransactions,
                    payees        = payees,
                    payeeAccounts = payeeAccounts,
                    onNavigateToPending = { navController.navigate("pending_transactions") },
                    onNavigateToIncome = { navController.navigate("income_list") },
                    onNavigateToExpenses = { navController.navigate("expense_list") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToBankAccounts = { navController.navigate("bank_accounts") },
                    onNavigateToPayees = { navController.navigate("payees") },
                    onNavigateToLoans = { navController.navigate("loans") }
                )
            }

            composable("loans") {
                val loanDao = remember { database.loanDao() }
                val loans by loanDao.getAllLoans().collectAsState(initial = emptyList())
                val scope = rememberCoroutineScope()
                LoansScreen(
                    loans = loans,
                    onBack = { navController.popBackStack() },
                    onAddLoan = { loan ->
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) { loanDao.insertLoan(loan) }
                    },
                    onDeleteLoan = { loan ->
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) { loanDao.deleteLoan(loan) }
                    }
                )
            }

            composable("bank_accounts") {
                val accountDao = remember { database.accountDao() }
                val scope = rememberCoroutineScope()
                BankAccountsScreen(
                    accounts = accounts,
                    onBack = { navController.popBackStack() },
                    onAddAccount = { acc ->
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) { accountDao.insertAccount(acc) }
                    },
                    onUpdateAccount = { acc ->
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) { accountDao.updateAccount(acc) }
                    },
                    onDeleteAccount = { acc ->
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) { accountDao.deleteAccount(acc) }
                    }
                )
            }

            composable("payees") {
                val scope = rememberCoroutineScope()
                val payeeDao = remember { database.payeeDao() }
                PayeesScreen(
                    payees = payees,
                    payeeAccounts = payeeAccounts,
                    onBack = { navController.popBackStack() },
                    onPayeeClick = { payee -> navController.navigate("payee_detail/${payee.id}") },
                    onAddPayee = { payee, accountsList ->
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            val payeeId = payeeDao.insertPayee(payee)
                            accountsList.forEach { acc ->
                                payeeDao.insertPayeeAccount(acc.copy(payeeId = payeeId.toInt()))
                            }
                        }
                    }
                )
            }

            composable("payee_detail/{payeeId}") { backStackEntry ->
                val payeeId = backStackEntry.arguments?.getString("payeeId")?.toIntOrNull() ?: -1
                val payee = remember(payees, payeeId) { payees.firstOrNull { it.id == payeeId } }
                val currentAccounts = remember(payeeAccounts, payeeId) { payeeAccounts.filter { it.payeeId == payeeId } }
                val scope = rememberCoroutineScope()
                val payeeDao = remember { database.payeeDao() }

                PayeeDetailScreen(
                    payee = payee,
                    accounts = currentAccounts,
                    onBack = { navController.popBackStack() },
                    onDeletePayee = {
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            payee?.let { payeeDao.deletePayee(it) }
                        }
                        navController.popBackStack()
                    },
                    onAddAccount = { acc ->
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) { payeeDao.insertPayeeAccount(acc.copy(payeeId = payeeId)) }
                    },
                    onUpdateAccount = { acc ->
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) { payeeDao.updatePayeeAccount(acc.copy(payeeId = payeeId)) }
                    },
                    onDeleteAccount = { acc ->
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) { payeeDao.deletePayeeAccount(acc) }
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    preferencesManager = preferencesManager,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("income_list") {
                val incomeTransactions = remember(allTransactions) {
                    allTransactions.filter { it.type == "INCOME" }
                }
                TransactionListScreen(
                    type = "INCOME",
                    transactions = incomeTransactions,
                    accounts = accounts,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("expense_list") {
                val expenseTransactions = remember(allTransactions) {
                    allTransactions.filter { it.type == "EXPENSE" }
                }
                TransactionListScreen(
                    type = "EXPENSE",
                    transactions = expenseTransactions,
                    accounts = accounts,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("pending_transactions") {
                val viewModel = remember { PendingTransactionsViewModel(database) }
                val pendingList by viewModel.pendingList.collectAsState()
                val mappingsList by viewModel.mappingsList.collectAsState()
                val potentialSenders by viewModel.potentialSenders.collectAsState()
                val context = androidx.compose.ui.platform.LocalContext.current
                PendingTransactionsScreen(
                    pendingList  = pendingList,
                    accounts     = accounts,
                    database     = database,
                    mappingsList = mappingsList,
                    potentialSenders = potentialSenders,
                    onAddMapping = { sender, accountId -> viewModel.addMapping(sender, accountId) },
                    onDeleteMapping = { viewModel.deleteMapping(it) },
                    onLoadPotentialSenders = { viewModel.loadPotentialSenders(context) },
                    onSyncSenderHistory = { sender, accountId, onComplete ->
                        viewModel.syncSenderHistory(context, sender, accountId, onComplete)
                    },
                    onConfirm    = { pending, edited -> viewModel.confirm(pending, edited) },
                    onDismiss    = { viewModel.dismiss(it) },
                    onUpdate     = { viewModel.update(it) },
                    onDismissAll = { viewModel.dismissAll() },
                    onBack       = { navController.popBackStack() }
                )
            }
        }
    }
}

// ─── Main Scaffold Container (Side Drawer + Bottom Nav) ────────

@Composable
fun MainDashboardContainer(
    preferencesManager: PreferencesManager,
    database: FinanceDatabase,
    accounts: List<AccountEntity>,
    allTransactions: List<TransactionEntity>,
    payees: List<PayeeEntity> = emptyList(),
    payeeAccounts: List<PayeeAccountEntity> = emptyList(),
    onNavigateToPending: () -> Unit,
    onNavigateToIncome: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBankAccounts: () -> Unit = {},
    onNavigateToPayees: () -> Unit = {},
    onNavigateToLoans: () -> Unit = {}
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope       = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf("home") } // "home", "budget", "goals"

    val profileName by preferencesManager.profileName.collectAsState(initial = "User")
    val profileImagePath by preferencesManager.profileImagePath.collectAsState(initial = "")
    val hideBalancesPref by preferencesManager.hideCardBalances.collectAsState(initial = false)
    var showEditProfileDialog by remember { mutableStateOf(false) }

    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = profileName,
            currentImagePath = profileImagePath,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, path ->
                scope.launch {
                    preferencesManager.setProfileName(name)
                    preferencesManager.setProfileImagePath(path)
                    showEditProfileDialog = false
                }
            }
        )
    }

    // Use null as the initial sentinel value so we never render the SMS dialog
    // during the brief DataStore loading window. The dialog only appears once
    // DataStore has confirmed the stored value is "PENDING" (i.e., first-run).
    val smsSyncChoice by preferencesManager.smsSyncChoice.collectAsState(initial = null)
    if (smsSyncChoice == "PENDING") {
        SmsPermissionHandler(
            preferencesManager = preferencesManager,
            database = database,
            onDismiss = {}
        )
    }

    // Local DB Flows
    // Pending SMS badge count
    val pendingSmsDao   = remember { database.pendingSmsDao() }
    val pendingCount    by pendingSmsDao.getPendingCount().collectAsState(initial = 0)

    val accountDao     = remember { database.accountDao() }
    val transactionDao = remember { database.transactionDao() }
    val budgetDao      = remember { database.budgetDao() }
    val goalDao        = remember { database.goalDao() }

    val startOfMonth = remember { getStartOfMonthTimestamp() }
    val monthlyIncome   by transactionDao.getMonthlyIncome(startOfMonth).collectAsState(initial = 0.0)
    val monthlyExpenses by transactionDao.getMonthlyExpenses(startOfMonth).collectAsState(initial = 0.0)

    val budgets by budgetDao.getAllBudgets().collectAsState(initial = emptyList())
    val categoryExpenseSums by transactionDao.getExpensesByCategoryFromDate(startOfMonth).collectAsState(initial = emptyList())
    val spentByCategory = remember(categoryExpenseSums) { categoryExpenseSums.associate { it.category to it.total } }

    val goals by goalDao.getAllGoals().collectAsState(initial = emptyList())

    val blurRadius by animateDpAsState(
        targetValue = if (drawerState.targetValue == DrawerValue.Open) 16.dp else 0.dp,
        label = "DrawerBlur"
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet(
                modifier             = Modifier.fillMaxWidth(0.70f),
                drawerContainerColor = SurfaceDark, // white in light mode, deep navy in dark
                drawerShape          = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Box (App Logo + App Name + Close Button in corner)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 28.dp, start = 20.dp, end = 12.dp, bottom = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 40.dp) // buffer for close icon
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.financebuddy),
                                    contentDescription = "App Logo",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text       = "FinanceBuddy",
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color      = TextPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .align(Alignment.CenterEnd)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CardDarker) // off-white in light, darker card in dark
                                .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                                .clickable { scope.launch { drawerState.close() } },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Close,
                                contentDescription = "Close Drawer",
                                tint               = TextPrimary,
                                modifier           = Modifier.size(18.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 20.dp))
                    
                    // User Profile Info Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardDarker) // off-white in light, deeper card in dark
                            .clickable { showEditProfileDialog = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(colors = listOf(AccentTeal.copy(alpha = 0.15f), AccentBlue.copy(alpha = 0.15f)))),
                            contentAlignment = Alignment.Center
                        ) {
                            val profileBitmap = remember(profileImagePath) {
                                if (profileImagePath.isNotEmpty()) {
                                    try {
                                        val file = java.io.File(profileImagePath)
                                        if (file.exists()) {
                                            android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                                        } else null
                                    } catch (e: Exception) {
                                        null
                                    }
                                } else null
                            }

                            if (profileBitmap != null) {
                                Image(
                                    bitmap = profileBitmap.asImageBitmap(),
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Default Profile Photo",
                                    tint = AccentTeal,
                                    modifier = Modifier.fillMaxSize(0.6f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = profileName,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tap to edit profile",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    DrawerMenuItem(
                        icon = Icons.Default.AccountBalance,
                        label = "Bank Accounts",
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToBankAccounts()
                        }
                    )
                    DrawerMenuItem(
                        icon = Icons.Default.People,
                        label = "Recipient Profiles",
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToPayees()
                        }
                    )
                    DrawerMenuItem(
                        icon = Icons.Default.MonetizationOn,
                        label = "Loans",
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                onNavigateToLoans()
                            }
                        }
                    )
                    DrawerMenuItem(
                        icon = Icons.Default.DateRange,
                        label = "Statistics",
                        onClick = { scope.launch { drawerState.close() } }
                    )
                    DrawerMenuItem(
                        icon = Icons.Default.Info,
                        label = "Investment Tracker",
                        onClick = { scope.launch { drawerState.close() } }
                    )
                    DrawerMenuItem(
                        icon = Icons.Default.Inbox,
                        label = "Transaction Inbox",
                        badgeText = if (pendingCount > 0) pendingCount.toString() else null,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToPending()
                        }
                    )
                    DrawerMenuItem(
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                onNavigateToSettings()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Footer section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Local Storage Secured",
                            color = AccentTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "v1.0.0 (AES-256 Encrypted)",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.blur(blurRadius),
            bottomBar = {
                NavigationBar(
                    containerColor = CardDarker,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == "home",
                        onClick  = { currentTab = "home" },
                        icon     = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                        label    = { Text("Home") },
                        colors   = NavigationBarItemColors()
                    )
                    NavigationBarItem(
                        selected = currentTab == "budget",
                        onClick  = { currentTab = "budget" },
                        icon     = { Icon(imageVector = Icons.Default.List, contentDescription = "Budget") },
                        label    = { Text("Budget") },
                        colors   = NavigationBarItemColors()
                    )
                    NavigationBarItem(
                        selected = currentTab == "goals",
                        onClick  = { currentTab = "goals" },
                        icon     = { Icon(imageVector = Icons.Default.Star, contentDescription = "Goals") },
                        label    = { Text("Goals") },
                        colors   = NavigationBarItemColors()
                    )
                    // Pending SMS/Inbox tab with live badge
                    NavigationBarItem(
                        selected = false,
                        onClick  = onNavigateToPending,
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (pendingCount > 0) {
                                        Badge(containerColor = ExpenseRed) {
                                            Text(
                                                text = if (pendingCount > 99) "99+" else pendingCount.toString(),
                                                color = TextPrimary,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inbox,
                                    contentDescription = "Transaction Inbox",
                                )
                            }
                        },
                        label    = { Text("Inbox") },
                        colors   = NavigationBarItemColors()
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                when (currentTab) {
                    "home" -> HomeScreen(
                        accounts           = accounts,
                        allTransactions    = allTransactions,
                        monthlyIncome      = monthlyIncome ?: 0.0,
                        monthlyExpenses    = monthlyExpenses ?: 0.0,
                        onSaveTransaction  = { tx, newFromAcc, newToAcc ->
                            scope.launch(Dispatchers.IO) {
                                var fromId = tx.fromAccountId
                                var toId = tx.toAccountId
                                if (newFromAcc != null) {
                                    fromId = database.accountDao().insertAccount(newFromAcc).toInt()
                                }
                                if (newToAcc != null) {
                                    toId = database.accountDao().insertAccount(newToAcc).toInt()
                                }
                                transactionDao.insertTransaction(tx.copy(fromAccountId = fromId, toAccountId = toId))
                            }
                        },
                        onOpenDrawer       = { scope.launch { drawerState.open() } },
                        onIncomeClick      = onNavigateToIncome,
                        onExpenseClick     = onNavigateToExpenses,
                        payees             = payees,
                        payeeAccounts      = payeeAccounts,
                        onSavePayee        = { name, bankName, accountNumber, type ->
                            scope.launch(Dispatchers.IO) {
                                val payeeDao = database.payeeDao()
                                val existingPayees = payeeDao.getAllPayeesOnce()
                                var payee = existingPayees.firstOrNull { it.name.equals(name, ignoreCase = true) }
                                val payeeId = if (payee == null) {
                                    val uniqueId = "PAY-" + java.util.UUID.randomUUID().toString().take(4).uppercase(java.util.Locale.ROOT)
                                    payeeDao.insertPayee(PayeeEntity(name = name, uniqueId = uniqueId)).toInt()
                                } else {
                                    payee.id
                                }
                                val existingAccs = payeeDao.getAccountsForPayeeOnce(payeeId)
                                val hasAccount = existingAccs.any { it.accountNumber == accountNumber && it.bankName == bankName }
                                if (!hasAccount) {
                                    payeeDao.insertPayeeAccount(
                                        PayeeAccountEntity(
                                            payeeId = payeeId,
                                            bankName = bankName,
                                            accountNumber = accountNumber,
                                            recipientName = name,
                                            type = type
                                        )
                                    )
                                }
                            }
                        },
                        hideBalancesPref = hideBalancesPref
                    )
                    "budget" -> BudgetScreen(
                        budgets           = budgets,
                        spentByCategory   = spentByCategory,
                        onAddBudget       = { budget ->
                            scope.launch(Dispatchers.IO) { budgetDao.insertBudget(budget) }
                        },
                        onDeleteBudget    = { budget ->
                            scope.launch(Dispatchers.IO) { budgetDao.deleteBudget(budget) }
                        }
                    )
                    "goals"  -> GoalsScreen(
                        goals        = goals,
                        onAddGoal    = { goal ->
                            scope.launch(Dispatchers.IO) { goalDao.insertGoal(goal) }
                        },
                        onDeposit    = { goalId, amount ->
                            scope.launch(Dispatchers.IO) { goalDao.depositToGoal(goalId, amount) }
                        },
                        onDeleteGoal = { goal ->
                            scope.launch(Dispatchers.IO) { goalDao.deleteGoal(goal) }
                        }
                    )
                }
            }
        }
    }
}

// ─── Navigation Helper Custom Colors ─────────────────────────

@Composable
private fun NavigationBarItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = AccentTeal,
    selectedTextColor = AccentTeal,
    unselectedIconColor = TextSecondary,
    unselectedTextColor = TextSecondary,
    indicatorColor = CardDark
)

@Composable
fun PageStub(title: String) {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text      = title,
            color     = TextPrimary,
            fontSize  = 20.sp,
            textAlign = TextAlign.Center,
        )
    }
}

// ─── Timestamp Helper ────────────────────────────────────────

private fun getStartOfMonthTimestamp(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

// ─── Custom Drawer Menu Item Composable ──────────────────────

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AccentTeal.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeText,
                    color = AccentTeal,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}