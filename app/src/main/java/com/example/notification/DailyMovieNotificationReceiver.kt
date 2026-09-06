package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.MovieDatabase
import com.example.data.SampleMovies
import com.example.model.Movie
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class DailyMovieNotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DailyMovieReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "Daily notification alarm triggered")

        // 1. Immediately schedule the next occurrence for tomorrow
        DailyNotificationScheduler.scheduleDailyReminder(context)

        // 2. Check if notifications are enabled
        if (!MovieNotificationHelper.isNotificationsEnabled(context)) {
            Log.d(TAG, "Notifications disabled by user, skipping dispatch")
            return
        }

        // 3. Use goAsync() to fetch data from Room & SampleMovies on background thread
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = MovieDatabase.getDatabase(context)
                val movieDao = db.movieDao()

                // Check Watchlist first: Priority reminder for unwatched items
                val watchlistIds = movieDao.getWatchlistMovieIdsDirect()
                var selectedMovie: Movie? = null
                var isFromWatchlist = false

                if (watchlistIds.isNotEmpty()) {
                    val dayIndex = (Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) % watchlistIds.size
                    val targetId = watchlistIds[dayIndex]
                    selectedMovie = SampleMovies.getMovieById(targetId)
                    if (selectedMovie != null) {
                        isFromWatchlist = true
                    }
                }

                // If not in watchlist, check user uploaded movies
                if (selectedMovie == null) {
                    val uploaded = movieDao.getUploadedMoviesDirect()
                    if (uploaded.isNotEmpty()) {
                        val dayIndex = (Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) % uploaded.size
                        selectedMovie = uploaded[dayIndex].toMovie()
                    }
                }

                // Fallback to rotating curated SampleMovies
                if (selectedMovie == null) {
                    val allCurated = SampleMovies.allMovies
                    if (allCurated.isNotEmpty()) {
                        val dayIndex = (Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) % allCurated.size
                        selectedMovie = allCurated[dayIndex]
                    }
                }

                if (selectedMovie != null) {
                    Log.d(TAG, "Dispatching daily movie reminder for '${selectedMovie.title}'")
                    MovieNotificationHelper.showDailyMovieNotification(
                        context = context,
                        movie = selectedMovie,
                        isFromWatchlist = isFromWatchlist
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating daily notification: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
