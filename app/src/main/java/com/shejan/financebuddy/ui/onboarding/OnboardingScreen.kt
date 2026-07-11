package com.shejan.financebuddy.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.shejan.financebuddy.ui.theme.AccentBlue
import com.shejan.financebuddy.ui.theme.AccentTeal
import com.shejan.financebuddy.ui.theme.BackgroundDark
import com.shejan.financebuddy.ui.theme.GradientEnd
import com.shejan.financebuddy.ui.theme.GradientStart
import com.shejan.financebuddy.ui.theme.IncomeGreen
import com.shejan.financebuddy.ui.theme.TextMuted
import com.shejan.financebuddy.ui.theme.TextPrimary
import com.shejan.financebuddy.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────
// Page definitions
// ─────────────────────────────────────────────────────────────

private val onboardingPages
    @Composable get() = listOf(
        OnboardingPage(
            title    = "Your Money,\nYour Control",
            subtitle = "Track every taka across all your banks and mobile wallets — all in one place.",
            visual   = { Visual1() }
        ),
        OnboardingPage(
            title    = "Built for\nBangladesh",
            subtitle = "Supports bKash, Nagad, Rocket, DBBL, BRAC Bank, and more — right out of the box.",
            visual   = { Visual2() }
        ),
        OnboardingPage(
            title    = "100% Private\n& Offline",
            subtitle = "All your data stays on your device. No cloud, no servers, no compromise.",
            visual   = { Visual3() }
        ),
    )

// ─────────────────────────────────────────────────────────────
// Main composable
// ─────────────────────────────────────────────────────────────

