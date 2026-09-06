package com.example.data.r2

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object R2Uploader {
    private const val TAG = "R2Uploader"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    sealed class UploadResult {
        data class Success(val objectKey: String, val publicUrl: String) : UploadResult()
        data class Failure(val errorMessage: String, val throwable: Throwable? = null) : UploadResult()
    }

    /**
     * Uploads bytes directly to Cloudflare R2 bucket with AWS SigV4 authentication.
     */
    suspend fun uploadBytes(
        bytes: ByteArray,
        objectKey: String,
        contentType: String = "video/mp4",
        onProgress: (bytesUploaded: Long, totalBytes: Long, percent: Int) -> Unit = { _, _, _ -> }
    ): UploadResult = withContext(Dispatchers.IO) {
        if (!R2Config.isR2Configured) {
            return@withContext UploadResult.Failure(
                "Cloudflare R2 credentials (R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY) are not set in the AI Studio Secrets panel."
            )
        }

        try {
            val accountId = R2Config.accountId
            val bucketName = R2Config.bucketName
            val accessKeyId = R2Config.accessKeyId
            val secretAccessKey = R2Config.secretAccessKey
            val host = "$accountId.r2.cloudflarestorage.com"
            val cleanKey = objectKey.trimStart('/')
            val endpointUrl = "https://$host/$bucketName/$cleanKey"

            val timeZone = TimeZone.getTimeZone("UTC")
            val amzFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply { this.timeZone = timeZone }
            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US).apply { this.timeZone = timeZone }
            val now = Date()
            val amzDate = amzFormat.format(now)
            val dateStamp = dateFormat.format(now)

            // SHA256 of payload or UNSIGNED-PAYLOAD
            val payloadHash = sha256Hex(bytes)

            // Canonical Request
            val canonicalUri = "/$bucketName/$cleanKey"
            val canonicalHeaders = "content-type:$contentType\nhost:$host\nx-amz-content-sha256:$payloadHash\nx-amz-date:$amzDate\n"
            val signedHeaders = "content-type;host;x-amz-content-sha256;x-amz-date"
            val canonicalRequest = "PUT\n$canonicalUri\n\n$canonicalHeaders\n$signedHeaders\n$payloadHash"

            // String to Sign
            val algorithm = "AWS4-HMAC-SHA256"
            val credentialScope = "$dateStamp/auto/s3/aws4_request"
            val stringToSign = "$algorithm\n$amzDate\n$credentialScope\n${sha256Hex(canonicalRequest.toByteArray(StandardCharsets.UTF_8))}"

            // Calculate Signature
            val signingKey = getSignatureKey(secretAccessKey, dateStamp, "auto", "s3")
            val signature = bytesToHex(hmacSha256(signingKey, stringToSign))
            val authorization = "$algorithm Credential=$accessKeyId/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"

            val rawRequestBody = RequestBody.create(contentType.toMediaTypeOrNull(), bytes)
            val countingRequestBody = CountingRequestBody(rawRequestBody) { bytesWritten, totalBytes ->
                val percent = if (totalBytes > 0) ((bytesWritten * 100) / totalBytes).toInt() else 0
                onProgress(bytesWritten, totalBytes, percent)
            }

            val request = Request.Builder()
                .url(endpointUrl)
                .put(countingRequestBody)
                .header("Host", host)
                .header("Content-Type", contentType)
                .header("x-amz-date", amzDate)
                .header("x-amz-content-sha256", payloadHash)
                .header("Authorization", authorization)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val publicUrl = R2Config.getPublicUrl(cleanKey)
                Log.d(TAG, "Uploaded to R2 successfully: $publicUrl")
                UploadResult.Success(cleanKey, publicUrl)
            } else {
                val errorBody = response.body?.string() ?: "No response body"
                Log.e(TAG, "R2 upload failed (${response.code}): $errorBody")
                UploadResult.Failure("R2 Upload failed HTTP ${response.code}: $errorBody")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during R2 upload", e)
            UploadResult.Failure("Error uploading to Cloudflare R2: ${e.localizedMessage ?: e.message}", e)
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
        onProgress: (bytesUploaded: Long, totalBytes: Long, percent: Int) -> Unit = { _, _, _ -> }
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext UploadResult.Failure("Unable to read selected media file.")
            uploadBytes(bytes, objectKey, contentType, onProgress)
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading Uri for upload: ${e.message}", e)
            UploadResult.Failure("Failed to read file from storage: ${e.message}", e)
        }
    }

    private fun sha256Hex(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(data)
        return bytesToHex(digest)
    }

    private fun hmacSha256(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key, "HmacSHA256")
        mac.init(secretKey)
        return mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
    }

    private fun getSignatureKey(key: String, dateStamp: String, regionName: String, serviceName: String): ByteArray {
        val kSecret = ("AWS4$key").toByteArray(StandardCharsets.UTF_8)
        val kDate = hmacSha256(kSecret, dateStamp)
        val kRegion = hmacSha256(kDate, regionName)
        val kService = hmacSha256(kRegion, serviceName)
        return hmacSha256(kService, "aws4_request")
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexArray = "0123456789abcdef".toCharArray()
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    private class CountingRequestBody(
        private val delegate: RequestBody,
        private val onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit
    ) : RequestBody() {

        override fun contentType() = delegate.contentType()

        override fun contentLength() = delegate.contentLength()

        override fun writeTo(sink: BufferedSink) {
            val countingSink = object : ForwardingSink(sink) {
                var bytesWritten = 0L
                val totalLength = contentLength()

                override fun write(source: Buffer, byteCount: Long) {
                    super.write(source, byteCount)
                    bytesWritten += byteCount
                    onProgress(bytesWritten, totalLength)
                }
            }
            val bufferedSink = countingSink.buffer()
            delegate.writeTo(bufferedSink)
            bufferedSink.flush()
        }
    }
}
