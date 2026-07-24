package com.shejan.financebuddy.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// === Global Theme Switcher State ===
var isDarkModeGlobal by mutableStateOf(true)

// === Dynamic Background & Surface (Names kept identical to prevent breaking existing imports) ===
val BackgroundDark: Color get() = if (isDarkModeGlobal) Color(0xFF0B0E1A) else Color(0xFFF1F5F9)
val SurfaceDark: Color    get() = if (isDarkModeGlobal) Color(0xFF141827) else Color(0xFFFFFFFF)
val CardDark: Color       get() = if (isDarkModeGlobal) Color(0xFF1C2235) else Color(0xFFFFFFFF)
val CardDarker: Color     get() = if (isDarkModeGlobal) Color(0xFF111525) else Color(0xFFE2E8F0)

// === Accent Colors (Soft matte for Light Mode, Vibrant for Dark Mode) ===
val AccentTeal: Color     get() = if (isDarkModeGlobal) Color(0xFF00D4AA) else Color(0xFF0D9488)
val AccentBlue: Color     get() = if (isDarkModeGlobal) Color(0xFF0096FF) else Color(0xFF2563EB)
val AccentPurple: Color   get() = if (isDarkModeGlobal) Color(0xFF7C5CFC) else Color(0xFF7C3AED)

// === Dynamic Text ===
val TextPrimary: Color    get() = if (isDarkModeGlobal) Color(0xFFFFFFFF) else Color(0xFF0F172A)
val TextSecondary: Color  get() = if (isDarkModeGlobal) Color(0xFF8A94B2) else Color(0xFF475569)
val TextMuted: Color      get() = if (isDarkModeGlobal) Color(0xFF4A5270) else Color(0xFF64748B)

// === Semantic (Rich Matte for Light Mode) ===
val IncomeGreen: Color    get() = if (isDarkModeGlobal) Color(0xFF00C897) else Color(0xFF059669)
val ExpenseRed: Color     get() = if (isDarkModeGlobal) Color(0xFFFF5C7C) else Color(0xFFE11D48)
val TransferYellow: Color get() = if (isDarkModeGlobal) Color(0xFFFFBD2E) else Color(0xFFD97706)

// === Gradient Stops ===
val GradientStart: Color  get() = if (isDarkModeGlobal) Color(0xFF00D4AA) else Color(0xFF0D9488)
val GradientEnd: Color    get() = if (isDarkModeGlobal) Color(0xFF0096FF) else Color(0xFF2563EB)

// === Dynamic Divider / Border ===
val DividerColor: Color   get() = if (isDarkModeGlobal) Color(0xFF1E2540) else Color(0xFFCBD5E1)

// === On-Accent (text/icons on AccentTeal/AccentBlue surfaces) ===
// Dark: deep navy; Light: pure white — ensures 4.5+:1 WCAG contrast on buttons
val OnAccent: Color       get() = if (isDarkModeGlobal) Color(0xFF0B0E1A) else Color(0xFFFFFFFF)

// === Chart Tokens ===
val ChartGridLine: Color  get() = if (isDarkModeGlobal) Color(0x1AFFFFFF) else Color(0x2694A3B8)
val ChartLabel: Color     get() = if (isDarkModeGlobal) Color(0x59FFFFFF) else Color(0xFF64748B)
val ChartSurface: Color   get() = if (isDarkModeGlobal) Color(0x0FFFFFFF) else Color(0x0F0F172A)

// === Scrim (bottom sheet / dialog overlay) ===
val ScrimColor: Color     get() = if (isDarkModeGlobal) Color(0xA6000000) else Color(0x59000000)