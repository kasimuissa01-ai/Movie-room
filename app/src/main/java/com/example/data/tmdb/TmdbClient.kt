package com.example.data.tmdb

import android.util.Log
import com.example.BuildConfig
import com.example.model.Movie
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object TmdbClient {
    private const val TAG = "TmdbClient"
    private const val BASE_URL = "https://api.themoviedb.org/3/"

    // Injected securely via BuildConfig & AI Studio Secrets
    val rawApiKey: String = try {
        BuildConfig.TMDB_API_KEY.trim()
    } catch (e: Throwable) {
        ""
    }

    val isApiKeyConfigured: Boolean
        get() = rawApiKey.isNotBlank() &&
                !rawApiKey.equals("YOUR_TMDB_API_KEY_HERE", ignoreCase = true) &&
                !rawApiKey.equals("MY_TMDB_API_KEY", ignoreCase = true)

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url

        if (!isApiKeyConfigured) {
            return@Interceptor chain.proceed(originalRequest)
        }

        val newRequest = if (rawApiKey.startsWith("ey", ignoreCase = false)) {
            // TMDB v4 Bearer Read Access Token
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $rawApiKey")
                .header("Accept", "application/json")
                .build()
        } else {
            // TMDB v3 API Key as query parameter
            val newUrl = originalUrl.newBuilder()
                .addQueryParameter("api_key", rawApiKey)
                .build()
            originalRequest.newBuilder()
                .url(newUrl)
                .header("Accept", "application/json")
                .build()
        }

        chain.proceed(newRequest)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: TmdbApiService = retrofit.create(TmdbApiService::class.java)

    /**
     * Fetches real movie details and cover image from TMDB by movie ID (e.g. 27205 for Inception)
     */
    suspend fun getMovieById(movieId: String): Movie? {
        if (!isApiKeyConfigured) {
            Log.d(TAG, "TMDB API Key is not configured in Secrets panel")
            return null
        }
        return try {
            val response = api.getMovieDetails(movieId = movieId)
            response.toDomainMovie()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch TMDB movie $movieId: ${e.message}", e)
            null
        }
    }

    /**
     * Search movies on TMDB by text or ID
     */
    suspend fun searchMovies(query: String): List<Movie> {
        if (!isApiKeyConfigured || query.isBlank()) return emptyList()

        return try {
            val numericId = query.trim().toIntOrNull()
            if (numericId != null) {
                // Direct lookup by TMDB ID (like 27205)
                val directMovie = getMovieById(numericId.toString())
                if (directMovie != null) {
                    return listOf(directMovie)
                }
            }

            val searchResponse = api.searchMovies(query = query)
            searchResponse.results?.map { it.toDomainMovie() } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search TMDB for '$query': ${e.message}", e)
            emptyList()
        }
    }
}
