package com.shejan.financebuddy.ui.investments

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.AccountEntity
import com.shejan.financebuddy.data.db.InvestmentEntity
import com.shejan.financebuddy.ui.theme.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

private val categoryColors = mapOf(
    "FDR" to Color(0xFF0096FF),           // Blue
    "SANCHAYAPATRA" to Color(0xFF00D4AA), // Teal
    "STOCKS" to Color(0xFFFF9F43),        // Orange
    "MUTUAL_FUND" to Color(0xFF7C5CFC),    // Purple
    "GOLD" to Color(0xFFFFBD2E),           // Gold
    "REAL_ESTATE" to Color(0xFF00C897),    // Green
    "CRYPTO" to Color(0xFFFF5C7C),         // Rose
    "OTHER" to Color(0xFF8A94B2)           // Muted
)

private val categoryLabels = mapOf(
    "FDR" to "Fixed Deposit (FDR/DPS)",
    "SANCHAYAPATRA" to "Sanchayapatra",
    "STOCKS" to "Stock Market (DSE)",
    "MUTUAL_FUND" to "Mutual Funds",
    "GOLD" to "Gold & Precious Metals",
    "REAL_ESTATE" to "Real Estate",
    "CRYPTO" to "Crypto & Forex",
    "OTHER" to "Other Investment"
)

private fun getCategoryIcon(type: String): ImageVector = when (type) {
    "FDR" -> Icons.Default.AccountBalance
    "SANCHAYAPATRA" -> Icons.Default.Savings
    "STOCKS" -> Icons.Default.ShowChart
    "MUTUAL_FUND" -> Icons.Default.PieChart
    "GOLD" -> Icons.Default.WorkspacePremium
    "REAL_ESTATE" -> Icons.Default.HomeWork
    "CRYPTO" -> Icons.Default.CurrencyBitcoin
    else -> Icons.Default.MonetizationOn
}

