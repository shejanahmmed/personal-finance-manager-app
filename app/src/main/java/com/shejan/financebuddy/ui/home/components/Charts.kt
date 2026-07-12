package com.shejan.financebuddy.ui.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.shejan.financebuddy.ui.theme.TextSecondary

// ─────────────────────────────────────────────────────────────
// Custom 7-Day Expense Bar Chart
// ─────────────────────────────────────────────────────────────

@Composable
fun ExpenseBarChart(
    days: List<String>,
    amounts: List<Double>,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(amounts) {
        animProgress.animateTo(1f, animationSpec = tween(1000))
    }

    val textMeasurer = rememberTextMeasurer()
    val maxVal = remember(amounts) { (amounts.maxOrNull() ?: 1.0).coerceAtLeast(1.0) }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val leftPadding = 48.dp.toPx()   // Space for Y-axis labels + margin
            val rightPadding = 16.dp.toPx()  // Space to prevent right bar clipping
            val topPadding = 12.dp.toPx()    // Space to prevent top label clipping
            val bottomPadding = 24.dp.toPx() // Space for day labels on X-axis

            val chartW = w - leftPadding - rightPadding
            val chartH = h - topPadding - bottomPadding
            val numBars = amounts.size
            if (numBars == 0) return@Canvas

            val spacing = chartW / (numBars + 1)
            val barW = 14.dp.toPx()

            // ── Grid Lines and Y-Axis Labels ──────────────────────
            val gridLines = 3
            for (i in 0..gridLines) {
                val y = topPadding + chartH * (i / gridLines.toFloat())
                val valAtLine = maxVal * (1f - (i / gridLines.toFloat()))
                
                // Draw grid line
                drawLine(
                    color       = DividerColor.copy(alpha = 0.8f),
                    start       = Offset(leftPadding, y),
                    end         = Offset(w - rightPadding, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect  = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                // Format number compactly (e.g. 1.5K, 500)
                val labelText = if (valAtLine >= 1000) {
                    val k = valAtLine / 1000.0
                    if (k % 1.0 == 0.0) "${k.toInt()}K" else String.format(java.util.Locale.US, "%.1fK", k)
                } else {
                    valAtLine.toInt().toString()
                }

                // Draw Y-Axis label (offset 8.dp from absolute left edge)
                drawText(
                    textMeasurer = textMeasurer,
                    text         = labelText,
                    topLeft      = Offset(8.dp.toPx(), y - 6.dp.toPx()),
                    style        = TextStyle(color = TextSecondary, fontSize = 10.sp)
                )
            }

            // ── Bars and X-Axis Labels ──────────────────────────
            amounts.forEachIndexed { i, amt ->
                val barH = (amt / maxVal).toFloat() * chartH * animProgress.value
                val x = leftPadding + spacing * (i + 1) - barW / 2f
                val y = (topPadding + chartH) - barH

                // Draw Bar
                drawRoundRect(
                    brush        = Brush.verticalGradient(listOf(ExpenseRed.copy(alpha = 0.85f), ExpenseRed.copy(alpha = 0.4f))),
                    topLeft      = Offset(x, y),
                    size         = Size(barW, barH.coerceAtLeast(1.dp.toPx())),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // Day Label
                val day = days.getOrNull(i) ?: ""
                val textLayoutResult = textMeasurer.measure(
                    text  = day,
                    style = TextStyle(color = TextSecondary, fontSize = 11.sp)
                )
                val textW = textLayoutResult.size.width
                drawText(
                    textMeasurer = textMeasurer,
                    text         = day,
                    topLeft      = Offset(leftPadding + spacing * (i + 1) - textW / 2f, topPadding + chartH + 6.dp.toPx()),
                    style        = TextStyle(color = TextSecondary, fontSize = 11.sp)
                )
            }
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
