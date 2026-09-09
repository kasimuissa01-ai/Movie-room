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
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume

object AppUpdateManager {
    private const val TAG = "AppUpdateManager"
    private const val CONFIG_COLLECTION = "config"
    private const val VERSION_DOC = "app_version"

    // Default GitHub repository owner & repo for automatic release polling
    private var githubRepoOwner: String = "grapherkidd"
    private var githubRepoName: String = "movieroom"

    fun configureGitHubRepo(owner: String, repo: String) {
        if (owner.isNotBlank()) githubRepoOwner = owner.trim()
        if (repo.isNotBlank()) githubRepoName = repo.trim()
    }

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
     * Checks for updates by querying Cloudflare Worker (/api/updates/latest),
     * falling back to Firestore and GitHub Releases if needed.
     */
    suspend fun checkForUpdates(context: Context): AppUpdateInfo = withContext(Dispatchers.IO) {
        val currentCode = getCurrentVersionCode(context)
        val currentName = getCurrentVersionName(context)

        // 1. Try Cloudflare Worker Backend (/api/updates/latest)
        val workerInfo = checkWorkerUpdate(context, currentCode, currentName)
        if (workerInfo != null && workerInfo.isUpdateAvailable) {
            return@withContext workerInfo
        }

        // 2. Try Firestore
        val firestoreInfo = checkFirestoreUpdate(context, currentCode, currentName)
        if (firestoreInfo != null && firestoreInfo.isUpdateAvailable) {
            return@withContext firestoreInfo
        }

        // 3. Automatically query GitHub Releases API directly
        val githubInfo = checkGitHubLatestRelease(context, currentCode, currentName)
        if (githubInfo != null && githubInfo.isUpdateAvailable) {
            return@withContext githubInfo
        }

        // 4. Return latest known info or current status
        return@withContext workerInfo ?: (firestoreInfo ?: (githubInfo ?: AppUpdateInfo(
            latestVersionCode = currentCode,
            latestVersionName = currentName,
            isUpdateAvailable = false
        )))
    }

    private fun checkWorkerUpdate(context: Context, currentCode: Long, currentName: String): AppUpdateInfo? {
        return try {
            val baseUrl = com.example.data.api.BackendConfig.getBaseUrl(context)
            val apiUrl = "$baseUrl/api/updates/latest"
            val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "CineVault-Android")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val responseStr = reader.use { it.readText() }
                connection.disconnect()

                val json = JSONObject(responseStr)
                if (json.optBoolean("success", false)) {
                    val latestCode = json.optLong("versionCode", currentCode)
                    val latestName = json.optString("versionName", currentName)
                    val downloadUrl = json.optString("downloadUrl", "$baseUrl/api/updates/download")
                    val notes = json.optString("releaseNotes", "New version of CineVault available.")
                    val fileSize = json.optString("fileSizeFormatted", "45 MB")
                    val force = json.optBoolean("forceUpdate", false)

                    val isAvailable = (latestCode > currentCode || isVersionNameNewer(latestName, currentName)) && downloadUrl.isNotBlank()

                    return AppUpdateInfo(
                        latestVersionCode = latestCode,
                        latestVersionName = latestName,
                        minSupportedVersionCode = if (force) latestCode else 1L,
                        apkDownloadUrl = downloadUrl,
                        releaseNotes = notes,
                        releaseDate = "Latest",
                        fileSizeFormatted = fileSize,
                        isForceUpdate = force,
                        isUpdateAvailable = isAvailable
                    )
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Worker update check error: ${e.message}")
            null
        }
    }

    private suspend fun checkFirestoreUpdate(context: Context, currentCode: Long, currentName: String): AppUpdateInfo? {
        val firestore = FirebaseAuthManager.getFirestore(context) ?: return null
        return try {
            val docSnapshot = suspendCancellableCoroutine<com.google.firebase.firestore.DocumentSnapshot?> { cont ->
                firestore.collection(CONFIG_COLLECTION).document(VERSION_DOC)
                    .get()
                    .addOnSuccessListener { doc -> cont.resume(doc) }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Firestore update check: ${e.message}")
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

                val isAvailable = (latestCode > currentCode || isVersionNameNewer(latestName, currentName)) && apkUrl.isNotBlank()

                AppUpdateInfo(
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
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Firestore check failed: ${e.message}")
            null
        }
    }

    /**
     * Checks GitHub API directly for the latest public release in the repo
     */
    private fun checkGitHubLatestRelease(context: Context, currentCode: Long, currentName: String): AppUpdateInfo? {
        return try {
            val apiUrl = "https://api.github.com/repos/$githubRepoOwner/$githubRepoName/releases/latest"
            val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "MovieRoom-App")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val responseStr = reader.use { it.readText() }
                connection.disconnect()

                val json = JSONObject(responseStr)
                val tagName = json.optString("tag_name", "").removePrefix("v")
                val releaseName = json.optString("name", "v$tagName")
                val body = json.optString("body", "New update released on GitHub")
                val publishedAt = json.optString("published_at", "")

                // Find the .apk download asset
                val assets = json.optJSONArray("assets") ?: JSONArray()
                var apkUrl = ""
                var apkSizeFormatted = "APK Update"

                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        val sizeBytes = asset.optLong("size", 0L)
                        if (sizeBytes > 0) {
                            apkSizeFormatted = String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0))
                        }
                        break
                    }
                }

                if (apkUrl.isNotBlank() && isVersionNameNewer(tagName, currentName)) {
                    val parsedCode = parseVersionNameToCode(tagName)
                    return AppUpdateInfo(
                        latestVersionCode = parsedCode.coerceAtLeast(currentCode + 1),
                        latestVersionName = tagName.ifBlank { "Latest" },
                        minSupportedVersionCode = 1L,
                        apkDownloadUrl = apkUrl,
                        releaseNotes = body.ifBlank { "New features & improvements from GitHub Release $releaseName" },
                        releaseDate = publishedAt.take(10).ifBlank { "Recent" },
                        fileSizeFormatted = apkSizeFormatted,
                        isForceUpdate = false,
                        isUpdateAvailable = true
                    )
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "GitHub releases check error: ${e.message}")
            null
        }
    }

    private fun isVersionNameNewer(latest: String, current: String): Boolean {
        val cleanLatest = latest.trim().removePrefix("v")
        val cleanCurrent = current.trim().removePrefix("v")
        if (cleanLatest.isBlank() || cleanCurrent.isBlank()) return false
        if (cleanLatest.equals(cleanCurrent, ignoreCase = true)) return false

        // Extract base version e.g. "1.0.2" from "1.0.2-b3"
        val baseLatest = cleanLatest.substringBefore("-").substringBefore("+").substringBefore("_")
        val baseCurrent = cleanCurrent.substringBefore("-").substringBefore("+").substringBefore("_")

        val latestParts = baseLatest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = baseCurrent.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }

        // If base versions are identical, check if latest contains build suffix that might indicate newer build
        val latestBuild = cleanLatest.substringAfter("-b", "").substringAfter("-", "").toIntOrNull() ?: 0
        val currentBuild = cleanCurrent.substringAfter("-b", "").substringAfter("-", "").toIntOrNull() ?: 0
        return latestBuild > currentBuild
    }

    private fun parseVersionNameToCode(versionName: String): Long {
        val clean = versionName.trim().removePrefix("v").substringBefore("-").substringBefore("+")
        val parts = clean.split(".").mapNotNull { it.toIntOrNull() }
        var code = 0L
        for (part in parts) {
            code = code * 100 + part
        }
        return if (code > 0) code else 3L
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

