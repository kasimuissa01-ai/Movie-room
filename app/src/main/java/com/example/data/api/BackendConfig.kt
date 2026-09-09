package com.example.data.api

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

/**
 * Backend Configuration:
 * Points exclusively to the Cloudflare Worker API.
 * Contains ZERO third-party API keys or Cloudflare R2 secrets.
 */
object BackendConfig {
    private const val PREFS_NAME = "backend_config_prefs"
    private const val KEY_CUSTOM_BASE_URL = "custom_base_url"
    private const val KEY_AUTH_TOKEN = "auth_jwt_token"
    private const val KEY_USER_ROLE = "user_role"

    // Default API Base URL (Supabase Edge Function r2-uploader bridge)
    const val DEFAULT_BASE_URL = "https://vqgnxqabvmmpfoiceass.supabase.co/functions/v1/r2-uploader"

    fun getBaseUrl(context: Context? = null): String {
        if (context != null) {
            val custom = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CUSTOM_BASE_URL, "")?.trim()
            if (!custom.isNullOrBlank()) {
                return custom.trimEnd('/')
            }
        }
        return try {
            val fromEnv = BuildConfig.API_BASE_URL.trim()
            if (fromEnv.isNotBlank() && !fromEnv.equals("YOUR_API_BASE_URL", ignoreCase = true)) {
                fromEnv.trimEnd('/')
            } else {
                DEFAULT_BASE_URL
            }
        } catch (e: Throwable) {
            DEFAULT_BASE_URL
        }
    }

    fun setCustomBaseUrl(context: Context, url: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_BASE_URL, url.trim().trimEnd('/'))
            .apply()
    }

    fun getAuthToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_AUTH_TOKEN, null)
    }

    fun setAuthToken(context: Context, token: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_AUTH_TOKEN, token)
            .apply()
    }

    fun getUserRole(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_ROLE, "user") ?: "user"
    }

    fun setUserRole(context: Context, role: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_ROLE, role)
            .apply()
    }
}
