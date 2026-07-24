package com.shejan.financebuddy.ui.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.shejan.financebuddy.data.BackupManager
import com.shejan.financebuddy.data.PreferencesManager
import com.shejan.financebuddy.data.db.FinanceDatabase
import com.shejan.financebuddy.security.BiometricHelper
import com.shejan.financebuddy.ui.security.PinSetupDialog
import com.shejan.financebuddy.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    preferencesManager: PreferencesManager,
    database: FinanceDatabase,
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
    val hideTotalBalance by preferencesManager.hideTotalBalance.collectAsState(initial = false)
    val blockScreenshots by preferencesManager.blockScreenshots.collectAsState(initial = false)

    val isAppLockEnabled by preferencesManager.isAppLockEnabled.collectAsState(initial = false)
    val appLockType by preferencesManager.appLockType.collectAsState(initial = "PIN")
    val appLockPin by preferencesManager.appLockPin.collectAsState(initial = "")
    val autoLockTimeout by preferencesManager.autoLockTimeout.collectAsState(initial = "IMMEDIATELY")

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showVerifyDialog by remember { mutableStateOf(false) }
    var pendingSecurityAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // ─── Backup & Restore State ─────────────────────────────────
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    var toastMessage by remember { mutableStateOf<String?>(null) }
    fun showToast(msg: String) {
        toastMessage = msg
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            kotlinx.coroutines.delay(2500)
            toastMessage = null
        }
    }
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // SAF launcher for creating a backup file
    // Using "*/*" MIME type to avoid ActivityNotFoundException on devices
    // that don't recognize custom file extensions like .financebuddy
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null) {
            isExporting = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            BackupManager.exportData(context, outputStream, database, preferencesManager)
                        } ?: throw Exception("Could not open output stream")
                    }
                    showToast("Backup exported successfully \uD83D\uDCE6")
                } catch (e: Exception) {
                    showToast("Export failed: ${e.message}")
                } finally {
                    isExporting = false
                }
            }
        }
    }

    // SAF launcher for opening a backup file
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showImportConfirmDialog = true
        }
    }

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
            // ─── Header Top Bar (Matching Bank Accounts Page Design) ─────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CardDarker)
                        .border(1.dp, DividerColor, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Settings",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customize your preference & local configuration",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
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
                            showToast("Theme set to Follow System")
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
                            showToast("Switched to Light Mode \u2600\uFE0F")
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
                            showToast("Switched to Dark Mode \uD83C\uDF19")
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
                            showToast("SMS Sync set to Historical")
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
                            showToast("SMS Sync set to Start From New")
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
                            showToast("SMS Auto-Parsing Disabled")
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
                            val nextState = !hideBalancesPref
                            scope.launch { preferencesManager.setHideCardBalances(nextState) }
                            showToast(if (nextState) "Card balances hidden" else "Card balances visible")
                        }
                    )

                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))

                    ThemeOptionRow(
                        title = "Hide Total Balance",
                        description = "Mask dashboard net balance with asterisks; tap eye icon to reveal",
                        icon = Icons.Default.VisibilityOff,
                        selected = hideTotalBalance,
                        onClick = {
                            val nextState = !hideTotalBalance
                            scope.launch { preferencesManager.setHideTotalBalance(nextState) }
                            showToast(if (nextState) "Total balance masked" else "Total balance visible")
                        }
                    )

                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))

                    // Block Screenshots & Recording Switch Row
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
                                    .background(if (blockScreenshots) AccentTeal.copy(alpha = 0.15f) else DividerColor.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = if (blockScreenshots) AccentTeal else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Block Screenshots & Recording",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Prevent taking screenshots or recording app screen for enhanced privacy",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Switch(
                            checked = blockScreenshots,
                            onCheckedChange = { enable ->
                                scope.launch { preferencesManager.setBlockScreenshots(enable) }
                                showToast(if (enable) "Screenshot protection enabled \uD83D\uDEE1\uFE0F" else "Screenshot protection disabled")
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnAccent,
                                checkedTrackColor = AccentTeal,
                                checkedBorderColor = Color.Transparent,
                                uncheckedThumbColor = SwitchThumbUnchecked,
                                uncheckedTrackColor = SwitchTrackUnchecked,
                                uncheckedBorderColor = SwitchBorderUnchecked
                            )
                        )
                    }
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

                        Spacer(modifier = Modifier.width(12.dp))

                        Switch(
                            checked = isAppLockEnabled,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    if (appLockPin.isEmpty()) {
                                        showPinSetupDialog = true
                                    } else {
                                        scope.launch { preferencesManager.setAppLockEnabled(true) }
                                        showToast("App Lock Enabled \uD83D\uDD12")
                                    }
                                } else {
                                    verifyUserBeforeAction {
                                        scope.launch { preferencesManager.setAppLockEnabled(false) }
                                        showToast("App Lock Disabled \uD83D\uDD13")
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnAccent,
                                checkedTrackColor = AccentTeal,
                                checkedBorderColor = Color.Transparent,
                                uncheckedThumbColor = SwitchThumbUnchecked,
                                uncheckedTrackColor = SwitchTrackUnchecked,
                                uncheckedBorderColor = SwitchBorderUnchecked
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
                                        showToast("Security set to 6-Digit PIN")
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
                                            showToast("Fingerprint Biometric Enabled \u261D\uFE0F")
                                        }
                                    } else {
                                        showToast("Biometric fingerprint not setup on device")
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
                                "IMMEDIATELY" to "Instant",
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
                                                        val tMsg = when(key) {
                                                            "IMMEDIATELY" -> "Auto-Lock set to Instant"
                                                            "1_MIN" -> "Auto-Lock set to 1 Minute"
                                                            "3_MIN" -> "Auto-Lock set to 3 Minutes"
                                                            else -> "Auto-Lock set to 5 Minutes"
                                                        }
                                                        showToast(tMsg)
                                                    }
                                                }
                                            }
                                            .padding(vertical = 8.dp, horizontal = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.5.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) OnAccent else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ─── SECTION 5: DATA BACKUP ─────────────────────────────
                Text(
                    text = "Data Backup",
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
                    // Export Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isExporting) {
                                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                exportLauncher.launch("FinanceBuddy_Backup_$dateStr.json")
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = AccentTeal,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    tint = AccentTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isExporting) "Exporting…" else "Export All Data",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Save all accounts, transactions, budgets, goals and loans to a backup file",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))

                    // Import Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isImporting) {
                                importLauncher.launch(arrayOf("*/*"))
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
                            if (isImporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = AccentBlue,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isImporting) "Importing…" else "Import Backup",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Restore data from a previous FinanceBuddy backup file",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ─── SECTION 6: APP INFO (LOCALIZED) ──────────────────────
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

        // Confirmation dialog before import
        if (showImportConfirmDialog) {
            AlertDialog(
                onDismissRequest = {
                    showImportConfirmDialog = false
                    pendingImportUri = null
                },
                containerColor = CardDark,
                titleContentColor = TextPrimary,
                textContentColor = TextSecondary,
                title = {
                    Text(
                        text = "Replace All Data?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        text = "Importing a backup will permanently replace all current accounts, transactions, budgets, goals, and loans with the data from the backup file.\n\nApp lock will be disabled after import. You will need to set up a new PIN.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showImportConfirmDialog = false
                            val uri = pendingImportUri ?: return@TextButton
                            pendingImportUri = null
                            isImporting = true
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                            BackupManager.importData(context, inputStream, database, preferencesManager)
                                        } ?: throw Exception("Could not open backup file")
                                    }
                                    Toast.makeText(context, "Backup restored successfully ✅", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isImporting = false
                                }
                            }
                        }
                    ) {
                        Text("Replace & Restore", color = ExpenseRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showImportConfirmDialog = false
                            pendingImportUri = null
                        }
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // Modern Floating Custom Toast Notification Banner
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp, start = 20.dp, end = 20.dp)
        ) {
            toastMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardDark,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, AccentTeal.copy(alpha = 0.4f)),
                    modifier = Modifier.wrapContentSize()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(AccentTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AccentTeal,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = msg,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
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

        Spacer(modifier = Modifier.width(12.dp))

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
