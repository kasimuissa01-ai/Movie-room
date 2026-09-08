package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.Movie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object MovieNotificationHelper {

    const val CHANNEL_DAILY_PICKS = "daily_movie_picks"
    const val CHANNEL_NEW_RELEASES = "new_movie_releases"

    const val PREFS_NAME = "cinestream_notification_prefs"
    const val KEY_NOTIFICATIONS_ENABLED = "daily_notifications_enabled"
    const val KEY_REMINDER_HOUR = "daily_reminder_hour" // 0-23, default 20 (8 PM)
    const val KEY_REMINDER_MINUTE = "daily_reminder_minute" // 0-59, default 0

    const val EXTRA_MOVIE_ID = "extra_movie_id"
    const val EXTRA_ACTION = "extra_action"
    const val ACTION_PLAY = "play"
    const val ACTION_DETAILS = "details"
    const val ACTION_EXPLORE = "explore"

    private const val NOTIFICATION_ID_DAILY = 1001
    private const val NOTIFICATION_ID_NEW_RELEASE = 1002
    private const val NOTIFICATION_ID_TEST = 1003

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isNotificationsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
        if (enabled) {
            DailyNotificationScheduler.scheduleDailyReminder(context)
        } else {
            DailyNotificationScheduler.cancelDailyReminder(context)
        }
    }

    fun getDailyReminderHour(context: Context): Int {
        return getPrefs(context).getInt(KEY_REMINDER_HOUR, 20) // Default: 8:00 PM
    }

    fun getDailyReminderMinute(context: Context): Int {
        return getPrefs(context).getInt(KEY_REMINDER_MINUTE, 0)
    }

    fun setDailyReminderTime(context: Context, hour: Int, minute: Int) {
        getPrefs(context).edit()
            .putInt(KEY_REMINDER_HOUR, hour)
            .putInt(KEY_REMINDER_MINUTE, minute)
            .apply()
        if (isNotificationsEnabled(context)) {
            DailyNotificationScheduler.scheduleDailyReminder(context)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // Channel 1: Daily Movie Recommendations & Watchlist Alerts
            val dailyChannel = NotificationChannel(
                CHANNEL_DAILY_PICKS,
                "Daily Movie Picks & Watchlist",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily evening movie recommendations, watchlist reminders, and curated titles."
                enableLights(true)
                lightColor = 0xFFE50914.toInt()
                enableVibration(true)
                setShowBadge(true)
            }

            // Channel 2: New Movie Releases
            val releaseChannel = NotificationChannel(
                CHANNEL_NEW_RELEASES,
                "New Releases & Premieres",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Instant alerts when new movies and trailers are added to Movie Room."
                enableLights(true)
                lightColor = 0xFFFFD700.toInt()
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(dailyChannel)
            notificationManager.createNotificationChannel(releaseChannel)
        }
    }

    /**
     * Dispatches daily movie notification with rich poster artwork and quick actions
     */
    suspend fun showDailyMovieNotification(
        context: Context,
        movie: Movie,
        isFromWatchlist: Boolean = false
    ) {
        if (!isNotificationsEnabled(context)) return
        createNotificationChannels(context)

        val title = if (isFromWatchlist) {
            "🎬 Still on your Watchlist: ${movie.title}"
        } else {
            "🍿 Tonight's Cinema Pick: ${movie.title}"
        }

        val text = if (isFromWatchlist) {
            "You saved this to watch! Dive in tonight in 4K Ultra HD (${movie.durationMinutes}m • ★ ${movie.rating})."
        } else {
            "${movie.description.take(110)}... Rated ★ ${movie.rating} • Stream now on Movie Room."
        }

        val posterBitmap = fetchBitmapFromUrl(movie.backdropUrl.ifBlank { movie.posterUrl })

        // 1. Content Intent (Tapping notification opens Movie Details)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_MOVIE_ID, movie.id)
            putExtra(EXTRA_ACTION, ACTION_DETAILS)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            movie.id.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Action: Watch Now (Directly opens Cinema Player)
        val playIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_MOVIE_ID, movie.id)
            putExtra(EXTRA_ACTION, ACTION_PLAY)
        }
        val playPendingIntent = PendingIntent.getActivity(
            context,
            movie.id.hashCode() + 1,
            playIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Action: Explore Catalog
        val exploreIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION, ACTION_EXPLORE)
        }
        val explorePendingIntent = PendingIntent.getActivity(
            context,
            10099,
            exploreIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_DAILY_PICKS)
            .setSmallIcon(R.drawable.ic_notification_movie)
            .setColor(0xFFE50914.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .addAction(R.drawable.ic_notification_movie, "▶ Watch Now", playPendingIntent)
            .addAction(R.drawable.ic_notification_movie, "Explore", explorePendingIntent)

        if (posterBitmap != null) {
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(posterBitmap)
                    .setBigContentTitle(title)
                    .setSummaryText("★ ${movie.rating} • ${movie.category} • ${movie.year}")
            )
        } else {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(text)
                    .setBigContentTitle(title)
                    .setSummaryText("★ ${movie.rating} • ${movie.category}")
            )
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_DAILY, builder.build())
        } catch (e: SecurityException) {
            // Missing POST_NOTIFICATIONS permission
        }
    }

    /**
     * Broadcasts notification when a new movie is uploaded or published
     */
    suspend fun showNewMovieNotification(
        context: Context,
        movie: Movie
    ) {
        if (!isNotificationsEnabled(context)) return
        createNotificationChannels(context)

        val title = "🔥 New Release: ${movie.title} is Now Streaming!"
        val text = "Just arrived on Movie Room! Rated ★ ${movie.rating} • Tap to watch now."
        val posterBitmap = fetchBitmapFromUrl(movie.backdropUrl.ifBlank { movie.posterUrl })

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_MOVIE_ID, movie.id)
            putExtra(EXTRA_ACTION, ACTION_DETAILS)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            movie.id.hashCode() + 200,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_MOVIE_ID, movie.id)
            putExtra(EXTRA_ACTION, ACTION_PLAY)
        }
        val playPendingIntent = PendingIntent.getActivity(
            context,
            movie.id.hashCode() + 201,
            playIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_NEW_RELEASES)
            .setSmallIcon(R.drawable.ic_notification_movie)
            .setColor(0xFFFFD700.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_PROMO)
            .addAction(R.drawable.ic_notification_movie, "▶ Stream Now", playPendingIntent)

        if (posterBitmap != null) {
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(posterBitmap)
                    .setBigContentTitle(title)
                    .setSummaryText("Brand New Release • ★ ${movie.rating}")
            )
        } else {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(movie.description)
                    .setBigContentTitle(title)
            )
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_NEW_RELEASE, builder.build())
        } catch (e: SecurityException) {
            // Missing POST_NOTIFICATIONS permission
        }
    }

    /**
     * Instantly dispatches a test notification for the user to preview on their screen
     */
    suspend fun showTestNotification(context: Context, movie: Movie? = null) {
        createNotificationChannels(context)

        val title = if (movie != null) "🎬 Daily Alert: ${movie.title}" else "🎬 Movie Room Daily Alert"
        val text = if (movie != null) {
            "Rated ★ ${movie.rating} • Ready to stream from your storage database."
        } else {
            "Your daily notifications are active! Check your storage catalog for new releases."
        }
        val posterBitmap = if (movie != null) fetchBitmapFromUrl(movie.backdropUrl.ifBlank { movie.posterUrl }) else null

        val playIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (movie != null) {
                putExtra(EXTRA_MOVIE_ID, movie.id)
                putExtra(EXTRA_ACTION, ACTION_PLAY)
            }
        }
        val playPendingIntent = PendingIntent.getActivity(
            context,
            9999,
            playIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_DAILY_PICKS)
            .setSmallIcon(R.drawable.ic_notification_movie)
            .setColor(0xFFE50914.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(playPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(R.drawable.ic_notification_movie, "▶ Open App", playPendingIntent)

        if (posterBitmap != null) {
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(posterBitmap)
                    .setBigContentTitle(title)
                    .setSummaryText("Daily Movie Room Alert")
            )
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TEST, builder.build())
        } catch (e: SecurityException) {
            // Security exception if permission denied
        }
    }

    private suspend fun fetchBitmapFromUrl(urlString: String): Bitmap? = withContext(Dispatchers.IO) {
        if (urlString.isBlank() || !urlString.startsWith("http")) return@withContext null
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.connect()
            val input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            null
        }
    }
}
