package com.shejan.financebuddy.ui.statistics

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.AccountEntity
import com.shejan.financebuddy.data.db.TransactionEntity
import com.shejan.financebuddy.ui.theme.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

// ─── Color palette for charts ───────────────────────────────────────────
private val chartPalette = listOf(
    Color(0xFF00D4AA), Color(0xFF0096FF), Color(0xFF7C5CFC),
    Color(0xFFFF5C7C), Color(0xFFFFBD2E), Color(0xFF00C897),
    Color(0xFFFF8C42), Color(0xFF44B4FF), Color(0xFFC97AFF),
    Color(0xFFFF6B6B)
)

@Composable
fun StatisticsScreen(
    allTransactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val currencyFormat = remember { DecimalFormat("##,##,##0.00") }

    var selectedPeriod by remember { mutableStateOf("MONTH") }

    val periodTransactions = remember(allTransactions, selectedPeriod) {
        val now = Calendar.getInstance()
        val startTime = when (selectedPeriod) {
            "WEEK" -> {
                val cal = now.clone() as Calendar
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            "MONTH" -> {
                val cal = now.clone() as Calendar
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            "YEAR" -> {
                val cal = now.clone() as Calendar
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            else -> 0L
        }
        allTransactions.filter { it.timestamp >= startTime }
    }

    val totalIncome = remember(periodTransactions) {
        periodTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }
    val totalExpense = remember(periodTransactions) {
        periodTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }
    val netSavings = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) ((netSavings / totalIncome) * 100).coerceAtLeast(0.0) else 0.0

    val daysInPeriod = remember(selectedPeriod) {
        val cal = Calendar.getInstance()
        when (selectedPeriod) {
            "WEEK" -> 7
            "MONTH" -> cal.get(Calendar.DAY_OF_MONTH)
            "YEAR" -> cal.get(Calendar.DAY_OF_YEAR)
            else -> 30
        }.coerceAtLeast(1)
    }

    val categoryExpenses = remember(periodTransactions) {
        periodTransactions.filter { it.type == "EXPENSE" }
            .groupBy { it.category.ifBlank { "Other" } }
            .mapValues { (_, v) -> v.sumOf { it.amount } }
            .toList().sortedByDescending { it.second }
    }

    // Last 6 months income/expense bar data
    val monthlyBarData = remember(allTransactions) {
        val cal = Calendar.getInstance()
        val monthNames = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        (5 downTo 0).map { offset ->
            val c = cal.clone() as Calendar
            c.add(Calendar.MONTH, -offset)
            c.set(Calendar.DAY_OF_MONTH, 1)
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            val start = c.timeInMillis
            c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
            c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59)
            c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999)
            val end = c.timeInMillis
            val txs = allTransactions.filter { it.timestamp in start..end }
            val inc = txs.filter { it.type == "INCOME" }.sumOf { it.amount }
            val exp = txs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val month = c.get(Calendar.MONTH)
            Triple(monthNames[month], inc, exp)
        }
    }

    // 30-day balance trend
    val balanceTrendData = remember(allTransactions) {
        val totalCurrentBalance = accounts.sumOf { it.balance }
        val today = Calendar.getInstance()
        (29 downTo 0).map { daysAgo ->
            val cal = today.clone() as Calendar
            cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
            val cutoff = cal.timeInMillis
            val netAfter = allTransactions.filter { it.timestamp > cutoff }.sumOf { tx ->
                when (tx.type) { "INCOME" -> tx.amount; "EXPENSE" -> -tx.amount; else -> 0.0 }
            }
            totalCurrentBalance - netAfter
        }
    }

    val topTransactions = remember(periodTransactions) {
        periodTransactions.sortedByDescending { it.amount }.take(5)
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(240.dp).background(
                Brush.verticalGradient(listOf(AccentTeal.copy(alpha = 0.08f), Color.Transparent))
            )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(CardDarker).border(1.dp, DividerColor, CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Financial Statistics", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Visual analytics & spending insights", color = TextMuted, fontSize = 12.sp)
                }
            }

            // ── Timeframe Selector ────────────────────────────────────────
            val periodOptions = listOf("WEEK" to "This Week", "MONTH" to "This Month", "YEAR" to "This Year", "ALL" to "All Time")
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(periodOptions) { (key, label) ->
                    val isSelected = selectedPeriod == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) AccentTeal.copy(alpha = 0.18f) else CardDark)
                            .border(1.dp, if (isSelected) AccentTeal else DividerColor, RoundedCornerShape(20.dp))
                            .clickable { selectedPeriod = key }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
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

            // ── Scrollable Content ─────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SECTION 1: KPI Metric Tiles
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricTile("Total Income", "৳${currencyFormat.format(totalIncome)}", IncomeGreen, Modifier.weight(1f))
                        MetricTile("Total Expense", "৳${currencyFormat.format(totalExpense)}", ExpenseRed, Modifier.weight(1f))
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricTile(
                            "Net Savings",
                            "${if (netSavings >= 0) "+" else ""}৳${currencyFormat.format(netSavings)}",
                            if (netSavings >= 0) AccentTeal else ExpenseRed,
                            Modifier.weight(1f)
                        )
                        MetricTile(
                            "Savings Rate",
                            "${String.format(Locale.US, "%.1f", savingsRate)}%",
                            AccentBlue,
                            Modifier.weight(1f)
                        )
                    }
                }

                // SECTION 2: Monthly Income vs Expense Bar Chart
                item {
                    StatCard(title = "6-Month Income vs Expense", subtitle = "Tap any bar to inspect") {
                        MonthlyComparisonBarChart(
                            data = monthlyBarData,
                            modifier = Modifier.fillMaxWidth().height(200.dp).padding(top = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            LegendDot(IncomeGreen, "Income")
                            LegendDot(ExpenseRed, "Expense")
                        }
                    }
                }

                // SECTION 3: Balance Trend Line Chart
                item {
                    StatCard(title = "30-Day Balance Trend", subtitle = "Running total balance over the past month") {
                        if (balanceTrendData.distinct().size > 1) {
                            BalanceTrendChart(
                                balances = balanceTrendData,
                                modifier = Modifier.fillMaxWidth().height(180.dp).padding(top = 8.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Not enough data to show trend", color = TextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // SECTION 4: Category Donut Chart
                if (categoryExpenses.isNotEmpty()) {
                    item {
                        StatCard(title = "Expense by Category", subtitle = "Distribution of spending across categories") {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                DonutChart(
                                    data = categoryExpenses,
                                    modifier = Modifier.size(170.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    categoryExpenses.take(6).forEachIndexed { index, (cat, amt) ->
                                        val pct = if (totalExpense > 0) (amt / totalExpense * 100) else 0.0
                                        val color = chartPalette[index % chartPalette.size]
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(color))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    cat, color = TextPrimary, fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    "${String.format(Locale.US, "%.1f", pct)}%",
                                                    color = TextMuted, fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                    if (categoryExpenses.size > 6) {
                                        Text("+${categoryExpenses.size - 6} more categories", color = TextMuted, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 5: Largest Transactions
                if (topTransactions.isNotEmpty()) {
                    item {
                        StatCard(title = "Largest Transactions", subtitle = "Top 5 by amount in selected period") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                topTransactions.forEach { tx ->
                                    val isIncome = tx.type == "INCOME"
                                    val txColor = if (isIncome) IncomeGreen else if (tx.type == "TRANSFER") TransferYellow else ExpenseRed
                                    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                                    .background(txColor.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isIncome) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                                    contentDescription = null, tint = txColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = tx.category.ifBlank { tx.type },
                                                    color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                                )
                                                Text(dateStr, color = TextMuted, fontSize = 10.5.sp)
                                            }
                                        }
                                        Text(
                                            text = "${if (isIncome) "+" else "-"}৳${DecimalFormat("##,##,##0.00").format(tx.amount)}",
                                            color = txColor, fontSize = 13.sp, fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Stat Card wrapper ─────────────────────────────────────────────────
@Composable
private fun StatCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, DividerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextMuted, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

// ─── KPI Metric tile ─────────────────────────────────────────────────
@Composable
private fun MetricTile(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(title, fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                color = color, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── Legend dot ───────────────────────────────────────────────────────
@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(5.dp))
        Text(label, color = TextMuted, fontSize = 11.sp)
    }
}

// ─── Monthly Income vs Expense Grouped Bar Chart ───────────────────────
@Composable
private fun MonthlyComparisonBarChart(
    data: List<Triple<String, Double, Double>>, // month, income, expense
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))
    }
    val textMeasurer = rememberTextMeasurer()
    val maxVal = remember(data) {
        data.maxOfOrNull { maxOf(it.second, it.third) }?.coerceAtLeast(1.0) ?: 1.0
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val leftPad = 52.dp.toPx()
        val rightPad = 12.dp.toPx()
        val topPad = 16.dp.toPx()
        val botPad = 28.dp.toPx()
        val chartW = w - leftPad - rightPad
        val chartH = h - topPad - botPad
        val n = data.size
        if (n == 0) return@Canvas

        // Grid lines + Y labels
        for (i in 0..4) {
            val y = topPad + chartH * (i / 4f)
            val v = maxVal * (1f - i / 4f)
            drawLine(
                color = ChartGridLine,
                start = Offset(leftPad, y), end = Offset(w - rightPad, y),
                strokeWidth = 1.dp.toPx()
            )
            val lbl = when {
                v >= 100_000 -> String.format(Locale.US, "%.0fL", v / 100_000)
                v >= 1_000 -> String.format(Locale.US, "%.0fK", v / 1000)
                else -> v.toInt().toString()
            }
            drawText(
                textMeasurer, lbl,
                topLeft = Offset(10.dp.toPx(), y - 7.dp.toPx()),
                style = TextStyle(color = ChartLabel, fontSize = 9.sp)
            )
        }

        val slotW = chartW / n
        val barW = (slotW * 0.36f).coerceAtMost(18.dp.toPx())
        val gap = 3.dp.toPx()

        data.forEachIndexed { i, (month, income, expense) ->
            val slotCenter = leftPad + slotW * i + slotW / 2f

            // Income bar (left of center)
            val incH = (income / maxVal).toFloat() * chartH * animProgress.value
            val incX = slotCenter - barW - gap / 2f
            if (incH > 0f) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(IncomeGreen, IncomeGreen.copy(alpha = 0.4f)),
                        startY = topPad + chartH - incH, endY = topPad + chartH
                    ),
                    topLeft = Offset(incX, topPad + chartH - incH),
                    size = Size(barW, incH.coerceAtLeast(2.dp.toPx())),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }

            // Expense bar (right of center)
            val expH = (expense / maxVal).toFloat() * chartH * animProgress.value
            val expX = slotCenter + gap / 2f
            if (expH > 0f) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(ExpenseRed, ExpenseRed.copy(alpha = 0.4f)),
                        startY = topPad + chartH - expH, endY = topPad + chartH
                    ),
                    topLeft = Offset(expX, topPad + chartH - expH),
                    size = Size(barW, expH.coerceAtLeast(2.dp.toPx())),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }

            // X-axis label
            val mResult = textMeasurer.measure(
                month,
                style = TextStyle(color = ChartLabel, fontSize = 9.sp)
            )
            drawText(
                textMeasurer, month,
                topLeft = Offset(slotCenter - mResult.size.width / 2f, topPad + chartH + 7.dp.toPx()),
                style = TextStyle(color = ChartLabel, fontSize = 9.sp)
            )
        }

        // X baseline
        drawLine(
            color = ChartGridLine,
            start = Offset(leftPad, topPad + chartH),
            end = Offset(w - rightPad, topPad + chartH),
            strokeWidth = 1.dp.toPx()
        )
    }
}

// ─── Balance Trend Line Chart ──────────────────────────────────────────
@Composable
private fun BalanceTrendChart(
    balances: List<Double>,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(balances) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(1100, easing = FastOutSlowInEasing))
    }
    val textMeasurer = rememberTextMeasurer()
    val minVal = balances.minOrNull() ?: 0.0
    val maxVal = (balances.maxOrNull() ?: 1.0).coerceAtLeast(minVal + 1.0)
    val range = maxVal - minVal

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val leftPad = 56.dp.toPx()
        val rightPad = 16.dp.toPx()
        val topPad = 12.dp.toPx()
        val botPad = 12.dp.toPx()
        val chartW = w - leftPad - rightPad
        val chartH = h - topPad - botPad
        val n = balances.size
        if (n < 2) return@Canvas

        // Grid
        for (i in 0..3) {
            val y = topPad + chartH * (i / 3f)
            val v = maxVal - range * (i / 3f)
            drawLine(
                color = DividerColor.copy(alpha = 0.4f),
                start = Offset(leftPad, y), end = Offset(w - rightPad, y),
                strokeWidth = 1.dp.toPx()
            )
            val lbl = when {
                v >= 100_000 -> String.format(Locale.US, "%.0fL", v / 100_000)
                v >= 1_000 -> String.format(Locale.US, "%.0fK", v / 1000)
                else -> v.toInt().toString()
            }
            drawText(
                textMeasurer, lbl,
                topLeft = Offset(10.dp.toPx(), y - 7.dp.toPx()),
                style = TextStyle(color = ChartLabel, fontSize = 9.sp)
            )
        }

        val xStep = chartW / (n - 1)
        val animN = (animProgress.value * (n - 1)).toInt().coerceIn(0, n - 2) + 1
        val visibleBalances = balances.take(animN + 1)

        fun toPoint(idx: Int, bal: Double): Offset {
            val nx = ((bal - minVal) / range).toFloat()
            return Offset(
                leftPad + idx * xStep,
                topPad + chartH - nx * chartH
            )
        }

        // Fill area
        val fillPath = Path()
        val visPoints = visibleBalances.mapIndexed { i, b -> toPoint(i, b) }
        if (visPoints.isNotEmpty()) {
            fillPath.moveTo(visPoints[0].x, topPad + chartH)
            fillPath.lineTo(visPoints[0].x, visPoints[0].y)
            for (i in 1 until visPoints.size) {
                val p = visPoints[i - 1]; val c = visPoints[i]
                val cx1 = p.x + (c.x - p.x) / 2f
                fillPath.cubicTo(cx1, p.y, cx1, c.y, c.x, c.y)
            }
            fillPath.lineTo(visPoints.last().x, topPad + chartH)
            fillPath.close()
            drawPath(
                fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(AccentTeal.copy(alpha = 0.25f), Color.Transparent),
                    startY = topPad, endY = topPad + chartH
                )
            )
        }

        // Stroke line
        val strokePath = Path()
        if (visPoints.isNotEmpty()) {
            strokePath.moveTo(visPoints[0].x, visPoints[0].y)
            for (i in 1 until visPoints.size) {
                val p = visPoints[i - 1]; val c = visPoints[i]
                val cx1 = p.x + (c.x - p.x) / 2f
                strokePath.cubicTo(cx1, p.y, cx1, c.y, c.x, c.y)
            }
        }
        drawPath(
            strokePath,
            brush = Brush.linearGradient(
                colors = listOf(AccentTeal, AccentBlue),
                start = Offset(leftPad, 0f), end = Offset(w - rightPad, 0f)
            ),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Endpoint dot
        if (visPoints.isNotEmpty()) {
            val lp = visPoints.last()
            drawCircle(color = AccentTeal.copy(alpha = 0.3f), radius = 10.dp.toPx(), center = lp)
            drawCircle(color = AccentTeal, radius = 4.dp.toPx(), center = lp)
        }
    }
}

// ─── Donut / Pie Chart ────────────────────────────────────────────────
@Composable
private fun DonutChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))
    }
    val total = data.sumOf { it.second }.coerceAtLeast(1.0)

    Canvas(modifier = modifier.padding(8.dp)) {
        val s = minOf(size.width, size.height)
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = s / 2f
        val innerR = s * 0.35f  // donut hole
        val strokeW = outerR - innerR
        var startAngle = -90f

        data.forEachIndexed { i, (_, value) ->
            val sweep = (value / total).toFloat() * 360f * animProgress.value
            val color = chartPalette[i % chartPalette.size]
            val arcR = innerR + strokeW / 2f
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(cx - arcR, cy - arcR),
                size = Size(arcR * 2f, arcR * 2f),
                style = Stroke(width = strokeW, cap = StrokeCap.Butt)
            )
            startAngle += sweep + 1.5f  // small gap between segments
        }

        // Center hole fill — uses BackgroundDark so it blends with card surface in both themes
        drawCircle(color = BackgroundDark, radius = innerR * 0.92f, center = Offset(cx, cy))
    }
}
