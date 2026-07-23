package com.shejan.financebuddy.ui.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.ui.theme.AccentBlue
import com.shejan.financebuddy.ui.theme.AccentTeal
import com.shejan.financebuddy.ui.theme.DividerColor
import com.shejan.financebuddy.ui.theme.ExpenseRed
import com.shejan.financebuddy.ui.theme.TextMuted
import androidx.compose.ui.platform.LocalDensity
import com.shejan.financebuddy.ui.theme.TextSecondary

// ─────────────────────────────────────────────────────────────
// Custom 7-Day Expense Bar Chart — Premium Redesign
// ─────────────────────────────────────────────────────────────

// Curated per-bar color palette (7 harmonious accent colors)
private val barPalette = listOf(
    Color(0xFF00D4AA), // Teal
    Color(0xFF0096FF), // Blue
    Color(0xFF7C5CFC), // Purple
    Color(0xFFFF5C7C), // Rose
    Color(0xFFFFBD2E), // Amber
    Color(0xFF00C897), // Green
    Color(0xFFFF8C42), // Orange
)

@Composable
fun ExpenseBarChart(
    days: List<String>,
    amounts: List<Double>,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(amounts) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing))
    }

    var selectedBar by remember { mutableStateOf(-1) }
    val textMeasurer = rememberTextMeasurer()
    val maxVal = remember(amounts) { (amounts.maxOrNull() ?: 1.0).coerceAtLeast(1.0) }
    val density = LocalDensity.current

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(amounts) {
                    detectTapGestures { tapOffset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        // Convert dp to px using density — this MUST match the Canvas drawing below
                        val leftPad  = with(density) { 52.dp.toPx() }
                        val rightPad = with(density) { 16.dp.toPx() }
                        val topPad   = with(density) { 28.dp.toPx() }
                        val botPad   = with(density) { 32.dp.toPx() }
                        val chartW   = w - leftPad - rightPad
                        val numBars  = amounts.size
                        if (numBars == 0) return@detectTapGestures
                        val slotW = chartW / numBars
                        // Use the full slot width as tap target for easy tapping
                        var hit = -1
                        amounts.forEachIndexed { i, _ ->
                            val slotLeft  = leftPad + slotW * i
                            val slotRight = leftPad + slotW * (i + 1)
                            if (tapOffset.x in slotLeft..slotRight) hit = i
                        }
                        selectedBar = if (selectedBar == hit) -1 else hit
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val leftPad  = 52.dp.toPx()
            val rightPad = 16.dp.toPx()
            val topPad   = 28.dp.toPx()
            val botPad   = 32.dp.toPx()

            val chartW = w - leftPad - rightPad
            val chartH = h - topPad - botPad
            val numBars = amounts.size
            if (numBars == 0) return@Canvas

            val slotW = chartW / numBars
            val barW  = (slotW * 0.52f).coerceAtMost(28.dp.toPx())

            // ── Horizontal Grid Lines + Y-axis Labels ──────────────
            val gridSteps = 4
            for (i in 0..gridSteps) {
                val y = topPad + chartH * (i.toFloat() / gridSteps)
                val valAtLine = maxVal * (1f - i.toFloat() / gridSteps)

                // Dashed grid line
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(leftPad, y),
                    end   = Offset(w - rightPad, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect  = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
                )

                // Y label
                val labelText = when {
                    valAtLine >= 100_000 -> String.format(java.util.Locale.US, "%.0fL", valAtLine / 100_000)
                    valAtLine >= 1_000   -> {
                        val k = valAtLine / 1000.0
                        if (k % 1.0 == 0.0) "${k.toInt()}K" else String.format(java.util.Locale.US, "%.1fK", k)
                    }
                    else -> valAtLine.toInt().toString()
                }
                drawText(
                    textMeasurer = textMeasurer,
                    text         = labelText,
                    topLeft      = Offset(4.dp.toPx(), y - 7.dp.toPx()),
                    style        = TextStyle(
                        color    = Color.White.copy(alpha = 0.35f),
                        fontSize = 9.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                )
            }

            // ── Bars ──────────────────────────────────────────────
            amounts.forEachIndexed { i, amt ->
                val isSelected    = selectedBar == i
                val isAnySelected = selectedBar >= 0
                val dimAlpha      = if (isAnySelected && !isSelected) 0.28f else 1f

                val barColor  = barPalette[i % barPalette.size]
                val barH  = (amt / maxVal).toFloat() * chartH * animProgress.value
                val cx    = leftPad + slotW * i + slotW / 2f
                val barX  = cx - barW / 2f
                val barY  = topPad + chartH - barH

                // Glow shadow under selected bar
                if (isSelected && barH > 0f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(barColor.copy(alpha = 0.35f), Color.Transparent),
                            startY = barY - 6.dp.toPx(),
                            endY   = topPad + chartH + 12.dp.toPx()
                        ),
                        topLeft      = Offset(barX - 6.dp.toPx(), barY - 6.dp.toPx()),
                        size         = Size(barW + 12.dp.toPx(), barH + 18.dp.toPx()),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                }

                // Main bar with gradient (no slime stripe)
                if (barH > 0f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                barColor.copy(alpha = dimAlpha),
                                barColor.copy(alpha = dimAlpha * 0.40f)
                            ),
                            startY = barY,
                            endY   = topPad + chartH
                        ),
                        topLeft      = Offset(barX, barY),
                        size         = Size(barW, barH.coerceAtLeast(3.dp.toPx())),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                } else {
                    // Empty bar placeholder line
                    drawRoundRect(
                        color        = Color.White.copy(alpha = 0.08f),
                        topLeft      = Offset(barX, topPad + chartH - 3.dp.toPx()),
                        size         = Size(barW, 3.dp.toPx()),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                }

                // ── Tooltip above selected bar ────────────────────
                if (isSelected && amt > 0.0) {
                    val amtText = "৳${
                        when {
                            amt >= 100_000 -> String.format(java.util.Locale.US, "%.1fL", amt / 100_000)
                            amt >= 1_000   -> String.format(java.util.Locale.US, "%.1fK", amt / 1000)
                            else           -> amt.toInt().toString()
                        }
                    }"
                    val tooltipResult = textMeasurer.measure(
                        text  = amtText,
                        style = TextStyle(
                            color      = Color.White,
                            fontSize   = 11.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    )
                    val tW  = tooltipResult.size.width + 16.dp.toPx()
                    val tH  = tooltipResult.size.height + 10.dp.toPx()
                    var tX  = cx - tW / 2f
                    tX = tX.coerceIn(leftPad, w - rightPad - tW)
                    val tY  = (barY - tH - 6.dp.toPx()).coerceAtLeast(4.dp.toPx())

                    // Tooltip bubble
                    drawRoundRect(
                        color        = barColor,
                        topLeft      = Offset(tX, tY),
                        size         = Size(tW, tH),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                    // Tooltip text
                    drawText(
                        textMeasurer = textMeasurer,
                        text         = amtText,
                        topLeft      = Offset(tX + 8.dp.toPx(), tY + 5.dp.toPx()),
                        style        = TextStyle(
                            color      = Color.White,
                            fontSize   = 11.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    )
                    // Small caret triangle
                    val caretSize = 5.dp.toPx()
                    val caretPath = Path().apply {
                        moveTo(cx - caretSize, tY + tH)
                        lineTo(cx + caretSize, tY + tH)
                        lineTo(cx, tY + tH + caretSize)
                        close()
                    }
                    drawPath(caretPath, color = barColor)
                }

                // ── X-Axis Day Label ──────────────────────────────
                val dayLabel = days.getOrNull(i) ?: ""
                val dayResult = textMeasurer.measure(
                    text  = dayLabel,
                    style = TextStyle(
                        color      = if (isSelected) barColor else Color.White.copy(alpha = 0.40f),
                        fontSize   = 10.sp,
                        fontWeight = if (isSelected)
                            androidx.compose.ui.text.font.FontWeight.Bold
                        else
                            androidx.compose.ui.text.font.FontWeight.Normal
                    )
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text         = dayLabel,
                    topLeft      = Offset(
                        cx - dayResult.size.width / 2f,
                        topPad + chartH + 8.dp.toPx()
                    ),
                    style = TextStyle(
                        color      = if (isSelected) barColor else Color.White.copy(alpha = 0.40f),
                        fontSize   = 10.sp,
                        fontWeight = if (isSelected)
                            androidx.compose.ui.text.font.FontWeight.Bold
                        else
                            androidx.compose.ui.text.font.FontWeight.Normal
                    )
                )
            }

            // ── X-Axis baseline ────────────────────────────────────
            drawLine(
                color       = Color.White.copy(alpha = 0.10f),
                start       = Offset(leftPad, topPad + chartH),
                end         = Offset(w - rightPad, topPad + chartH),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}


// ─────────────────────────────────────────────────────────────
// Custom 30-Day Balance Trend Line Chart (Bezier Curve)
// ─────────────────────────────────────────────────────────────

@Composable
fun BalanceTrendLineChart(
    balances: List<Double>,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(balances) {
        animProgress.animateTo(1f, animationSpec = tween(1200))
    }

    val textMeasurer = rememberTextMeasurer()
    val minVal = remember(balances) { balances.minOrNull() ?: 0.0 }
    val maxVal = remember(balances) { (balances.maxOrNull() ?: 1.0).coerceAtLeast(minVal + 1.0) }
    val valRange = maxVal - minVal

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val leftPadding = 48.dp.toPx()   // Space for Y-axis labels + margin
            val rightPadding = 16.dp.toPx()  // Space to prevent line/dot clipping on the right
            val topPadding = 16.dp.toPx()    // Space to prevent line/dot clipping on the top
            val bottomPadding = 16.dp.toPx() // Space to prevent line clipping on the bottom

            val chartW = w - leftPadding - rightPadding
            val chartH = h - topPadding - bottomPadding
            val pointsCount = balances.size
            if (pointsCount < 2) return@Canvas

            // ── Grid Lines and Y-Axis Labels ────────────────────
            val gridLines = 3
            for (i in 0..gridLines) {
                val y = topPadding + chartH * (i / gridLines.toFloat())
                val valAtLine = maxVal - (valRange * (i / gridLines.toFloat()))

                drawLine(
                    color       = DividerColor.copy(alpha = 0.5f),
                    start       = Offset(leftPadding, y),
                    end         = Offset(w - rightPadding, y),
                    strokeWidth = 1.dp.toPx()
                )

                val labelText = if (valAtLine >= 1000) {
                    val k = valAtLine / 1000.0
                    if (k % 1.0 == 0.0) "${k.toInt()}K" else String.format(java.util.Locale.US, "%.1fK", k)
                } else {
                    valAtLine.toInt().toString()
                }

                drawText(
                    textMeasurer = textMeasurer,
                    text         = labelText,
                    topLeft      = Offset(8.dp.toPx(), y - 6.dp.toPx()),
                    style        = TextStyle(color = TextSecondary, fontSize = 10.sp)
                )
            }

            // ── Path Generation ─────────────────────────────────
            val xStep = chartW / (pointsCount - 1)
            val points = balances.mapIndexed { idx, bal ->
                val normalizedY = ((bal - minVal) / valRange).toFloat()
                Offset(leftPadding + idx * xStep, (topPadding + chartH) - (normalizedY * chartH * animProgress.value))
            }

            // Draw line curve via bezier
            val strokePath = Path().apply {
                if (points.isNotEmpty()) {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        // Control points for smooth bezier curve
                        val cx1 = prev.x + (curr.x - prev.x) / 2f
                        val cy1 = prev.y
                        val cx2 = prev.x + (curr.x - prev.x) / 2f
                        val cy2 = curr.y
                        cubicTo(cx1, cy1, cx2, cy2, curr.x, curr.y)
                    }
                }
            }

            // Draw filled gradient area under the curve
            val fillPath = Path().apply {
                addPath(strokePath)
                lineTo(w - rightPadding, topPadding + chartH)
                lineTo(leftPadding, topPadding + chartH)
                close()
            }

            // Draw area gradient
            drawPath(
                path  = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(AccentTeal.copy(alpha = 0.15f), Color.Transparent),
                    startY = topPadding,
                    endY   = topPadding + chartH
                )
            )

            // Draw path stroke
            drawPath(
                path  = strokePath,
                brush = Brush.linearGradient(
                    colors = listOf(AccentTeal, AccentBlue),
                    start  = Offset(leftPadding, size.height / 2f),
                    end    = Offset(w - rightPadding, size.height / 2f)
                ),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw dynamic nodes on endpoints
            if (points.isNotEmpty()) {
                val lastPoint = points.last()
                drawCircle(
                    color  = AccentTeal,
                    radius = 5.dp.toPx(),
                    center = lastPoint
                )
                drawCircle(
                    color  = AccentTeal.copy(alpha = 0.25f),
                    radius = 12.dp.toPx(),
                    center = lastPoint
                )
            }
        }
    }
}
// Add explicit StrokeCap import helper
typealias StrokeCap = androidx.compose.ui.graphics.StrokeCap
