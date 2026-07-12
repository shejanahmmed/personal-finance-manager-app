package com.shejan.financebuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.blur
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import com.shejan.financebuddy.data.PreferencesManager
import com.shejan.financebuddy.data.db.FinanceDatabase
import com.shejan.financebuddy.data.db.TransactionEntity
import com.shejan.financebuddy.data.db.BudgetEntity
import com.shejan.financebuddy.ui.budget.BudgetScreen
import com.shejan.financebuddy.ui.home.HomeScreen
import com.shejan.financebuddy.ui.onboarding.OnboardingScreenRoot
import com.shejan.financebuddy.ui.theme.AccentBlue
import com.shejan.financebuddy.ui.theme.AccentTeal
import com.shejan.financebuddy.ui.theme.BackgroundDark
import com.shejan.financebuddy.ui.theme.CardDark
import com.shejan.financebuddy.ui.theme.CardDarker
import com.shejan.financebuddy.ui.theme.DividerColor
import com.shejan.financebuddy.ui.theme.FinanceBuddyTheme
import com.shejan.financebuddy.ui.theme.TextMuted
import com.shejan.financebuddy.ui.theme.TextPrimary
import com.shejan.financebuddy.ui.theme.TextSecondary
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
            FinanceBuddyTheme {
                AppNavigation(
                    preferencesManager = preferencesManager,
                    database           = database
                )
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
    val navController = rememberNavController()
    val onboardingCompleted by preferencesManager.isOnboardingCompleted.collectAsState(initial = false)
    val startDestination = if (onboardingCompleted) "main_dashboard" else "onboarding"

    NavHost(
        navController    = navController,
        startDestination = startDestination,
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
            MainDashboardContainer(database = database)
        }
    }
}

// ─── Main Scaffold Container (Side Drawer + Bottom Nav) ────────

@Composable
fun MainDashboardContainer(database: FinanceDatabase) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope       = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf("home") } // "home", "budget", "goals"

    // Local DB Flows
    val accountDao     = remember { database.accountDao() }
    val transactionDao = remember { database.transactionDao() }
    val budgetDao      = remember { database.budgetDao() }

    val accounts        by accountDao.getAllAccounts().collectAsState(initial = emptyList())
    val allTransactions by transactionDao.getAllTransactions().collectAsState(initial = emptyList())

    val startOfMonth = remember { getStartOfMonthTimestamp() }
    val monthlyIncome   by transactionDao.getMonthlyIncome(startOfMonth).collectAsState(initial = 0.0)
    val monthlyExpenses by transactionDao.getMonthlyExpenses(startOfMonth).collectAsState(initial = 0.0)

    val budgets by budgetDao.getAllBudgets().collectAsState(initial = emptyList())
    val categoryExpenseSums by transactionDao.getExpensesByCategoryFromDate(startOfMonth).collectAsState(initial = emptyList())
    val spentByCategory = remember(categoryExpenseSums) { categoryExpenseSums.associate { it.category to it.total } }

    val blurRadius by animateDpAsState(
        targetValue = if (drawerState.isOpen) 16.dp else 0.dp,
        label = "DrawerBlur"
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet(
                modifier             = Modifier.fillMaxWidth(0.60f),
                drawerContainerColor = CardDarker,
                drawerShape          = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text       = "⚡ FinanceBuddy",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { scope.launch { drawerState.close() } }) {
                        Icon(
                            imageVector        = Icons.Default.Close,
                            contentDescription = "Close Drawer",
                            tint               = TextSecondary
                        )
                    }
                }
                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(modifier = Modifier.height(16.dp))

                // Menu items
                NavigationDrawerItem(
                    label      = { Text("Settings", color = TextPrimary, fontSize = 14.sp) },
                    selected   = false,
                    onClick    = { scope.launch { drawerState.close() } },
                    icon       = { Icon(Icons.Default.Settings, contentDescription = null, tint = TextSecondary) },
                    colors     = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                    modifier   = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label      = { Text("Statistics", color = TextPrimary, fontSize = 14.sp) },
                    selected   = false,
                    onClick    = { scope.launch { drawerState.close() } },
                    icon       = { Icon(Icons.Default.DateRange, contentDescription = null, tint = TextSecondary) },
                    colors     = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                    modifier   = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label      = { Text("Investment Tracker", color = TextPrimary, fontSize = 14.sp) },
                    selected   = false,
                    onClick    = { scope.launch { drawerState.close() } },
                    icon       = { Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary) },
                    colors     = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                    modifier   = Modifier.padding(horizontal = 8.dp)
                )
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
                        onSaveTransaction  = { tx ->
                            scope.launch(Dispatchers.IO) {
                                transactionDao.insertTransaction(tx)
                            }
                        },
                        onOpenDrawer       = { scope.launch { drawerState.open() } }
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
                    "goals"  -> PageStub(title = "🎯 Savings Goals\n(Coming soon)")
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