@Composable
fun InvestmentsScreen(
    investments: List<InvestmentEntity>,
    accounts: List<AccountEntity>,
    onBack: () -> Unit,
    onAddInvestment: (InvestmentEntity) -> Unit,
    onUpdateInvestment: (InvestmentEntity) -> Unit,
    onDeleteInvestment: (InvestmentEntity) -> Unit,
    onLogDividend: (investment: InvestmentEntity, amount: Double, accountId: Int, note: String) -> Unit
) {
    BackHandler(onBack = onBack)

    val currencyFormat = remember { DecimalFormat("##,##,##0.00") }

    var selectedFilter by remember { mutableStateOf("ALL") }
    var showAddSheet by remember { mutableStateOf(false) }
    var editingInvestment by remember { mutableStateOf<InvestmentEntity?>(null) }
    var updatingValueInvestment by remember { mutableStateOf<InvestmentEntity?>(null) }
    var loggingDividendInvestment by remember { mutableStateOf<InvestmentEntity?>(null) }
    var deletingInvestment by remember { mutableStateOf<InvestmentEntity?>(null) }

    // Summary Statistics
    val activeInvestments = remember(investments) { investments.filter { it.status == "ACTIVE" } }
    val totalInvested = remember(activeInvestments) { activeInvestments.sumOf { it.investedAmount } }
    val totalCurrentValue = remember(activeInvestments) { activeInvestments.sumOf { it.currentValue } }
    val totalProfitLoss = totalCurrentValue - totalInvested
    val overallRoiPct = if (totalInvested > 0) (totalProfitLoss / totalInvested) * 100 else 0.0

    val filteredInvestments = remember(investments, selectedFilter) {
        when (selectedFilter) {
            "ALL" -> investments
            "ACTIVE" -> investments.filter { it.status == "ACTIVE" }
            else -> investments.filter { it.type == selectedFilter }
        }
    }

    // Category breakdown for portfolio allocation bar
    val categoryBreakdown = remember(activeInvestments, totalCurrentValue) {
        if (totalCurrentValue <= 0) emptyList()
        else {
            activeInvestments
                .groupBy { it.type }
                .mapValues { (_, list) -> list.sumOf { it.currentValue } }
                .toList()
                .sortedByDescending { it.second }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        // Ambient gradient glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(AccentTeal.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {},
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = Color.Transparent,
                    contentColor = BackgroundDark,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(colors = listOf(AccentTeal, AccentBlue))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Investment",
                            tint = OnAccent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                // ── Screen Header (Matches Bank Accounts & Loans Page Design) ──
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
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Investment Tracker",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Portfolio, ROI & Asset Distribution",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                // ── Main Content Scroll Area ─────────────────────────────────
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── 1. Main Portfolio Overview Card ────────────────────────
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CardDark),
                            border = BorderStroke(1.dp, DividerColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Portfolio Net Value", fontSize = 12.sp, color = TextMuted)
                                        Text(
                                            "৳${currencyFormat.format(totalCurrentValue)}",
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TextPrimary
                                        )
                                    }
                                    // Overall ROI Badge
                                    val isPositive = totalProfitLoss >= 0
                                    val badgeBg = if (isPositive) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f)
                                    val badgeColor = if (isPositive) IncomeGreen else ExpenseRed

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = badgeBg,
                                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                                contentDescription = null,
                                                tint = badgeColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "${if (isPositive) "+" else ""}${String.format(Locale.US, "%.2f", overallRoiPct)}%",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = badgeColor
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = DividerColor.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Total Invested", fontSize = 11.sp, color = TextMuted)
                                        Text(
                                            "৳${currencyFormat.format(totalInvested)}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Total Profit / Loss", fontSize = 11.sp, color = TextMuted)
                                        Text(
                                            "${if (totalProfitLoss >= 0) "+" else ""}৳${currencyFormat.format(totalProfitLoss)}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (totalProfitLoss >= 0) IncomeGreen else ExpenseRed
                                        )
                                    }
                                }

                                // Asset allocation horizontal segmented bar
                                if (categoryBreakdown.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Asset Allocation", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                    ) {
                                        categoryBreakdown.forEach { (catType, valAmount) ->
                                            val weight = (valAmount / totalCurrentValue).toFloat()
                                            val color = categoryColors[catType] ?: Color.Gray
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .weight(weight.coerceAtLeast(0.01f))
                                                    .background(color)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        categoryBreakdown.take(4).forEach { (catType, valAmount) ->
                                            val pct = (valAmount / totalCurrentValue) * 100
                                            val color = categoryColors[catType] ?: Color.Gray
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(color)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "${categoryLabels[catType]?.take(10)}.. ${String.format(Locale.US, "%.0f", pct)}%",
                                                    fontSize = 10.sp,
                                                    color = TextMuted
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── 2. Filter Category Chips ───────────────────────────────
                    item {
                        val filterOptions = listOf(
                            "ALL" to "All",
                            "ACTIVE" to "Active Only",
                            "FDR" to "FDR / DPS",
                            "SANCHAYAPATRA" to "Sanchayapatra",
                            "STOCKS" to "Stocks",
                            "MUTUAL_FUND" to "Mutual Funds",
                            "GOLD" to "Gold",
                            "REAL_ESTATE" to "Real Estate",
                            "CRYPTO" to "Crypto",
                            "OTHER" to "Other"
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(filterOptions) { (key, label) ->
                                val isSelected = selectedFilter == key
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) AccentTeal.copy(alpha = 0.18f) else CardDark)
                                        .border(1.dp, if (isSelected) AccentTeal else DividerColor, RoundedCornerShape(20.dp))
                                        .clickable { selectedFilter = key }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        label,
                                        color = if (isSelected) AccentTeal else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // ── 3. Investment Cards List ───────────────────────────────
                    if (filteredInvestments.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = CardDark),
                                border = BorderStroke(1.dp, DividerColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShowChart,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "No Investments Found",
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Tap + at bottom right to add your FDR, Sanchayapatra, or Stocks",
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredInvestments, key = { it.id }) { investment ->
                            InvestmentCard(
                                investment = investment,
                                currencyFormat = currencyFormat,
                                onUpdateValue = { updatingValueInvestment = investment },
                                onLogDividend = { loggingDividendInvestment = investment },
                                onEdit = { editingInvestment = investment },
                                onDelete = { deletingInvestment = investment }
                            )
                        }
                    }
                }
            }
        }

        // ── Modals & Bottom Sheets ───────────────────────────────────────
        if (showAddSheet) {
            AddEditInvestmentSheet(
                investmentToEdit = null,
                onDismiss = { showAddSheet = false },
                onSave = { newInvestment ->
                    onAddInvestment(newInvestment)
                    showAddSheet = false
                }
            )
        }

        editingInvestment?.let { itemToEdit ->
            AddEditInvestmentSheet(
                investmentToEdit = itemToEdit,
                onDismiss = { editingInvestment = null },
                onSave = { updated ->
                    onUpdateInvestment(updated)
                    editingInvestment = null
                }
            )
        }

        updatingValueInvestment?.let { inv ->
            QuickUpdateValueDialog(
                investment = inv,
                currencyFormat = currencyFormat,
                onDismiss = { updatingValueInvestment = null },
                onSave = { newValue ->
                    onUpdateInvestment(inv.copy(currentValue = newValue))
                    updatingValueInvestment = null
                }
            )
        }

        loggingDividendInvestment?.let { inv ->
            LogDividendDialog(
                investment = inv,
                accounts = accounts,
                currencyFormat = currencyFormat,
                onDismiss = { loggingDividendInvestment = null },
                onConfirm = { amount, accountId, note ->
                    onLogDividend(inv, amount, accountId, note)
                    loggingDividendInvestment = null
                }
            )
        }

        deletingInvestment?.let { inv ->
            AlertDialog(
                onDismissRequest = { deletingInvestment = null },
                containerColor = CardDark,
                title = { Text("Delete Investment?", color = TextPrimary) },
                text = { Text("Are you sure you want to delete '${inv.name}'? This action cannot be undone.", color = TextMuted) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteInvestment(inv)
                            deletingInvestment = null
                        }
                    ) {
                        Text("Delete", color = ExpenseRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletingInvestment = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}

// ─── Single Investment Item Card ──────────────────────────────────────────
@Composable
private fun InvestmentCard(
    investment: InvestmentEntity,
    currencyFormat: DecimalFormat,
    onUpdateValue: () -> Unit,
    onLogDividend: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val profit = investment.currentValue - investment.investedAmount
    val roiPct = if (investment.investedAmount > 0) (profit / investment.investedAmount) * 100 else 0.0
    val catColor = categoryColors[investment.type] ?: Color.Gray
    val isPositive = profit >= 0

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, DividerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(catColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(investment.type),
                        contentDescription = null,
                        tint = catColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = investment.name,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (investment.status != "ACTIVE") {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CardDarker
                            ) {
                                Text(
                                    investment.status,
                                    fontSize = 9.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = "${categoryLabels[investment.type] ?: investment.type} • ${investment.institution.ifBlank { "N/A" }}",
                        color = TextMuted,
                        fontSize = 11.5.sp
                    )
                }

                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardDarker)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Invested Principal", fontSize = 10.5.sp, color = TextMuted)
                    Text(
                        "৳${currencyFormat.format(investment.investedAmount)}",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Current Value", fontSize = 10.5.sp, color = TextMuted)
                    Text(
                        "৳${currencyFormat.format(investment.currentValue)}",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Profit / Gain", fontSize = 10.5.sp, color = TextMuted)
                    Text(
                        "${if (isPositive) "+" else ""}৳${currencyFormat.format(profit)}",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) IncomeGreen else ExpenseRed
                    )
                    Text(
                        "${if (isPositive) "+" else ""}${String.format(Locale.US, "%.1f", roiPct)}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPositive) IncomeGreen else ExpenseRed
                    )
                }
            }

            if (investment.expectedReturnRate > 0 || investment.maturityDate != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (investment.expectedReturnRate > 0) {
                        Text(
                            "Expected Return: ${investment.expectedReturnRate}% p.a.",
                            fontSize = 11.sp,
                            color = AccentTeal,
                            fontWeight = FontWeight.Medium
                        )
                    } else Spacer(modifier = Modifier.width(1.dp))

                    investment.maturityDate?.let { mDate ->
                        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(mDate))
                        Text(
                            "Matures: $dateStr",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onUpdateValue,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = BorderStroke(1.dp, DividerColor),
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentBlue)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Update Value", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onLogDividend,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal.copy(alpha = 0.18f), contentColor = AccentTeal),
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentTeal)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Returns", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── Add / Edit Investment Bottom Sheet (Scrollable & Non-Clipping) ────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditInvestmentSheet(
    investmentToEdit: InvestmentEntity?,
    onDismiss: () -> Unit,
    onSave: (InvestmentEntity) -> Unit
) {
    var name by remember { mutableStateOf(investmentToEdit?.name ?: "") }
    var selectedType by remember { mutableStateOf(investmentToEdit?.type ?: "SANCHAYAPATRA") }
    var institution by remember { mutableStateOf(investmentToEdit?.institution ?: "") }
    var investedAmountStr by remember { mutableStateOf(investmentToEdit?.investedAmount?.let { if (it > 0) String.format(Locale.US, "%.0f", it) else "" } ?: "") }
    var currentValueStr by remember { mutableStateOf(investmentToEdit?.currentValue?.let { if (it > 0) String.format(Locale.US, "%.0f", it) else "" } ?: "") }
    var expectedReturnStr by remember { mutableStateOf(investmentToEdit?.expectedReturnRate?.let { if (it > 0) it.toString() else "" } ?: "") }
    var note by remember { mutableStateOf(investmentToEdit?.note ?: "") }
    var status by remember { mutableStateOf(investmentToEdit?.status ?: "ACTIVE") }

    var errorMessage by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (investmentToEdit == null) "Add Investment" else "Edit Investment",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            if (errorMessage.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ExpenseRed.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        errorMessage,
                        color = ExpenseRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Title Input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorMessage = "" },
                label = { Text("Investment Name") },
                placeholder = { Text("e.g. 3-Year Sanchayapatra", color = TextMuted.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                maxLines = 1,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentTeal,
                    unfocusedBorderColor = DividerColor,
                    focusedLabelColor = AccentTeal,
                    unfocusedLabelColor = TextMuted
                )
            )

            // Category Selection Row
            Column {
                Text("Category / Asset Type", fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categoryLabels.toList()) { (typeKey, label) ->
                        val isSelected = selectedType == typeKey
                        val color = categoryColors[typeKey] ?: Color.Gray
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) color.copy(alpha = 0.2f) else CardDarker)
                                .border(1.dp, if (isSelected) color else DividerColor, RoundedCornerShape(12.dp))
                                .clickable { selectedType = typeKey }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                label,
                                fontSize = 11.5.sp,
                                color = if (isSelected) color else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Institution / Broker
            OutlinedTextField(
                value = institution,
                onValueChange = { institution = it },
                label = { Text("Institution / Broker") },
                placeholder = { Text("e.g. BRAC Bank, DSE, Savings Bureau", color = TextMuted.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                maxLines = 1,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentTeal,
                    unfocusedBorderColor = DividerColor,
                    focusedLabelColor = AccentTeal,
                    unfocusedLabelColor = TextMuted
                )
            )

            // Invested & Current Amounts (Side by side)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = investedAmountStr,
                    onValueChange = {
                        investedAmountStr = it
                        if (currentValueStr.isEmpty()) currentValueStr = it
                        errorMessage = ""
                    },
                    label = { Text("Invested (৳)") },
                    placeholder = { Text("0.00", color = TextMuted.copy(alpha = 0.5f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    maxLines = 1,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentTeal,
                        unfocusedBorderColor = DividerColor,
                        focusedLabelColor = AccentTeal,
                        unfocusedLabelColor = TextMuted
                    )
                )

                OutlinedTextField(
                    value = currentValueStr,
                    onValueChange = { currentValueStr = it; errorMessage = "" },
                    label = { Text("Current Value (৳)") },
                    placeholder = { Text("0.00", color = TextMuted.copy(alpha = 0.5f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    maxLines = 1,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentTeal,
                        unfocusedBorderColor = DividerColor,
                        focusedLabelColor = AccentTeal,
                        unfocusedLabelColor = TextMuted
                    )
                )
            }

            // Return Rate & Notes
            OutlinedTextField(
                value = expectedReturnStr,
                onValueChange = { expectedReturnStr = it },
                label = { Text("Expected Return Rate (% p.a.)") },
                placeholder = { Text("e.g. 11.5", color = TextMuted.copy(alpha = 0.5f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                maxLines = 1,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentTeal,
                    unfocusedBorderColor = DividerColor,
                    focusedLabelColor = AccentTeal,
                    unfocusedLabelColor = TextMuted
                )
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Notes & References") },
                placeholder = { Text("e.g. Certificate #, Folio no", color = TextMuted.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                maxLines = 1,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentTeal,
                    unfocusedBorderColor = DividerColor,
                    focusedLabelColor = AccentTeal,
                    unfocusedLabelColor = TextMuted
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Save Button
            Button(
                onClick = {
                    val invAmt = investedAmountStr.toDoubleOrNull()
                    val curVal = currentValueStr.toDoubleOrNull()
                    val rate = expectedReturnStr.toDoubleOrNull() ?: 0.0

                    if (name.isBlank()) {
                        errorMessage = "Please enter an investment name"
                        return@Button
                    }
                    if (invAmt == null || invAmt <= 0) {
                        errorMessage = "Please enter a valid invested amount"
                        return@Button
                    }
                    if (curVal == null || curVal < 0) {
                        errorMessage = "Please enter a valid current value"
                        return@Button
                    }

                    val entity = investmentToEdit?.copy(
                        name = name.trim(),
                        type = selectedType,
                        institution = institution.trim(),
                        investedAmount = invAmt,
                        currentValue = curVal,
                        expectedReturnRate = rate,
                        note = note.trim(),
                        status = status
                    ) ?: InvestmentEntity(
                        name = name.trim(),
                        type = selectedType,
                        institution = institution.trim(),
                        investedAmount = invAmt,
                        currentValue = curVal,
                        expectedReturnRate = rate,
                        note = note.trim(),
                        status = "ACTIVE"
                    )

                    onSave(entity)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
            ) {
                Text("Save Investment", color = OnAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// ─── Quick Update Current Market Value Dialog ─────────────────────────────
@Composable
private fun QuickUpdateValueDialog(
    investment: InvestmentEntity,
    currencyFormat: DecimalFormat,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var valStr by remember { mutableStateOf(investment.currentValue.toInt().toString()) }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = {
            Text("Update Market Value", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Investment: ${investment.name}", color = TextMuted, fontSize = 12.sp)
                Text("Invested Principal: ৳${currencyFormat.format(investment.investedAmount)}", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = valStr,
                    onValueChange = { valStr = it; errorMsg = "" },
                    label = { Text("New Current Value (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentTeal,
                        unfocusedBorderColor = DividerColor
                    )
                )
                if (errorMsg.isNotEmpty()) {
                    Text(errorMsg, color = ExpenseRed, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newVal = valStr.toDoubleOrNull()
                    if (newVal == null || newVal < 0) {
                        errorMsg = "Please enter a valid amount"
                        return@Button
                    }
                    onSave(newVal)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
            ) {
                Text("Update", color = OnAccent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

// ─── Log Dividend / Returns Dialog ─────────────────────────────────────────
@Composable
private fun LogDividendDialog(
    investment: InvestmentEntity,
    accounts: List<AccountEntity>,
    currencyFormat: DecimalFormat,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, accountId: Int, note: String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 0) }
    var note by remember { mutableStateOf("Dividend payout from ${investment.name}") }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = {
            Text("Log Return / Dividend", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Record a dividend or interest payout received into one of your accounts.", color = TextMuted, fontSize = 12.sp)

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it; errorMsg = "" },
                    label = { Text("Payout Amount (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentTeal,
                        unfocusedBorderColor = DividerColor
                    )
                )

                Text("Deposit To Account", fontSize = 12.sp, color = TextMuted)
                if (accounts.isEmpty()) {
                    Text("No accounts found. Please add an account first.", color = ExpenseRed, fontSize = 11.sp)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(accounts) { acc ->
                            val isSel = selectedAccountId == acc.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) AccentTeal.copy(alpha = 0.2f) else CardDarker)
                                    .border(1.dp, if (isSel) AccentTeal else DividerColor, RoundedCornerShape(10.dp))
                                    .clickable { selectedAccountId = acc.id }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(acc.name, fontSize = 12.sp, color = if (isSel) AccentTeal else TextSecondary)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Description") },
                    singleLine = true,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentTeal,
                        unfocusedBorderColor = DividerColor
                    )
                )

                if (errorMsg.isNotEmpty()) {
                    Text(errorMsg, color = ExpenseRed, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull()
                    if (amt == null || amt <= 0) {
                        errorMsg = "Please enter a valid payout amount"
                        return@Button
                    }
                    if (selectedAccountId == 0) {
                        errorMsg = "Please select a deposit account"
                        return@Button
                    }
                    onConfirm(amt, selectedAccountId, note)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
            ) {
                Text("Log Income", color = OnAccent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
