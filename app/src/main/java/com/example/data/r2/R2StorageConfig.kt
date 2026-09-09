package com.example.data.r2

import android.content.Context
import android.util.Log

/**
 * Configuration for Cloudflare R2 storage domain and public streaming URL resolver.
 * Sensitive secrets are kept securely in GitHub Secrets and backend environments, not saved in the app.
 */
object R2StorageConfig {
    private const val TAG = "R2StorageConfig"

    const val DEFAULT_ACCOUNT_ID = "fb27b7d70175201e4a9bc30bbe7ce866"
    const val DEFAULT_BUCKET_NAME = "stories"
    const val DEFAULT_PUBLIC_DOMAIN = "https://pub-cinestream.r2.dev"

    fun getAccountId(context: Context): String = DEFAULT_ACCOUNT_ID

    fun getBucketName(context: Context): String = DEFAULT_BUCKET_NAME

    fun getAccessKeyId(context: Context): String = ""

    fun getSecretAccessKey(context: Context): String = ""

    fun getPublicDomain(context: Context): String {
        return DEFAULT_PUBLIC_DOMAIN.trimEnd('/')
    }

    fun isConfigured(context: Context): Boolean = false

    /**
     * Constructs public streaming URL for a given object key.
     */
    fun getPublicUrl(context: Context, objectKey: String): String {
        val domain = getPublicDomain(context)
        val cleanKey = objectKey.trimStart('/')
        return "$domain/$cleanKey"
    }
}
