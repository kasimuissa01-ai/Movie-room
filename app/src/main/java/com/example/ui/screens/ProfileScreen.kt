package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.CineBlack
import com.example.ui.theme.CineCardBorder
import com.example.ui.theme.CineGold
import com.example.ui.theme.CineRedPrimary
import com.example.ui.theme.CineSurface
import com.example.ui.theme.CineSurfaceElevated
import com.example.ui.theme.CineTextMuted
import com.example.ui.theme.CineTextPrimary
import com.example.ui.theme.CineTextSecondary

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import com.example.data.firebase.FirestoreUserRecord
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import com.example.ui.theme.CineGreen
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.SampleMovies
import com.example.notification.MovieNotificationHelper
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onReplayOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
    isTmdbLive: Boolean = false,
    isAdminLoggedIn: Boolean = false,
    onAdminLoginClick: () -> Unit = {},
    onAdminLogoutClick: () -> Unit = {},
    onUploadMovieClick: () -> Unit = {},
    onOpenAdminReleasePublisher: () -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    currentVersionName: String = "1.0.0",
    currentVersionCode: Long = 1L,
    uploadedMoviesCount: Int = 0,
    isGoogleSignedIn: Boolean = false,
    isFirestoreSynced: Boolean = false,
    userDisplayName: String = "",
    userEmail: String = "",
    userPhotoUrl: String = "",
    firestoreUsers: List<FirestoreUserRecord> = emptyList(),
    isLoadingFirestoreUsers: Boolean = false,
    onRefreshFirestoreUsers: () -> Unit = {},
    onGoogleAuthClick: () -> Unit = {},
    onSignOutGoogleClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var streamingQuality by remember { mutableStateOf("4K Ultra HD") }
    var downloadWifiOnly by remember { mutableStateOf(true) }

    var notificationsEnabled by remember {
        mutableStateOf(MovieNotificationHelper.isNotificationsEnabled(context))
    }
    var reminderHour by remember {
        mutableStateOf(MovieNotificationHelper.getDailyReminderHour(context))
    }
    var hasPostNotificationPermission by remember {
        mutableStateOf(MovieNotificationHelper.hasNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPostNotificationPermission = isGranted
        if (isGranted) {
            notificationsEnabled = true
            MovieNotificationHelper.setNotificationsEnabled(context, true)
            Toast.makeText(context, "Daily movie alerts enabled!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission is required to receive movie alerts", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CineBlack)
            .statusBarsPadding()
            .testTag("profile_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 20.dp,
                vertical = 16.dp
            )
        ) {
            // Profile Header Card
            item(key = "user_header") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CineSurface),
                    border = BorderStroke(1.dp, CineCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // User Avatar
                            Box {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(
                                            if (isGoogleSignedIn && userPhotoUrl.isNotBlank()) userPhotoUrl
                                            else "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=300&q=80"
                                        )
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = userDisplayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, if (isGoogleSignedIn) Color(0xFF4285F4) else CineGold, CircleShape)
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = if (isGoogleSignedIn) Color.White else CineGold,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(22.dp)
                                ) {
                                    if (isGoogleSignedIn) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_google_logo),
                                            contentDescription = "Google",
                                            modifier = Modifier.padding(2.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.WorkspacePremium,
                                            contentDescription = "VIP",
                                            tint = CineBlack,
                                            modifier = Modifier.padding(3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isGoogleSignedIn && userDisplayName.isNotBlank()) userDisplayName else if (isGoogleSignedIn) userEmail else "Guest User (Mgeni)",
                                    color = CineTextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (isGoogleSignedIn) "Google Connected • $userEmail" else "Movie Room Ultra VIP • 4K HDR",
                                    color = if (isGoogleSignedIn) Color(0xFF4285F4) else CineGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isGoogleSignedIn && isFirestoreSynced) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDone,
                                            contentDescription = "Firestore Synced",
                                            tint = Color(0xFF34A853),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Firestore Cloud Active",
                                            color = Color(0xFF34A853),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else {
                                        Text(
                                            text = "Movie Universe Member",
                                            color = CineTextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Google Sign-In / Account Action Banner
                        if (isGoogleSignedIn) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onGoogleAuthClick,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CineTextPrimary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_google_logo),
                                        contentDescription = "Google",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Switch Account", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = onSignOutGoogleClick,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CineTextMuted),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Sign Out", fontSize = 12.sp)
                                }
                            }
                        } else {
                            Surface(
                                onClick = onGoogleAuthClick,
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFA20916),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_google_logo),
                                        contentDescription = "Google Logo",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Jiunge na Google (Sign In)",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "section_preferences") {
                Text(
                    text = "Preferences",
                    color = CineTextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
            }

            // Streaming Quality Setting
            item(key = "setting_quality") {
                SettingItem(
                    icon = Icons.Default.HighQuality,
                    title = "Streaming Quality",
                    value = streamingQuality,
                    onClick = {
                        streamingQuality = when (streamingQuality) {
                            "4K Ultra HD" -> "1080p Full HD"
                            "1080p Full HD" -> "Data Saver (720p)"
                            else -> "4K Ultra HD"
                        }
                    }
                )
            }

            // Download preferences
            item(key = "setting_downloads") {
                SettingToggleItem(
                    icon = Icons.Default.Download,
                    title = "Download over Wi-Fi Only",
                    subtitle = "Saves cellular data consumption",
                    checked = downloadWifiOnly,
                    onCheckedChange = { downloadWifiOnly = it }
                )
            }

            // Daily Movie Push Notifications
            item(key = "setting_notifications") {
                SettingToggleItem(
                    icon = if (notificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                    title = "Daily Movie Recommendations",
                    subtitle = if (!hasPostNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        "Permission required • Tap to grant notification access"
                    } else {
                        "Daily watch reminders, watchlist picks & new movie alerts"
                    },
                    checked = notificationsEnabled,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPostNotificationPermission) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                notificationsEnabled = true
                                MovieNotificationHelper.setNotificationsEnabled(context, true)
                                Toast.makeText(context, "Daily movie alerts enabled!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            notificationsEnabled = false
                            MovieNotificationHelper.setNotificationsEnabled(context, false)
                            Toast.makeText(context, "Daily movie alerts disabled", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // Daily Reminder Schedule Time
            if (notificationsEnabled) {
                item(key = "setting_reminder_time") {
                    val timeLabel = when (reminderHour) {
                        13 -> "1:00 PM (Matinee)"
                        18 -> "6:00 PM (Evening)"
                        20 -> "8:00 PM (Prime Time)"
                        21 -> "9:00 PM (Late Night)"
                        else -> "$reminderHour:00"
                    }
                    SettingItem(
                        icon = Icons.Default.Alarm,
                        title = "Daily Alert Schedule",
                        value = timeLabel,
                        onClick = {
                            val nextHour = when (reminderHour) {
                                20 -> 21
                                21 -> 13
                                13 -> 18
                                else -> 20
                            }
                            reminderHour = nextHour
                            MovieNotificationHelper.setDailyReminderTime(context, nextHour, 0)
                            Toast.makeText(context, "Daily alert set to $timeLabel", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Instant Test Notification Trigger
                item(key = "setting_test_notification") {
                    SettingItem(
                        icon = Icons.Default.Send,
                        title = "Send Test Notification Now",
                        value = "Preview Alert",
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPostNotificationPermission) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                coroutineScope.launch {
                                    val movie = SampleMovies.allMovies.firstOrNull() ?: return@launch
                                    MovieNotificationHelper.showTestNotification(context, movie)
                                    Toast.makeText(context, "Test notification dispatched! Check your status bar.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
            }

            item(key = "section_account") {
                Text(
                    text = "Application & Privacy",
                    color = CineTextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
            }

            // Google Account Authentication
            item(key = "setting_google_auth") {
                SettingItem(
                    icon = Icons.Default.Security,
                    title = "Google Account",
                    value = if (isGoogleSignedIn) userEmail else "Connect Account",
                    onClick = onGoogleAuthClick
                )
            }

            // Security & Privacy
            item(key = "setting_security") {
                SettingItem(
                    icon = Icons.Default.Security,
                    title = "Account Security & Devices",
                    value = "Active",
                    onClick = {}
                )
            }

            // Check for In-App Updates
            item(key = "setting_check_updates") {
                SettingItem(
                    icon = Icons.Default.SystemUpdate,
                    title = "App Version & Updates",
                    value = "v$currentVersionName",
                    onClick = onCheckForUpdates
                )
            }

            // Admin Portal & Firestore Database Management (ONLY visible when admin grapherkidd0@gmail.com is logged in)
            if (isAdminLoggedIn) {
                item(key = "section_admin") {
                    Text(
                        text = "Administrator Portal (grapherkidd0@gmail.com)",
                        color = CineGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                    )
                }

                item(key = "admin_dashboard_card") {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CineSurface),
                        border = BorderStroke(1.dp, CineRedPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(CineRedPrimary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AdminPanelSettings,
                                            contentDescription = null,
                                            tint = CineRedPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Administrator Mode",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = CineTextPrimary
                                        )
                                        Text(
                                            text = "Authorized as $userEmail",
                                            fontSize = 11.sp,
                                            color = CineGreen
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CineGreen.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "UNLOCKED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = CineGreen
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Catalog: $uploadedMoviesCount uploaded movie(s) stored in Cloudflare R2 bucket.",
                                fontSize = 12.sp,
                                color = CineTextMuted
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Primary Admin Upload Button
                            Button(
                                onClick = onUploadMovieClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_profile_upload_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Upload Movie (Admin Only)", fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Admin Publish OTA App Update Button
                            Button(
                                onClick = onOpenAdminReleasePublisher,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CineGold)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Publish,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Publish OTA App Update to Users", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = onAdminLogoutClick,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = null,
                                    tint = CineTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Exit Administrator Mode", color = CineTextMuted, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Firestore Users Management Card (Admin only)
                item(key = "section_firestore_database") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Firestore Users Directory",
                            color = CineTextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Surface(
                            onClick = onRefreshFirestoreUsers,
                            shape = RoundedCornerShape(8.dp),
                            color = CineSurfaceElevated,
                            border = BorderStroke(1.dp, CineCardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isLoadingFirestoreUsers) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = CineGold,
                                        strokeWidth = 1.5.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        tint = CineGold,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Refresh",
                                    color = CineGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item(key = "firestore_users_card") {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CineSurface),
                        border = BorderStroke(1.dp, if (firestoreUsers.isNotEmpty()) Color(0xFF34A853).copy(alpha = 0.5f) else CineCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF34A853).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.People,
                                            contentDescription = null,
                                            tint = Color(0xFF34A853),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Collection: users",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = CineTextPrimary
                                        )
                                        Text(
                                            text = "movieroom-334fb Firestore",
                                            fontSize = 10.sp,
                                            color = CineTextMuted
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF34A853).copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${firestoreUsers.size} REGISTERED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF34A853)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (firestoreUsers.isEmpty()) {
                                if (isLoadingFirestoreUsers) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = CineGold,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Loading users from Firestore...",
                                            fontSize = 12.sp,
                                            color = CineTextMuted
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(CineSurfaceElevated, RoundedCornerShape(10.dp))
                                            .padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Current user: $userEmail",
                                            fontSize = 12.sp,
                                            color = CineTextSecondary,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    firestoreUsers.forEach { userDoc ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(CineSurfaceElevated, RoundedCornerShape(10.dp))
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                AsyncImage(
                                                    model = if (userDoc.photoUrl.isNotBlank()) userDoc.photoUrl
                                                    else "https://image.tmdb.org/t/p/w185/wo2hJpn04vbtmh0B9utCFdsQhxM.jpg",
                                                    contentDescription = userDoc.displayName,
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .border(1.dp, Color(0xFF4285F4), CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = userDoc.displayName.ifBlank { "User" },
                                                        color = CineTextPrimary,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Text(
                                                        text = userDoc.email.ifBlank { userDoc.uid },
                                                        color = CineTextMuted,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFF4285F4).copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = userDoc.role.uppercase(),
                                                    color = Color(0xFF4285F4),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Replay Onboarding
            item(key = "setting_onboarding") {
                SettingItem(
                    icon = Icons.Default.Replay,
                    title = "Replay Welcome Tour",
                    value = "Preview",
                    onClick = onReplayOnboarding
                )
            }

            // App Version Footer
            item(key = "app_version_footer") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 36.dp, bottom = 96.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Movie Room for Android",
                        color = CineTextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Version 2.4.0 (Build 2026.04) • Dolby Atmos Certified",
                        color = CineTextMuted.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CineSurface),
        border = BorderStroke(1.dp, CineCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CineRedPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = title,
                    color = CineTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    color = CineTextMuted,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = CineTextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CineSurface),
        border = BorderStroke(1.dp, CineCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CineRedPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        color = CineTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = subtitle,
                        color = CineTextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = CineRedPrimary,
                    uncheckedThumbColor = CineTextMuted,
                    uncheckedTrackColor = CineSurfaceElevated
                )
            )
        }
    }
}
