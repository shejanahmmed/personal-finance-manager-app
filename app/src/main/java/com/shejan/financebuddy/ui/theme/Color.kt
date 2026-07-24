package com.shejan.financebuddy.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// === Global Theme Switcher State ===
var currentThemeModeState by mutableStateOf("DARK")
var isDarkModeGlobal: Boolean
    get() = currentThemeModeState != "LIGHT"
    set(value) {
        currentThemeModeState = if (value) "DARK" else "LIGHT"
    }

// === Dynamic Background & Surface ===
val BackgroundDark: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFF000000)
    "LIGHT"  -> Color(0xFFF1F5F9)
    else     -> Color(0xFF0B0E1A)
}

val SurfaceDark: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFF000000)
    "LIGHT"  -> Color(0xFFFFFFFF)
    else     -> Color(0xFF141827)
}

val CardDark: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFF000000)
    "LIGHT"  -> Color(0xFFFFFFFF)
    else     -> Color(0xFF1C2235)
}

val CardDarker: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFF000000)
    "LIGHT"  -> Color(0xFFE2E8F0)
    else     -> Color(0xFF111525)
}

// === Accent Colors ===
val AccentTeal: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFFFFFFFF)
    "LIGHT"  -> Color(0xFF0D9488)
    else     -> Color(0xFF00D4AA)
}

val AccentBlue: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFFFFFFFF)
    "LIGHT"  -> Color(0xFF2563EB)
    else     -> Color(0xFF0096FF)
}

val AccentPurple: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFFFFFFFF)
    "LIGHT"  -> Color(0xFF7C3AED)
    else     -> Color(0xFF7C5CFC)
}

// === Dynamic Text ===
val TextPrimary: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFFFFFFFF) // 100%
    "LIGHT"  -> Color(0xFF0F172A)
    else     -> Color(0xFFFFFFFF)
}

val TextSecondary: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xB3FFFFFF) // 70%
    "LIGHT"  -> Color(0xFF475569)
    else     -> Color(0xFF8A94B2)
}

val TextMuted: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0x80FFFFFF) // 50%
    "LIGHT"  -> Color(0xFF64748B)
    else     -> Color(0xFF4A5270)
}

// === Semantic ===
val IncomeGreen: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFFFFFFFF) // 100%
    "LIGHT"  -> Color(0xFF059669)
    else     -> Color(0xFF00C897)
}

val ExpenseRed: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xB3FFFFFF) // 70%
    "LIGHT"  -> Color(0xFFE11D48)
    else     -> Color(0xFFFF5C7C)
}

val TransferYellow: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0x80FFFFFF) // 50%
    "LIGHT"  -> Color(0xFFD97706)
    else     -> Color(0xFFFFBD2E)
}

// === Gradient Stops ===
val GradientStart: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFFFFFFFF)
    "LIGHT"  -> Color(0xFF0D9488)
    else     -> Color(0xFF00D4AA)
}

val GradientEnd: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFFFFFFFF)
    "LIGHT"  -> Color(0xFF2563EB)
    else     -> Color(0xFF0096FF)
}

// === Dynamic Divider / Border ===
val DividerColor: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0x33FFFFFF) // 20% opacity border
    "LIGHT"  -> Color(0xFFCBD5E1)
    else     -> Color(0xFF1E2540)
}

// === On-Accent ===
val OnAccent: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFF000000)
    "LIGHT"  -> Color(0xFFFFFFFF)
    else     -> Color(0xFF0B0E1A)
}

// === Chart Tokens ===
val ChartGridLine: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0x1AFFFFFF) // 10%
    "LIGHT"  -> Color(0x2694A3B8)
    else     -> Color(0x1AFFFFFF)
}

val ChartLabel: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0x80FFFFFF) // 50%
    "LIGHT"  -> Color(0xFF64748B)
    else     -> Color(0x59FFFFFF)
}

val ChartSurface: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xFF000000)
    "LIGHT"  -> Color(0x0F0F172A)
    else     -> Color(0x0FFFFFFF)
}

// === Scrim (bottom sheet / dialog overlay) ===
val ScrimColor: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0xCC000000)
    "LIGHT"  -> Color(0x59000000)
    else     -> Color(0xA6000000)
}

// === Toggle / Switch Colors ===
val SwitchTrackUnchecked: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0x33FFFFFF)
    "LIGHT"  -> Color(0xFFCBD5E1)
    else     -> Color(0xFF1E2538)
}

val SwitchThumbUnchecked: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0x80FFFFFF)
    "LIGHT"  -> Color(0xFF64748B)
    else     -> Color(0xFF9AA3B8)
}

val SwitchBorderUnchecked: Color get() = when (currentThemeModeState) {
    "AMOLED" -> Color(0x40FFFFFF)
    "LIGHT"  -> Color(0xFF94A3B8)
    else     -> Color(0xFF333B54)
}