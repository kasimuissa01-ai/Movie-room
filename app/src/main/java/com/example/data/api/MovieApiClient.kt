package com.example.data.api

import android.content.Context
import android.util.Log
import com.example.data.SampleMovies
import com.example.model.Movie
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object MovieApiClient {
    private const val TAG = "MovieApiClient"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private var currentBaseUrl: String = ""
    private var cachedService: MovieApiService? = null

    private fun getService(context: Context? = null): MovieApiService {
        val baseUrl = BackendConfig.getBaseUrl(context)
        val formattedBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        if (cachedService == null || currentBaseUrl != formattedBase) {
            currentBaseUrl = formattedBase
            cachedService = Retrofit.Builder()
                .baseUrl(formattedBase)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(MovieApiService::class.java)
        }
        return cachedService!!
    }

    suspend fun getTrendingMovies(context: Context? = null): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val response = getService(context).getTrendingMovies()
            if (response.success && response.data != null) {
                response.data.map { it.toMovie() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed fetching trending from Worker API: ${e.message}")
            emptyList()
        }
    }

    suspend fun getPopularMovies(context: Context? = null): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val response = getService(context).getPopularMovies()
            if (response.success && response.data != null) {
                response.data.map { it.toMovie() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed fetching popular from Worker API: ${e.message}")
            emptyList()
        }
    }

    suspend fun getNowPlayingMovies(context: Context? = null): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val response = getService(context).getNowPlayingMovies()
            if (response.success && response.data != null) {
                response.data.map { it.toMovie() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed fetching now-playing from Worker API: ${e.message}")
            emptyList()
        }
    }

    suspend fun getUpcomingMovies(context: Context? = null): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val response = getService(context).getUpcomingMovies()
            if (response.success && response.data != null) {
                response.data.map { it.toMovie() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed fetching upcoming from Worker API: ${e.message}")
            emptyList()
        }
    }

    suspend fun getActionMovies(context: Context? = null): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val response = getService(context).getActionMovies()
            if (response.success && response.data != null) {
                response.data.map { it.toMovie() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed fetching action from Worker API: ${e.message}")
            emptyList()
        }
    }

    suspend fun getMoviesByCategory(context: Context? = null, category: String): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val normalized = category.lowercase().replace("[^a-z0-9]".toRegex(), "")
            val response = getService(context).getMoviesByCategory(normalized)
            if (response.success && response.data != null) {
                response.data.map { it.toMovie() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed fetching category $category from Worker API: ${e.message}")
            emptyList()
        }
    }

    suspend fun searchMovies(context: Context? = null, query: String): List<Movie> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val response = getService(context).searchMovies(query.trim())
            if (response.success && response.data != null) {
                response.data.map { it.toMovie() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed searching movies on Worker API: ${e.message}")
            emptyList()
        }
    }

    suspend fun getMovieById(context: Context? = null, id: String): Movie? = withContext(Dispatchers.IO) {
        val cleanId = id.trim()
        val numericId = cleanId.removePrefix("tmdb_")
        val idsToTry = listOf(cleanId, numericId).distinct()

        for (targetId in idsToTry) {
            // Attempt 1: /api/movies/movie/{id}
            try {
                val response = getService(context).getMovieById(targetId)
                if (response.success && response.data != null) {
                    return@withContext response.data.toMovie()
                }
            } catch (e: Exception) {
                // Ignore and try alternative route
            }

            // Attempt 2: /api/movies/{id}
            try {
                val response = getService(context).getMovieByIdAlt(targetId)
                if (response.success && response.data != null) {
                    return@withContext response.data.toMovie()
                }
            } catch (e: Exception) {
                // Ignore and proceed
            }
        }

        // Return null if not found on backend
        null
    }

    suspend fun getRecommendations(context: Context? = null, id: String): List<Movie> = withContext(Dispatchers.IO) {
        val cleanId = id.trim()
        val numericId = cleanId.removePrefix("tmdb_")
        val idsToTry = listOf(cleanId, numericId).distinct()

        for (targetId in idsToTry) {
            try {
                val response = getService(context).getRecommendations(targetId)
                if (response.success && !response.data.isNullOrEmpty()) {
                    return@withContext response.data.map { it.toMovie() }
                }
            } catch (e: Exception) {
                // Try alt route
            }

            try {
                val response = getService(context).getRecommendationsAlt(targetId)
                if (response.success && !response.data.isNullOrEmpty()) {
                    return@withContext response.data.map { it.toMovie() }
                }
            } catch (e: Exception) {
                // Proceed
            }
        }

        emptyList()
    }

    // --- Authentication ---
    suspend fun login(context: Context, email: String? = null, name: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val res = getService(context).login(LoginRequest(email, name))
            if (res.success && res.token != null) {
                BackendConfig.setAuthToken(context, res.token)
                BackendConfig.setUserRole(context, res.user?.role ?: "user")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login failed: ${e.message}")
            false
        }
    }

    suspend fun adminLogin(context: Context, passcode: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val res = getService(context).adminLogin(AdminLoginRequest(passcode))
            if (res.success && res.token != null) {
                BackendConfig.setAuthToken(context, res.token)
                BackendConfig.setUserRole(context, "admin")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Admin login error: ${e.message}")
            false
        }
    }

    // --- Admin Media Upload to Cloudflare R2 via Worker ---
    suspend fun uploadMediaToR2(
        context: Context,
        filename: String,
        bytes: ByteArray,
        contentType: String,
        folder: String = "movies"
    ): Result<R2UploadResponse> = withContext(Dispatchers.IO) {
        val token = BackendConfig.getAuthToken(context)
        val authHeader = if (!token.isNullOrBlank()) "Bearer $token" else ""

        try {
            val requestBody = bytes.toRequestBody(contentType.toMediaTypeOrNull())
            val res = getService(context).adminUploadR2(
                authHeader = authHeader,
                filename = filename,
                folder = folder,
                contentType = contentType,
                fileBody = requestBody
            )

            if (res.success) {
                Result.success(res)
            } else {
                Result.failure(Exception(res.error ?: res.message ?: "Upload failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "R2 upload to worker error: ${e.message}", e)
            Result.failure(Exception("Unable to upload media to server: ${e.message}"))
        }
    }

    suspend fun adminCreateMovie(context: Context, movieMap: Map<String, Any>): Result<MovieDto> = withContext(Dispatchers.IO) {
        val token = BackendConfig.getAuthToken(context)
        val authHeader = if (!token.isNullOrBlank()) "Bearer $token" else ""
        try {
            val res = getService(context).adminCreateMovie(authHeader, movieMap)
            if (res.success && res.data != null) {
                Result.success(res.data)
            } else {
                Result.failure(Exception(res.error ?: "Failed to create movie"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminPublishMovie(context: Context, movieId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val token = BackendConfig.getAuthToken(context)
        val authHeader = if (!token.isNullOrBlank()) "Bearer $token" else ""
        try {
            val res = getService(context).adminPublishMovie(authHeader, movieId)
            if (res.success) {
                Result.success(true)
            } else {
                Result.failure(Exception(res.error ?: "Failed to publish movie"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
