package com.shejan.financebuddy.ui.loans

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.LoanEntity
import com.shejan.financebuddy.data.db.AccountEntity
import com.shejan.financebuddy.ui.theme.*
import java.text.DecimalFormat
import java.util.Locale

// ─── Supported Bangladeshi Banks Preset List ─────────────────
private val BANK_PRESETS = listOf(
    "BRAC Bank PLC",
    "Dutch-Bangla Bank PLC (DBBL)",
    "The City Bank PLC",
    "Eastern Bank PLC (EBL)",
    "Prime Bank PLC",
    "Mutual Trust Bank PLC",
    "Islami Bank Bangladesh PLC (IBBL)",
    "Al-Arafah Islami Bank PLC",
    "Shahjalal Islami Bank PLC",
    "Other Bank"
)

// Colors based on banks to make it look premium
private val BANK_COLORS = mapOf(
    "BRAC Bank PLC" to "#0096FF",
    "The City Bank PLC" to "#007A33",
    "Eastern Bank PLC (EBL)" to "#003366",
    "Dutch-Bangla Bank PLC (DBBL)" to "#7C5CFC",
    "Prime Bank PLC" to "#FF5722",
    "Mutual Trust Bank PLC" to "#0C2340",
    "Islami Bank Bangladesh PLC (IBBL)" to "#1B5E20",
    "Al-Arafah Islami Bank PLC" to "#2E7D32",
    "Shahjalal Islami Bank PLC" to "#008080",
    "Other Bank" to "#7C5CFC"
)

private fun getBankColor(bankName: String): Color {
    val hex = BANK_COLORS[bankName] ?: "#00D4AA"
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        AccentTeal
    }
}

@Composable
private fun loanTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor       = TextPrimary,
    unfocusedTextColor     = TextPrimary,
    focusedBorderColor     = AccentTeal,
    unfocusedBorderColor   = DividerColor,
    focusedContainerColor  = CardDarker,
    unfocusedContainerColor = CardDarker,
    focusedLabelColor      = AccentTeal,
    unfocusedLabelColor    = TextSecondary
)

