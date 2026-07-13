package com.shejan.financebuddy.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val FinanceBuddyDarkColorScheme = darkColorScheme(
    primary          = AccentTeal,
    onPrimary        = Color(0xFF0B0E1A),
    primaryContainer = Color(0xFF1C2235),
    secondary        = AccentBlue,
    onSecondary      = Color(0xFF0B0E1A),
    tertiary         = AccentPurple,
    background       = Color(0xFF0B0E1A),
    onBackground     = Color(0xFFFFFFFF),
    surface          = Color(0xFF141827),
    onSurface        = Color(0xFFFFFFFF),
    surfaceVariant   = Color(0xFF1C2235),
    onSurfaceVariant = Color(0xFF8A94B2),
    outline          = Color(0xFF1E2540),
    error            = Color(0xFFFF5C7C),
    onError          = Color(0xFFFFFFFF),
)

private val FinanceBuddyLightColorScheme = lightColorScheme(
    primary          = AccentTeal,
    onPrimary        = Color.White,
    primaryContainer = Color.White,
    secondary        = AccentBlue,
    onSecondary      = Color.White,
    tertiary         = AccentPurple,
    background       = Color(0xFFF5F7FA),
    onBackground     = Color(0xFF191D24),
    surface          = Color.White,
    onSurface        = Color(0xFF191D24),
    surfaceVariant   = Color.White,
    onSurfaceVariant = Color(0xFF5A6275),
    outline          = Color(0xFFE2E6F0),
    error            = Color(0xFFFF5C7C),
    onError          = Color.White,
)

@Composable
fun FinanceBuddyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Synchronize global theme state variable
    isDarkModeGlobal = darkTheme

    val colorScheme = if (darkTheme) FinanceBuddyDarkColorScheme else FinanceBuddyLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Transparent status & navigation bars — edge-to-edge
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars    = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content     = content,
    )
}