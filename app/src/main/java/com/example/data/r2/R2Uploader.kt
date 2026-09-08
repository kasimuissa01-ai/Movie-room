package com.example.data.r2

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.data.api.MovieApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object R2Uploader {
    private const val TAG = "R2Uploader"

    sealed class UploadResult {
        data class Success(val objectKey: String, val publicUrl: String) : UploadResult()
        data class Failure(val errorMessage: String, val throwable: Throwable? = null) : UploadResult()
    }

    /**
     * Memory-safe streaming RequestBody for Android Content URIs.
     * Streams in 64KB chunks directly into the HTTP sink without loading file into JVM heap memory.
     */
    class UriStreamingRequestBody(
        private val context: Context,
        private val uri: Uri,
        private val contentTypeStr: String,
        private val onProgress: (bytesUploaded: Long, totalBytes: Long, percent: Int) -> Unit = { _, _, _ -> }
    ) : RequestBody() {
        private val calculatedLength: Long by lazy {
            try {
                // Try openFileDescriptor statSize
                val pfdSize = context.contentResolver.openFileDescriptor(uri, "r")?.use {
                    it.statSize
                } ?: -1L
                if (pfdSize > 0) return@lazy pfdSize

                // Fallback to OpenableColumns.SIZE query
                context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIdx != -1) cursor.getLong(sizeIdx) else -1L
                    } else -1L
                } ?: -1L
            } catch (e: Exception) {
                -1L
            }
        }

        override fun contentType(): MediaType? = contentTypeStr.toMediaTypeOrNull()

        override fun contentLength(): Long = calculatedLength

        override fun writeTo(sink: BufferedSink) {
            val total = calculatedLength
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Unable to open stream for Uri: $uri")

            inputStream.use { input ->
                val buffer = ByteArray(64 * 1024) // 64 KB buffer chunks
                var uploaded = 0L
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    sink.write(buffer, 0, read)
                    uploaded += read
                    val percent = if (total > 0) {
                        ((uploaded * 100) / total).toInt().coerceIn(0, 100)
                    } else {
                        50
                    }
                    onProgress(uploaded, total, percent)
                }
            }
        }
    }

    /**
     * Memory-safe streaming RequestBody for local Files.
     */
    class FileStreamingRequestBody(
        private val file: File,
        private val contentTypeStr: String,
        private val onProgress: (bytesUploaded: Long, totalBytes: Long, percent: Int) -> Unit = { _, _, _ -> }
    ) : RequestBody() {
        override fun contentType(): MediaType? = contentTypeStr.toMediaTypeOrNull()

        override fun contentLength(): Long = file.length()

        override fun writeTo(sink: BufferedSink) {
            val total = file.length()
            file.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                var uploaded = 0L
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    sink.write(buffer, 0, read)
                    uploaded += read
                    val percent = if (total > 0) {
                        ((uploaded * 100) / total).toInt().coerceIn(0, 100)
                    } else {
                        50
                    }
                    onProgress(uploaded, total, percent)
                }
            }
        }
    }

    /**
     * Safely copies an Android Uri into internal application storage in 64KB chunks.
     * Prevents OutOfMemoryErrors and allows offline streaming directly from device storage.
     */
    suspend fun copyUriToAppStorage(
        context: Context,
        uri: Uri,
        destDirName: String,
        destFileName: String,
        onProgress: ((percent: Int) -> Unit)? = null
    ): File? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, destDirName).apply { mkdirs() }
            val targetFile = File(dir, destFileName)

            val totalSize = try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
            } catch (e: Exception) {
                -1L
            }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    val buffer = ByteArray(64 * 1024)
                    var copied = 0L
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        copied += bytesRead
                        if (totalSize > 0 && onProgress != null) {
                            val percent = ((copied * 100) / totalSize).toInt().coerceIn(0, 100)
                            onProgress(percent)
                        }
                    }
                    outputStream.flush()
                }
            }
            if (targetFile.exists() && targetFile.length() > 0) {
                targetFile
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed copying Uri to app storage: ${e.message}", e)
            null
        }
    }

    /**
     * Uploads bytes securely to Cloudflare R2 via our Cloudflare Worker backend.
     */
    suspend fun uploadBytes(
        bytes: ByteArray,
        objectKey: String,
        contentType: String = "video/mp4",
        folder: String = "movies",
        onProgress: (bytesUploaded: Long, totalBytes: Long, percent: Int) -> Unit = { _, _, _ -> }
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            onProgress(0, bytes.size.toLong(), 10)
            
            val cleanFilename = objectKey.substringAfterLast('/')
            val result = MovieApiClient.uploadMediaToR2(
                context = com.example.CineApplication.instance ?: throw IllegalStateException("Application context unavailable"),
                filename = cleanFilename,
                bytes = bytes,
                contentType = contentType,
                folder = folder
            )

            onProgress(bytes.size.toLong(), bytes.size.toLong(), 100)

            result.fold(
                onSuccess = { res ->
                    UploadResult.Success(res.key ?: objectKey, res.url ?: "https://pub-cinestream.r2.dev/${res.key ?: objectKey}")
                },
                onFailure = { err ->
                    UploadResult.Failure(err.message ?: "Upload failed", err)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Worker R2 upload", e)
            UploadResult.Failure("Error uploading to media storage: ${e.localizedMessage ?: e.message}", e)
        }
    }

    /**
     * Uploads content from Android Uri to Cloudflare R2 using non-blocking stream chunks.
     * Prevents heap exhaustion and app crashes.
     */
    suspend fun uploadFromUri(
        context: Context,
        uri: Uri,
        objectKey: String,
        contentType: String,
        folder: String = "movies",
        onProgress: (bytesUploaded: Long, totalBytes: Long, percent: Int) -> Unit = { _, _, _ -> }
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            onProgress(0, 100, 5)
            val cleanFilename = objectKey.substringAfterLast('/')
            val streamingBody = UriStreamingRequestBody(
                context = context,
                uri = uri,
                contentTypeStr = contentType,
                onProgress = onProgress
            )

            val result = MovieApiClient.uploadStreamingMediaToR2(
                context = context,
                filename = cleanFilename,
                requestBody = streamingBody,
                contentType = contentType,
                folder = folder
            )

            result.fold(
                onSuccess = { res ->
                    UploadResult.Success(res.key ?: objectKey, res.url ?: "https://pub-cinestream.r2.dev/${res.key ?: objectKey}")
                },
                onFailure = { err ->
                    UploadResult.Failure(err.message ?: "Upload failed", err)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed streaming upload for Uri: ${e.message}", e)
            UploadResult.Failure("Failed to upload file to storage: ${e.message}", e)
        }
    }

    /**
     * Uploads content from local File to Cloudflare R2 using streaming chunks.
     */
    suspend fun uploadFromFile(
        context: Context,
        file: File,
        objectKey: String,
        contentType: String,
        folder: String = "movies",
        onProgress: (bytesUploaded: Long, totalBytes: Long, percent: Int) -> Unit = { _, _, _ -> }
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            onProgress(0, file.length(), 5)
            val cleanFilename = objectKey.substringAfterLast('/')
            val streamingBody = FileStreamingRequestBody(
                file = file,
                contentTypeStr = contentType,
                onProgress = onProgress
            )

            val result = MovieApiClient.uploadStreamingMediaToR2(
                context = context,
                filename = cleanFilename,
                requestBody = streamingBody,
                contentType = contentType,
                folder = folder
            )

            result.fold(
                onSuccess = { res ->
                    UploadResult.Success(res.key ?: objectKey, res.url ?: "https://pub-cinestream.r2.dev/${res.key ?: objectKey}")
                },
                onFailure = { err ->
                    UploadResult.Failure(err.message ?: "Upload failed", err)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed streaming upload for File: ${e.message}", e)
            UploadResult.Failure("Failed to upload file to storage: ${e.message}", e)
        }
    }
}
