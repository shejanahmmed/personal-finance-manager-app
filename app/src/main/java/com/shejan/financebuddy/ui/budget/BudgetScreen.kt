package com.shejan.financebuddy.ui.budget

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.BudgetEntity
import com.shejan.financebuddy.ui.theme.AccentBlue
import com.shejan.financebuddy.ui.theme.AccentTeal
import com.shejan.financebuddy.ui.theme.BackgroundDark
import com.shejan.financebuddy.ui.theme.CardDark
import com.shejan.financebuddy.ui.theme.CardDarker
import com.shejan.financebuddy.ui.theme.DividerColor
import com.shejan.financebuddy.ui.theme.ExpenseRed
import com.shejan.financebuddy.ui.theme.GradientEnd
import com.shejan.financebuddy.ui.theme.GradientStart
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.shejan.financebuddy.ui.theme.IncomeGreen
import com.shejan.financebuddy.ui.theme.TextMuted
import com.shejan.financebuddy.ui.theme.TextPrimary
import com.shejan.financebuddy.ui.theme.TextSecondary
import com.shejan.financebuddy.ui.theme.TransferYellow
import java.text.DecimalFormat

// ─────────────────────────────────────────────────────────────
// Budget Screen — per-category monthly spending limits
// ─────────────────────────────────────────────────────────────

private val expenseCategories = listOf(
    "Food", "Groceries", "Rent", "Utilities", "Travel",
    "Shopping", "Entertainment", "Medical", "Other"
)

private val categoryColors = mapOf(
    "Food"          to "#FF5C7C",
    "Groceries"     to "#00C897",
    "Rent"          to "#0096FF",
    "Utilities"     to "#FFBD2E",
    "Travel"        to "#7C5CFC",
    "Shopping"      to "#FF7A45",
    "Entertainment" to "#00D4AA",
    "Medical"       to "#FF3B6F",
    "Other"         to "#8A94B2"
)

private val niceColors = listOf(
    "#FF5C7C", "#00C897", "#0096FF", "#FFBD2E", "#7C5CFC",
    "#FF7A45", "#00D4AA", "#FF3B6F", "#E040FB", "#00E5FF",
    "#FFB300", "#1B5E20", "#3F51B5", "#9C27B0", "#00BCD4"
)

