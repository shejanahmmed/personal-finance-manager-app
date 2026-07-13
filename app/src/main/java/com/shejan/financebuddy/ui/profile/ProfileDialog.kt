package com.shejan.financebuddy.ui.profile

import android.content.Context
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shejan.financebuddy.ui.theme.*
import java.io.File
import java.io.FileOutputStream

@Composable
fun EditProfileDialog(
    currentName: String,
    currentImagePath: String,
    onDismiss: () -> Unit,
    onSave: (name: String, imagePath: String) -> Unit
) {
    val context = LocalContext.current
    var nameInput by remember { mutableStateOf(currentName) }
    var tempImagePath by remember { mutableStateOf(currentImagePath) }

    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val path = saveUriToInternalStorage(context, uri)
            if (path != null) {
                tempImagePath = path
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardDarker),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Profile",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    
                    // Close button (matching user sizing preferences: 34.dp box, 18.dp icon, 8.dp corners)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardDark)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Profile Image Picker Section
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Profile Image Container (clipped to circle shape)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(2.dp, Brush.linearGradient(colors = listOf(AccentTeal, AccentBlue)), CircleShape)
                            .clickable {
                                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val bitmap = remember(tempImagePath) {
                            if (tempImagePath.isNotEmpty()) {
                                try {
                                    val file = File(tempImagePath)
                                    if (file.exists()) {
                                        BitmapFactory.decodeFile(file.absolutePath)
                                    } else null
                                } catch (e: Exception) {
                                    null
                                }
                            } else null
                        }

                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(colors = listOf(AccentTeal.copy(alpha = 0.15f), AccentBlue.copy(alpha = 0.15f)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Default Profile Photo",
                                    tint = AccentTeal,
                                    modifier = Modifier.fillMaxSize(0.6f)
                                )
                            }
                        }
                    }

                    // Small camera overlay icon floating outside the circular clip boundary
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(AccentTeal)
                            .clickable {
                                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Change Photo",
                            tint = Color(0xFF0B0E1A), // deep navy color for contrast against bright AccentTeal
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Name Input field wrapped in a Box to prevent label clipping
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder = { Text("User", color = TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentTeal,
                            unfocusedBorderColor = DividerColor,
                            focusedContainerColor = CardDark,
                            unfocusedContainerColor = CardDark,
                            cursorColor = AccentTeal
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Overlapping custom label with thin border and background
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .align(Alignment.TopStart)
                            .offset(y = (-12).dp)
                            .background(CardDark, RoundedCornerShape(4.dp))
                            .border(1.dp, DividerColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 0.5.dp)
                    ) {
                        Text(
                            text = "Full Name",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel button
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary
                        ),
                        border = BorderStroke(1.dp, DividerColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            color = TextSecondary, // Explicit high contrast text color
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    // Save button
                    Button(
                        onClick = {
                            val finalName = nameInput.trim().ifEmpty { "User" }
                            onSave(finalName, tempImagePath)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentTeal,
                            contentColor = Color(0xFF0B0E1A) // High contrast deep navy text on bright teal
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Save",
                            color = Color(0xFF0B0E1A), // Explicit high contrast deep navy text
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Copies a Uri content to the app's secure filesDir and returns its absolute path.
 * Deletes any previous profile picture files to avoid storage bloating.
 */
private fun saveUriToInternalStorage(context: Context, uri: Uri): String? {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(uri)
    val extension = when (mimeType) {
        "image/png" -> "png"
        "image/gif" -> "gif"
        "image/webp" -> "webp"
        else -> "jpg"
    }

    val fileName = "profile_pic_${System.currentTimeMillis()}.$extension"
    val file = File(context.filesDir, fileName)

    return try {
        // Clean up previous profile picture files
        context.filesDir.listFiles()?.forEach { f ->
            if (f.name.startsWith("profile_pic_")) {
                f.delete()
            }
        }

        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
