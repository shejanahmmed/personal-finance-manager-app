package com.shejan.financebuddy.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.PreferencesManager
import com.shejan.financebuddy.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    preferencesManager: PreferencesManager,
    onBack: () -> Unit
) {
    // Hardware back press handler
    BackHandler(onBack = onBack)

    val scope = rememberCoroutineScope()
    val themeMode by preferencesManager.themeMode.collectAsState(initial = "SYSTEM")
    val smsSyncChoice by preferencesManager.smsSyncChoice.collectAsState(initial = null)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Decorative ambient top glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AccentTeal.copy(alpha = 0.05f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ─── Header Top Bar ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 20.dp, end = 20.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Settings",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Customize your preference & local configuration",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Request: Close button styled to match TransactionListScreen
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardDark)
                            .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Go Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // ─── SECTION 1: THEME SELECTION ───────────────────────────
                Text(
                    text = "App Theme",
                    color = AccentTeal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardDark)
                        .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
                        .padding(4.dp)
                ) {
                    ThemeOptionRow(
                        title = "Follow System Default",
                        description = "Matches device's system theme settings",
                        icon = Icons.Default.Devices,
                        selected = themeMode == "SYSTEM",
                        onClick = {
                            scope.launch { preferencesManager.setThemeMode("SYSTEM") }
                        }
                    )
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                    ThemeOptionRow(
                        title = "Light Mode",
                        description = "Crisp, bright background for daylight use",
                        icon = Icons.Default.LightMode,
                        selected = themeMode == "LIGHT",
                        onClick = {
                            scope.launch { preferencesManager.setThemeMode("LIGHT") }
                        }
                    )
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                    ThemeOptionRow(
                        title = "Dark Mode",
                        description = "Power-saving, dark canvas optimal for night",
                        icon = Icons.Default.DarkMode,
                        selected = themeMode == "DARK",
                        onClick = {
                            scope.launch { preferencesManager.setThemeMode("DARK") }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ─── SECTION 2: SMS SYNCHRONIZATION ───────────────────────
                Text(
                    text = "SMS Auto-Tracking",
                    color = AccentTeal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardDark)
                        .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
                        .padding(4.dp)
                ) {
                    ThemeOptionRow(
                        title = "Sync Historical SMS",
                        description = "Fetch previous transactional SMS to construct initial budget records",
                        icon = Icons.Default.Sync,
                        selected = smsSyncChoice == "SYNC_PREVIOUS",
                        onClick = {
                            scope.launch { preferencesManager.setSmsSyncChoice("SYNC_PREVIOUS") }
                        }
                    )
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                    ThemeOptionRow(
                        title = "Start From New Only",
                        description = "Ignore previous logs; only capture inbox messages received from now on",
                        icon = Icons.Default.PhoneAndroid,
                        selected = smsSyncChoice == "START_NEW",
                        onClick = {
                            scope.launch { preferencesManager.setSmsSyncChoice("START_NEW") }
                        }
                    )
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                    ThemeOptionRow(
                        title = "Disable SMS Parsing",
                        description = "Manually record transactions; do not read or process local SMS records",
                        icon = Icons.Default.SyncDisabled,
                        selected = smsSyncChoice == "DISABLED" || smsSyncChoice == "PENDING",
                        onClick = {
                            scope.launch { preferencesManager.setSmsSyncChoice("DISABLED") }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ─── SECTION 3: APP INFO (LOCALIZED) ──────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardDarker)
                        .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Security Status",
                        tint = AccentTeal,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "FinanceBuddy",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Secure local-first personal financial manager built specifically for users in Bangladesh.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                    Text(
                        text = "Made in Bangladesh 🇧🇩",
                        color = AccentBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "All database records are stored fully offline on your device, secured with AES-256 local encryption.",
                        color = TextMuted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ThemeOptionRow(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rounded square icon backdrop
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) AccentTeal.copy(alpha = 0.15f) else DividerColor.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) AccentTeal else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (selected) AccentTeal else TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
            )
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }

        // Selection dot
        Box(
            modifier = Modifier
                .size(16.dp)
                .border(2.dp, if (selected) AccentTeal else TextMuted, RoundedCornerShape(8.dp))
                .padding(3.dp)
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(4.dp))
                        .background(AccentTeal)
                )
            }
        }
    }
}
