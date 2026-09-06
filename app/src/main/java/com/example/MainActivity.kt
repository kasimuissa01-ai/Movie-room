package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.notification.DailyNotificationScheduler
import com.example.notification.MovieNotificationHelper
import com.example.ui.CineApp
import com.example.ui.theme.CineBlack
import com.example.ui.theme.MyApplicationTheme

data class NotificationPayload(
    val movieId: String?,
    val action: String?
)

class MainActivity : ComponentActivity() {

    private var pendingNotificationPayload by mutableStateOf<NotificationPayload?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize Notification Channels (Daily picks & New releases)
        MovieNotificationHelper.createNotificationChannels(this)

        // 2. Schedule Daily Background Reminder (8:00 PM evening or user preference)
        DailyNotificationScheduler.scheduleDailyReminder(this)

        // 3. Handle incoming notification clicks on cold start
        extractNotificationPayload(intent)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CineBlack
                ) {
                    CineApp(
                        notificationPayload = pendingNotificationPayload,
                        onClearNotificationPayload = { pendingNotificationPayload = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractNotificationPayload(intent)
    }

    private fun extractNotificationPayload(intent: Intent?) {
        val movieId = intent?.getStringExtra(MovieNotificationHelper.EXTRA_MOVIE_ID)
        val action = intent?.getStringExtra(MovieNotificationHelper.EXTRA_ACTION)
        if (!movieId.isNullOrBlank() || !action.isNullOrBlank()) {
            pendingNotificationPayload = NotificationPayload(movieId, action)
        }
    }
}
