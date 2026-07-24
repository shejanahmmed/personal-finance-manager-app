package com.shejan.financebuddy.ui.theme

import android.app.Activity
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private object NoIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): Modifier.Node {
        return object : Modifier.Node() {}
    }
    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = System.identityHashCode(this)
}

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
    primary              = Color(0xFF0D9488), // Matte Teal 600
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFCCFBF1), // Soft Teal container
    onPrimaryContainer   = Color(0xFF115E59),
    secondary            = Color(0xFF2563EB), // Matte Royal Blue 600
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E40AF),
    tertiary             = Color(0xFF7C3AED), // Matte Violet 600
    onTertiary           = Color.White,
    background           = Color(0xFFF1F5F9), // Slate 100 matte canvas
    onBackground         = Color(0xFF0F172A), // Slate 900 primary text
    surface              = Color(0xFFFFFFFF), // Crisp pure white card surface
    onSurface            = Color(0xFF0F172A),
    surfaceVariant       = Color(0xFFE2E8F0), // Secondary input/card container
    onSurfaceVariant     = Color(0xFF475569), // Secondary label text
    outline              = Color(0xFFCBD5E1), // Crisp subtle border outline
    outlineVariant       = Color(0xFFE2E8F0),
    error                = Color(0xFFE11D48), // Rose 600 error
    onError              = Color.White,
    errorContainer       = Color(0xFFFFE4E6),
    onErrorContainer     = Color(0xFF9F1239),
    scrim                = Color(0x59000000)
)

private val FinanceBuddyAmoledColorScheme = darkColorScheme(
    primary              = Color.White,
    onPrimary            = Color.Black,
    primaryContainer     = Color.Black,
    onPrimaryContainer   = Color.White,
    secondary            = Color.White,
    onSecondary          = Color.Black,
    secondaryContainer   = Color.Black,
    onSecondaryContainer = Color.White,
    tertiary             = Color.White,
    onTertiary           = Color.Black,
    background           = Color.Black,
    onBackground         = Color.White,
    surface              = Color.Black,
    onSurface            = Color.White,
    surfaceVariant       = Color.Black,
    onSurfaceVariant     = Color(0xB3FFFFFF),
    outline              = Color(0x33FFFFFF),
    outlineVariant       = Color(0x1AFFFFFF),
    error                = Color.White,
    onError              = Color.Black,
    errorContainer       = Color.Black,
    onErrorContainer     = Color.White,
    scrim                = Color(0xCC000000)
)

@Composable
fun FinanceBuddyTheme(
    themeMode: String = "SYSTEM",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val activeMode = when (themeMode) {
        "AMOLED" -> "AMOLED"
        "LIGHT"  -> "LIGHT"
        "DARK"   -> "DARK"
        else     -> if (darkTheme) "DARK" else "LIGHT"
    }

    // Synchronize global theme state
    currentThemeModeState = activeMode

    val colorScheme = when (activeMode) {
        "AMOLED" -> FinanceBuddyAmoledColorScheme
        "LIGHT"  -> FinanceBuddyLightColorScheme
        else     -> FinanceBuddyDarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Transparent status & navigation bars — edge-to-edge
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars    = activeMode == "LIGHT"
                isAppearanceLightNavigationBars = activeMode == "LIGHT"
            }
        }
    }

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    CompositionLocalProvider(
        androidx.compose.material3.LocalRippleConfiguration provides null,
        LocalIndication provides NoIndication
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = AppTypography,
            content     = content,
        )
    }
}