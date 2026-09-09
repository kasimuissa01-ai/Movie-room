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

    private val uploadOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.MINUTES)
            .writeTimeout(30, TimeUnit.MINUTES)
            .callTimeout(30, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .build()
    }

    // --- Admin Media Upload to Cloudflare R2 via Worker ---
    suspend fun uploadStreamingMediaToR2(
        context: Context,
        filename: String,
        requestBody: okhttp3.RequestBody,
        contentType: String,
        folder: String = "movies"
    ): Result<R2UploadResponse> = withContext(Dispatchers.IO) {
        val (authHeader, adminKey) = getAdminAuth(context)

        try {
            val baseUrl = BackendConfig.getBaseUrl(context)
            val formattedBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val uploadService = Retrofit.Builder()
                .baseUrl(formattedBase)
                .client(uploadOkHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(MovieApiService::class.java)

            val res = uploadService.adminUploadR2(
                authHeader = authHeader,
                adminKey = adminKey,
                filename = filename,
                folder = folder,
                contentType = contentType,
                fileBody = requestBody
            )

            Log.d(TAG, "[Diagnostic] Worker upload HTTP response: success=${res.success}, key=${res.key}, url=${res.url}")

            if (res.success) {
                Result.success(res)
            } else {
                val errMsg = res.error ?: res.message ?: "Upload failed"
                Log.e(TAG, "[Diagnostic] Worker returned failure response: $errMsg")
                Result.failure(Exception(errMsg))
            }
        } catch (e: retrofit2.HttpException) {
            val code = e.code()
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            Log.e(TAG, "[Diagnostic] HTTP $code error during upload: ${e.message()}. Body: $errorBody", e)
            Result.failure(Exception("HTTP $code Upload Error: ${e.message()} - $errorBody"))
        } catch (e: Exception) {
            val exType = e.javaClass.simpleName
            val exMsg = e.localizedMessage ?: e.message ?: "Unknown I/O error"
            Log.e(TAG, "[Diagnostic] Upload exception ($exType): $exMsg", e)
            Result.failure(Exception("Upload Error ($exType): $exMsg"))
        }
    }

    private suspend fun getAdminAuth(context: Context): Pair<String, String> {
        val adminKey = com.example.data.r2.R2Config.adminPasscode
        var token = BackendConfig.getAuthToken(context)
        if (token.isNullOrBlank()) {
            try {
                val loginRes = getService(context).adminLogin(AdminLoginRequest(adminKey))
                if (loginRes.success && !loginRes.token.isNullOrBlank()) {
                    BackendConfig.setAuthToken(context, loginRes.token)
                    BackendConfig.setUserRole(context, "admin")
                    token = loginRes.token
                }
            } catch (e: Exception) {
                Log.w(TAG, "Notice: auto admin login fallback: ${e.message}")
            }
        }
        val authHeader = if (!token.isNullOrBlank()) "Bearer $token" else "Bearer $adminKey"
        return Pair(authHeader, adminKey)
    }

    suspend fun uploadMediaToR2(
        context: Context,
        filename: String,
        bytes: ByteArray,
        contentType: String,
        folder: String = "movies"
    ): Result<R2UploadResponse> = withContext(Dispatchers.IO) {
        val (authHeader, adminKey) = getAdminAuth(context)

        try {
            val requestBody = bytes.toRequestBody(contentType.toMediaTypeOrNull())
            val res = getService(context).adminUploadR2(
                authHeader = authHeader,
                adminKey = adminKey,
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
        val (authHeader, adminKey) = getAdminAuth(context)
        try {
            val res = getService(context).adminCreateMovie(authHeader, adminKey, movieMap)
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
        val (authHeader, adminKey) = getAdminAuth(context)
        try {
            val res = getService(context).adminPublishMovie(authHeader, adminKey, movieId)
            if (res.success) {
                Result.success(true)
            } else {
                Result.failure(Exception(res.error ?: "Failed to publish movie"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminUpdateMovie(context: Context, movieId: String, updates: Map<String, Any?>): Result<MovieDto> = withContext(Dispatchers.IO) {
        val (authHeader, adminKey) = getAdminAuth(context)
        try {
            val res = getService(context).adminUpdateMovie(authHeader, adminKey, movieId, updates)
            if (res.success && res.data != null) {
                Result.success(res.data)
            } else {
                Result.failure(Exception(res.error ?: res.message ?: "Failed to update movie"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminDeleteMovie(context: Context, movieId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val (authHeader, adminKey) = getAdminAuth(context)
        try {
            val res = getService(context).adminDeleteMovie(authHeader, adminKey, movieId)
            if (res.success) {
                Result.success(true)
            } else {
                Result.failure(Exception(res.error ?: res.message ?: "Failed to delete movie"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun initiateR2Upload(
        context: Context,
        filename: String,
        fileSize: Long,
        contentType: String,
        folder: String = "movies"
    ): Result<InitiateUploadResponse> = withContext(Dispatchers.IO) {
        val (authHeader, adminKey) = getAdminAuth(context)
        try {
            val req = InitiateUploadRequest(filename, folder, fileSize, contentType)
            val res = getService(context).adminInitiateUpload(authHeader, adminKey, req)
            if (res.success) {
                Result.success(res)
            } else {
                Result.failure(Exception(res.error ?: res.message ?: "Failed to initiate upload"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSignPartUrl(
        context: Context,
        key: String,
        uploadId: String,
        partNumber: Int
    ): Result<SignPartResponse> = withContext(Dispatchers.IO) {
        val (authHeader, adminKey) = getAdminAuth(context)
        try {
            val req = SignPartRequest(key, uploadId, partNumber)
            val res = getService(context).adminSignPart(authHeader, adminKey, req)
            if (res.success && !res.uploadUrl.isNullOrBlank()) {
                Result.success(res)
            } else {
                Result.failure(Exception(res.error ?: "Failed to sign part $partNumber"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadMultipartChunkToWorker(
        context: Context,
        key: String,
        uploadId: String,
        partNumber: Int,
        chunkData: ByteArray,
        contentType: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val (authHeader, adminKey) = getAdminAuth(context)
        try {
            val body = chunkData.toRequestBody(contentType.toMediaTypeOrNull())
            val baseUrl = BackendConfig.getBaseUrl(context)
            val formattedBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val uploadService = Retrofit.Builder()
                .baseUrl(formattedBase)
                .client(uploadOkHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(MovieApiService::class.java)

            val res = uploadService.adminUploadPart(
                authHeader = authHeader,
                adminKey = adminKey,
                key = key,
                uploadId = uploadId,
                partNumber = partNumber,
                contentType = contentType,
                partBody = body
            )
            if (res.success && !res.etag.isNullOrBlank()) {
                Result.success(res.etag)
            } else {
                Result.failure(Exception(res.error ?: "Fallback part upload failed for part $partNumber"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeR2Upload(
        context: Context,
        key: String,
        uploadId: String?,
        parts: List<CompletePartDto>?,
        movieId: String? = null,
        isTrailer: Boolean? = null,
        movieData: Map<String, Any?>? = null
    ): Result<CompleteUploadResponse> = withContext(Dispatchers.IO) {
        val (authHeader, adminKey) = getAdminAuth(context)
        try {
            val req = CompleteUploadRequest(key, uploadId, parts, movieId, isTrailer, movieData)
            val res = getService(context).adminCompleteUpload(authHeader, adminKey, req)
            if (res.success) {
                Result.success(res)
            } else {
                Result.failure(Exception(res.error ?: res.message ?: "Failed to complete upload"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
