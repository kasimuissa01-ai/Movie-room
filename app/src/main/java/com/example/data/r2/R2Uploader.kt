package com.example.data.r2

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.api.MovieApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object R2Uploader {
    private const val TAG = "R2Uploader"

    sealed class UploadResult {
        data class Success(val objectKey: String, val publicUrl: String) : UploadResult()
        data class Failure(val errorMessage: String, val throwable: Throwable? = null) : UploadResult()
    }

    /**
     * Uploads bytes securely to Cloudflare R2 via our Cloudflare Worker backend.
     * ZERO client-side AWS SigV4 keys or R2 Secret Access keys required.
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
     * Uploads content from Android Uri (e.g. from Photo/Media Picker) to Cloudflare R2
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
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext UploadResult.Failure("Unable to read selected media file.")
            
            val cleanFilename = objectKey.substringAfterLast('/')
            val result = MovieApiClient.uploadMediaToR2(
                context = context,
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
            Log.e(TAG, "Failed reading Uri for upload: ${e.message}", e)
            UploadResult.Failure("Failed to read file from storage: ${e.message}", e)
        }
    }
}
