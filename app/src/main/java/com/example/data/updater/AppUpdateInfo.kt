package com.example.data.updater

data class AppUpdateInfo(
    val latestVersionCode: Long = 1L,
    val latestVersionName: String = "1.0.0",
    val minSupportedVersionCode: Long = 1L,
    val apkDownloadUrl: String = "",
    val releaseNotes: String = "",
    val releaseDate: String = "",
    val fileSizeFormatted: String = "",
    val isForceUpdate: Boolean = false,
    val isUpdateAvailable: Boolean = false
)

sealed class UpdateDownloadProgress {
    object Idle : UpdateDownloadProgress()
    object Checking : UpdateDownloadProgress()
    data class Downloading(val progressPercent: Int, val bytesRead: Long, val totalBytes: Long) : UpdateDownloadProgress()
    data class ReadyToInstall(val apkFilePath: String) : UpdateDownloadProgress()
    data class Error(val message: String) : UpdateDownloadProgress()
}
