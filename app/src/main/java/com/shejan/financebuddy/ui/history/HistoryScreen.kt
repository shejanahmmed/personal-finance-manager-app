package com.shejan.financebuddy.ui.history

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.AccountEntity
import com.shejan.financebuddy.data.db.TransactionEntity
import com.shejan.financebuddy.ui.theme.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val currencyFormat = remember { DecimalFormat("##,##,##0.00") }

    // Search query state
    var searchQuery by remember { mutableStateOf("") }

    // Type filter state: "ALL", "INCOME", "EXPENSE", "TRANSFER", "LOAN_REPAYMENT"
    var selectedTypeFilter by remember { mutableStateOf("ALL") }

    // Period filter state: "ALL", "WEEK", "MONTH", "YEAR", "CUSTOM"
    var selectedPeriodFilter by remember { mutableStateOf("ALL") }
    var customDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Accounts map for quick lookup
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

    // Date Picker launcher for Custom Date selection
    if (showDatePicker) {
        val cal = Calendar.getInstance()
        if (customDateMillis != null) {
            cal.timeInMillis = customDateMillis!!
        }
        DisposableEffect(Unit) {
            val dialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val selectedCal = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    customDateMillis = selectedCal.timeInMillis
                    selectedPeriodFilter = "CUSTOM"
                    showDatePicker = false
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            dialog.setOnCancelListener { showDatePicker = false }
            dialog.show()
            onDispose { dialog.dismiss() }
        }
    }

    // Filtered transaction list
    val filteredTransactions = remember(transactions, searchQuery, selectedTypeFilter, selectedPeriodFilter, customDateMillis) {
        transactions.filter { tx ->
            // 1. Type Filter
            val isLoanRepayment = tx.category.contains("Loan Repayment", ignoreCase = true) || tx.note.contains("Repayment to", ignoreCase = true)
            val typeMatch = when (selectedTypeFilter) {
                "INCOME" -> tx.type == "INCOME"
                "EXPENSE" -> tx.type == "EXPENSE" && !isLoanRepayment
                "TRANSFER" -> tx.type == "TRANSFER"
                "LOAN_REPAYMENT" -> isLoanRepayment
                else -> true
            }

            // 2. Period Filter
            val periodMatch = when (selectedPeriodFilter) {
                "WEEK" -> {
                    val startOfWeek = Calendar.getInstance().apply {
                        firstDayOfWeek = Calendar.MONDAY
                        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    tx.timestamp >= startOfWeek
                }
                "MONTH" -> {
                    val startOfMonth = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    tx.timestamp >= startOfMonth
                }
                "YEAR" -> {
                    val startOfYear = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_YEAR, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    tx.timestamp >= startOfYear
                }
                "CUSTOM" -> {
                    if (customDateMillis == null) true
                    else {
                        val startOfDay = customDateMillis!!
                        val endOfDay = Calendar.getInstance().apply {
                            timeInMillis = customDateMillis!!
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }.timeInMillis
                        tx.timestamp in startOfDay..endOfDay
                    }
                }
                else -> true
            }

            // 3. Search Query Match
            val fromAccName = accountMap[tx.fromAccountId]?.name ?: ""
            val toAccName = tx.toAccountId?.let { accountMap[it]?.name } ?: ""
            val queryMatch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                tx.category.lowercase().contains(q) ||
                        tx.note.lowercase().contains(q) ||
                        tx.amount.toString().contains(q) ||
                        fromAccName.lowercase().contains(q) ||
                        toAccName.lowercase().contains(q)
            }

            typeMatch && periodMatch && queryMatch
        }.sortedByDescending { it.timestamp }
    }

    // Totals calculations
    val totalInflow = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }
    val totalOutflow = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }
    val totalTransfers = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "TRANSFER" }.sumOf { it.amount }
    }

    // Grouping by date
    val groupedTransactions = remember(filteredTransactions) {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val todayStr = dateFormat.format(Date())
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = dateFormat.format(cal.time)

        filteredTransactions.groupBy { tx ->
            val dateStr = dateFormat.format(Date(tx.timestamp))
            when (dateStr) {
                todayStr -> "Today"
                yesterdayStr -> "Yesterday"
                else -> dateStr
            }
        }
    }

    val typeFilters = listOf(
        "ALL" to "All",
        "INCOME" to "Income",
        "EXPENSE" to "Expenses",
        "TRANSFER" to "Transfers",
        "LOAN_REPAYMENT" to "Loan Repay"
    )

    val periodFilters = listOf(
        "ALL" to "All Time",
        "WEEK" to "This Week",
        "MONTH" to "This Month",
        "YEAR" to "This Year",
        "CUSTOM" to if (selectedPeriodFilter == "CUSTOM" && customDateMillis != null) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(customDateMillis!!))
        } else "Select Date"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Ambient background glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AccentTeal.copy(alpha = 0.05f), Color.Transparent)
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // ─── Header Top Bar (Matching Bank Accounts Page Design) ─────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CardDarker)
                        .border(1.dp, DividerColor, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Transaction History",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Complete record of all financial activity",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // ─── Search Bar ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardDark)
                    .border(1.dp, if (searchQuery.isNotEmpty()) AccentTeal else DividerColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search transactions, accounts, notes...",
                                color = TextMuted,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(AccentTeal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // ─── Filter Chips Bars ────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Type filter row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(typeFilters) { (key, label) ->
                        val isSelected = selectedTypeFilter == key
                        val chipBg = if (isSelected) AccentTeal.copy(alpha = 0.18f) else CardDark
                        val chipBorder = if (isSelected) AccentTeal else DividerColor
                        val chipTextColor = if (isSelected) AccentTeal else TextSecondary

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(chipBg)
                                .border(1.dp, chipBorder, RoundedCornerShape(20.dp))
                                .clickable { selectedTypeFilter = key }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                color = chipTextColor,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Date period filter row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(periodFilters) { (key, label) ->
                        val isSelected = selectedPeriodFilter == key
                        val chipBg = if (isSelected) AccentBlue.copy(alpha = 0.18f) else CardDarker
                        val chipBorder = if (isSelected) AccentBlue else DividerColor.copy(alpha = 0.6f)
                        val chipTextColor = if (isSelected) AccentBlue else TextMuted

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(chipBg)
                                .border(1.dp, chipBorder, RoundedCornerShape(20.dp))
                                .clickable {
                                    if (key == "CUSTOM") {
                                        showDatePicker = true
                                    } else {
                                        selectedPeriodFilter = key
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (key == "CUSTOM") {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = chipTextColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Text(
                                    text = label,
                                    color = chipTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // ─── Content List ─────────────────────────────────────────────
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("No matching transactions", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Try adjusting your search query or filter options", color = TextMuted, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Summary Aggregates Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HistoryMetricCard(
                                title = "Inflow",
                                amount = totalInflow,
                                color = IncomeGreen,
                                formatter = currencyFormat,
                                modifier = Modifier.weight(1f)
                            )
                            HistoryMetricCard(
                                title = "Outflow",
                                amount = totalOutflow,
                                color = ExpenseRed,
                                formatter = currencyFormat,
                                modifier = Modifier.weight(1f)
                            )
                            HistoryMetricCard(
                                title = "Transfers",
                                amount = totalTransfers,
                                color = TransferYellow,
                                formatter = currencyFormat,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Grouped Transactions
                    groupedTransactions.forEach { (dateHeader, txs) ->
                        item {
                            Text(
                                text = dateHeader,
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp)
                            )
                        }

                        items(txs) { tx ->
                            HistoryTransactionCard(
                                tx = tx,
                                accountsMap = accountMap,
                                formatter = currencyFormat
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryMetricCard(
    title: String,
    amount: Double,
    color: Color,
    formatter: DecimalFormat,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(title, fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Medium)
            Text(
                text = "৳${formatter.format(amount)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HistoryTransactionCard(
    tx: TransactionEntity,
    accountsMap: Map<Int, AccountEntity>,
    formatter: DecimalFormat
) {
    val isLoanRepayment = tx.category.contains("Loan Repayment", ignoreCase = true) || tx.note.contains("Repayment to", ignoreCase = true)

    val (badgeText, badgeColor, icon) = when {
        isLoanRepayment -> Triple("LOAN REPAY", AccentTeal, Icons.Default.CreditCard)
        tx.type == "INCOME" -> Triple("INCOME", IncomeGreen, Icons.Default.TrendingUp)
        tx.type == "EXPENSE" -> Triple("EXPENSE", ExpenseRed, Icons.Default.TrendingDown)
        else -> Triple("TRANSFER", TransferYellow, Icons.Default.SwapHoriz)
    }

    val fromAccount = accountsMap[tx.fromAccountId]
    val toAccount = tx.toAccountId?.let { accountsMap[it] }

    val formattedTime = remember(tx.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
    }

    fun formatAccNum(num: String): String {
        return if (num.length > 4) "•••• ${num.takeLast(4)}" else num
    }

    val accountSubtext = when {
        tx.type == "TRANSFER" && fromAccount != null && toAccount != null ->
            "${fromAccount.name} (${formatAccNum(fromAccount.accountNumber)}) ➔ ${toAccount.name} (${formatAccNum(toAccount.accountNumber)})"
        tx.type == "TRANSFER" && fromAccount != null ->
            "From: ${fromAccount.name} (${formatAccNum(fromAccount.accountNumber)})"
        fromAccount != null ->
            "${fromAccount.name} (${formatAccNum(fromAccount.accountNumber)})"
        else -> "General Account"
    }

    val amountPrefix = when {
        tx.type == "INCOME" -> "+"
        tx.type == "EXPENSE" -> "-"
        tx.type == "TRANSFER" -> "⇄"
        else -> "↺"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon Box
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (tx.category.isNotBlank()) tx.category else tx.type,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeColor.copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = badgeText,
                                color = badgeColor,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = accountSubtext,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (tx.note.isNotBlank() && tx.note != tx.category) {
                        Text(
                            text = tx.note,
                            color = TextMuted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix৳${formatter.format(tx.amount)}",
                    color = badgeColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formattedTime,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}
