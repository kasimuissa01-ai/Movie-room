package com.example.data.r2

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Direct Android-to-Cloudflare R2 storage client.
 * Connects directly to Cloudflare R2's S3-compatible API using AWS SigV4 authorization.
 * Completely bypasses Cloudflare Workers for video uploads, eliminating timeout and size limit issues.
 */
object R2DirectS3Client {
    private const val TAG = "R2DirectS3Client"

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.MINUTES)
        .readTimeout(30, TimeUnit.MINUTES)
        .retryOnConnectionFailure(true)
        .build()

    data class MultipartPart(val partNumber: Int, val etag: String)

    sealed class UploadResult {
        data class Success(val objectKey: String, val etag: String) : UploadResult()
        data class Failure(val errorMessage: String, val throwable: Throwable? = null) : UploadResult()
    }

    /**
     * Uploads media file from Android Uri directly to Cloudflare R2 bucket.
     * Uses single PUT for files <= 16 MB, and S3 multipart upload for larger files.
     */
    suspend fun uploadFromUri(
        context: Context,
        uri: Uri,
        objectKey: String,
        contentType: String,
        accountId: String,
        bucket: String,
        accessKeyId: String,
        secretAccessKey: String,
        onProgress: (bytesUploaded: Long, totalBytes: Long, percent: Int) -> Unit = { _, _, _ -> }
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            val fileSize = getFileSize(context, uri)
            val host = "${accountId.trim()}.r2.cloudflarestorage.com"
            val cleanKey = objectKey.trim().trimStart('/')
            val path = "/${bucket.trim()}/$cleanKey"

            Log.i(TAG, "Direct R2 upload starting: key=$cleanKey, size=$fileSize bytes")

            // Threshold: 16 MB (16,777,216 bytes)
            val multipartThreshold = 16 * 1024 * 1024L

            if (fileSize in 1..multipartThreshold) {
                // Direct Single PUT
                uploadSinglePut(
                    context = context,
                    uri = uri,
                    host = host,
                    path = path,
                    objectKey = cleanKey,
                    contentType = contentType,
                    fileSize = fileSize,
                    accessKeyId = accessKeyId,
                    secretAccessKey = secretAccessKey,
                    onProgress = onProgress
                )
            } else {
                // Direct S3 Multipart Upload
                uploadMultipart(
                    context = context,
                    uri = uri,
                    host = host,
                    path = path,
                    objectKey = cleanKey,
                    contentType = contentType,
                    fileSize = fileSize,
                    accessKeyId = accessKeyId,
                    secretAccessKey = secretAccessKey,
                    onProgress = onProgress
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Direct upload exception: ${e.message}", e)
            UploadResult.Failure("Direct R2 upload failed: ${e.localizedMessage ?: e.message}", e)
        }
    }

    private suspend fun uploadSinglePut(
        context: Context,
        uri: Uri,
        host: String,
        path: String,
        objectKey: String,
        contentType: String,
        fileSize: Long,
        accessKeyId: String,
        secretAccessKey: String,
        onProgress: (Long, Long, Int) -> Unit
    ): UploadResult = withContext(Dispatchers.IO) {
        val streamingBody = object : RequestBody() {
            override fun contentType() = contentType.toMediaTypeOrNull()
            override fun contentLength() = fileSize

            override fun writeTo(sink: BufferedSink) {
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("Unable to open stream for $uri")
                input.use { stream ->
                    val buffer = ByteArray(64 * 1024)
                    var uploaded = 0L
                    var read: Int
                    var lastPercent = -1
                    var lastNotify = 0L

                    while (stream.read(buffer).also { read = it } != -1) {
                        sink.write(buffer, 0, read)
                        uploaded += read
                        val percent = if (fileSize > 0) ((uploaded * 100) / fileSize).toInt().coerceIn(0, 100) else 50
                        val now = System.currentTimeMillis()
                        if (percent != lastPercent || (now - lastNotify) > 200) {
                            lastPercent = percent
                            lastNotify = now
                            onProgress(uploaded, fileSize, percent)
                        }
                    }
                }
            }
        }

        val request = buildSignedRequest(
            method = "PUT",
            host = host,
            path = path,
            queryParams = emptyMap(),
            body = streamingBody,
            contentType = contentType,
            accessKeyId = accessKeyId,
            secretAccessKey = secretAccessKey,
            contentLength = fileSize
        )

        okHttpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val etag = response.header("ETag")?.replace("\"", "")?.trim() ?: ""
                onProgress(fileSize, fileSize, 100)
                UploadResult.Success(objectKey, etag)
            } else {
                val body = response.body?.string() ?: ""
                UploadResult.Failure("Direct upload failed (HTTP ${response.code}): $body")
            }
        }
    }

    private suspend fun uploadMultipart(
        context: Context,
        uri: Uri,
        host: String,
        path: String,
        objectKey: String,
        contentType: String,
        fileSize: Long,
        accessKeyId: String,
        secretAccessKey: String,
        onProgress: (Long, Long, Int) -> Unit
    ): UploadResult = withContext(Dispatchers.IO) {
        // Step 1: Initiate Multipart Upload
        val initiateResult = initiateMultipartUpload(host, path, contentType, accessKeyId, secretAccessKey)
        if (initiateResult.isFailure) {
            return@withContext UploadResult.Failure(initiateResult.exceptionOrNull()?.message ?: "Failed to initiate multipart upload")
        }
        val uploadId = initiateResult.getOrThrow()
        Log.i(TAG, "Multipart upload initiated: uploadId=$uploadId")

        // 10 MB chunks (ideal balance for mobile network and memory safety)
        val partSize = 10 * 1024 * 1024L
        val totalParts = if (fileSize > 0) {
            Math.ceil(fileSize.toDouble() / partSize.toDouble()).toInt().coerceAtLeast(1)
        } else {
            1
        }

        val completedParts = mutableListOf<MultipartPart>()
        var bytesUploadedAccumulated = 0L

        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return@withContext UploadResult.Failure("Unable to open input stream for $uri")

        try {
            inputStream.use { input ->
                var partNumber = 1
                val buffer = ByteArray(partSize.toInt())

                while (true) {
                    var bytesReadTotal = 0
                    while (bytesReadTotal < partSize) {
                        val read = input.read(buffer, bytesReadTotal, (partSize.toInt() - bytesReadTotal))
                        if (read == -1) break
                        bytesReadTotal += read
                    }

                    if (bytesReadTotal == 0) break

                    val partData = if (bytesReadTotal == partSize.toInt()) buffer else buffer.copyOf(bytesReadTotal)

                    // Upload part with retry (up to 3 attempts)
                    var uploadPartSuccess = false
                    var partEtag = ""
                    var lastError = ""

                    for (attempt in 1..3) {
                        val partResult = uploadPart(
                            host = host,
                            path = path,
                            uploadId = uploadId,
                            partNumber = partNumber,
                            data = partData,
                            accessKeyId = accessKeyId,
                            secretAccessKey = secretAccessKey
                        )

                        if (partResult.isSuccess) {
                            partEtag = partResult.getOrThrow()
                            uploadPartSuccess = true
                            break
                        } else {
                            lastError = partResult.exceptionOrNull()?.message ?: "Upload error"
                            Log.w(TAG, "Part $partNumber attempt $attempt failed: $lastError. Retrying...")
                            delay(1000L * attempt)
                        }
                    }

                    if (!uploadPartSuccess) {
                        abortMultipartUpload(host, path, uploadId, accessKeyId, secretAccessKey)
                        return@withContext UploadResult.Failure("Failed to upload part $partNumber/$totalParts: $lastError")
                    }

                    completedParts.add(MultipartPart(partNumber, partEtag))
                    bytesUploadedAccumulated += bytesReadTotal

                    val totalForCalc = if (fileSize > 0) fileSize else bytesUploadedAccumulated
                    val percent = if (totalForCalc > 0) ((bytesUploadedAccumulated * 100) / totalForCalc).toInt().coerceIn(0, 99) else 50
                    onProgress(bytesUploadedAccumulated, totalForCalc, percent)

                    Log.d(TAG, "Uploaded part $partNumber/$totalParts (ETag: $partEtag, $bytesUploadedAccumulated/$fileSize bytes)")
                    partNumber++
                }
            }

            // Step 3: Complete Multipart Upload
            val completeResult = completeMultipartUpload(host, path, uploadId, completedParts, accessKeyId, secretAccessKey)
            if (completeResult.isSuccess) {
                val finalEtag = completeResult.getOrThrow()
                onProgress(bytesUploadedAccumulated, bytesUploadedAccumulated, 100)
                Log.i(TAG, "Multipart upload completed successfully: key=$objectKey, parts=${completedParts.size}")
                UploadResult.Success(objectKey, finalEtag)
            } else {
                abortMultipartUpload(host, path, uploadId, accessKeyId, secretAccessKey)
                UploadResult.Failure("Failed to finalize multipart upload: ${completeResult.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            abortMultipartUpload(host, path, uploadId, accessKeyId, secretAccessKey)
            Log.e(TAG, "Multipart upload aborted due to exception: ${e.message}", e)
            UploadResult.Failure("Multipart upload error: ${e.localizedMessage ?: e.message}", e)
        }
    }

    private suspend fun initiateMultipartUpload(
        host: String,
        path: String,
        contentType: String,
        accessKeyId: String,
        secretAccessKey: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val emptyBody = RequestBody.create(contentType.toMediaTypeOrNull(), "")
            val request = buildSignedRequest(
                method = "POST",
                host = host,
                path = path,
                queryParams = mapOf("uploads" to ""),
                body = emptyBody,
                contentType = contentType,
                accessKeyId = accessKeyId,
                secretAccessKey = secretAccessKey,
                contentLength = 0L
            )

            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val uploadId = parseXmlTag(body, "UploadId")
                    if (!uploadId.isNullOrBlank()) {
                        Result.success(uploadId)
                    } else {
                        Result.failure(IOException("UploadId missing in initiate response XML: $body"))
                    }
                } else {
                    Result.failure(IOException("Initiate multipart failed (HTTP ${response.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadPart(
        host: String,
        path: String,
        uploadId: String,
        partNumber: Int,
        data: ByteArray,
        accessKeyId: String,
        secretAccessKey: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = RequestBody.create("application/octet-stream".toMediaTypeOrNull(), data)
            val request = buildSignedRequest(
                method = "PUT",
                host = host,
                path = path,
                queryParams = mapOf(
                    "partNumber" to partNumber.toString(),
                    "uploadId" to uploadId
                ),
                body = body,
                contentType = "application/octet-stream",
                accessKeyId = accessKeyId,
                secretAccessKey = secretAccessKey,
                contentLength = data.size.toLong()
            )

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val rawEtag = response.header("ETag") ?: response.header("etag") ?: ""
                    val cleanEtag = rawEtag.replace("\"", "").trim()
                    if (cleanEtag.isNotBlank()) {
                        Result.success(cleanEtag)
                    } else {
                        Result.failure(IOException("Missing ETag header in part $partNumber response"))
                    }
                } else {
                    val errBody = response.body?.string() ?: ""
                    Result.failure(IOException("Part $partNumber failed (HTTP ${response.code}): $errBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun completeMultipartUpload(
        host: String,
        path: String,
        uploadId: String,
        parts: List<MultipartPart>,
        accessKeyId: String,
        secretAccessKey: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val xmlParts = StringBuilder()
            xmlParts.append("<CompleteMultipartUpload>")
            parts.sortedBy { it.partNumber }.forEach { part ->
                xmlParts.append("<Part>")
                xmlParts.append("<PartNumber>${part.partNumber}</PartNumber>")
                xmlParts.append("<ETag>\"${part.etag}\"</ETag>")
                xmlParts.append("</Part>")
            }
            xmlParts.append("</CompleteMultipartUpload>")

            val xmlBytes = xmlParts.toString().toByteArray(StandardCharsets.UTF_8)
            val body = RequestBody.create("application/xml".toMediaTypeOrNull(), xmlBytes)

            val request = buildSignedRequest(
                method = "POST",
                host = host,
                path = path,
                queryParams = mapOf("uploadId" to uploadId),
                body = body,
                contentType = "application/xml",
                accessKeyId = accessKeyId,
                secretAccessKey = secretAccessKey,
                contentLength = xmlBytes.size.toLong()
            )

            okHttpClient.newCall(request).execute().use { response ->
                val respBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val etag = parseXmlTag(respBody, "ETag")?.replace("\"", "")?.trim() ?: ""
                    Result.success(etag)
                } else {
                    Result.failure(IOException("Complete multipart upload failed (HTTP ${response.code}): $respBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun abortMultipartUpload(
        host: String,
        path: String,
        uploadId: String,
        accessKeyId: String,
        secretAccessKey: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = buildSignedRequest(
                method = "DELETE",
                host = host,
                path = path,
                queryParams = mapOf("uploadId" to uploadId),
                body = null,
                contentType = null,
                accessKeyId = accessKeyId,
                secretAccessKey = secretAccessKey
            )
            okHttpClient.newCall(request).execute().use { it.isSuccessful }
        } catch (ignored: Exception) {
            false
        }
    }

    // =========================================================================
    // AWS SigV4 Request Signer for S3-Compatible Cloudflare R2
    // =========================================================================

    private fun buildSignedRequest(
        method: String,
        host: String,
        path: String,
        queryParams: Map<String, String>,
        body: RequestBody?,
        contentType: String?,
        accessKeyId: String,
        secretAccessKey: String,
        contentLength: Long? = null
    ): Request {
        val isoFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dateOnlyFormat = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val now = Date()
        val amzDate = isoFormat.format(now)
        val dateStamp = dateOnlyFormat.format(now)

        val canonicalUri = encodePath(path)
        val canonicalQueryString = queryParams.toSortedMap().map { (key, value) ->
            "${urlEncode(key)}=${urlEncode(value)}"
        }.joinToString("&")

        // In S3 streaming, use UNSIGNED-PAYLOAD for body streaming
        val payloadHash = "UNSIGNED-PAYLOAD"

        val canonicalHeaders = "host:$host\nx-amz-content-sha256:$payloadHash\nx-amz-date:$amzDate\n"
        val signedHeaders = "host;x-amz-content-sha256;x-amz-date"

        val canonicalRequest = "$method\n$canonicalUri\n$canonicalQueryString\n$canonicalHeaders\n$signedHeaders\n$payloadHash"
        val canonicalRequestHash = sha256Hex(canonicalRequest)

        val region = "auto"
        val service = "s3"
        val credentialScope = "$dateStamp/$region/$service/aws4_request"
        val stringToSign = "AWS4-HMAC-SHA256\n$amzDate\n$credentialScope\n$canonicalRequestHash"

        val kDate = hmac(("AWS4$secretAccessKey").toByteArray(StandardCharsets.UTF_8), dateStamp)
        val kRegion = hmac(kDate, region)
        val kService = hmac(kRegion, service)
        val kSigning = hmac(kService, "aws4_request")

        val signature = hex(hmac(kSigning, stringToSign))

        val authHeader = "AWS4-HMAC-SHA256 Credential=$accessKeyId/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"

        val fullUrl = if (canonicalQueryString.isNotBlank()) {
            "https://$host$canonicalUri?$canonicalQueryString"
        } else {
            "https://$host$canonicalUri"
        }

        val requestBuilder = Request.Builder()
            .url(fullUrl)
            .method(method, body)
            .header("Host", host)
            .header("x-amz-date", amzDate)
            .header("x-amz-content-sha256", payloadHash)
            .header("Authorization", authHeader)

        if (!contentType.isNullOrBlank()) {
            requestBuilder.header("Content-Type", contentType)
        }
        if (contentLength != null && contentLength >= 0) {
            requestBuilder.header("Content-Length", contentLength.toString())
        }

        return requestBuilder.build()
    }

    private fun hmac(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
    }

    private fun hex(data: ByteArray): String {
        val sb = StringBuilder()
        for (b in data) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private fun sha256Hex(data: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return hex(md.digest(data.toByteArray(StandardCharsets.UTF_8)))
    }

    private fun urlEncode(value: String): String {
        return URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    }

    private fun encodePath(path: String): String {
        return path.split("/").joinToString("/") { segment ->
            if (segment.isEmpty()) "" else urlEncode(segment)
        }
    }

    private fun parseXmlTag(xml: String, tagName: String): String? {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc: Document = builder.parse(ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8)))
            val nodes = doc.getElementsByTagName(tagName)
            if (nodes.length > 0) {
                nodes.item(0).textContent?.trim()
            } else {
                null
            }
        } catch (e: Exception) {
            // Regex fallback if XML parser encounters namespace peculiarities
            val regex = "<$tagName>(.*?)</$tagName>".toRegex()
            regex.find(xml)?.groupValues?.get(1)?.trim()
        }
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            val pfdSize = context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
            if (pfdSize > 0) return pfdSize

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
}
