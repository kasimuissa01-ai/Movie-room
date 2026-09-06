package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.updater.AppUpdateInfo
import com.example.ui.theme.CineGold
import com.example.ui.theme.CineRedPrimary
import com.example.ui.theme.CineSurface
import com.example.ui.theme.CineSurfaceElevated
import com.example.ui.theme.CineTextMuted
import com.example.ui.theme.CineTextPrimary
import com.example.ui.theme.CineTextSecondary

@Composable
fun AdminReleasePublisherDialog(
    currentUpdateInfo: AppUpdateInfo?,
    currentVersionCode: Long,
    currentVersionName: String,
    onPublish: (versionCode: Long, versionName: String, apkUrl: String, notes: String, size: String, isForce: Boolean) -> Unit,
    onDismiss: () -> Unit,
    isPublishing: Boolean = false,
    modifier: Modifier = Modifier
) {
    var versionCodeText by remember {
        mutableStateOf(((currentUpdateInfo?.latestVersionCode ?: currentVersionCode) + 1).toString())
    }
    var versionNameText by remember {
        mutableStateOf(
            if (currentUpdateInfo != null && currentUpdateInfo.latestVersionName.isNotBlank()) {
                val parts = currentUpdateInfo.latestVersionName.split(".")
                if (parts.size >= 2) "${parts[0]}.${(parts.getOrNull(1)?.toIntOrNull() ?: 0) + 1}.0"
                else "1.1.0"
            } else "1.1.0"
        )
    }
    var apkUrlText by remember {
        mutableStateOf(currentUpdateInfo?.apkDownloadUrl ?: "")
    }
    var releaseNotesText by remember {
        mutableStateOf(
            currentUpdateInfo?.releaseNotes?.ifBlank {
                "• New blockbuster movies added\n• Fast streaming player enhancements\n• Bug fixes & stability"
            } ?: "• New blockbuster movies added\n• Fast streaming player enhancements\n• Bug fixes & stability"
        )
    }
    var fileSizeText by remember {
        mutableStateOf(currentUpdateInfo?.fileSizeFormatted?.ifBlank { "42 MB" } ?: "42 MB")
    }
    var isForceUpdate by remember {
        mutableStateOf(currentUpdateInfo?.isForceUpdate ?: false)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CineSurface),
            border = BorderStroke(1.dp, CineGold.copy(alpha = 0.5f)),
            modifier = modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Publish OTA App Update",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineGold
                        )
                        Text(
                            text = "Pushes version metadata to Firestore for all users",
                            fontSize = 11.sp,
                            color = CineTextMuted
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = CineTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Current app version indicator
                Text(
                    text = "Current Installed: v$currentVersionName (Code $currentVersionCode)",
                    fontSize = 12.sp,
                    color = CineTextSecondary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = versionCodeText,
                        onValueChange = { versionCodeText = it },
                        label = { Text("Version Code", fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CineSurfaceElevated,
                            unfocusedContainerColor = CineSurfaceElevated,
                            focusedBorderColor = CineGold,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                            focusedTextColor = CineTextPrimary,
                            unfocusedTextColor = CineTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = versionNameText,
                        onValueChange = { versionNameText = it },
                        label = { Text("Version Name", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CineSurfaceElevated,
                            unfocusedContainerColor = CineSurfaceElevated,
                            focusedBorderColor = CineGold,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                            focusedTextColor = CineTextPrimary,
                            unfocusedTextColor = CineTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = apkUrlText,
                    onValueChange = { apkUrlText = it },
                    label = { Text("Direct APK Download URL (R2 / Firebase / GitHub)", fontSize = 12.sp) },
                    placeholder = { Text("https://pub-...r2.dev/cinestream-update.apk", fontSize = 11.sp, color = CineTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CineSurfaceElevated,
                        unfocusedContainerColor = CineSurfaceElevated,
                        focusedBorderColor = CineGold,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                        focusedTextColor = CineTextPrimary,
                        unfocusedTextColor = CineTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = fileSizeText,
                    onValueChange = { fileSizeText = it },
                    label = { Text("APK File Size Display (e.g., 45 MB)", fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CineSurfaceElevated,
                        unfocusedContainerColor = CineSurfaceElevated,
                        focusedBorderColor = CineGold,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                        focusedTextColor = CineTextPrimary,
                        unfocusedTextColor = CineTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = releaseNotesText,
                    onValueChange = { releaseNotesText = it },
                    label = { Text("Release Notes (What's New)", fontSize = 12.sp) },
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CineSurfaceElevated,
                        unfocusedContainerColor = CineSurfaceElevated,
                        focusedBorderColor = CineGold,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                        focusedTextColor = CineTextPrimary,
                        unfocusedTextColor = CineTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isForceUpdate,
                        onCheckedChange = { isForceUpdate = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = CineRedPrimary,
                            uncheckedColor = CineTextMuted
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Mandatory / Force Update",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CineTextPrimary
                        )
                        Text(
                            text = "Requires users to update before accessing the app",
                            fontSize = 10.sp,
                            color = CineTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val code = versionCodeText.toLongOrNull() ?: (currentVersionCode + 1)
                        onPublish(
                            code,
                            versionNameText.ifBlank { "1.1.0" },
                            apkUrlText.ifBlank { "https://movieroom-334fb.firebasestorage.app/releases/cinestream-latest.apk" },
                            releaseNotesText,
                            fileSizeText,
                            isForceUpdate
                        )
                    },
                    enabled = !isPublishing && apkUrlText.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CineGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isPublishing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Publishing to Firestore...", color = Color.Black, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.Publish, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Publish Update for All Users", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
