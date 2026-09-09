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
            val totalMb = if (total > 0) total / (1024 * 1024) else 0
            Log.d(TAG, "[Diagnostic] Streaming upload started. Total size: $total bytes ($totalMb MB)")
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
                        Log.d(TAG, "[Diagnostic] Upload progress: $uploaded / $total bytes ($percent%)")
                        onProgress(uploaded, total, percent)
                    }
                }
                Log.d(TAG, "[Diagnostic] Finished streaming bytes to sink: $uploaded bytes written")
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
     * Uploads content directly from Android Uri to Cloudflare R2.
     * Bypasses the Cloudflare Worker media proxy completely for unlimited file sizes and high stability.
     */
    suspend fun uploadFromUri(
        context: Context,
        uri: Uri,
        objectKey: String,
        contentType: String,
        folder: String = "movies",
        movieId: String? = null,
        isTrailer: Boolean? = null,
        movieData: Map<String, Any?>? = null,
        onProgress: (bytesUploaded: Long, totalBytes: Long, percent: Int) -> Unit = { _, _, _ -> }
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            val cleanFilename = objectKey.substringAfterLast('/')

            val fileSize = try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
            } catch (e: Exception) {
                -1L
            }

            Log.d(TAG, "Preparing upload for $cleanFilename (size: $fileSize bytes)")
            onProgress(0, if (fileSize > 0) fileSize else 100, 1)

            // PRIORITY 1: Direct Android -> Cloudflare R2 S3 Upload (Zero Worker Involvement)
            if (R2StorageConfig.isConfigured(context)) {
                val accountId = R2StorageConfig.getAccountId(context)
                val bucket = R2StorageConfig.getBucketName(context)
                val accessKeyId = R2StorageConfig.getAccessKeyId(context)
                val secretAccessKey = R2StorageConfig.getSecretAccessKey(context)

                Log.i(TAG, "Using Direct S3 Upload to Cloudflare R2 (bucket: $bucket, account: $accountId)")
                val directResult = R2DirectS3Client.uploadFromUri(
                    context = context,
                    uri = uri,
                    objectKey = objectKey,
                    contentType = contentType,
                    accountId = accountId,
                    bucket = bucket,
                    accessKeyId = accessKeyId,
                    secretAccessKey = secretAccessKey,
                    onProgress = onProgress
                )

                return@withContext when (directResult) {
                    is R2DirectS3Client.UploadResult.Success -> {
                        val publicUrl = R2StorageConfig.getPublicUrl(context, directResult.objectKey)
                        Log.i(TAG, "Direct R2 S3 upload successful: $publicUrl")
                        UploadResult.Success(directResult.objectKey, publicUrl)
                    }
                    is R2DirectS3Client.UploadResult.Failure -> {
                        Log.e(TAG, "Direct R2 S3 upload failed: ${directResult.errorMessage}")
                        UploadResult.Failure(directResult.errorMessage, directResult.throwable)
                    }
                }
            }

            val initResult = MovieApiClient.initiateR2Upload(
                context = context,
                filename = cleanFilename,
                fileSize = if (fileSize > 0) fileSize else 10 * 1024 * 1024,
                contentType = contentType,
                folder = folder
            )

            if (initResult.isFailure) {
                val err = initResult.exceptionOrNull()?.message ?: "Failed to authorize upload"
                Log.e(TAG, "Initiate upload failed: $err")
                return@withContext UploadResult.Failure(err)
            }

            val initRes = initResult.getOrThrow()
            val targetKey = initRes.key ?: objectKey
            val publicUrl = initRes.publicUrl ?: "https://pub-cinestream.r2.dev/$targetKey"

            val okHttpClient = okhttp3.OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.MINUTES)
                .readTimeout(30, java.util.concurrent.TimeUnit.MINUTES)
                .build()

            // Single PUT mode (for small files like posters)
            if (initRes.mode == "single" || initRes.uploadId.isNullOrBlank()) {
                val uploadUrl = initRes.uploadUrl
                if (!uploadUrl.isNullOrBlank()) {
                    Log.d(TAG, "Performing direct single PUT upload to R2")
                    val streamingBody = UriStreamingRequestBody(
                        context = context,
                        uri = uri,
                        contentTypeStr = contentType,
                        onProgress = onProgress
                    )
                    val putReq = okhttp3.Request.Builder()
                        .url(uploadUrl)
                        .put(streamingBody)
                        .header("Content-Type", contentType)
                        .build()

                    okHttpClient.newCall(putReq).execute().use { response ->
                        if (!response.isSuccessful) {
                            val errBody = response.body?.string() ?: ""
                            Log.e(TAG, "Direct R2 single upload failed: host=${response.request.url.host}, path=${response.request.url.encodedPath}, code=${response.code}, body=$errBody")
                            return@withContext UploadResult.Failure("Direct R2 upload failed with HTTP ${response.code}: $errBody")
                        }
                    }
                } else {
                    val streamingBody = UriStreamingRequestBody(context, uri, contentType, onProgress)
                    val workerRes = MovieApiClient.uploadStreamingMediaToR2(context, cleanFilename, streamingBody, contentType, folder)
                    if (workerRes.isFailure) {
                        return@withContext UploadResult.Failure(workerRes.exceptionOrNull()?.message ?: "Worker upload failed")
                    }
                }

                val completeRes = MovieApiClient.completeR2Upload(
                    context = context,
                    key = targetKey,
                    uploadId = null,
                    parts = null,
                    movieId = movieId,
                    isTrailer = isTrailer,
                    movieData = movieData
                )
                return@withContext if (completeRes.isSuccess) {
                    UploadResult.Success(targetKey, publicUrl)
                } else {
                    UploadResult.Failure(completeRes.exceptionOrNull()?.message ?: "Failed to finalize upload")
                }
            }

            // Multipart Upload mode (for 1 GB+ movie files)
            val uploadId = initRes.uploadId!!
            val partSize = initRes.partSize // 16 MB
            val totalParts = initRes.totalParts

            Log.d(TAG, "Beginning direct multipart upload: uploadId=$uploadId, totalParts=$totalParts, partSize=$partSize")
            val completedParts = mutableListOf<com.example.data.api.CompletePartDto>()

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext UploadResult.Failure("Unable to open stream for $uri")

            val totalBytesCount = if (fileSize > 0) fileSize else totalParts.toLong() * partSize
            var uploadedBytesAccumulated = 0L

            inputStream.use { input ->
                for (partNum in 1..totalParts) {
                    val signRes = MovieApiClient.getSignPartUrl(context, targetKey, uploadId, partNum)
                    val partUploadUrl = signRes.getOrNull()?.uploadUrl

                    val buffer = ByteArray(partSize.toInt())
                    var bytesReadTotal = 0
                    while (bytesReadTotal < partSize) {
                        val read = input.read(buffer, bytesReadTotal, partSize.toInt() - bytesReadTotal)
                        if (read == -1) break
                        bytesReadTotal += read
                    }

                    if (bytesReadTotal == 0) break

                    val partData = if (bytesReadTotal == partSize.toInt()) buffer else buffer.copyOf(bytesReadTotal)

                    var etag = ""
                    var attempts = 0
                    var partSuccess = false
                    var lastPartError = ""

                    // 1. Try direct presigned S3 PUT if uploadUrl is available
                    if (!partUploadUrl.isNullOrBlank()) {
                        while (attempts < 2 && !partSuccess) {
                            attempts++
                            try {
                                val partBody = RequestBody.create(contentType.toMediaTypeOrNull(), partData)
                                val partReq = okhttp3.Request.Builder()
                                    .url(partUploadUrl)
                                    .put(partBody)
                                    .build()

                                okHttpClient.newCall(partReq).execute().use { resp ->
                                    if (resp.isSuccessful) {
                                        val rawEtag = resp.header("ETag") ?: resp.header("etag") ?: ""
                                        etag = rawEtag.replace("\"", "").trim()
                                        if (etag.isNotBlank()) {
                                            partSuccess = true
                                        } else {
                                            lastPartError = "Empty ETag header from R2"
                                        }
                                    } else {
                                        val errBody = resp.body?.string() ?: ""
                                        Log.e(TAG, "R2 part $partNum PUT failed: code=${resp.code}, body=$errBody")
                                        lastPartError = "HTTP ${resp.code}: $errBody"
                                    }
                                }
                            } catch (e: Exception) {
                                lastPartError = e.message ?: "Network error"
                                kotlinx.coroutines.delay(500)
                            }
                        }
                    }

                    // 2. Fallback: Stream part to Cloudflare Worker R2 native binding
                    if (!partSuccess) {
                        Log.i(TAG, "Direct R2 PUT skipped or failed for part $partNum ($lastPartError). Attempting Worker fallback chunk upload...")
                        val fallbackRes = MovieApiClient.uploadMultipartChunkToWorker(
                            context = context,
                            key = targetKey,
                            uploadId = uploadId,
                            partNumber = partNum,
                            chunkData = partData,
                            contentType = contentType
                        )
                        if (fallbackRes.isSuccess) {
                            etag = fallbackRes.getOrThrow()
                            partSuccess = true
                            Log.i(TAG, "Worker fallback chunk upload succeeded for part $partNum: etag=$etag")
                        } else {
                            val fbError = fallbackRes.exceptionOrNull()?.message ?: "Unknown worker error"
                            // If part 1 fails on chunk route (e.g. 404 because worker hasn't been redeployed yet)
                            // and the file is <= 100 MB, stream directly via Worker's live single upload endpoint
                            if (partNum == 1 && fileSize in 1..104857600L) {
                                Log.i(TAG, "Chunk route unavailable ($fbError). Falling back to live Worker single-stream upload for file ($fileSize bytes)...")
                                val streamingBody = UriStreamingRequestBody(context, uri, contentType, onProgress)
                                val workerRes = MovieApiClient.uploadStreamingMediaToR2(context, cleanFilename, streamingBody, contentType, folder)
                                if (workerRes.isSuccess) {
                                    val uploadData = workerRes.getOrThrow()
                                    val streamKey = uploadData.key ?: targetKey
                                    val streamUrl = uploadData.url ?: "https://pub-cinestream.r2.dev/$streamKey"
                                    MovieApiClient.completeR2Upload(
                                        context = context,
                                        key = streamKey,
                                        uploadId = null,
                                        parts = null,
                                        movieId = movieId,
                                        isTrailer = isTrailer,
                                        movieData = movieData
                                    )
                                    return@withContext UploadResult.Success(streamKey, streamUrl)
                                } else {
                                    val sErr = workerRes.exceptionOrNull()?.message ?: "Single-stream upload failed"
                                    Log.e(TAG, "Worker single-stream fallback also failed: $sErr")
                                }
                            }
                            Log.e(TAG, "Part $partNum failed both direct and worker fallback: $fbError")
                            return@withContext UploadResult.Failure("Failed uploading part $partNum/$totalParts: $fbError. To upload large files, please run 'wrangler deploy' in the worker directory.")
                        }
                    }

                    completedParts.add(com.example.data.api.CompletePartDto(partNum, etag))
                    uploadedBytesAccumulated += bytesReadTotal

                    val percent = ((uploadedBytesAccumulated * 100) / totalBytesCount).toInt().coerceIn(0, 99)
                    Log.d(TAG, "Multipart progress: $uploadedBytesAccumulated / $totalBytesCount bytes ($percent%) - Part $partNum/$totalParts done")
                    onProgress(uploadedBytesAccumulated, totalBytesCount, percent)
                }
            }

            Log.d(TAG, "Completing multipart upload with ${completedParts.size} parts")
            val completeRes = MovieApiClient.completeR2Upload(
                context = context,
                key = targetKey,
                uploadId = uploadId,
                parts = completedParts,
                movieId = movieId,
                isTrailer = isTrailer,
                movieData = movieData
            )

            return@withContext completeRes.fold(
                onSuccess = { res ->
                    onProgress(totalBytesCount, totalBytesCount, 100)
                    UploadResult.Success(targetKey, res.url ?: publicUrl)
                },
                onFailure = { err ->
                    UploadResult.Failure("Failed to complete multipart upload: ${err.message}")
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Direct R2 upload error: ${e.message}", e)
            UploadResult.Failure("Upload error: ${e.localizedMessage ?: e.message}", e)
        }
    }
}
