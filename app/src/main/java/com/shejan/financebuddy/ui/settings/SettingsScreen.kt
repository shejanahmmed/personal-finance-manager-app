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

import android.widget.Toast
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.shejan.financebuddy.security.BiometricHelper
import com.shejan.financebuddy.ui.security.PinSetupDialog

@Composable
fun SettingsScreen(
    preferencesManager: PreferencesManager,
    onBack: () -> Unit
) {
    // Hardware back press handler
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()

    val themeMode by preferencesManager.themeMode.collectAsState(initial = "SYSTEM")
    val smsSyncChoice by preferencesManager.smsSyncChoice.collectAsState(initial = null)
    val hideBalancesPref by preferencesManager.hideCardBalances.collectAsState(initial = false)

    val isAppLockEnabled by preferencesManager.isAppLockEnabled.collectAsState(initial = false)
    val appLockType by preferencesManager.appLockType.collectAsState(initial = "PIN")
    val appLockPin by preferencesManager.appLockPin.collectAsState(initial = "")
    val autoLockTimeout by preferencesManager.autoLockTimeout.collectAsState(initial = "IMMEDIATELY")

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showVerifyDialog by remember { mutableStateOf(false) }
    var pendingSecurityAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun verifyUserBeforeAction(onVerifiedAction: () -> Unit) {
        if (!isAppLockEnabled) {
            onVerifiedAction()
            return
        }

        pendingSecurityAction = onVerifiedAction

        if (appLockType == "FINGERPRINT" && activity != null && BiometricHelper.isBiometricAvailable(context)) {
            BiometricHelper.showBiometricPrompt(
                activity = activity,
                title = "Security Verification",
                subtitle = "Verify fingerprint to change security settings",
                negativeButtonText = "Use PIN",
                onSuccess = {
                    onVerifiedAction()
                    pendingSecurityAction = null
                },
                onError = { err ->
                    if (err != "CANCELLED") {
                        showVerifyDialog = true
                    }
                }
            )
        } else {
            showVerifyDialog = true
        }
    }

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

                Spacer(modifier = Modifier.height(24.dp))

                // ─── SECTION 3: PRIVACY & DISPLAY ─────────────────────────
                Text(
                    text = "Privacy & Display",
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
                        title = "Hide Card Balances",
                        description = "Mask bank and mobile account balances on the Home dashboard by default",
                        icon = Icons.Default.Shield,
                        selected = hideBalancesPref,
                        onClick = {
                            scope.launch { preferencesManager.setHideCardBalances(!hideBalancesPref) }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ─── SECTION 4: APP SECURITY & LOCK ───────────────────────
                Text(
                    text = "App Security & Lock",
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
                    // App Lock Enable Switch Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isAppLockEnabled) AccentTeal.copy(alpha = 0.15f) else DividerColor.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isAppLockEnabled) AccentTeal else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Enable App Lock",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Require authentication when launching FinanceBuddy",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        Switch(
                            checked = isAppLockEnabled,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    if (appLockPin.isEmpty()) {
                                        showPinSetupDialog = true
                                    } else {
                                        scope.launch { preferencesManager.setAppLockEnabled(true) }
                                    }
                                } else {
                                    verifyUserBeforeAction {
                                        scope.launch { preferencesManager.setAppLockEnabled(false) }
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentTeal
                            )
                        )
                    }

                    if (isAppLockEnabled) {
                        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))

                        // Lock Type Options
                        ThemeOptionRow(
                            title = "6-Digit PIN",
                            description = "Unlock app with numeric 6-digit passcode",
                            icon = Icons.Default.Key,
                            selected = appLockType == "PIN",
                            onClick = {
                                if (appLockType != "PIN") {
                                    verifyUserBeforeAction {
                                        scope.launch { preferencesManager.setAppLockType("PIN") }
                                    }
                                }
                            }
                        )

                        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))

                        ThemeOptionRow(
                            title = "Fingerprint Biometric",
                            description = "Unlock app using touch fingerprint sensor",
                            icon = Icons.Default.Fingerprint,
                            selected = appLockType == "FINGERPRINT",
                            onClick = {
                                if (appLockType != "FINGERPRINT") {
                                    if (BiometricHelper.isBiometricAvailable(context)) {
                                        verifyUserBeforeAction {
                                            scope.launch { preferencesManager.setAppLockType("FINGERPRINT") }
                                        }
                                    } else {
                                        Toast.makeText(context, "Biometric fingerprint is not set up on this device.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        )

                        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))

                        // Change PIN Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    verifyUserBeforeAction {
                                        showPinSetupDialog = true
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Change 6-Digit PIN",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Update your security PIN code",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))

                        // Auto-Lock Timeout Selection
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AccentTeal.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = AccentTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Auto-Lock Timeout",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Automatically lock app when left in background",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Timeout selection chips
                            val timeoutOptions = listOf(
                                "IMMEDIATELY" to "Immediately",
                                "1_MIN" to "1 Min",
                                "3_MIN" to "3 Mins",
                                "5_MIN" to "5 Mins"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for ((key, label) in timeoutOptions) {
                                    val isSelected = autoLockTimeout == key
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) AccentTeal else CardDarker)
                                            .border(1.dp, if (isSelected) AccentTeal else DividerColor, RoundedCornerShape(10.dp))
                                            .clickable {
                                                if (autoLockTimeout != key) {
                                                    verifyUserBeforeAction {
                                                        scope.launch { preferencesManager.setAutoLockTimeout(key) }
                                                    }
                                                }
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ─── SECTION 4: APP INFO (LOCALIZED) ──────────────────────
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

        // Dialog for setting up or changing 6-digit PIN
        if (showPinSetupDialog) {
            PinSetupDialog(
                title = if (appLockPin.isEmpty()) "Set 6-Digit PIN" else "Change 6-Digit PIN",
                onDismiss = { showPinSetupDialog = false },
                onPinConfirmed = { newPin ->
                    scope.launch {
                        preferencesManager.setAppLockPin(newPin)
                        preferencesManager.setAppLockEnabled(true)
                    }
                    showPinSetupDialog = false
                    Toast.makeText(context, "6-digit PIN saved successfully.", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Dialog for verifying current PIN before making security changes
        if (showVerifyDialog) {
            PinSetupDialog(
                title = "Verify Current PIN",
                isVerificationOnly = true,
                expectedPin = appLockPin,
                onDismiss = {
                    showVerifyDialog = false
                    pendingSecurityAction = null
                },
                onPinConfirmed = {
                    showVerifyDialog = false
                    pendingSecurityAction?.invoke()
                    pendingSecurityAction = null
                }
            )
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
