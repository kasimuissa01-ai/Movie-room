package com.example.data.updater

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.firebase.FirebaseAuthManager
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume

object AppUpdateManager {
    private const val TAG = "AppUpdateManager"
    private const val CONFIG_COLLECTION = "config"
    private const val VERSION_DOC = "app_version"

    /**
     * Retrieves current running application version code
     */
    fun getCurrentVersionCode(context: Context): Long {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }
    }

    /**
     * Retrieves current running application version name
     */
    fun getCurrentVersionName(context: Context): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    /**
     * Checks Firestore for remote application version info
     */
    suspend fun checkForUpdates(context: Context): AppUpdateInfo = withContext(Dispatchers.IO) {
        val currentCode = getCurrentVersionCode(context)
        val currentName = getCurrentVersionName(context)
        val firestore = FirebaseAuthManager.getFirestore(context)

        if (firestore == null) {
            return@withContext AppUpdateInfo(
                latestVersionCode = currentCode,
                latestVersionName = currentName,
                isUpdateAvailable = false
            )
        }

        try {
            val docSnapshot = suspendCancellableCoroutine<com.google.firebase.firestore.DocumentSnapshot?> { cont ->
                firestore.collection(CONFIG_COLLECTION).document(VERSION_DOC)
                    .get()
                    .addOnSuccessListener { doc -> cont.resume(doc) }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Failed to check update info: ${e.message}")
                        cont.resume(null)
                    }
            }

            if (docSnapshot != null && docSnapshot.exists()) {
                val latestCode = docSnapshot.getLong("latestVersionCode") ?: currentCode
                val latestName = docSnapshot.getString("latestVersionName") ?: currentName
                val minCode = docSnapshot.getLong("minSupportedVersionCode") ?: 1L
                val apkUrl = docSnapshot.getString("apkDownloadUrl") ?: ""
                val notes = docSnapshot.getString("releaseNotes") ?: "New features and performance optimizations."
                val date = docSnapshot.getString("releaseDate") ?: "Latest"
                val size = docSnapshot.getString("fileSizeFormatted") ?: "APK Update"
                val force = docSnapshot.getBoolean("isForceUpdate") ?: (currentCode < minCode)

                val isAvailable = latestCode > currentCode && apkUrl.isNotBlank()

                return@withContext AppUpdateInfo(
                    latestVersionCode = latestCode,
                    latestVersionName = latestName,
                    minSupportedVersionCode = minCode,
                    apkDownloadUrl = apkUrl,
                    releaseNotes = notes,
                    releaseDate = date,
                    fileSizeFormatted = size,
                    isForceUpdate = force,
                    isUpdateAvailable = isAvailable
                )
            } else {
                // If doc doesn't exist yet, return current status
                return@withContext AppUpdateInfo(
                    latestVersionCode = currentCode,
                    latestVersionName = currentName,
                    isUpdateAvailable = false
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking updates: ${e.message}")
            return@withContext AppUpdateInfo(
                latestVersionCode = currentCode,
                latestVersionName = currentName,
                isUpdateAvailable = false
            )
        }
    }

    /**
     * Downloads the APK file with progress reporting and triggers in-place installation
     */
    suspend fun downloadAndInstallApk(
        context: Context,
        updateInfo: AppUpdateInfo,
        onProgress: (UpdateDownloadProgress) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (updateInfo.apkDownloadUrl.isBlank()) {
            onProgress(UpdateDownloadProgress.Error("Invalid download URL."))
            return@withContext
        }

        try {
            onProgress(UpdateDownloadProgress.Downloading(0, 0L, 0L))

            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
            if (!downloadDir.exists()) downloadDir.mkdirs()

            val apkFile = File(downloadDir, "movieroom-v${updateInfo.latestVersionName}.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            val url = URL(updateInfo.apkDownloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                requestMethod = "GET"
                instanceFollowRedirects = true
            }
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                onProgress(UpdateDownloadProgress.Error("Download failed with HTTP ${connection.responseCode}"))
                return@withContext
            }

            val totalLength = connection.contentLength.toLong()
            var bytesRead = 0L

            val inputStream: InputStream = connection.inputStream
            val outputStream = FileOutputStream(apkFile)

            val buffer = ByteArray(8 * 1024)
            var count: Int

            while (inputStream.read(buffer).also { count = it } != -1) {
                outputStream.write(buffer, 0, count)
                bytesRead += count
                val percent = if (totalLength > 0) {
                    ((bytesRead * 100) / totalLength).toInt().coerceIn(0, 100)
                } else {
                    -1
                }
                onProgress(UpdateDownloadProgress.Downloading(percent, bytesRead, totalLength))
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            connection.disconnect()

            onProgress(UpdateDownloadProgress.ReadyToInstall(apkFile.absolutePath))

            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download error: ${e.message}", e)
            onProgress(UpdateDownloadProgress.Error(e.message ?: "Failed to download update."))
        }
    }

    /**
     * Launches the native Android package installer to install the downloaded APK
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists()) {
                Log.e(TAG, "APK file does not exist: ${apkFile.absolutePath}")
                return
            }

            // For Android 8.0 (API 26) and above, check unknown sources install permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch installer: ${e.message}", e)
        }
    }

    /**
     * Admin functionality to publish a new APK release update directly to Firestore
     */
    suspend fun publishNewRelease(
        context: Context,
        versionCode: Long,
        versionName: String,
        apkDownloadUrl: String,
        releaseNotes: String,
        fileSizeFormatted: String = "45 MB",
        isForceUpdate: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val firestore = FirebaseAuthManager.getFirestore(context) ?: return@withContext false
        return@withContext try {
            val updateData = hashMapOf(
                "latestVersionCode" to versionCode,
                "latestVersionName" to versionName,
                "minSupportedVersionCode" to if (isForceUpdate) versionCode else 1L,
                "apkDownloadUrl" to apkDownloadUrl.trim(),
                "releaseNotes" to releaseNotes.trim(),
                "releaseDate" to java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US).format(java.util.Date()),
                "fileSizeFormatted" to fileSizeFormatted.trim(),
                "isForceUpdate" to isForceUpdate,
                "updatedBy" to "grapherkidd0@gmail.com",
                "updatedAt" to System.currentTimeMillis()
            )

            suspendCancellableCoroutine<Boolean> { cont ->
                firestore.collection(CONFIG_COLLECTION).document(VERSION_DOC)
                    .set(updateData, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.i(TAG, "Update release v$versionName ($versionCode) published to Firestore successfully!")
                        cont.resume(true)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to publish release: ${e.message}")
                        cont.resume(false)
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error publishing release: ${e.message}")
            false
        }
    }
}
