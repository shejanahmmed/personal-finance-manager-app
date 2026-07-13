package com.shejan.financebuddy.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    type: String, // "INCOME" or "EXPENSE"
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    onBack: () -> Unit
) {
    // Standard back handler to support system back gestures
    BackHandler(onBack = onBack)

    val currencyFormat = remember { DecimalFormat("##,##,##0.00") }
    val totalAmount = remember(transactions) { transactions.sumOf { it.amount } }

    // Group transactions by date string
    val groupedTransactions = remember(transactions) {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val todayStr = dateFormat.format(Date())
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = dateFormat.format(cal.time)

        transactions.groupBy { tx ->
            val dateStr = dateFormat.format(Date(tx.timestamp))
            when (dateStr) {
                todayStr -> "Today"
                yesterdayStr -> "Yesterday"
                else -> dateStr
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Ambient background glow
        val glowColor = if (type == "INCOME") IncomeGreen else ExpenseRed
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(glowColor.copy(alpha = 0.05f), Color.Transparent)
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
                            text = if (type == "INCOME") "Income Ledger" else "Expense Ledger",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Historical transaction records",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Request: "add a small back button at the top right corner when tap it will work"
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

            // ─── Content List ─────────────────────────────────────────────
            if (transactions.isEmpty()) {
                EmptyTransactionsState(type = type)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Summary Aggregates Card
                    item {
                        SummaryAggregateCard(
                            type = type,
                            totalAmount = totalAmount,
                            count = transactions.size,
                            formatter = currencyFormat
                        )
                    }

                    // Transaction list grouped by day
                    groupedTransactions.forEach { (dateHeader, txs) ->
                        // Sticky Header style section title
                        item {
                            Text(
                                text = dateHeader,
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 8.dp)
                            )
                        }

                        items(txs) { tx ->
                            TransactionDetailsRow(
                                tx = tx,
                                accounts = accounts,
                                formatter = currencyFormat
                            )
                            HorizontalDivider(
                                color = DividerColor.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryAggregateCard(
    type: String,
    totalAmount: Double,
    count: Int,
    formatter: DecimalFormat
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .border(1.dp, DividerColor, RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Total ${if (type == "INCOME") "Inflow" else "Outflow"}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "৳${formatter.format(totalAmount)}",
                    color = if (type == "INCOME") IncomeGreen else ExpenseRed,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (type == "INCOME") IncomeGreen.copy(alpha = 0.1f) else ExpenseRed.copy(alpha = 0.1f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$count Trans.",
                    color = if (type == "INCOME") IncomeGreen else ExpenseRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TransactionDetailsRow(
    tx: TransactionEntity,
    accounts: List<AccountEntity>,
    formatter: DecimalFormat
) {
    val timeStr = remember(tx.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
    }
    val accountName = remember(tx.fromAccountId) {
        accounts.find { it.id == tx.fromAccountId }?.name ?: "Unknown Account"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundDark)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Styled indicator badge
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CardDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (tx.type == "INCOME") Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                contentDescription = null,
                tint = if (tx.type == "INCOME") IncomeGreen else ExpenseRed,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Transaction metadata
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tx.category,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$accountName • $timeStr",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            if (tx.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tx.note,
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Amount Display
        Text(
            text = "${if (tx.type == "INCOME") "+" else "-"}৳${formatter.format(tx.amount)}",
            color = if (tx.type == "INCOME") IncomeGreen else ExpenseRed,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyTransactionsState(type: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(CardDark)
                    .border(1.dp, DividerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "No Records Found",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You haven't recorded any ${if (type == "INCOME") "income" else "expenses"} yet.",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
