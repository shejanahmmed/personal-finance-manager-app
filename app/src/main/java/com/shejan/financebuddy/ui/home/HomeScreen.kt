package com.shejan.financebuddy.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.AccountEntity
import com.shejan.financebuddy.data.db.TransactionEntity
import com.shejan.financebuddy.data.db.PayeeEntity
import com.shejan.financebuddy.data.db.PayeeAccountEntity
import com.shejan.financebuddy.ui.home.components.BalanceTrendLineChart
import com.shejan.financebuddy.ui.home.components.ExpenseBarChart
import com.shejan.financebuddy.ui.theme.AccentBlue
import com.shejan.financebuddy.ui.theme.AccentTeal
import com.shejan.financebuddy.ui.theme.BackgroundDark
import com.shejan.financebuddy.ui.theme.CardDark
import com.shejan.financebuddy.ui.theme.CardDarker
import com.shejan.financebuddy.ui.theme.DividerColor
import com.shejan.financebuddy.ui.theme.ExpenseRed
import com.shejan.financebuddy.ui.theme.IncomeGreen
import com.shejan.financebuddy.ui.theme.TextMuted
import com.shejan.financebuddy.ui.theme.TextPrimary
import com.shejan.financebuddy.ui.theme.TextSecondary
import com.shejan.financebuddy.ui.theme.TransferYellow
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    accounts: List<AccountEntity>,
    allTransactions: List<TransactionEntity>,
    monthlyIncome: Double,
    monthlyExpenses: Double,
    onSaveTransaction: (TransactionEntity) -> Unit,
    onOpenDrawer: () -> Unit,
    onIncomeClick: () -> Unit,
    onExpenseClick: () -> Unit,
    payees: List<PayeeEntity> = emptyList(),
    payeeAccounts: List<PayeeAccountEntity> = emptyList(),
    onSavePayee: (String, String, String, String) -> Unit = { _, _, _, _ -> }
) {
    var showAddSheet by remember { mutableStateOf(false) }
    val sheetState   = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val recentTransactions = remember(allTransactions) { allTransactions.take(5) }
    val currencyFormat = remember { DecimalFormat("##,##,##0.00") }
    val totalBalance   = accounts.sumOf { it.balance }

    val sortedAccounts = remember(accounts, allTransactions) {
        accounts.sortedWith(
            compareByDescending<AccountEntity> { account ->
                allTransactions
                    .filter { it.fromAccountId == account.id || it.toAccountId == account.id }
                    .maxOfOrNull { it.timestamp } ?: 0L
            }.thenBy { it.name }
        )
    }


    var isTopBarVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < -12f) {
                    isTopBarVisible = false
                } else if (delta > 12f) {
                    isTopBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .nestedScroll(nestedScrollConnection)
    ) {
        // Ambient top glow flowing under the status bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AccentTeal.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            // Empty topBar so we can overlay the floating top bar smoothly
            topBar = {},
            floatingActionButton = {
                // ── Floating Action Button (FAB) ─────────────────────
                FloatingActionButton(
                    onClick         = { showAddSheet = true },
                    containerColor  = Color.Transparent,
                    contentColor    = BackgroundDark,
                    shape           = CircleShape,
                    modifier        = Modifier.size(56.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(colors = listOf(AccentTeal, AccentBlue))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction", tint = BackgroundDark)
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .verticalScroll(rememberScrollState())
            ) {
                // Spacer matching top bar height (64.dp) + status bar padding
                Spacer(modifier = Modifier.statusBarsPadding().height(64.dp))

                // ── 1. Balance Overview Card ──────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(text = "Total Balance", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text       = "৳${currencyFormat.format(totalBalance)}",
                        style      = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary
                    )
                }

                // Swipeable Account Chips (only show accounts with a non-zero balance)
                val activeAccounts = remember(sortedAccounts) { sortedAccounts.filter { it.balance > 0 } }
                if (activeAccounts.isNotEmpty()) {
                    LazyRow(
                        contentPadding        = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(activeAccounts) { account ->
                            AccountCardChip(account = account, currencyFormat = currencyFormat)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardDark)
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No active accounts — add funds via Bank Accounts",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }


                Spacer(modifier = Modifier.height(12.dp))

                // ── 2. Income vs Expense Summary ──────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SummaryCard(
                        title      = "Income",
                        amount     = monthlyIncome,
                        color      = IncomeGreen,
                        modifier   = Modifier.weight(1f),
                        formatter  = currencyFormat,
                        onClick    = onIncomeClick
                    )
                    SummaryCard(
                        title      = "Expenses",
                        amount     = monthlyExpenses,
                        color      = ExpenseRed,
                        modifier   = Modifier.weight(1f),
                        formatter  = currencyFormat,
                        onClick    = onExpenseClick
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── 3. Expense Graph (Weekly Chart) ───────────────────
                SectionHeader(title = "Weekly Spending")
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .height(200.dp)
                ) {
                    ExpenseBarChart(
                        days     = getLast7DayNames(),
                        amounts  = getActualWeeklyExpenses(allTransactions),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ── 4. Last Recorded Overview ──────────────────────────
                SectionHeader(title = "Recent Transactions")
                if (recentTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardDark)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "No transactions recorded yet.", color = TextMuted, fontSize = 14.sp)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardDark)
                    ) {
                        recentTransactions.forEachIndexed { index, tx ->
                            TransactionRowItem(
                                tx             = tx,
                                accounts       = accounts,
                                currencyFormat = currencyFormat
                            )
                            if (index < recentTransactions.size - 1) {
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(DividerColor)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ── 5. Balance Trend (Line Chart) ─────────────────────
                SectionHeader(title = "Balance Trend")
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .height(180.dp)
                ) {
                    BalanceTrendLineChart(
                        balances = getActualBalanceTrend(totalBalance, allTransactions),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp, bottom = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ── 6. Upcoming Planned Payments ──────────────────────
                SectionHeader(title = "Upcoming Planned Payments")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardDark)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text     = "No upcoming planned payments scheduled.",
                        color    = TextMuted,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        // ── Top Bar Overlay (Translucent and Animated) ───────────────
        AnimatedVisibility(
            visible = isTopBarVisible,
            enter   = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit    = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = TextPrimary)
                }

                Text(
                    text       = "FinanceBuddy",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary
                )

                IconButton(onClick = { /* notification action */ }) {
                    Icon(imageVector = Icons.Default.Notifications, contentDescription = "Notifications", tint = TextPrimary)
                }
            }
        }

        // Bottom sheet for transaction additions
        if (showAddSheet) {
            AddTransactionSheet(
                accounts          = accounts,
                sheetState        = sheetState,
                onDismiss         = { showAddSheet = false },
                onSaveTransaction = onSaveTransaction,
                payees            = payees,
                payeeAccounts     = payeeAccounts,
                onSavePayee       = onSavePayee
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Components & Stubs
// ─────────────────────────────────────────────────────────────

@Composable
fun AccountCardChip(
    account: AccountEntity,
    currencyFormat: DecimalFormat
) {
    val cardColor = remember { Color(android.graphics.Color.parseColor(account.colorHex)) }
    Card(
        shape   = RoundedCornerShape(16.dp),
        colors  = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.15f)),
        border  = androidx.compose.foundation.BorderStroke(1.dp, cardColor.copy(alpha = 0.5f)),
        modifier = Modifier
            .width(180.dp)
            .height(115.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Text(
                    text = account.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(cardColor.copy(alpha = 0.25f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (account.accountSubtype.isNotBlank()) account.accountSubtype else account.type,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = cardColor
                    )
                }
            }

            Text(
                text  = "৳${currencyFormat.format(account.balance)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier,
    formatter: DecimalFormat,
    onClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Card(
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = CardDark),
        modifier = modifier
            .height(84.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, fontSize = 12.sp, color = TextSecondary)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text       = "৳${formatter.format(amount)}",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary
                )
            }
        }
    }
}

@Composable
fun TransactionRowItem(
    tx: TransactionEntity,
    accounts: List<AccountEntity>,
    currencyFormat: DecimalFormat
) {
    val dateString = remember(tx.timestamp) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
    }
    val sourceAccount = remember(tx.fromAccountId) {
        accounts.find { it.id == tx.fromAccountId }?.name ?: "Unknown"
    }
    val destAccount = remember(tx.toAccountId, tx.note) {
        if (tx.toAccountId != null) {
            accounts.find { it.id == tx.toAccountId }?.name ?: "Unknown"
        } else {
            if (tx.note.startsWith("To: ")) {
                tx.note.removePrefix("To: ").substringBefore(" - ").ifBlank { "Other Person" }
            } else {
                "Other Person"
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Semantic Indicator
        val bulletColor = when (tx.type) {
            "INCOME" -> IncomeGreen
            "EXPENSE" -> ExpenseRed
            else -> TransferYellow
        }
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(bulletColor)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Transaction Details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = when (tx.type) {
                    "TRANSFER" -> "Transfer to $destAccount"
                    else -> tx.category
                },
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text  = "$sourceAccount • $dateString",
                fontSize = 11.sp,
                color    = TextSecondary
            )
        }

        // Amount
        val sign = when (tx.type) {
            "INCOME" -> "+"
            "EXPENSE" -> "-"
            else -> ""
        }
        val amountColor = when (tx.type) {
            "INCOME" -> IncomeGreen
            "EXPENSE" -> ExpenseRed
            else -> TextPrimary
        }
        Text(
            text       = "$sign৳${currencyFormat.format(tx.amount)}",
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color      = amountColor
        )
    }
}

