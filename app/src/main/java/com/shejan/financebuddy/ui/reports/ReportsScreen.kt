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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
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

    // Report view mode state: "MONTHLY" or "YEARLY"
    var selectedReportType by remember { mutableStateOf("MONTHLY") }

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

    // ─── Monthly View Calculations ─────────────────────────────────
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

    val monthTransactions = remember(allTransactions, startOfMonthMillis, endOfMonthMillis) {
        allTransactions.filter { it.timestamp in startOfMonthMillis..endOfMonthMillis }
            .sortedByDescending { it.timestamp }
    }

    val totalIncome = remember(monthTransactions) {
        monthTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }

    val totalExpense = remember(monthTransactions) {
        monthTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }

    val totalCurrentBalance = remember(accounts) {
        accounts.sumOf { it.balance }
    }

    val netChangesAfterStart = remember(allTransactions, startOfMonthMillis) {
        allTransactions.filter { it.timestamp >= startOfMonthMillis }.sumOf { tx ->
            when (tx.type) {
                "INCOME" -> tx.amount
                "EXPENSE" -> -tx.amount
                else -> 0.0
            }
        }
    }

    val startingBalance = remember(totalCurrentBalance, netChangesAfterStart) {
        totalCurrentBalance - netChangesAfterStart
    }

    val remainingBalance = remember(startingBalance, totalIncome, totalExpense) {
        startingBalance + totalIncome - totalExpense
    }

    // ─── Yearly View Calculations (All 12 Months for Selected Year) ──
    val yearlyMonthlySummaries = remember(allTransactions, totalCurrentBalance, year) {
        val monthNames = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val list = mutableListOf<MonthSummaryData>()

        for (m in 0..11) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, m)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val mStart = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val mEnd = cal.timeInMillis

            val mTxs = allTransactions.filter { it.timestamp in mStart..mEnd }
            val mInc = mTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
            val mExp = mTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }

            val mNetAfter = allTransactions.filter { it.timestamp >= mStart }.sumOf { tx ->
                when (tx.type) {
                    "INCOME" -> tx.amount
                    "EXPENSE" -> -tx.amount
                    else -> 0.0
                }
            }
            val mStartBal = totalCurrentBalance - mNetAfter
            val mRemBal = mStartBal + mInc - mExp

            list.add(
                MonthSummaryData(
                    monthName = monthNames[m],
                    startingBalance = mStartBal,
                    totalIncome = mInc,
                    totalExpense = mExp,
                    remainingBalance = mRemBal
                )
            )
        }
        list
    }

    val totalAnnualIncome = remember(yearlyMonthlySummaries) {
        yearlyMonthlySummaries.sumOf { it.totalIncome }
    }

    val totalAnnualExpense = remember(yearlyMonthlySummaries) {
        yearlyMonthlySummaries.sumOf { it.totalExpense }
    }

    val annualStartingBalance = remember(yearlyMonthlySummaries) {
        yearlyMonthlySummaries.firstOrNull()?.startingBalance ?: 0.0
    }

    val annualRemainingBalance = remember(yearlyMonthlySummaries) {
        yearlyMonthlySummaries.lastOrNull()?.remainingBalance ?: 0.0
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
                        text = "Financial Report",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Monthly & annual statement overview",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // ─── Segmented Tab Switch (Monthly vs Yearly) ─────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardDarker)
                    .padding(4.dp)
            ) {
                val isMonthly = selectedReportType == "MONTHLY"
                val isYearly = selectedReportType == "YEARLY"

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isMonthly) AccentTeal else Color.Transparent)
                        .clickable { selectedReportType = "MONTHLY" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Monthly Breakdown",
                        color = if (isMonthly) OnAccent else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isMonthly) FontWeight.Bold else FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isYearly) AccentTeal else Color.Transparent)
                        .clickable { selectedReportType = "YEARLY" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Yearly Summary",
                        color = if (isYearly) OnAccent else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isYearly) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            // ─── Selector Bar (Month/Year or Year) ───────────────────────
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
                            if (selectedReportType == "MONTHLY") {
                                newCal.add(Calendar.MONTH, -1)
                            } else {
                                newCal.add(Calendar.YEAR, -1)
                            }
                            selectedCalendar.value = newCal
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CardDarker)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Period",
                            tint = TextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (selectedReportType == "MONTHLY") "$monthName $year" else "Calendar Year $year",
                            color = AccentTeal,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedReportType == "MONTHLY") "${monthTransactions.size} transactions recorded" else "12 Months Financial History",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            val newCal = selectedCalendar.value.clone() as Calendar
                            if (selectedReportType == "MONTHLY") {
                                newCal.add(Calendar.MONTH, 1)
                            } else {
                                newCal.add(Calendar.YEAR, 1)
                            }
                            selectedCalendar.value = newCal
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CardDarker)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Period",
                            tint = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ─── Content List ─────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedReportType == "MONTHLY") {
                    // ───────────────────────────────────────────────────────
                    // MONTHLY VIEW DETAILED TABLE
                    // ───────────────────────────────────────────────────────
                    item {
                        Text(
                            text = "Monthly Transactions Breakdown",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                        )

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
                            val shape = if (isLast) RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp) else RoundedCornerShape(0.dp)
                            val bgColor = if (index % 2 == 0) CardDark else CardDarker
                            val formattedDate = remember(tx.timestamp) { SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(tx.timestamp)) }
                            val accName = accountsMap[tx.fromAccountId]?.name ?: "Account #${tx.fromAccountId}"

                            val (typeLabel, typeColor) = when (tx.type) {
                                "INCOME" -> "Income" to IncomeGreen
                                "EXPENSE" -> "Expense" to ExpenseRed
                                else -> "Transfer" to TransferYellow
                            }

                            Surface(shape = shape, color = bgColor, modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = formattedDate, color = TextPrimary, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false, modifier = Modifier.weight(0.9f))
                                        Text(text = tx.category, color = TextPrimary, fontSize = 10.5.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1.1f))
                                        Box(modifier = Modifier.weight(0.8f)) {
                                            Surface(shape = RoundedCornerShape(4.dp), color = typeColor.copy(alpha = 0.15f)) {
                                                Text(text = typeLabel, color = typeColor, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp))
                                            }
                                        }
                                        Text(text = accName, color = TextSecondary, fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1.0f))
                                        val prefix = if (tx.type == "INCOME") "+৳" else if (tx.type == "EXPENSE") "-৳" else "৳"
                                        Text(text = "$prefix${currencyFormat.format(tx.amount)}", color = typeColor, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End, modifier = Modifier.weight(1.7f))
                                    }
                                    if (!isLast) HorizontalDivider(color = DividerColor.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }

                    // Summary Box (Monthly)
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AccentTeal.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text("Monthly Balance & Statement Summary", color = AccentTeal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Closing calculation for $monthName $year", color = TextMuted, fontSize = 11.sp)

                                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("🏦 Starting Bank / Accounts Balance", color = TextSecondary, fontSize = 12.sp)
                                    Text("৳${currencyFormat.format(startingBalance)}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("📈 Total Income (+)", color = TextSecondary, fontSize = 12.sp)
                                    Text("+৳${currencyFormat.format(totalIncome)}", color = IncomeGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("📉 Total Expense (-)", color = TextSecondary, fontSize = 12.sp)
                                    Text("-৳${currencyFormat.format(totalExpense)}", color = ExpenseRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("💳 Remaining Balance (Closing)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("৳${currencyFormat.format(remainingBalance)}", color = AccentTeal, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }

                } else {
                    // ───────────────────────────────────────────────────────
                    // YEARLY VIEW (12 MONTHS SUMMARY TABLE)
                    // ───────────────────────────────────────────────────────
                    item {
                        Text(
                            text = "Annual Monthly History ($year)",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                        )

                        // Yearly Table Headers
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                .background(CardDarker)
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Month", color = TextMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.weight(0.9f))
                            Text("Starting Bal", color = TextMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.weight(1.1f))
                            Text("Income (+)", color = TextMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.weight(1.1f))
                            Text("Expense (-)", color = TextMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.weight(1.1f))
                            Text("Closing Bal", color = TextMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                        }
                    }

                    itemsIndexed(yearlyMonthlySummaries) { index, mData ->
                        val isLast = index == yearlyMonthlySummaries.size - 1
                        val shape = if (isLast) RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp) else RoundedCornerShape(0.dp)
                        val bgColor = if (index % 2 == 0) CardDark else CardDarker

                        Surface(shape = shape, color = bgColor, modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = mData.monthName, color = TextPrimary, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, modifier = Modifier.weight(0.9f))
                                    Text(text = "৳${currencyFormat.format(mData.startingBalance)}", color = TextSecondary, fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1.1f))
                                    Text(text = "+৳${currencyFormat.format(mData.totalIncome)}", color = IncomeGreen, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1.1f))
                                    Text(text = "-৳${currencyFormat.format(mData.totalExpense)}", color = ExpenseRed, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1.1f))
                                    Text(text = "৳${currencyFormat.format(mData.remainingBalance)}", color = AccentTeal, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                                }
                                if (!isLast) HorizontalDivider(color = DividerColor.copy(alpha = 0.5f))
                            }
                        }
                    }

                    // Summary Box (Yearly)
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AccentTeal.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text("Annual Financial Summary ($year)", color = AccentTeal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Full 12-month consolidated overview", color = TextMuted, fontSize = 11.sp)

                                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("🏦 Year-Start Opening Balance", color = TextSecondary, fontSize = 12.sp)
                                    Text("৳${currencyFormat.format(annualStartingBalance)}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("📈 Total Annual Income (+)", color = TextSecondary, fontSize = 12.sp)
                                    Text("+৳${currencyFormat.format(totalAnnualIncome)}", color = IncomeGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("📉 Total Annual Expenses (-)", color = TextSecondary, fontSize = 12.sp)
                                    Text("-৳${currencyFormat.format(totalAnnualExpense)}", color = ExpenseRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("💳 Year-End Closing Balance", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("৳${currencyFormat.format(annualRemainingBalance)}", color = AccentTeal, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }

            // ─── Export as PDF Button (Bottom Action Bar) ────────────────
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
                            if (selectedReportType == "MONTHLY") {
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
                            } else {
                                PdfReportGenerator.exportYearlyReportPdf(
                                    context = context,
                                    year = year,
                                    monthlySummaries = yearlyMonthlySummaries,
                                    annualStartingBalance = annualStartingBalance,
                                    totalAnnualIncome = totalAnnualIncome,
                                    totalAnnualExpense = totalAnnualExpense,
                                    annualRemainingBalance = annualRemainingBalance
                                )
                            }
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
                            tint = OnAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedReportType == "MONTHLY") "Export Monthly Statement as PDF" else "Export Annual Report as PDF",
                            color = OnAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