private fun getCategoryColor(category: String): String {
    val existingColor = categoryColors[category]
    if (existingColor != null) return existingColor
    val index = Math.abs(category.hashCode()) % niceColors.size
    return niceColors[index]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    budgets: List<BudgetEntity>,
    spentByCategory: Map<String, Double>,
    onAddBudget: (BudgetEntity) -> Unit,
    onDeleteBudget: (BudgetEntity) -> Unit
) {
    val currencyFormat = remember { DecimalFormat("##,##,##0.00") }
    val totalBudgeted  = budgets.sumOf { it.limitAmount }
    val totalSpent     = budgets.sumOf { spentByCategory[it.category] ?: 0.0 }

    var showAddSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
        // Ambient top glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AccentBlue.copy(alpha = 0.07f), Color.Transparent)
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {},
            floatingActionButton = {
                FloatingActionButton(
                    onClick        = { showAddSheet = true },
                    containerColor = Color.Transparent,
                    contentColor   = BackgroundDark,
                    shape          = CircleShape,
                    modifier       = Modifier.size(56.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(colors = listOf(AccentBlue, AccentTeal))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Budget", tint = BackgroundDark)
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding()),
                contentPadding      = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Spacer matching top bar height (64.dp) + status bar padding
                item {
                    Spacer(modifier = Modifier.statusBarsPadding().height(64.dp))
                }
                // ── Monthly Overview Arc Card ──────────────────────
                item {
                    MonthlyOverviewCard(
                        totalBudgeted  = totalBudgeted,
                        totalSpent     = totalSpent,
                        currencyFormat = currencyFormat
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ── Section header ──────────────────────────────────
                item {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text       = "Category Budgets",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = TextPrimary
                        )
                        Text(
                            text  = "${budgets.size} set",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── Empty state ─────────────────────────────────────
                if (budgets.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(CardDark)
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "📊", fontSize = 36.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text      = "No budgets set yet",
                                    color     = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize  = 16.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text      = "Tap + to set a spending limit\nfor each category",
                                    color     = TextMuted,
                                    fontSize  = 13.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }

                // ── Budget item cards ───────────────────────────────
                items(budgets, key = { it.id }) { budget ->
                    val spent = spentByCategory[budget.category] ?: 0.0
                    BudgetItemCard(
                        budget         = budget,
                        spent          = spent,
                        currencyFormat = currencyFormat,
                        onDelete       = { onDeleteBudget(budget) }
                    )
                }
            }
        }

        // Add Budget Bottom Sheet
        if (showAddSheet) {
            AddBudgetSheet(
                sheetState        = sheetState,
                existingCategories = budgets.map { it.category },
                onDismiss         = { showAddSheet = false },
                onSave            = { budget ->
                    onAddBudget(budget)
                    showAddSheet = false
                }
            )
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
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text       = "Budget",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary
                )
                Text(
                    text  = "This Month",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Monthly Overview Arc Card
// ─────────────────────────────────────────────────────────────

@Composable
fun MonthlyOverviewCard(
    totalBudgeted: Double,
    totalSpent: Double,
    currencyFormat: DecimalFormat
) {
    val progress = if (totalBudgeted > 0) (totalSpent / totalBudgeted).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animatedProgress.animateTo(progress, animationSpec = tween(1000))
    }

    val arcColor = when {
        progress >= 1f  -> ExpenseRed
        progress >= 0.9f -> ExpenseRed.copy(alpha = 0.85f)
        progress >= 0.7f -> TransferYellow
        else            -> IncomeGreen
    }

    val remaining = (totalBudgeted - totalSpent).coerceAtLeast(0.0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape  = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Arc chart
            Box(
                modifier         = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val sweepAngle = animatedProgress.value * 270f
                    val strokeW    = 18.dp.toPx()

                    // Background arc track
                    drawArc(
                        color      = CardDarker,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter  = false,
                        style      = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                    // Filled arc
                    if (sweepAngle > 0f) {
                        drawArc(
                            brush      = Brush.sweepGradient(
                                listOf(arcColor.copy(alpha = 0.6f), arcColor)
                            ),
                            startAngle = 135f,
                            sweepAngle = sweepAngle,
                            useCenter  = false,
                            style      = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                    }
                }

                // Center content
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = "${(animatedProgress.value * 100).toInt()}%",
                        style      = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color      = arcColor
                    )
                    Text(
                        text  = "used",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BudgetStatItem(label = "Budgeted",  value = "৳${currencyFormat.format(totalBudgeted)}", color = TextPrimary)
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(DividerColor)
                )
                BudgetStatItem(label = "Spent",     value = "৳${currencyFormat.format(totalSpent)}",    color = arcColor)
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(DividerColor)
                )
                BudgetStatItem(label = "Remaining", value = "৳${currencyFormat.format(remaining)}",     color = IncomeGreen)
            }
        }
    }
}

@Composable
private fun BudgetStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = value,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color      = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Budget Item Card — per category with animated progress bar
// ─────────────────────────────────────────────────────────────

@Composable
fun BudgetItemCard(
    budget: BudgetEntity,
    spent: Double,
    currencyFormat: DecimalFormat,
    onDelete: () -> Unit
) {
    val progress = if (budget.limitAmount > 0) (spent / budget.limitAmount).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress = remember(spent) { Animatable(0f) }
    LaunchedEffect(progress) {
        animatedProgress.animateTo(progress, animationSpec = tween(800))
    }

    val accentColor = remember {
        try { Color(android.graphics.Color.parseColor(budget.colorHex)) }
        catch (e: Exception) { AccentTeal }
    }

    val barColor = when {
        progress >= 1f   -> ExpenseRed
        progress >= 0.9f -> ExpenseRed.copy(alpha = 0.85f)
        progress >= 0.7f -> TransferYellow
        else             -> accentColor
    }

    val overBudget = spent > budget.limitAmount
    val remaining  = (budget.limitAmount - spent).coerceAtLeast(0.0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = if (overBudget)
            androidx.compose.foundation.BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.4f))
        else
            androidx.compose.foundation.BorderStroke(1.dp, DividerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Category color dot
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text       = budget.category,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = TextPrimary
                    )
                    if (overBudget) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ExpenseRed.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text      = "Over Budget",
                                fontSize  = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color     = ExpenseRed
                            )
                        }
                    }
                }
                IconButton(
                    onClick  = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.Delete,
                        contentDescription = "Delete budget",
                        tint               = TextMuted,
                        modifier           = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(CardDarker)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.value)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                colors = if (overBudget)
                                    listOf(ExpenseRed.copy(alpha = 0.7f), ExpenseRed)
                                else
                                    listOf(accentColor.copy(alpha = 0.7f), barColor)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text  = "৳${currencyFormat.format(spent)} spent",
                    style = MaterialTheme.typography.labelSmall,
                    color = barColor
                )
                Text(
                    text  = if (overBudget)
                        "৳${currencyFormat.format(spent - budget.limitAmount)} over"
                    else
                        "৳${currencyFormat.format(remaining)} left of ৳${currencyFormat.format(budget.limitAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (overBudget) ExpenseRed else TextSecondary
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Add Budget Bottom Sheet
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetSheet(
    sheetState: androidx.compose.material3.SheetState,
    existingCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (BudgetEntity) -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("finance_buddy_prefs", Context.MODE_PRIVATE) }
    val customExpenseCategories = remember {
        sharedPreferences.getString("custom_expense_categories", "")
            ?.split("|")
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
    }
    val allExpenseCategories = remember(customExpenseCategories) {
        expenseCategories + customExpenseCategories
    }

    var selectedCategory by remember(allExpenseCategories) {
        val availableCats = allExpenseCategories.filter { it !in existingCategories }
        mutableStateOf(if (availableCats.isNotEmpty()) availableCats.first() else "")
    }
    var limitAmount      by remember { mutableStateOf("") }
    var error            by remember { mutableStateOf<String?>(null) }

    // Available categories = those not already budgeted
    val available = remember(allExpenseCategories, existingCategories) {
        allExpenseCategories.filter { it !in existingCategories }
    }

    // If all categories are budgeted, close the sheet
    LaunchedEffect(available) {
        if (available.isEmpty()) onDismiss()
        else if (selectedCategory !in available) selectedCategory = available.first()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = CardDarker,
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                text      = "Set Budget Limit",
                style     = MaterialTheme.typography.titleLarge,
                color     = TextPrimary,
                modifier  = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Category selector chips
            Text(
                text  = "Category",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            androidx.compose.foundation.layout.FlowRow(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp)
            ) {
                available.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    val catColor = try {
                        Color(android.graphics.Color.parseColor(getCategoryColor(cat)))
                    } catch (e: Exception) { AccentTeal }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) catColor.copy(alpha = 0.2f) else CardDark
                            )
                            .then(
                                if (isSelected) Modifier.then(
                                    Modifier.clip(RoundedCornerShape(12.dp))
                                ) else Modifier
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text       = cat,
                            fontSize   = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color      = if (isSelected) catColor else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Amount input
            Text(
                text  = "Monthly Limit (৳)",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value         = limitAmount,
                onValueChange = { limitAmount = it.filter { c -> c.isDigit() || c == '.' }; error = null },
                modifier      = Modifier.fillMaxWidth(),
                placeholder   = { Text("e.g. 5000", color = TextMuted) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine    = true,
                shape         = RoundedCornerShape(16.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = AccentTeal,
                    unfocusedBorderColor = DividerColor,
                    focusedTextColor     = TextPrimary,
                    unfocusedTextColor   = TextPrimary,
                    cursorColor          = AccentTeal
                )
            )

            if (error != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = error!!, color = ExpenseRed, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Save button
            Button(
                onClick = {
                    val amount = limitAmount.toDoubleOrNull()
                    when {
                        amount == null || amount <= 0 -> error = "Please enter a valid amount"
                        else -> {
                            onSave(
                                BudgetEntity(
                                    category    = selectedCategory,
                                    limitAmount = amount,
                                    colorHex    = getCategoryColor(selectedCategory)
                                )
                            )
                        }
                    }
                },
                modifier       = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape          = RoundedCornerShape(16.dp),
                colors         = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(colors = listOf(GradientStart, GradientEnd))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "Save Budget",
                        style = MaterialTheme.typography.titleMedium,
                        color = BackgroundDark
                    )
                }
            }
        }
    }
}