@Composable
fun PlannedPaymentItem(
    title: String,
    date: String,
    amount: Double,
    formatter: DecimalFormat
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = date, fontSize = 11.sp, color = TextMuted)
        }
        Text(
            text       = "৳${formatter.format(amount)}",
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color      = TextPrimary
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text       = title,
        fontSize   = 15.sp,
        fontWeight = FontWeight.Bold,
        color      = TextPrimary,
        modifier   = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

// ── Actual Data Math Helpers ───────────────────────────────

private fun getActualWeeklyExpenses(transactions: List<TransactionEntity>): List<Double> {
    val calendar = Calendar.getInstance()
    val dailySums = DoubleArray(7)

    // Set calendar to end of today
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)

    val endOfDays = LongArray(7)
    val startOfDays = LongArray(7)

    for (i in 0..6) {
        val idx = 6 - i // today is index 6, yesterday is 5, etc.
        endOfDays[idx] = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        startOfDays[idx] = calendar.timeInMillis

        // Go to previous day
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        // Reset to end of day for next iteration
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
    }

    for (tx in transactions) {
        if (tx.type == "EXPENSE") {
            for (i in 0..6) {
                if (tx.timestamp in startOfDays[i]..endOfDays[i]) {
                    dailySums[i] += tx.amount
                    break
                }
            }
        }
    }

    return dailySums.toList()
}

