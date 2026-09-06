package com.example.data.supabase

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.firebase.AuthUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class SupabaseAuthResult {
    data class Success(val user: AuthUser, val message: String) : SupabaseAuthResult()
    data class Error(val message: String) : SupabaseAuthResult()
}

object SupabaseClient {
    private const val TAG = "SupabaseClient"
    private const val DEFAULT_URL = "https://vqgnxqabvmmpfoiceass.supabase.co"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun getSupabaseUrl(): String {
        val buildUrl = try { BuildConfig.SUPABASE_URL } catch (e: Throwable) { "" }
        return if (buildUrl.isNotBlank() && !buildUrl.startsWith("YOUR_")) buildUrl else DEFAULT_URL
    }

    fun getSupabaseAnonKey(): String {
        val buildKey = try { BuildConfig.SUPABASE_ANON_KEY } catch (e: Throwable) { "" }
        return if (buildKey.isNotBlank() && !buildKey.startsWith("YOUR_")) buildKey else ""
    }

    suspend fun signInWithEmail(email: String, password: String): SupabaseAuthResult = withContext(Dispatchers.IO) {
        val anonKey = getSupabaseAnonKey()
        if (anonKey.isBlank()) {
            return@withContext SupabaseAuthResult.Error("Supabase anon key is missing. Add SUPABASE_ANON_KEY in Secrets.")
        }

        try {
            val url = "${getSupabaseUrl()}/auth/v1/token?grant_type=password"
            val json = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
            }

            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val resJson = JSONObject(body)
                val userJson = resJson.getJSONObject("user")
                val uid = userJson.getString("id")
                val userEmail = userJson.optString("email", email)
                val meta = userJson.optJSONObject("user_metadata")
                val name = meta?.optString("full_name", userEmail.substringBefore("@")) ?: userEmail.substringBefore("@")
                val photo = meta?.optString("avatar_url", "") ?: ""

                val user = AuthUser(
                    uid = uid,
                    displayName = name,
                    email = userEmail,
                    photoUrl = photo,
                    firestoreSynced = true
                )
                SupabaseAuthResult.Success(user, "Karibu tena, $name!")
            } else {
                val errorMsg = try {
                    JSONObject(body).optString("error_description", JSONObject(body).optString("msg", "Invalid email or password"))
                } catch (e: Exception) {
                    "Authentication failed"
                }
                SupabaseAuthResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign in error: ${e.message}", e)
            SupabaseAuthResult.Error(e.message ?: "Network error during sign in")
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, displayName: String): SupabaseAuthResult = withContext(Dispatchers.IO) {
        val anonKey = getSupabaseAnonKey()
        if (anonKey.isBlank()) {
            return@withContext SupabaseAuthResult.Error("Supabase anon key is missing. Add SUPABASE_ANON_KEY in Secrets.")
        }

        try {
            val url = "${getSupabaseUrl()}/auth/v1/signup"
            val meta = JSONObject().apply {
                put("full_name", displayName.ifBlank { email.substringBefore("@") })
            }
            val json = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
                put("data", meta)
            }

            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val resJson = JSONObject(body)
                val userJson = resJson.optJSONObject("user")
                val uid = userJson?.optString("id") ?: "user_${System.currentTimeMillis()}"
                val user = AuthUser(
                    uid = uid,
                    displayName = displayName.ifBlank { email.substringBefore("@") },
                    email = email,
                    photoUrl = "",
                    firestoreSynced = true
                )
                SupabaseAuthResult.Success(user, "Akaunti imefunguliwa! Welcome!")
            } else {
                val errorMsg = try {
                    JSONObject(body).optString("error_description", JSONObject(body).optString("msg", "Failed to create account"))
                } catch (e: Exception) {
                    "Signup failed"
                }
                SupabaseAuthResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Signup error: ${e.message}", e)
            SupabaseAuthResult.Error(e.message ?: "Network error during sign up")
        }
    }
}
