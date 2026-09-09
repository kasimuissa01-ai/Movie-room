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
                var lastPercent = -1
                var lastNotifyTime = 0L

                while (input.read(buffer).also { read = it } != -1) {
                    sink.write(buffer, 0, read)
                    uploaded += read
                    val percent = if (total > 0) {
                        ((uploaded * 100) / total).toInt().coerceIn(0, 100)
                    } else {
                        50
                    }
                    val now = System.currentTimeMillis()
                    if (percent != lastPercent || (now - lastNotifyTime) > 150) {
                        lastPercent = percent
                        lastNotifyTime = now
                        onProgress(uploaded, total, percent)
                    }
                }
            }
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
     * Uploads content directly from Android Uri to Cloudflare R2 using non-blocking stream chunks.
     * Prevents heap exhaustion, app crashes, and avoids saving multi-GB video files onto phone disk.
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
                    val publicUrl = res.url?.takeIf { it.isNotBlank() }
                        ?: R2Config.getPublicUrl(res.key ?: objectKey)
                    UploadResult.Success(res.key ?: objectKey, publicUrl)
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
}