private fun getActualBalanceTrend(currentTotalBalance: Double, transactions: List<TransactionEntity>): List<Double> {
    val calendar = Calendar.getInstance()

    // Start of today
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    val dayStarts = LongArray(7)
    for (i in 0..6) {
        val idx = 6 - i
        dayStarts[idx] = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1)
    }

    val trend = DoubleArray(7)
    trend[6] = currentTotalBalance

    for (i in 5 downTo 0) {
        val dayDStart = dayStarts[i + 1]
        val dayDEnd = if (i + 2 < 7) dayStarts[i + 2] else Long.MAX_VALUE

        var netChange = 0.0
        for (tx in transactions) {
            if (tx.timestamp in dayDStart until dayDEnd) {
                when (tx.type) {
                    "INCOME" -> netChange += tx.amount
                    "EXPENSE" -> netChange -= tx.amount
                }
            }
        }
        trend[i] = trend[i + 1] - netChange
    }

    return trend.toList()
}

private fun getLast7DayNames(): List<String> {
    val sdf = SimpleDateFormat("EEE", Locale.getDefault())
    val calendar = Calendar.getInstance()
    val names = mutableListOf<String>()
    for (i in 0..6) {
        names.add(sdf.format(calendar.time))
        calendar.add(Calendar.DAY_OF_YEAR, -1)
    }
    return names.reversed()
}

