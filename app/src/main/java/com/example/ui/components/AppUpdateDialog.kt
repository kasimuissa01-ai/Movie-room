package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.updater.AppUpdateInfo
import com.example.data.updater.AppUpdateManager
import com.example.data.updater.UpdateDownloadProgress
import com.example.ui.theme.CineCardBorder
import com.example.ui.theme.CineGold
import com.example.ui.theme.CineGreen
import com.example.ui.theme.CineRedPrimary
import com.example.ui.theme.CineSurface
import com.example.ui.theme.CineSurfaceElevated
import com.example.ui.theme.CineTextMuted
import com.example.ui.theme.CineTextPrimary
import com.example.ui.theme.CineTextSecondary
import kotlinx.coroutines.launch

/**
 * Reusable Compose UpdateDialog component that displays the new version number
 * and provides a 'Download Now' button that triggers the APK download flow via AppUpdateManager.
 */
@Composable
fun UpdateDialog(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current
) {
    val coroutineScope = rememberCoroutineScope()
    var downloadProgress by remember { mutableStateOf<UpdateDownloadProgress>(UpdateDownloadProgress.Idle) }

    AppUpdateDialog(
        updateInfo = updateInfo,
        downloadProgress = downloadProgress,
        onStartUpdate = {
            coroutineScope.launch {
                AppUpdateManager.downloadAndInstallApk(context, updateInfo) { progress ->
                    downloadProgress = progress
                }
            }
        },
        onDismiss = onDismiss,
        modifier = modifier
    )
}

@Composable
fun AppUpdateDialog(
    updateInfo: AppUpdateInfo,
    downloadProgress: UpdateDownloadProgress,
    onStartUpdate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDownloading = downloadProgress is UpdateDownloadProgress.Downloading
    val isReady = downloadProgress is UpdateDownloadProgress.ReadyToInstall
    val isError = downloadProgress is UpdateDownloadProgress.Error

    Dialog(
        onDismissRequest = {
            if (!updateInfo.isForceUpdate && !isDownloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !updateInfo.isForceUpdate && !isDownloading,
            dismissOnClickOutside = !updateInfo.isForceUpdate && !isDownloading,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CineSurface),
            border = BorderStroke(1.dp, CineRedPrimary.copy(alpha = 0.6f)),
            modifier = modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
                .testTag("app_update_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header with icon and version number
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(CineRedPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = CineRedPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Update Available",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = CineTextPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CineGold.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "v${updateInfo.latestVersionName}",
                                        color = CineGold,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.testTag("new_version_text")
                                    )
                                }
                                if (updateInfo.fileSizeFormatted.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• ${updateInfo.fileSizeFormatted}",
                                        fontSize = 12.sp,
                                        color = CineTextMuted
                                    )
                                }
                            }
                        }
                    }

                    if (!updateInfo.isForceUpdate && !isDownloading) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = CineTextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Release Notes Section
                Text(
                    text = "What's New:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CineSurfaceElevated,
                    border = BorderStroke(1.dp, CineCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = updateInfo.releaseNotes.ifBlank { "• Performance optimizations\n• Enhanced video playback\n• Security updates" },
                            fontSize = 13.sp,
                            color = CineTextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar or Status
                AnimatedVisibility(visible = isDownloading) {
                    val progressState = downloadProgress as? UpdateDownloadProgress.Downloading
                    val percent = progressState?.progressPercent ?: 0
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Downloading APK update...",
                                fontSize = 12.sp,
                                color = CineTextSecondary
                            )
                            Text(
                                text = if (percent >= 0) "$percent%" else "Downloading...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CineGold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        if (percent >= 0) {
                            LinearProgressIndicator(
                                progress = { percent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = CineRedPrimary,
                                trackColor = CineSurfaceElevated
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = CineRedPrimary,
                                trackColor = CineSurfaceElevated
                            )
                        }
                    }
                }

                if (isReady) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CineGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Download complete. Starting installer...",
                            fontSize = 12.sp,
                            color = CineGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (isError) {
                    val errMsg = (downloadProgress as UpdateDownloadProgress.Error).message
                    Text(
                        text = "Error: $errMsg",
                        fontSize = 12.sp,
                        color = Color(0xFFFF5252),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons with 'Download Now'
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onStartUpdate,
                        enabled = !isDownloading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("download_now_button")
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Downloading Update...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isReady) "Install Now" else "Download Now",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    if (!updateInfo.isForceUpdate && !isDownloading) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CineCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("remind_me_later_button")
                        ) {
                            Text(
                                text = "Remind Me Later",
                                color = CineTextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
