package com.shejan.financebuddy.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val FinanceBuddyDarkColorScheme = darkColorScheme(
    primary          = AccentTeal,
    onPrimary        = BackgroundDark,
    primaryContainer = CardDark,
    secondary        = AccentBlue,
    onSecondary      = BackgroundDark,
    tertiary         = AccentPurple,
    background       = BackgroundDark,
    onBackground     = TextPrimary,
    surface          = SurfaceDark,
    onSurface        = TextPrimary,
    surfaceVariant   = CardDark,
    onSurfaceVariant = TextSecondary,
    outline          = DividerColor,
    error            = ExpenseRed,
    onError          = TextPrimary,
)

@Composable
fun FinanceBuddyTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = FinanceBuddyDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Transparent status & navigation bars — edge-to-edge
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars    = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content     = content,
    )
}