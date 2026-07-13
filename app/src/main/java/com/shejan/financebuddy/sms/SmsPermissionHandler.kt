package com.shejan.financebuddy.sms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sms
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.shejan.financebuddy.data.PreferencesManager
import com.shejan.financebuddy.data.db.FinanceDatabase
import com.shejan.financebuddy.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SmsPermissionHandler(
    preferencesManager: PreferencesManager,
    database: FinanceDatabase,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showSyncChoiceDialog by remember { mutableStateOf(true) }
    var selectedChoice by remember { mutableStateOf<String?>(null) }
    var isSyncing by remember { mutableStateOf(false) }

    // Required permissions depending on choice
    val receivePermission = Manifest.permission.RECEIVE_SMS
    val readPermission = Manifest.permission.READ_SMS

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val receiveGranted = results[receivePermission] == true
        val readGranted = results[readPermission] == true

        scope.launch {
            if (selectedChoice == "SYNC_PREVIOUS") {
                if (receiveGranted && readGranted) {
                    isSyncing = true
                    preferencesManager.setSmsSyncChoice("SYNC_PREVIOUS")
                    // Scan inbox for last 30 days
                    val count = SmsSyncHelper.syncPreviousSms(context, database)
                    isSyncing = false
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Success! Imported $count transaction messages.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else if (receiveGranted) {
                    // Fallback to START_NEW if READ was denied but RECEIVE was granted
                    preferencesManager.setSmsSyncChoice("START_NEW")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Read SMS permission denied. Fallback to capturing new transactions only.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    preferencesManager.setSmsSyncChoice("DISABLED")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "SMS automation disabled (permissions denied).",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else if (selectedChoice == "START_NEW") {
                if (receiveGranted) {
                    preferencesManager.setSmsSyncChoice("START_NEW")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "SMS automation active for new transactions.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    preferencesManager.setSmsSyncChoice("DISABLED")
                }
            }
            showSyncChoiceDialog = false
            onDismiss()
        }
    }

    if (showSyncChoiceDialog) {
        Dialog(onDismissRequest = {
            // Dismissing the dialog sets it as skipped (DISABLED) so we don't block the user
            scope.launch {
                preferencesManager.setSmsSyncChoice("DISABLED")
                showSyncChoiceDialog = false
                onDismiss()
            }
        }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardDarker, RoundedCornerShape(24.dp))
                    .border(1.dp, DividerColor, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                if (isSyncing) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = AccentTeal)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Scanning Inbox...",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Finding bank and mobile transaction SMS messages from the last 30 days.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Title badge
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
                                imageVector = Icons.Default.Sms,
                                contentDescription = null,
                                tint = BackgroundDark,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "SMS Transaction Setup",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "FinanceBuddy can automatically scan SMS notifications from your bank or mobile wallets (bKash, Nagad, etc.) and place transactions in your inbox for approval.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Sync Previous Option Card
                        OptionCard(
                            title = "Sync History & Future SMS",
                            description = "Scans last 30 days of messages to populate your inbox immediately, and monitors future transactions.",
                            icon = Icons.Default.History,
                            tag = "Recommended",
                            onClick = {
                                selectedChoice = "SYNC_PREVIOUS"
                                val hasReceive = ContextCompat.checkSelfPermission(context, receivePermission) == PackageManager.PERMISSION_GRANTED
                                val hasRead = ContextCompat.checkSelfPermission(context, readPermission) == PackageManager.PERMISSION_GRANTED
                                if (hasReceive && hasRead) {
                                    scope.launch {
                                        isSyncing = true
                                        preferencesManager.setSmsSyncChoice("SYNC_PREVIOUS")
                                        val count = SmsSyncHelper.syncPreviousSms(context, database)
                                        isSyncing = false
                                        Toast.makeText(context, "Imported $count historical transactions!", Toast.LENGTH_SHORT).show()
                                        showSyncChoiceDialog = false
                                        onDismiss()
                                    }
                                } else {
                                    launcher.launch(arrayOf(receivePermission, readPermission))
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Start New Option Card
                        OptionCard(
                            title = "Track Future SMS Only",
                            description = "Starts fresh. Only detects transactions that arrive in real-time from this moment forward.",
                            icon = Icons.Default.PlayArrow,
                            onClick = {
                                selectedChoice = "START_NEW"
                                val hasReceive = ContextCompat.checkSelfPermission(context, receivePermission) == PackageManager.PERMISSION_GRANTED
                                if (hasReceive) {
                                    scope.launch {
                                        preferencesManager.setSmsSyncChoice("START_NEW")
                                        showSyncChoiceDialog = false
                                        onDismiss()
                                    }
                                } else {
                                    launcher.launch(arrayOf(receivePermission))
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Skip option
                        TextButton(
                            onClick = {
                                scope.launch {
                                    preferencesManager.setSmsSyncChoice("DISABLED")
                                    showSyncChoiceDialog = false
                                    onDismiss()
                                }
                            }
                        ) {
                            Text(
                                text = "Not Now, Keep Manual",
                                color = TextMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Security notice
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Secure, offline parsing. No data is sent to servers.",
                                color = TextMuted,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    tag: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, DividerColor.copy(alpha = 0.8f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentTeal,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (tag != null) {
                    Box(
                        modifier = Modifier
                            .background(AccentTeal.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tag,
                            color = AccentTeal,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}
