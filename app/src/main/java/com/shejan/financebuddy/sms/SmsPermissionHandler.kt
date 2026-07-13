package com.shejan.financebuddy.sms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.shejan.financebuddy.ui.theme.*

/**
 * Handles runtime SMS permission requests with a clear, user-friendly rationale dialog.
 *
 * Design:
 *  - Shows a rationale explaining WHY the app needs SMS access before the system prompt.
 *  - If denied, the feature is silently disabled — the app keeps working normally.
 *  - Never calls READ_SMS; only RECEIVE_SMS is strictly needed for real-time detection.
 *
 * @param onPermissionResult  Callback: true = granted, false = denied.
 */
@Composable
fun SmsPermissionHandler(
    onPermissionResult: (granted: Boolean) -> Unit
) {
    val context = LocalContext.current
    var showRationaleDialog by remember { mutableStateOf(false) }

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        arrayOf(Manifest.permission.RECEIVE_SMS)
    } else {
        emptyArray()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        onPermissionResult(granted)
    }

    // Check if already granted — skip dialog if so
    LaunchedEffect(Unit) {
        val alreadyGranted = permissions.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
        if (alreadyGranted) {
            onPermissionResult(true)
        } else {
            showRationaleDialog = true
        }
    }

    if (showRationaleDialog) {
        Dialog(onDismissRequest = {
            showRationaleDialog = false
            onPermissionResult(false)
        }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardDarker, RoundedCornerShape(20.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    // Icon badge
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                brush = Brush.linearGradient(listOf(AccentTeal, AccentBlue)),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "SMS Auto-Detection",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "FinanceBuddy can automatically detect transactions from your bank and mobile banking SMS messages (bKash, Nagad, BRAC Bank, etc.) and add them as pending entries for your review.\n\n" +
                               "📱 SMS are read on-device only — nothing is sent to any server.\n" +
                               "🔒 All data is encrypted with AES-256.\n" +
                               "✅ You confirm every transaction before it's saved.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Deny button
                        OutlinedButton(
                            onClick = {
                                showRationaleDialog = false
                                onPermissionResult(false)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor)
                        ) {
                            Text("Not Now", fontSize = 13.sp)
                        }

                        // Allow button
                        Button(
                            onClick = {
                                showRationaleDialog = false
                                launcher.launch(permissions)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                        ) {
                            Text("Allow", fontSize = 13.sp, color = BackgroundDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/** Returns true if RECEIVE_SMS permission is currently granted. */
fun isSmsPermissionGranted(context: android.content.Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECEIVE_SMS
    ) == PackageManager.PERMISSION_GRANTED
}