// ─── Helper for EMI & Repayment Calculations ───────────────
private fun calculateEmi(principal: Double, annualRate: Double, months: Int): Double {
    if (months <= 0) return 0.0
    if (annualRate <= 0.0) return principal / months
    val monthlyRate = annualRate / 12.0 / 100.0
    val emi = (principal * monthlyRate * Math.pow(1.0 + monthlyRate, months.toDouble())) /
            (Math.pow(1.0 + monthlyRate, months.toDouble()) - 1.0)
    return if (emi.isNaN() || emi.isInfinite()) 0.0 else emi
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    loans: List<LoanEntity>,
    accounts: List<AccountEntity>,
    onBack: () -> Unit,
    onAddLoan: (LoanEntity, accountId: Int) -> Unit,
    onDeleteLoan: (LoanEntity) -> Unit,
    onRepayLoan: (LoanEntity, Double, Int) -> Unit,
    onNavigateToAccounts: () -> Unit
) {
    val currencyFormat = remember { DecimalFormat("##,##,##0.00") }

    // Summary calculations (showing remaining totals for active tracking)
    val totalRemainingPrincipal = remember(loans) {
        loans.sumOf { loan ->
            val emi = calculateEmi(loan.loanAmount, loan.interestRate, loan.durationMonths)
            val originalRepayable = emi * loan.durationMonths
            val remainingRepayable = (originalRepayable - loan.repaidAmount).coerceAtLeast(0.0)
            val principalRatio = if (originalRepayable > 0) loan.loanAmount / originalRepayable else 1.0
            remainingRepayable * principalRatio
        }
    }
    
    val totalRemainingRepayable = remember(loans) {
        loans.sumOf { loan ->
            val emi = calculateEmi(loan.loanAmount, loan.interestRate, loan.durationMonths)
            val originalRepayable = emi * loan.durationMonths
            (originalRepayable - loan.repaidAmount).coerceAtLeast(0.0)
        }
    }

    val totalRemainingInterest = remember(loans, totalRemainingRepayable, totalRemainingPrincipal) {
        (totalRemainingRepayable - totalRemainingPrincipal).coerceAtLeast(0.0)
    }

    val totalRepaid = remember(loans) { loans.sumOf { it.repaidAmount } }

    var showAddSheet by remember { mutableStateOf(false) }
    var deletingLoan by remember { mutableStateOf<LoanEntity?>(null) }
    var repayingLoan by remember { mutableStateOf<LoanEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Delete confirmation dialog
    deletingLoan?.let { loan ->
        AlertDialog(
            onDismissRequest = { deletingLoan = null },
            containerColor = CardDark,
            title = { Text("Delete Loan?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to remove the loan from \"${loan.bankName}\" of ৳${currencyFormat.format(loan.loanAmount)}?",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteLoan(loan)
                    deletingLoan = null
                }) {
                    Text("Delete", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingLoan = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Ambient background gradient glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AccentBlue.copy(alpha = 0.08f), Color.Transparent)
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
                            .background(Brush.linearGradient(colors = listOf(AccentBlue, AccentTeal))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Loan", tint = Color.White)
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                // ─── Screen Header ──────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onBack() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("My Loans", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Track borrowed funds & EMI details", fontSize = 12.sp, color = TextMuted)
                    }
                }

                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // ─── Dashboard Overview Card ────────────────────────
                    item {
                        LoanSummaryOverview(
                            totalPrincipal = totalRemainingPrincipal,
                            totalRepayable = totalRemainingRepayable,
                            totalInterest = totalRemainingInterest,
                            totalRepaid = totalRepaid,
                            currencyFormat = currencyFormat
                        )
                    }

                    // Section Title
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Active Loans",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${loans.size} total",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    // Empty State
                    if (loans.isEmpty()) {
                        item {
                            LoansEmptyState()
                        }
                    }

                    // Loan Cards
                    items(loans, key = { it.id }) { loan ->
                        val linkedAccount = remember(accounts, loan.accountId) {
                            accounts.find { it.id == loan.accountId }
                        }
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 },
                            exit = fadeOut(tween(250)) + slideOutVertically(tween(250)) { it / 4 }
                        ) {
                            LoanCardItem(
                                loan = loan,
                                linkedAccount = linkedAccount,
                                currencyFormat = currencyFormat,
                                onDeleteClick = { deletingLoan = loan },
                                onRepayClick = { repayingLoan = loan }
                            )
                        }
                    }
                }
            }
        }
    }

    // ─── Add Loan Bottom Sheet ──────────────────────────────────
    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState,
            containerColor = SurfaceDark,
            dragHandle = { BottomSheetDefaults.DragHandle(color = DividerColor) }
        ) {
            AddLoanFormSheet(
                accounts = accounts,
                onDismiss = { showAddSheet = false },
                onAddLoan = { bank, amount, months, rate, accountId ->
                    onAddLoan(
                        LoanEntity(
                            bankName = bank,
                            loanAmount = amount,
                            durationMonths = months,
                            interestRate = rate,
                            accountId = accountId
                        ),
                        accountId
                    )
                    showAddSheet = false
                },
                onNavigateToAccounts = onNavigateToAccounts,
                currencyFormat = currencyFormat
            )
        }
    }

    // ─── Repay Loan Bottom Sheet ────────────────────────────────
    if (repayingLoan != null) {
        ModalBottomSheet(
            onDismissRequest = { repayingLoan = null },
            containerColor = SurfaceDark,
            dragHandle = { BottomSheetDefaults.DragHandle(color = DividerColor) }
        ) {
            val loan = repayingLoan!!
            val linkedAccount = remember(accounts, loan.accountId) {
                accounts.find { it.id == loan.accountId }
            }
            RepayLoanFormSheet(
                loan = loan,
                account = linkedAccount,
                accounts = accounts,
                currencyFormat = currencyFormat,
                onDismiss = { repayingLoan = null },
                onRepay = { amount, accountId ->
                    onRepayLoan(loan, amount, accountId)
                    repayingLoan = null
                }
            )
        }
    }
}

