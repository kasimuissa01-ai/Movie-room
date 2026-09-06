package com.example.data.r2

import com.example.BuildConfig

object R2Config {
    val accountId: String
        get() = try {
            BuildConfig.R2_ACCOUNT_ID.trim().takeIf {
                it.isNotBlank() && !it.equals("YOUR_CLOUDFLARE_ACCOUNT_ID", ignoreCase = true)
            } ?: ""
        } catch (e: Throwable) {
            ""
        }

    val accessKeyId: String
        get() = try {
            BuildConfig.R2_ACCESS_KEY_ID.trim().takeIf {
                it.isNotBlank() && !it.equals("YOUR_R2_ACCESS_KEY_ID", ignoreCase = true)
            } ?: ""
        } catch (e: Throwable) {
            ""
        }

    val secretAccessKey: String
        get() = try {
            BuildConfig.R2_SECRET_ACCESS_KEY.trim().takeIf {
                it.isNotBlank() && !it.equals("YOUR_R2_SECRET_ACCESS_KEY", ignoreCase = true)
            } ?: ""
        } catch (e: Throwable) {
            ""
        }

    val bucketName: String
        get() = try {
            BuildConfig.R2_BUCKET_NAME.trim().takeIf {
                it.isNotBlank() && !it.equals("YOUR_R2_BUCKET_NAME", ignoreCase = true)
            } ?: "cinestream-movies"
        } catch (e: Throwable) {
            "cinestream-movies"
        }

    val publicUrlBase: String
        get() = try {
            val base = BuildConfig.R2_PUBLIC_URL_BASE.trim()
            if (base.isNotBlank() && !base.equals("YOUR_R2_PUBLIC_URL_BASE", ignoreCase = true)) {
                base.trimEnd('/')
            } else {
                "https://pub-r2.dev"
            }
        } catch (e: Throwable) {
            "https://pub-r2.dev"
        }

    val adminPasscode: String
        get() = try {
            BuildConfig.ADMIN_PASSCODE.trim().takeIf { it.isNotBlank() } ?: "admin2026"
        } catch (e: Throwable) {
            "admin2026"
        }

    val isR2Configured: Boolean
        get() = accountId.isNotBlank() && accessKeyId.isNotBlank() && secretAccessKey.isNotBlank()

    /**
     * Endpoint for Cloudflare R2 S3-compatible API:
     * https://<account_id>.r2.cloudflarestorage.com
     */
    val endpointUrl: String
        get() = if (accountId.isNotBlank()) {
            "https://$accountId.r2.cloudflarestorage.com"
        } else {
            "https://cloudflare.r2.cloudflarestorage.com"
        }

    /**
     * Constructs public streaming URL for a given object key stored in R2.
     */
    fun getPublicUrl(objectKey: String): String {
        val cleanKey = objectKey.trimStart('/')
        return if (publicUrlBase.isNotBlank()) {
            "$publicUrlBase/$cleanKey"
        } else if (accountId.isNotBlank()) {
            "https://$accountId.r2.cloudflarestorage.com/$bucketName/$cleanKey"
        } else {
            "https://pub-r2.dev/$cleanKey"
        }
    }
}
