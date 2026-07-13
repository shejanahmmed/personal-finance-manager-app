package com.shejan.financebuddy.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// === Global Theme Switcher State ===
var isDarkModeGlobal by mutableStateOf(true)

// === Dynamic Background & Surface (Names kept identical to prevent breaking existing imports) ===
val BackgroundDark: Color get() = if (isDarkModeGlobal) Color(0xFF0B0E1A) else Color(0xFFF5F7FA)
val SurfaceDark: Color    get() = if (isDarkModeGlobal) Color(0xFF141827) else Color(0xFFFFFFFF)
val CardDark: Color       get() = if (isDarkModeGlobal) Color(0xFF1C2235) else Color(0xFFFFFFFF)
val CardDarker: Color     get() = if (isDarkModeGlobal) Color(0xFF111525) else Color(0xFFEEF2F6)

// === Accent Colors ===
val AccentTeal     = Color(0xFF00D4AA)
val AccentBlue     = Color(0xFF0096FF)
val AccentPurple   = Color(0xFF7C5CFC)

// === Dynamic Text ===
val TextPrimary: Color    get() = if (isDarkModeGlobal) Color(0xFFFFFFFF) else Color(0xFF191D24)
val TextSecondary: Color  get() = if (isDarkModeGlobal) Color(0xFF8A94B2) else Color(0xFF5A6275)
val TextMuted: Color      get() = if (isDarkModeGlobal) Color(0xFF4A5270) else Color(0xFF9AA3B8)

// === Semantic ===
val IncomeGreen    = Color(0xFF00C897)
val ExpenseRed     = Color(0xFFFF5C7C)
val TransferYellow = Color(0xFFFFBD2E)

// === Gradient Stops ===
val GradientStart  = Color(0xFF00D4AA)
val GradientEnd    = Color(0xFF0096FF)

// === Dynamic Divider / Border ===
val DividerColor: Color   get() = if (isDarkModeGlobal) Color(0xFF1E2540) else Color(0xFFE2E6F0)