// ─── Composable: Summary Overview Card ────────────────────────
@Composable
fun LoanSummaryOverview(
    totalPrincipal: Double,
    totalRepayable: Double,
    totalInterest: Double,
    totalRepaid: Double,
    currencyFormat: DecimalFormat
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(colors = listOf(AccentBlue.copy(alpha = 0.95f), AccentPurple.copy(alpha = 0.95f)))
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Total Remaining Borrowed (Principal)",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "৳${currencyFormat.format(totalPrincipal)}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(
                            text = "Remaining Payable",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "৳${currencyFormat.format(totalRepayable)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Remaining Interest",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "৳${currencyFormat.format(totalInterest)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total Repaid",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "৳${currencyFormat.format(totalRepaid)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentTeal
                        )
                    }
                }
            }
        }
    }
}

// ─── Composable: Loans Empty State ──────────────────────────
@Composable
fun LoansEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AccentTeal.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = null,
                    tint = AccentTeal,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No active loans added",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap the + button to calculate and store your bank loan liabilities.",
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Composable: Loan Card Item ──────────────────────────────
@Composable
fun LoanCardItem(
    loan: LoanEntity,
    linkedAccount: AccountEntity?,
    currencyFormat: DecimalFormat,
    onDeleteClick: () -> Unit,
    onRepayClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(targetValue = if (expanded) 180f else 0f)

    val emi = calculateEmi(loan.loanAmount, loan.interestRate, loan.durationMonths)
    val originalRepayable = emi * loan.durationMonths
    val originalInterest = (originalRepayable - loan.loanAmount).coerceAtLeast(0.0)

    val remainingRepayable = (originalRepayable - loan.repaidAmount).coerceAtLeast(0.0)
    val principalRatio = if (originalRepayable > 0) loan.loanAmount / originalRepayable else 1.0
    val remainingPrincipal = remainingRepayable * principalRatio
    val remainingInterest = remainingRepayable * (1.0 - principalRatio)

    val percentPaid = if (originalRepayable > 0) (loan.repaidAmount / originalRepayable * 100).toFloat() else 0f

    val bankColor = remember(loan.bankName, linkedAccount) {
        if (linkedAccount != null) {
            try {
                Color(android.graphics.Color.parseColor(linkedAccount.colorHex))
            } catch (e: Exception) {
                getBankColor(loan.bankName)
            }
        } else {
            getBankColor(loan.bankName)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column {
            // Clickable upper part (Header & metrics)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp)
            ) {
                // Card Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Bank Indicator Dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(bankColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = loan.bankName,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${loan.durationMonths} Months | ${loan.interestRate}% APR",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Loan",
                            tint = ExpenseRed.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Main Info: Amount & EMI Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Remaining Principal",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                        Text(
                            text = "৳${currencyFormat.format(remainingPrincipal)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Monthly EMI",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                        Text(
                            text = "৳${currencyFormat.format(emi)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentTeal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Repayment Progress Bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Repaid: ${String.format(Locale.US, "%.1f%%", percentPaid)}",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "৳${currencyFormat.format(loan.repaidAmount)} / ৳${currencyFormat.format(originalRepayable)}",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Custom Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(DividerColor.copy(alpha = 0.3f))
                    ) {
                        if (percentPaid > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = (percentPaid / 100f).coerceIn(0f, 1f))
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(AccentTeal)
                            )
                        }
                    }
                }

                // Expand Arrow Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = TextMuted,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(rotationState)
                    )
                }
            }

            // Expanded Details Block
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    HorizontalDivider(color = DividerColor.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Doughnut Chart (Repaid vs Remaining Principal vs Interest)
                        LoanDoughnutChart(
                            principalRemaining = remainingPrincipal,
                            interestRemaining = remainingInterest,
                            repaid = loan.repaidAmount,
                            modifier = Modifier
                                .size(110.dp)
                                .padding(end = 4.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Numbers Breakdown
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DetailTextRow(label = "Original Principal", value = "৳${currencyFormat.format(loan.loanAmount)}", valueColor = TextPrimary)
                            DetailTextRow(label = "Remaining Principal", value = "৳${currencyFormat.format(remainingPrincipal)}", valueColor = AccentTeal)
                            DetailTextRow(label = "Remaining Interest", value = "৳${currencyFormat.format(remainingInterest)}", valueColor = ExpenseRed)
                            DetailTextRow(label = "Remaining Payable", value = "৳${currencyFormat.format(remainingRepayable)}", valueColor = TextPrimary)
                            if (linkedAccount != null) {
                                DetailTextRow(label = "Account Linked", value = linkedAccount.name, valueColor = bankColor)
                            }
                        }
                    }

                    // Repay Loan Button or Fully Repaid Indicator
                    if (remainingRepayable <= 0.0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .background(AccentTeal.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .border(1.dp, AccentTeal.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Fully Repaid", color = AccentTeal, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    } else {
                        Button(
                            onClick = onRepayClick,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Repay Loan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailTextRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
        Text(text = value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─── Composable: Custom Canvas Doughnut Chart ───────────────
@Composable
fun LoanDoughnutChart(
    principalRemaining: Double,
    interestRemaining: Double,
    repaid: Double,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(principalRemaining, interestRemaining, repaid) {
        animProgress.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
    }

    val total = principalRemaining + interestRemaining + repaid
    val repaidPct = if (total > 0) (repaid / total).toFloat() else 0f
    val principalPct = if (total > 0) (principalRemaining / total).toFloat() else 0f
    val interestPct = if (total > 0) (interestRemaining / total).toFloat() else 0f

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = 8.dp.toPx()
            val canvasSize = size.minDimension
            val radiusSize = canvasSize - strokeW
            val rectOffset = Offset(
                (size.width - radiusSize) / 2f,
                (size.height - radiusSize) / 2f
            )

            // Gray background ring
            drawArc(
                color = DividerColor.copy(alpha = 0.5f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = rectOffset,
                size = Size(radiusSize, radiusSize),
                style = Stroke(width = strokeW)
            )

            val rSweep = repaidPct * 360f * animProgress.value
            val pSweep = principalPct * 360f * animProgress.value
            val iSweep = interestPct * 360f * animProgress.value

            // Repaid Sector (AccentTeal)
            if (rSweep > 0f) {
                drawArc(
                    color = AccentTeal,
                    startAngle = -90f,
                    sweepAngle = rSweep,
                    useCenter = false,
                    topLeft = rectOffset,
                    size = Size(radiusSize, radiusSize),
                    style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                )
            }

            // Remaining Principal Sector (AccentBlue)
            if (pSweep > 0f) {
                drawArc(
                    color = AccentBlue,
                    startAngle = -90f + rSweep,
                    sweepAngle = pSweep,
                    useCenter = false,
                    topLeft = rectOffset,
                    size = Size(radiusSize, radiusSize),
                    style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                )
            }

            // Remaining Interest Sector (ExpenseRed)
            if (iSweep > 0f) {
                drawArc(
                    color = ExpenseRed,
                    startAngle = -90f + rSweep + pSweep,
                    sweepAngle = iSweep,
                    useCenter = false,
                    topLeft = rectOffset,
                    size = Size(radiusSize, radiusSize),
                    style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                )
            }
        }

        // Percentage in center
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format(Locale.US, "%.0f%%", repaidPct * 100),
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Repaid",
                color = TextMuted,
                fontSize = 8.sp
            )
        }
    }
}

// ─── Composable: Add Loan Form Sheet ─────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanFormSheet(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onAddLoan: (bank: String, amount: Double, months: Int, rate: Double, accountId: Int) -> Unit,
    onNavigateToAccounts: () -> Unit,
    currencyFormat: DecimalFormat
) {
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()) }
    var amountInput by remember { mutableStateOf("") }
    var monthsInput by remember { mutableStateOf("") }
    var rateInput by remember { mutableStateOf("") }

    var expandedDropdown by remember { mutableStateOf(false) }

    // Parsed states for calculations
    val parsedAmount = remember(amountInput) { amountInput.toDoubleOrNull() ?: 0.0 }
    val parsedMonths = remember(monthsInput) { monthsInput.toIntOrNull() ?: 0 }
    val parsedRate = remember(rateInput) { rateInput.toDoubleOrNull() ?: 0.0 }

    // Live Calculations
    val liveEmi = remember(parsedAmount, parsedRate, parsedMonths) {
        calculateEmi(parsedAmount, parsedRate, parsedMonths)
    }
    val liveRepayable = remember(liveEmi, parsedMonths) { liveEmi * parsedMonths }
    val liveInterest = remember(liveRepayable, parsedAmount) { (liveRepayable - parsedAmount).coerceAtLeast(0.0) }

    val isFormValid = remember(selectedAccount, parsedAmount, parsedMonths, parsedRate) {
        selectedAccount != null && parsedAmount > 0.0 && parsedMonths > 0 && parsedRate >= 0.0
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(20.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (accounts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Bank Accounts Found",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You must link a bank/MFS account to receive the loan funds and pay EMI.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        onDismiss()
                        onNavigateToAccounts()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = BackgroundDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add Bank Account", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Text(
                text = "Calculate & Add Loan",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // Account Selection Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedAccount?.let { "${it.name} [${it.accountSubtype}]" } ?: "Select Bank Account",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Account to Deposit Funds", color = TextSecondary) },
                    colors = loanTextFieldColors(),
                    trailingIcon = {
                        IconButton(onClick = { expandedDropdown = !expandedDropdown }) {
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown Options", tint = TextPrimary)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedDropdown = !expandedDropdown }
                )

                DropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardDarker)
                ) {
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(account.name, color = TextPrimary, fontWeight = FontWeight.Medium)
                                    Text("৳${currencyFormat.format(account.balance)}", color = AccentTeal, fontSize = 13.sp)
                                }
                            },
                            onClick = {
                                selectedAccount = account
                                expandedDropdown = false
                            }
                        )
                    }
                }
            }

            // Amount Input (৳)
            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it.filter { char -> char.isDigit() || char == '.' } },
                label = { Text("Loan Amount (৳)", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = loanTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            // Month Repayment Period
            OutlinedTextField(
                value = monthsInput,
                onValueChange = { monthsInput = it.filter { char -> char.isDigit() } },
                label = { Text("Repayment Period (Months)", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = loanTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            // Interest Rate (%)
            OutlinedTextField(
                value = rateInput,
                onValueChange = { rateInput = it.filter { char -> char.isDigit() || char == '.' } },
                label = { Text("Interest Rate (% per year)", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = loanTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            // Dynamic Calculations Card (Premium View)
            if (parsedAmount > 0 || parsedMonths > 0 || parsedRate > 0) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDarker),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, DividerColor), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "LIVE ESTIMATIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentTeal)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Monthly EMI:", color = TextSecondary, fontSize = 13.sp)
                            Text(text = "৳${currencyFormat.format(liveEmi)}", color = AccentTeal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total Interest:", color = TextSecondary, fontSize = 13.sp)
                            Text(text = "৳${currencyFormat.format(liveInterest)}", color = ExpenseRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total Repayable:", color = TextSecondary, fontSize = 13.sp)
                            Text(text = "৳${currencyFormat.format(liveRepayable)}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Action Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = BorderStroke(1.dp, DividerColor),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (isFormValid && selectedAccount != null) {
                            onAddLoan(
                                selectedAccount!!.name,
                                parsedAmount,
                                parsedMonths,
                                parsedRate,
                                selectedAccount!!.id
                            )
                        }
                    },
                    enabled = isFormValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentTeal,
                        contentColor = BackgroundDark,
                        disabledContainerColor = CardDark,
                        disabledContentColor = TextMuted
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add Loan")
                }
            }
        }
    }
}

// ─── Composable: Repay Loan Form Sheet ───────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepayLoanFormSheet(
    loan: LoanEntity,
    account: AccountEntity?,
    accounts: List<AccountEntity>,
    currencyFormat: DecimalFormat,
    onDismiss: () -> Unit,
    onRepay: (amount: Double, accountId: Int) -> Unit
) {
    val emi = remember(loan) { calculateEmi(loan.loanAmount, loan.interestRate, loan.durationMonths) }
    val originalRepayable = remember(loan, emi) { emi * loan.durationMonths }
    val remainingRepayable = remember(loan, originalRepayable) { (originalRepayable - loan.repaidAmount).coerceAtLeast(0.0) }

    val initialAmount = remember(emi, remainingRepayable) {
        val amount = if (remainingRepayable < emi) remainingRepayable else emi
        String.format(Locale.US, "%.2f", amount)
    }

    var repayAmountInput by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialAmount,
                selection = TextRange(initialAmount.length)
            )
        )
    }
    var selectedAccount by remember { mutableStateOf(account ?: accounts.firstOrNull()) }
    var expandedDropdown by remember { mutableStateOf(false) }

    val parsedAmount = remember(repayAmountInput.text) { repayAmountInput.text.toDoubleOrNull() ?: 0.0 }
    val accountBalance = remember(selectedAccount) { selectedAccount?.balance ?: 0.0 }

    // Validation
    val isInsufficientBalance = parsedAmount > accountBalance
    val isExceedingRemaining = parsedAmount > remainingRepayable
    val isFormValid = parsedAmount > 0.0 && !isInsufficientBalance && !isExceedingRemaining && selectedAccount != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(20.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Repay Loan - ${loan.bankName}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        // Payment Account Information
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedAccount?.let { "${it.name} [Bal: ৳${currencyFormat.format(it.balance)}]" } ?: "Select Payment Account",
                onValueChange = {},
                readOnly = true,
                label = { Text("Repay From Account", color = TextSecondary) },
                colors = loanTextFieldColors(),
                trailingIcon = {
                    IconButton(onClick = { expandedDropdown = !expandedDropdown }) {
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown Options", tint = TextPrimary)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedDropdown = !expandedDropdown }
            )

            DropdownMenu(
                expanded = expandedDropdown,
                onDismissRequest = { expandedDropdown = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardDarker)
            ) {
                accounts.forEach { acc ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(acc.name, color = TextPrimary, fontWeight = FontWeight.Medium)
                                Text("৳${currencyFormat.format(acc.balance)}", color = AccentTeal, fontSize = 13.sp)
                            }
                        },
                        onClick = {
                            selectedAccount = acc
                            expandedDropdown = false
                        }
                    )
                }
            }
        }

        // Display current loan details
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardDarker),
            modifier = Modifier.fillMaxWidth().border(BorderStroke(1.dp, DividerColor), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "LOAN SUMMARY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentTeal)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Remaining Payable:", color = TextSecondary, fontSize = 13.sp)
                    Text(text = "৳${currencyFormat.format(remainingRepayable)}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Monthly EMI Amount:", color = TextSecondary, fontSize = 13.sp)
                    Text(text = "৳${currencyFormat.format(emi)}", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Repayment Amount Input
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(
                value = repayAmountInput,
                onValueChange = { newValue ->
                    val filteredText = newValue.text.filter { char -> char.isDigit() || char == '.' }
                    if (filteredText == newValue.text) {
                        repayAmountInput = newValue
                    } else {
                        repayAmountInput = TextFieldValue(
                            text = filteredText,
                            selection = TextRange(filteredText.length)
                        )
                    }
                },
                label = { Text("Repayment Amount (৳)", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor       = TextPrimary,
                    unfocusedTextColor     = TextPrimary,
                    focusedBorderColor     = if (isInsufficientBalance || isExceedingRemaining) ExpenseRed else AccentTeal,
                    unfocusedBorderColor   = if (isInsufficientBalance || isExceedingRemaining) ExpenseRed else DividerColor,
                    focusedContainerColor  = CardDarker,
                    unfocusedContainerColor = CardDarker,
                    focusedLabelColor      = if (isInsufficientBalance || isExceedingRemaining) ExpenseRed else AccentTeal,
                    unfocusedLabelColor    = TextSecondary
                ),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(
                        onClick = {
                            val payAllText = String.format(Locale.US, "%.2f", remainingRepayable)
                            repayAmountInput = TextFieldValue(text = payAllText, selection = TextRange(payAllText.length))
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("PAY ALL", color = AccentTeal, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            )

            if (isInsufficientBalance) {
                Text(
                    text = "Insufficient balance in selected account (৳${currencyFormat.format(accountBalance)})",
                    color = ExpenseRed,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            } else if (isExceedingRemaining) {
                Text(
                    text = "Amount exceeds the remaining loan balance (৳${currencyFormat.format(remainingRepayable)})",
                    color = ExpenseRed,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                border = BorderStroke(1.dp, DividerColor),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    if (isFormValid && selectedAccount != null) {
                        onRepay(parsedAmount, selectedAccount!!.id)
                    }
                },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentTeal,
                    contentColor = BackgroundDark,
                    disabledContainerColor = CardDark,
                    disabledContentColor = TextMuted
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm Repay", fontWeight = FontWeight.Bold)
            }
        }
    }
}