@Composable
fun OnboardingScreenRoot(onFinished: () -> Unit) {
    val pages      = onboardingPages
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope      = rememberCoroutineScope()

    // Fade-in entrance
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(700, easing = EaseOutCubic))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Ambient top glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AccentTeal.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {

            // ── Skip button ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(onClick = onFinished) {
                        Text(
                            text  = "Skip",
                            color = TextMuted,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            // ── Visual pager — fills remaining top half ───────────
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),              // takes all remaining space above mid
            ) { page ->
                Box(
                    modifier         = Modifier
                        .fillMaxSize()
                        .padding(vertical = 16.dp),  // equal breathing room top & bottom
                    contentAlignment = Alignment.Center,
                ) {
                    pages[page].visual()
                }
            }

            // ── Page indicator pills ─────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                repeat(pages.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(if (selected) 28.dp else 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (selected) AccentTeal else TextMuted.copy(alpha = 0.35f)
                            )
                    )
                }
            }

            // ── Text content ─────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
            ) {
                // Slide counter
                Text(
                    text  = "0${pagerState.currentPage + 1} / 0${pages.size}",
                    color = AccentTeal,
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.height(10.dp))
                // Headline
                Text(
                    text  = pages[pagerState.currentPage].title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(12.dp))
                // Subtitle
                Text(
                    text  = pages[pagerState.currentPage].subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // ── CTA button ────────────────────────────────────────
            val isLast = pagerState.currentPage == pages.size - 1
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 32.dp),
            ) {
                Button(
                    onClick = {
                        if (isLast) {
                            onFinished()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier       = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape          = RoundedCornerShape(16.dp),
                    colors         = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(GradientStart, GradientEnd)
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text  = if (isLast) "Get Started  →" else "Next  →",
                            style = MaterialTheme.typography.titleMedium,
                            color = BackgroundDark,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Visual 1 — Orbiting rings (control / holistic overview)
// ─────────────────────────────────────────────────────────────

@Composable
fun Visual1() {
    val inf = rememberInfiniteTransition(label = "v1")
    val rot by inf.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label         = "rot1",
    )
    val pulse by inf.animateFloat(
        initialValue  = 0.85f,
        targetValue   = 1.0f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "pulse1",
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.65f)
            .aspectRatio(1f)
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r  = size.minDimension / 2f

        // Outer faint ring
        drawCircle(
            color  = AccentTeal.copy(alpha = 0.12f),
            radius = r * 0.92f,
            center = Offset(cx, cy),
            style  = Stroke(width = 1.5.dp.toPx()),
        )
        // Middle ring
        drawCircle(
            color  = AccentTeal.copy(alpha = 0.22f),
            radius = r * 0.62f,
            center = Offset(cx, cy),
            style  = Stroke(width = 1.dp.toPx()),
        )
        // Rotating gradient arc
        rotate(rot, pivot = Offset(cx, cy)) {
            drawArc(
                brush      = Brush.sweepGradient(listOf(AccentTeal, AccentBlue, Color.Transparent)),
                startAngle = 0f,
                sweepAngle = 220f,
                useCenter  = false,
                topLeft    = Offset(cx - r * 0.92f, cy - r * 0.92f),
                size       = Size(r * 1.84f, r * 1.84f),
                style      = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        // Orbiting dot — outer ring
        val angleRad = Math.toRadians(rot.toDouble())
        val orbitR1  = r * 0.92f
        val dotPos1  = Offset(cx + orbitR1 * cos(angleRad).toFloat(), cy + orbitR1 * sin(angleRad).toFloat())
        drawCircle(color = AccentTeal.copy(alpha = 0.25f), radius = 14.dp.toPx(), center = dotPos1)
        drawCircle(color = AccentTeal, radius = 6.dp.toPx(), center = dotPos1)
        // Orbiting dot — inner ring (offset 180°)
        val angleRad2 = Math.toRadians((rot + 180.0))
        val orbitR2   = r * 0.62f
        val dotPos2   = Offset(cx + orbitR2 * cos(angleRad2).toFloat(), cy + orbitR2 * sin(angleRad2).toFloat())
        drawCircle(color = AccentBlue, radius = 5.dp.toPx(), center = dotPos2)
        // Central glow
        drawCircle(
            brush  = Brush.radialGradient(
                colors = listOf(AccentTeal.copy(alpha = 0.9f * pulse), Color.Transparent),
                center = Offset(cx, cy),
                radius = r * 0.24f,
            ),
            radius = r * 0.24f,
            center = Offset(cx, cy),
        )
        drawCircle(color = AccentTeal, radius = r * 0.07f * pulse, center = Offset(cx, cy))
    }
}

// ─────────────────────────────────────────────────────────────
// Visual 2 — Grid map with glowing nodes (Bangladesh / local)
// ─────────────────────────────────────────────────────────────

@Composable
fun Visual2() {
    val inf = rememberInfiniteTransition(label = "v2")
    val pulse by inf.animateFloat(
        initialValue  = 0.5f,
        targetValue   = 1.0f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "pulse2",
    )

    val nodes = listOf(
        Offset(0.50f, 0.18f),  // top center
        Offset(0.18f, 0.45f),  // left
        Offset(0.80f, 0.42f),  // right
        Offset(0.35f, 0.68f),  // lower left
        Offset(0.68f, 0.72f),  // lower right
        Offset(0.50f, 0.50f),  // center hub
    )
    val edges = listOf(0 to 5, 1 to 5, 2 to 5, 3 to 5, 4 to 5, 3 to 4, 1 to 3, 0 to 2)

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.72f)
            .aspectRatio(1f)
    ) {
        val w = size.width
        val h = size.height
        val gridStep = w / 7f

        // Grid lines
        for (i in 0..7) {
            drawLine(color = AccentTeal.copy(alpha = 0.07f), start = Offset(i * gridStep, 0f), end = Offset(i * gridStep, h), strokeWidth = 1.dp.toPx())
            drawLine(color = AccentTeal.copy(alpha = 0.07f), start = Offset(0f, i * gridStep), end = Offset(w, i * gridStep), strokeWidth = 1.dp.toPx())
        }
        // Edges
        for ((a, b) in edges) {
            val from = Offset(nodes[a].x * w, nodes[a].y * h)
            val to   = Offset(nodes[b].x * w, nodes[b].y * h)
            drawLine(
                brush       = Brush.linearGradient(colors = listOf(AccentTeal.copy(0.35f), AccentBlue.copy(0.35f)), start = from, end = to),
                start       = from,
                end         = to,
                strokeWidth = 1.5.dp.toPx(),
            )
        }
        // Nodes
        for ((i, n) in nodes.withIndex()) {
            val pos   = Offset(n.x * w, n.y * h)
            val isHub = i == 5
            val r     = if (isHub) 10.dp.toPx() else 5.dp.toPx()
            val col   = if (isHub) AccentTeal else AccentBlue
            drawCircle(color = col.copy(alpha = 0.20f * pulse), radius = r * 2.8f, center = pos)
            drawCircle(color = col, radius = r, center = pos)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Visual 3 — Padlock icon (Private & Offline)
// ─────────────────────────────────────────────────────────────

@Composable
fun Visual3() {
    val inf = rememberInfiniteTransition(label = "v3")

    // Slow pulse for ambient glow rings
    val pulse by inf.animateFloat(
        initialValue  = 0.4f,
        targetValue   = 1.0f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "pulse3",
    )

    // Shackle gently rises and drops (locked → slightly open → locked)
    val shackleOffset by inf.animateFloat(
        initialValue  = 0f,
        targetValue   = -1f,           // barely lifts — stays essentially locked
        animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "shackle3",
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.55f)
            .aspectRatio(1f)
    ) {
        val cx = size.width  / 2f
        val cy = size.height / 2f

        // ── Measurements (all relative to canvas size) ─────────
        val bodyW      = size.width  * 0.36f          // lock body width (reduced to fit in circle)
        val bodyH      = size.height * 0.26f          // lock body height (reduced to fit in circle)
        val bodyCorner = size.width  * 0.06f          // rounded corner radius
        val bodyTop    = cy - size.height * 0.07f     // body top edge (centered vertically)
        val bodyLeft   = cx - bodyW / 2f
        val bodyBottom = bodyTop + bodyH

        val shackleR      = bodyW * 0.32f             // shackle arc radius
        val shackleStroke = size.width * 0.04f        // stroke thickness
        val shackleCx     = cx
        val shackleCy     = bodyTop + shackleOffset.dp.toPx()  // animates upward

        val keyholeR      = bodyW * 0.12f             // outer keyhole circle
        val keyholeSlotH  = bodyH * 0.30f             // slot drop below keyhole center
        val keyholeSlotW  = keyholeR * 0.70f

        // ── Ambient glow rings ──────────────────────────────────
        drawCircle(
            color  = IncomeGreen.copy(alpha = 0.08f * pulse),
            radius = size.minDimension * 0.46f,
            center = Offset(cx, cy),
        )
        drawCircle(
            color  = IncomeGreen.copy(alpha = 0.05f * pulse),
            radius = size.minDimension * 0.40f,
            center = Offset(cx, cy),
            style  = Stroke(width = 1.dp.toPx()),
        )

        // ── Shackle (arc on top of lock body) ──────────────────
        // Draw filled shackle as two thick arcs to form a U shape
        drawArc(
            color      = IncomeGreen.copy(alpha = 0.20f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter  = false,
            topLeft    = Offset(shackleCx - shackleR, shackleCy - shackleR * 1.1f),
            size       = Size(shackleR * 2f, shackleR * 2.2f),
            style      = Stroke(width = shackleStroke * 2.2f, cap = StrokeCap.Round),
        )
        drawArc(
            brush      = Brush.sweepGradient(
                colors = listOf(AccentTeal, IncomeGreen, AccentTeal),
                center = Offset(shackleCx, shackleCy),
            ),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter  = false,
            topLeft    = Offset(shackleCx - shackleR, shackleCy - shackleR * 1.1f),
            size       = Size(shackleR * 2f, shackleR * 2.2f),
            style      = Stroke(width = shackleStroke, cap = StrokeCap.Round),
        )

        // ── Lock body (filled rounded rect) ────────────────────
        // Glow behind body
        drawRoundRect(
            color       = IncomeGreen.copy(alpha = 0.10f * pulse),
            topLeft     = Offset(bodyLeft - 6.dp.toPx(), bodyTop - 6.dp.toPx()),
            size        = Size(bodyW + 12.dp.toPx(), bodyH + 12.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(bodyCorner + 3.dp.toPx()),
        )
        // Body fill
        drawRoundRect(
            brush        = Brush.linearGradient(
                colors = listOf(IncomeGreen.copy(alpha = 0.22f), AccentTeal.copy(alpha = 0.15f)),
                start  = Offset(bodyLeft, bodyTop),
                end    = Offset(bodyLeft + bodyW, bodyBottom),
            ),
            topLeft      = Offset(bodyLeft, bodyTop),
            size         = Size(bodyW, bodyH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(bodyCorner),
        )
        // Body stroke
        drawRoundRect(
            brush        = Brush.linearGradient(
                colors = listOf(IncomeGreen, AccentTeal),
                start  = Offset(bodyLeft, bodyTop),
                end    = Offset(bodyLeft + bodyW, bodyBottom),
            ),
            topLeft      = Offset(bodyLeft, bodyTop),
            size         = Size(bodyW, bodyH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(bodyCorner),
            style        = Stroke(width = 2.dp.toPx()),
        )

        // ── Keyhole — circle ────────────────────────────────────
        val keyholeCenter = Offset(cx, bodyTop + bodyH * 0.40f)
        // Glow
        drawCircle(
            color  = IncomeGreen.copy(alpha = 0.25f * pulse),
            radius = keyholeR * 1.6f,
            center = keyholeCenter,
        )
        // Filled circle
        drawCircle(
            color  = IncomeGreen,
            radius = keyholeR,
            center = keyholeCenter,
        )

        // ── Keyhole — drop slot (trapezoid below circle) ────────
        val slotPath = Path().apply {
            val slotTop    = keyholeCenter.y + keyholeR * 0.5f
            val slotBottom = keyholeCenter.y + keyholeR * 0.5f + keyholeSlotH
            moveTo(cx - keyholeSlotW / 2f, slotTop)
            lineTo(cx + keyholeSlotW / 2f, slotTop)
            lineTo(cx + keyholeSlotW * 0.35f, slotBottom)
            lineTo(cx - keyholeSlotW * 0.35f, slotBottom)
            close()
        }
        drawPath(path = slotPath, color = IncomeGreen)
    }
}
