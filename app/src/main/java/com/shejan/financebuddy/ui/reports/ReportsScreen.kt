package com.shejan.financebuddy.ui.reports

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.AccountEntity
import com.shejan.financebuddy.data.db.TransactionEntity
import com.shejan.financebuddy.ui.theme.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    allTransactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val currencyFormat = remember { DecimalFormat("##,##,##0.00") }
    val accountsMap = remember(accounts) { accounts.associateBy { it.id } }

    // Calendar state for month/year selection
    val selectedCalendar = remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        })
    }

    val monthName = remember(selectedCalendar.value.timeInMillis) {
        SimpleDateFormat("MMMM", Locale.getDefault()).format(selectedCalendar.value.time)
    }

    val year = remember(selectedCalendar.value.timeInMillis) {
        selectedCalendar.value.get(Calendar.YEAR)
    }

    // Start & End of selected month timestamps
    val startOfMonthMillis = remember(selectedCalendar.value.timeInMillis) {
        selectedCalendar.value.timeInMillis
    }

    val endOfMonthMillis = remember(selectedCalendar.value.timeInMillis) {
        val cal = selectedCalendar.value.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        cal.timeInMillis
    }

    // Transactions in selected month
    val monthTransactions = remember(allTransactions, startOfMonthMillis, endOfMonthMillis) {
        allTransactions.filter { it.timestamp in startOfMonthMillis..endOfMonthMillis }
            .sortedByDescending { it.timestamp }
    }

    // Calculations for the month
    val totalIncome = remember(monthTransactions) {
        monthTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }

    val totalExpense = remember(monthTransactions) {
        monthTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }

    // Total Current Balance of all accounts today
    val totalCurrentBalance = remember(accounts) {
        accounts.sumOf { it.balance }
    }

    // Net changes that occurred AFTER the start of the selected month up to now
    val netChangesAfterStart = remember(allTransactions, startOfMonthMillis) {
        allTransactions.filter { it.timestamp >= startOfMonthMillis }.sumOf { tx ->
            when (tx.type) {
                "INCOME" -> tx.amount
                "EXPENSE" -> -tx.amount
                else -> 0.0
            }
        }
    }

    // Starting Balance at the start of the selected month
    val startingBalance = remember(totalCurrentBalance, netChangesAfterStart) {
        totalCurrentBalance - netChangesAfterStart
    }

    val remainingBalance = remember(startingBalance, totalIncome, totalExpense) {
        startingBalance + totalIncome - totalExpense
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Ambient Header Glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AccentTeal.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // ─── Header Top Bar ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 20.dp, end = 20.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Financial Report",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Monthly statement & balance overview",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardDark)
                            .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Go Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ─── Month & Year Selector Bar ───────────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            val newCal = selectedCalendar.value.clone() as Calendar
                            newCal.add(Calendar.MONTH, -1)
                            selectedCalendar.value = newCal
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CardDarker)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = TextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$monthName $year",
                            color = AccentTeal,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${monthTransactions.size} transactions recorded",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            val newCal = selectedCalendar.value.clone() as Calendar
                            newCal.add(Calendar.MONTH, 1)
                            selectedCalendar.value = newCal
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CardDarker)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Month",
                            tint = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ─── Content List (Table + Summary Box) ──────────────────────
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Table Header Card
                item {
                    Text(
                        text = "Monthly Transactions Breakdown",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                    )

                    // Table Column Headers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .background(CardDarker)
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Date", color = TextMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.weight(0.9f))
                        Text("Category", color = TextMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.weight(1.1f))
                        Text("Type", color = TextMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.weight(0.8f))
                        Text("Account", color = TextMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.weight(1.0f))
                        Text("Amount", color = TextMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.weight(1.7f), textAlign = TextAlign.End)
                    }
                }

                if (monthTransactions.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = CardDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No transactions found for $monthName $year",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(monthTransactions) { index, tx ->
                        val isLast = index == monthTransactions.size - 1
                        val shape = if (isLast) {
                            RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                        } else {
                            RoundedCornerShape(0.dp)
                        }

                        val bgColor = if (index % 2 == 0) CardDark else CardDarker
                        val formattedDate = remember(tx.timestamp) {
                            SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(tx.timestamp))
                        }
                        val accName = accountsMap[tx.fromAccountId]?.name ?: "Account #${tx.fromAccountId}"

                        val (typeLabel, typeColor) = when (tx.type) {
                            "INCOME" -> "Income" to IncomeGreen
                            "EXPENSE" -> "Expense" to ExpenseRed
                            else -> "Transfer" to TransferYellow
                        }

                        Surface(
                            shape = shape,
                            color = bgColor,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formattedDate,
                                        color = TextPrimary,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.weight(0.9f)
                                    )
                                    Text(
                                        text = tx.category,
                                        color = TextPrimary,
                                        fontSize = 10.5.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1.1f)
                                    )
                                    Box(modifier = Modifier.weight(0.8f)) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = typeColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = typeLabel,
                                                color = typeColor,
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = accName,
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1.0f)
                                    )
                                    val prefix = if (tx.type == "INCOME") "+৳" else if (tx.type == "EXPENSE") "-৳" else "৳"
                                    Text(
                                        text = "$prefix${currencyFormat.format(tx.amount)}",
                                        color = typeColor,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1.7f)
                                    )
                                }
                                if (!isLast) {
                                    HorizontalDivider(color = DividerColor.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }

                // ─── Summary Box (At the end in a styled card box) ────────
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AccentTeal.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Monthly Balance & Statement Summary",
                                color = AccentTeal,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Closing calculation for $monthName $year",
                                color = TextMuted,
                                fontSize = 11.sp
                            )

                            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))

                            // 1. Starting Balance
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🏦 Starting Bank / Accounts Balance", color = TextSecondary, fontSize = 12.sp)
                                Text("৳${currencyFormat.format(startingBalance)}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 2. Total Income
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📈 Total Income (+)", color = TextSecondary, fontSize = 12.sp)
                                Text("+৳${currencyFormat.format(totalIncome)}", color = IncomeGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 3. Total Expense
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📉 Total Expense (-)", color = TextSecondary, fontSize = 12.sp)
                                Text("-৳${currencyFormat.format(totalExpense)}", color = ExpenseRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 10.dp))

                            // 4. Remaining Balance
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💳 Remaining Balance (Closing)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "৳${currencyFormat.format(remainingBalance)}",
                                    color = AccentTeal,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            // ─── Export as PDF Button (Bottom Sticky Action Bar) ─────────
            Surface(
                color = CardDarker,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = {
                            PdfReportGenerator.exportMonthlyReportPdf(
                                context = context,
                                monthName = monthName,
                                year = year,
                                transactions = monthTransactions,
                                accountsMap = accountsMap,
                                startingBalance = startingBalance,
                                totalIncome = totalIncome,
                                totalExpense = totalExpense,
                                remainingBalance = remainingBalance
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF Icon",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Export Statement as PDF",
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
