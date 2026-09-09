package com.example.data.tmdb

import android.content.Context
import android.util.Log
import com.example.data.api.MovieApiClient
import com.example.model.Movie

/**
 * TMDB Client Proxy:
 * Routes movie queries securely through our Cloudflare Worker backend.
 * ZERO private TMDB API keys are stored in or required by the Android client APK.
 */
object TmdbClient {
    private const val TAG = "TmdbClient"

    val isApiKeyConfigured: Boolean
        get() = true

    suspend fun getMovieById(movieId: String): Movie? {
        return try {
            val cleanId = if (movieId.startsWith("tmdb_")) movieId else "tmdb_$movieId"
            MovieApiClient.getMovieById(id = cleanId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed fetching movie via backend: ${e.message}")
            null
        }
    }

    suspend fun searchMovies(query: String, context: Context? = null): List<Movie> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return emptyList()

        return try {
            MovieApiClient.searchMovies(context = context, query = cleanQuery)
        } catch (e: Exception) {
            Log.e(TAG, "Search error via backend: ${e.message}")
            emptyList()
        }
    }

    suspend fun getTrendingMovies(): List<Movie> {
        return try {
            MovieApiClient.getTrendingMovies()
        } catch (e: Exception) {
            Log.e(TAG, "Trending fetch error: ${e.message}")
            emptyList()
        }
    }
}
