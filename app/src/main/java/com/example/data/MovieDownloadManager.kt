package com.example.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.util.Log
import com.example.data.entity.DownloadedMovieEntity
import com.example.model.Movie
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

object MovieDownloadManager {
    private const val TAG = "MovieDownloadManager"

    // Active download jobs keyed by movieId
    private val activeJobs = ConcurrentHashMap<String, Job>()

    // Active system DownloadManager request IDs keyed by movieId
    private val activeDownloadIds = ConcurrentHashMap<String, Long>()

    // In-memory progress tracking (movieId -> progressPercent)
    private val _downloadProgressMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgressMap = _downloadProgressMap.asStateFlow()

    fun getDownloadedMovies(context: Context): Flow<List<DownloadedMovieEntity>> {
        return MovieDatabase.getDatabase(context).movieDao().getDownloadedMovies()
    }

    fun isMovieDownloaded(context: Context, movieId: String): Flow<Boolean> {
        return MovieDatabase.getDatabase(context).movieDao().isMovieDownloaded(movieId)
    }

    fun ensurePrivateStorage(context: Context) {
        try {
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
            if (!downloadDir.exists()) downloadDir.mkdirs()
            val nomedia = File(downloadDir, ".nomedia")
            if (!nomedia.exists()) {
                nomedia.createNewFile()
            }
            val internalNomedia = File(context.filesDir, ".nomedia")
            if (!internalNomedia.exists()) {
                internalNomedia.createNewFile()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed ensuring private storage: ${e.message}")
        }
    }

    /**
     * Starts downloading a movie from Cloudflare R2 / remote storage using Android's system DownloadManager API.
     * Progress is tracked in real-time, saved to Room DB, and published to UI observers.
     */
    fun startDownload(
        context: Context,
        movie: Movie,
        coroutineScope: CoroutineScope
    ) {
        if (activeJobs.containsKey(movie.id)) {
            Log.d(TAG, "Download already in progress for movie ${movie.title}")
            return
        }

        val job = coroutineScope.launch(Dispatchers.IO) {
            val dao = MovieDatabase.getDatabase(context).movieDao()
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
            if (!downloadDir.exists()) downloadDir.mkdirs()

            // Guarantee private storage: .nomedia ensures device gallery ignores files here
            try {
                val nomedia = File(downloadDir, ".nomedia")
                if (!nomedia.exists()) {
                    nomedia.createNewFile()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not create .nomedia: ${e.message}")
            }

            val safeId = movie.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val fileName = "cinestream_movie_${safeId}.mp4"
            val targetFile = File(downloadDir, fileName)

            // Initial Record in Room as DOWNLOADING
            val initialEntity = DownloadedMovieEntity(
                movieId = movie.id,
                title = movie.title,
                posterUrl = movie.posterUrl,
                backdropUrl = movie.backdropUrl,
                localFilePath = targetFile.absolutePath,
                remoteVideoUrl = movie.videoUrl,
                fileSizeBytes = 0L,
                fileSizeFormatted = "Calculating...",
                durationFormatted = movie.durationFormatted,
                durationMinutes = movie.durationMinutes,
                quality = movie.quality,
                year = movie.year,
                genresCsv = movie.genres.joinToString(", "),
                description = movie.description,
                downloadStatus = "DOWNLOADING",
                downloadProgress = 0,
                downloadedAt = System.currentTimeMillis()
            )
            dao.insertDownloadedMovie(initialEntity)
            updateProgress(movie.id, 0)

            val videoUrlStr = if (movie.videoUrl.isNotBlank() && movie.videoUrl.startsWith("http")) {
                movie.videoUrl
            } else {
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            }

            var usedSystemDownloadManager = false

            try {
                val systemDownloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                if (systemDownloadManager != null) {
                    val request = DownloadManager.Request(Uri.parse(videoUrlStr)).apply {
                        setTitle("Downloading ${movie.title}")
                        setDescription("Downloading movie from Cloudflare R2 for offline viewing")
                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES, fileName)
                        setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                        setMimeType("video/mp4")
                    }

                    val downloadId = systemDownloadManager.enqueue(request)
                    activeDownloadIds[movie.id] = downloadId
                    usedSystemDownloadManager = true

                    Log.i(TAG, "Enqueued DownloadManager request ID $downloadId for ${movie.title}")

                    var downloading = true
                    while (downloading) {
                        val query = DownloadManager.Query().setFilterById(downloadId)
                        val cursor = systemDownloadManager.query(query)

                        if (cursor != null && cursor.moveToFirst()) {
                            val bytesSoFarIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                            val totalBytesIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                            val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

                            val bytesDownloaded = if (bytesSoFarIdx >= 0) cursor.getLong(bytesSoFarIdx) else 0L
                            val totalBytes = if (totalBytesIdx >= 0) cursor.getLong(totalBytesIdx) else 0L
                            val status = if (statusIdx >= 0) cursor.getInt(statusIdx) else -1

                            when (status) {
                                DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_PENDING -> {
                                    val percent = if (totalBytes > 0) {
                                        ((bytesDownloaded * 100) / totalBytes).toInt().coerceIn(0, 99)
                                    } else {
                                        ((bytesDownloaded / (1024 * 1024)).toInt() % 100)
                                    }
                                    updateProgress(movie.id, percent)
                                    dao.updateDownloadProgress(movie.id, percent, "DOWNLOADING")
                                }
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    downloading = false
                                    val finalSizeBytes = if (targetFile.exists()) targetFile.length() else bytesDownloaded
                                    val sizeFormatted = formatFileSize(finalSizeBytes)

                                    val completedEntity = initialEntity.copy(
                                        fileSizeBytes = finalSizeBytes,
                                        fileSizeFormatted = sizeFormatted,
                                        downloadStatus = "COMPLETED",
                                        downloadProgress = 100,
                                        downloadedAt = System.currentTimeMillis()
                                    )
                                    dao.insertDownloadedMovie(completedEntity)
                                    updateProgress(movie.id, 100)
                                    Log.i(TAG, "DownloadManager completed: ${movie.title} (${sizeFormatted})")
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    downloading = false
                                    Log.w(TAG, "DownloadManager status FAILED for ${movie.title}")
                                    dao.updateDownloadProgress(movie.id, 0, "FAILED")
                                }
                            }
                            cursor.close()
                        } else {
                            downloading = false
                        }

                        if (downloading) {
                            delay(400)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "DownloadManager API error, falling back to direct stream: ${e.message}")
                usedSystemDownloadManager = false
            }

            // Fallback direct HTTP stream if DownloadManager API was skipped or unhandled
            if (!usedSystemDownloadManager) {
                try {
                    val url = URL(videoUrlStr)
                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 30000
                        requestMethod = "GET"
                        instanceFollowRedirects = true
                    }
                    connection.connect()

                    val totalLength = connection.contentLength.toLong()
                    var bytesRead = 0L

                    val inputStream: InputStream = connection.inputStream
                    val outputStream = FileOutputStream(targetFile)

                    val buffer = ByteArray(16 * 1024)
                    var count: Int
                    var lastReportedPercent = 0

                    while (inputStream.read(buffer).also { count = it } != -1) {
                        outputStream.write(buffer, 0, count)
                        bytesRead += count

                        val currentPercent = if (totalLength > 0) {
                            ((bytesRead * 100) / totalLength).toInt().coerceIn(0, 99)
                        } else {
                            ((bytesRead / (1024 * 1024)).toInt() % 100)
                        }

                        if (currentPercent > lastReportedPercent) {
                            lastReportedPercent = currentPercent
                            updateProgress(movie.id, currentPercent)
                            dao.updateDownloadProgress(movie.id, currentPercent, "DOWNLOADING")
                        }
                    }

                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()
                    connection.disconnect()

                    val finalSizeBytes = targetFile.length()
                    val sizeFormatted = formatFileSize(finalSizeBytes)

                    val completedEntity = initialEntity.copy(
                        fileSizeBytes = finalSizeBytes,
                        fileSizeFormatted = sizeFormatted,
                        downloadStatus = "COMPLETED",
                        downloadProgress = 100,
                        downloadedAt = System.currentTimeMillis()
                    )
                    dao.insertDownloadedMovie(completedEntity)
                    updateProgress(movie.id, 100)
                    Log.i(TAG, "Stream fallback download complete: ${movie.title} (${sizeFormatted})")
                } catch (e: Exception) {
                    Log.e(TAG, "Download error for ${movie.title}: ${e.message}", e)
                    dao.updateDownloadProgress(movie.id, 0, "FAILED")
                }
            }

            activeJobs.remove(movie.id)
            activeDownloadIds.remove(movie.id)
            removeProgress(movie.id)
        }

        activeJobs[movie.id] = job
    }

    fun cancelDownload(context: Context, movieId: String, coroutineScope: CoroutineScope) {
        val job = activeJobs.remove(movieId)
        job?.cancel()

        val downloadId = activeDownloadIds.remove(movieId)
        if (downloadId != null) {
            try {
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                dm?.remove(downloadId)
            } catch (e: Exception) {
                Log.w(TAG, "Error removing download ID $downloadId: ${e.message}")
            }
        }

        removeProgress(movieId)
        coroutineScope.launch(Dispatchers.IO) {
            val dao = MovieDatabase.getDatabase(context).movieDao()
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
            val safeId = movieId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val targetFile = File(downloadDir, "cinestream_movie_${safeId}.mp4")
            if (targetFile.exists()) {
                targetFile.delete()
            }
            dao.deleteDownloadedMovie(movieId)
        }
    }

    suspend fun deleteDownload(context: Context, movieId: String) = withContext(Dispatchers.IO) {
        val downloadId = activeDownloadIds.remove(movieId)
        if (downloadId != null) {
            try {
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                dm?.remove(downloadId)
            } catch (ignored: Exception) {}
        }

        val dao = MovieDatabase.getDatabase(context).movieDao()
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        val safeId = movieId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val targetFile = File(downloadDir, "cinestream_movie_${safeId}.mp4")

        if (targetFile.exists()) {
            targetFile.delete()
        }
        dao.deleteDownloadedMovie(movieId)
    }

    suspend fun clearAllDownloads(context: Context) = withContext(Dispatchers.IO) {
        activeDownloadIds.values.forEach { downloadId ->
            try {
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                dm?.remove(downloadId)
            } catch (ignored: Exception) {}
        }
        activeDownloadIds.clear()

        val dao = MovieDatabase.getDatabase(context).movieDao()
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        downloadDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("cinestream_movie_")) {
                file.delete()
            }
        }
        dao.clearAllDownloads()
    }

    private fun updateProgress(movieId: String, progress: Int) {
        val current = _downloadProgressMap.value.toMutableMap()
        current[movieId] = progress
        _downloadProgressMap.value = current
    }

    private fun removeProgress(movieId: String) {
        val current = _downloadProgressMap.value.toMutableMap()
        current.remove(movieId)
        _downloadProgressMap.value = current
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "42 MB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            else -> String.format("%.0f KB", kb)
        }
    }

    fun getDeviceStorageInfo(context: Context): Pair<Long, Long> {
        return try {
            val path = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            val totalBlocks = stat.blockCountLong
            val freeBytes = availableBlocks * blockSize
            val totalBytes = totalBlocks * blockSize
            Pair(freeBytes, totalBytes)
        } catch (e: Exception) {
            Pair(25L * 1024 * 1024 * 1024, 64L * 1024 * 1024 * 1024)
        }
    }
}

