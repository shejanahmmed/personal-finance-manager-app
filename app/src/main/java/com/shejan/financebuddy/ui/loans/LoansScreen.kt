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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.LoanEntity
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
    onBack: () -> Unit,
    onAddLoan: (LoanEntity) -> Unit,
    onDeleteLoan: (LoanEntity) -> Unit
) {
    val currencyFormat = remember { DecimalFormat("##,##,##0.00") }

    // Summary calculations
    val totalPrincipal = remember(loans) { loans.sumOf { it.loanAmount } }
    val totalRepayable = remember(loans) {
        loans.sumOf { loan ->
            val emi = calculateEmi(loan.loanAmount, loan.interestRate, loan.durationMonths)
            emi * loan.durationMonths
        }
    }
    val totalInterest = remember(loans, totalRepayable) { (totalRepayable - totalPrincipal).coerceAtLeast(0.0) }

    var showAddSheet by remember { mutableStateOf(false) }
    var deletingLoan by remember { mutableStateOf<LoanEntity?>(null) }
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
                            totalPrincipal = totalPrincipal,
                            totalRepayable = totalRepayable,
                            totalInterest = totalInterest,
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
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 },
                            exit = fadeOut(tween(250)) + slideOutVertically(tween(250)) { it / 4 }
                        ) {
                            LoanCardItem(
                                loan = loan,
                                currencyFormat = currencyFormat,
                                onDeleteClick = { deletingLoan = loan }
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
                onDismiss = { showAddSheet = false },
                onAddLoan = { bank, amount, months, rate ->
                    onAddLoan(
                        LoanEntity(
                            bankName = bank,
                            loanAmount = amount,
                            durationMonths = months,
                            interestRate = rate
                        )
                    )
                    showAddSheet = false
                },
                currencyFormat = currencyFormat
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
                    text = "Total Active Borrowed",
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Total Repayable",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "৳${currencyFormat.format(totalRepayable)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total Interest",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "৳${currencyFormat.format(totalInterest)}",
                            fontSize = 15.sp,
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
    currencyFormat: DecimalFormat,
    onDeleteClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(targetValue = if (expanded) 180f else 0f)

    val emi = calculateEmi(loan.loanAmount, loan.interestRate, loan.durationMonths)
    val totalRepayable = emi * loan.durationMonths
    val totalInterest = (totalRepayable - loan.loanAmount).coerceAtLeast(0.0)

    val bankColor = remember(loan.bankName) { getBankColor(loan.bankName) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        text = "Borrowed",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                    Text(
                        text = "৳${currencyFormat.format(loan.loanAmount)}",
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

            // Expanded Details Block
            if (expanded) {
                HorizontalDivider(color = DividerColor.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Doughnut Chart (Principal vs Interest)
                    LoanDoughnutChart(
                        principal = loan.loanAmount,
                        interest = totalInterest,
                        modifier = Modifier
                            .size(100.dp)
                            .padding(end = 8.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Numbers Breakdown
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DetailTextRow(label = "Principal", value = "৳${currencyFormat.format(loan.loanAmount)}", valueColor = AccentTeal)
                        DetailTextRow(label = "Total Interest", value = "৳${currencyFormat.format(totalInterest)}", valueColor = ExpenseRed)
                        DetailTextRow(label = "Total Repayable", value = "৳${currencyFormat.format(totalRepayable)}", valueColor = TextPrimary)
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
    principal: Double,
    interest: Double,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(principal, interest) {
        animProgress.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
    }

    val total = principal + interest
    val principalPct = if (total > 0) (principal / total).toFloat() else 0f
    val interestPct = if (total > 0) (interest / total).toFloat() else 0f

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

            // Principal Sector (AccentTeal / Blue)
            val pSweep = principalPct * 360f * animProgress.value
            drawArc(
                color = AccentTeal,
                startAngle = -90f,
                sweepAngle = pSweep,
                useCenter = false,
                topLeft = rectOffset,
                size = Size(radiusSize, radiusSize),
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            // Interest Sector (ExpenseRed)
            val iSweep = interestPct * 360f * animProgress.value
            if (iSweep > 0) {
                drawArc(
                    color = ExpenseRed,
                    startAngle = -90f + pSweep,
                    sweepAngle = iSweep,
                    useCenter = false,
                    topLeft = rectOffset,
                    size = Size(radiusSize, radiusSize),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }
        }

        // Percentage in center
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format(Locale.US, "%.0f%%", principalPct * 100),
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Principal",
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
    onDismiss: () -> Unit,
    onAddLoan: (bank: String, amount: Double, months: Int, rate: Double) -> Unit,
    currencyFormat: DecimalFormat
) {
    var bankInput by remember { mutableStateOf("") }
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

    val isFormValid = remember(bankInput, parsedAmount, parsedMonths, parsedRate) {
        bankInput.isNotBlank() && parsedAmount > 0.0 && parsedMonths > 0 && parsedRate >= 0.0
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(20.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Calculate & Add Loan",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        // Bank Selection Dropdown
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = bankInput,
                onValueChange = { bankInput = it },
                label = { Text("Select or Enter Bank", color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentTeal,
                    unfocusedBorderColor = DividerColor,
                    focusedLabelColor = AccentTeal,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                trailingIcon = {
                    IconButton(onClick = { expandedDropdown = !expandedDropdown }) {
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown Options", tint = TextPrimary)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            DropdownMenu(
                expanded = expandedDropdown,
                onDismissRequest = { expandedDropdown = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(CardDarker)
            ) {
                BANK_PRESETS.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(preset, color = TextPrimary) },
                        onClick = {
                            bankInput = if (preset == "Other Bank") "" else preset
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
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentTeal,
                unfocusedBorderColor = DividerColor,
                focusedLabelColor = AccentTeal,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Month Repayment Period
        OutlinedTextField(
            value = monthsInput,
            onValueChange = { monthsInput = it.filter { char -> char.isDigit() } },
            label = { Text("Repayment Period (Months)", color = TextSecondary) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentTeal,
                unfocusedBorderColor = DividerColor,
                focusedLabelColor = AccentTeal,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Interest Rate (%)
        OutlinedTextField(
            value = rateInput,
            onValueChange = { rateInput = it.filter { char -> char.isDigit() || char == '.' } },
            label = { Text("Interest Rate (% per year)", color = TextSecondary) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentTeal,
                unfocusedBorderColor = DividerColor,
                focusedLabelColor = AccentTeal,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
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
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
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
                    if (isFormValid) {
                        onAddLoan(bankInput, parsedAmount, parsedMonths, parsedRate)
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
