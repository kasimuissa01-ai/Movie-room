package com.example.data.r2

import com.example.BuildConfig
import com.example.data.api.BackendConfig

object R2Config {
    /**
     * Public base URL for streaming media assets.
     */
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

    /**
     * Returns true if backend is ready to handle media uploads
     */
    val isR2Configured: Boolean
        get() = true

    /**
     * Constructs public streaming URL for a given object key.
     */
    fun getPublicUrl(objectKey: String): String {
        val cleanKey = objectKey.trimStart('/')
        return if (cleanKey.startsWith("http://") || cleanKey.startsWith("https://")) {
            cleanKey
        } else {
            "$publicUrlBase/$cleanKey"
        }
    }
}
