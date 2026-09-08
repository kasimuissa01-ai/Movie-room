package com.example.data.tmdb

import android.util.Log
import com.example.data.api.MovieApiClient
import com.example.model.Movie

/**
 * TMDB Client Proxy:
 * Routes movie queries securely through our Cloudflare Worker backend.
 * ZERO private TMDB API keys are required or stored in the APK.
 */
object TmdbClient {
    private const val TAG = "TmdbClient"

    /**
     * Always returns true because the Cloudflare Worker backend securely handles TMDB credentials.
     */
    val isApiKeyConfigured: Boolean
        get() = true

    /**
     * Fetches movie details and cover image via our secure Cloudflare Worker API
     */
    suspend fun getMovieById(movieId: String): Movie? {
        return try {
            val cleanId = if (movieId.startsWith("tmdb_")) movieId else "tmdb_$movieId"
            MovieApiClient.getMovieById(id = cleanId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed fetching movie via backend: ${e.message}")
            null
        }
    }

    /**
     * Search movies securely via our Cloudflare Worker backend
     */
    suspend fun searchMovies(query: String): List<Movie> {
        return try {
            MovieApiClient.searchMovies(query = query)
        } catch (e: Exception) {
            Log.e(TAG, "Search error via backend: ${e.message}")
            emptyList()
        }
    }

    /**
     * Fetches trending movies securely from the Cloudflare Worker backend
     */
    suspend fun getTrendingMovies(): List<Movie> {
        return try {
            MovieApiClient.getTrendingMovies()
        } catch (e: Exception) {
            Log.e(TAG, "Trending fetch error: ${e.message}")
            emptyList()
        }
    }
}
