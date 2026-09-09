package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.text.style.TextOverflow
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
        onInstallApk = {
            val readyState = downloadProgress as? UpdateDownloadProgress.ReadyToInstall
            if (readyState != null) {
                AppUpdateManager.installApk(context, java.io.File(readyState.apkFilePath))
            }
        },
        onDismiss = onDismiss,
        onMinimize = onDismiss,
        modifier = modifier
    )
}

@Composable
fun AppUpdateDialog(
    updateInfo: AppUpdateInfo,
    downloadProgress: UpdateDownloadProgress,
    onStartUpdate: () -> Unit,
    onInstallApk: () -> Unit,
    onDismiss: () -> Unit,
    onMinimize: () -> Unit = onDismiss,
    modifier: Modifier = Modifier
) {
    val isDownloading = downloadProgress is UpdateDownloadProgress.Downloading
    val isReady = downloadProgress is UpdateDownloadProgress.ReadyToInstall
    val isError = downloadProgress is UpdateDownloadProgress.Error

    Dialog(
        onDismissRequest = {
            if (!updateInfo.isForceUpdate) {
                if (isDownloading) {
                    onMinimize()
                } else {
                    onDismiss()
                }
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !updateInfo.isForceUpdate,
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
                // Header with icon, version number and minimize button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                                text = if (isReady) "Update Ready" else "Update Available",
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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Minimize button for downloading state
                        if (isDownloading) {
                            IconButton(
                                onClick = onMinimize,
                                modifier = Modifier.testTag("minimize_update_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Minimize to background",
                                    tint = CineGold,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        if (!updateInfo.isForceUpdate) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("close_update_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = CineTextMuted
                                )
                            }
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color = CineGold,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Downloading in background...",
                                    fontSize = 12.sp,
                                    color = CineTextSecondary
                                )
                            }
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
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "You can minimize this dialog and continue browsing movies while downloading.",
                            fontSize = 11.sp,
                            color = CineTextMuted
                        )
                    }
                }

                if (isReady) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CineGreen.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, CineGreen.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CineGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Download complete! Tap 'Install Now' to finish updating.",
                                fontSize = 12.sp,
                                color = CineGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
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

                // Action Buttons with 'Download Now' / 'Install Now' / 'Minimize'
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isReady) {
                        Button(
                            onClick = onInstallApk,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CineGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("install_now_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.InstallMobile,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Install Update Now",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontSize = 15.sp
                            )
                        }
                    } else if (isDownloading) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onMinimize,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("minimize_download_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Minimize & Continue", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    } else {
                        Button(
                            onClick = onStartUpdate,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("download_now_button")
                        ) {
                            Icon(
                                imageVector = if (isError) Icons.Default.Refresh else Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isError) "Retry Download" else "Download Now",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    if (!updateInfo.isForceUpdate) {
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
                                text = if (isDownloading) "Hide Dialog" else "Remind Me Later",
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

/**
 * Compact floating banner shown at the top of the app when an update is downloading
 * or ready in the background after the user minimizes the full dialog.
 */
@Composable
fun BackgroundUpdateBanner(
    updateInfo: AppUpdateInfo,
    downloadProgress: UpdateDownloadProgress,
    onExpand: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDownloading = downloadProgress is UpdateDownloadProgress.Downloading
    val isReady = downloadProgress is UpdateDownloadProgress.ReadyToInstall

    AnimatedVisibility(
        visible = isDownloading || isReady,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isReady) Color(0xFF1B2E1E) else CineSurfaceElevated,
            border = BorderStroke(1.dp, if (isReady) CineGreen else CineGold.copy(alpha = 0.5f)),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable {
                    if (isReady) onInstall() else onExpand()
                }
                .testTag("background_update_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isReady) CineGreen.copy(alpha = 0.2f) else CineGold.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isReady) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CineGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = CineGold,
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isReady) "v${updateInfo.latestVersionName} Ready to Install" else "Updating to v${updateInfo.latestVersionName}...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isReady) CineGreen else CineTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isDownloading) {
                            val percent = (downloadProgress as? UpdateDownloadProgress.Downloading)?.progressPercent ?: 0
                            Text(
                                text = if (percent >= 0) "Downloading in background ($percent%)" else "Downloading update in background...",
                                fontSize = 11.sp,
                                color = CineTextMuted
                            )
                        } else {
                            Text(
                                text = "Tap here to complete installation",
                                fontSize = 11.sp,
                                color = CineTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (isReady) {
                    Button(
                        onClick = onInstall,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CineGreen),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Install", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    IconButton(
                        onClick = onExpand,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandLess,
                            contentDescription = "Expand update details",
                            tint = CineGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

