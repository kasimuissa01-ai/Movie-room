package com.example.data.api

import com.example.model.Movie
import com.squareup.moshi.Json

data class ApiResponse<T>(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "count") val count: Int? = null,
    @Json(name = "data") val data: T? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "message") val message: String? = null
)

data class MovieDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "tmdbId") val tmdbId: Int? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "overview") val overview: String? = null,
    @Json(name = "posterUrl") val posterUrl: String? = null,
    @Json(name = "backdropUrl") val backdropUrl: String? = null,
    @Json(name = "videoUrl") val videoUrl: String? = null,
    @Json(name = "releaseYear") val releaseYear: String? = null,
    @Json(name = "rating") val rating: Double? = null,
    @Json(name = "durationMinutes") val durationMinutes: Int? = null,
    @Json(name = "genres") val genres: List<String>? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "isTrending") val isTrending: Boolean? = null,
    @Json(name = "isPopular") val isPopular: Boolean? = null,
    @Json(name = "isFeatured") val isFeatured: Boolean? = null
) {
    fun toMovie(): Movie {
        val safeId = id ?: (tmdbId?.let { "tmdb_$it" } ?: java.util.UUID.randomUUID().toString())
        val safeTitle = title ?: "Untitled Movie"
        val safeOverview = overview ?: ""
        val safePoster = posterUrl ?: ""
        val safeBackdrop = if (!backdropUrl.isNullOrBlank()) backdropUrl else safePoster
        val safeYear = releaseYear?.toIntOrNull() ?: 2026
        val safeRating = (rating ?: 7.5).toFloat()
        val safeDuration = durationMinutes ?: 120
        val safeCategory = category ?: "Action"
        val safeGenres = if (!genres.isNullOrEmpty()) genres else listOf(safeCategory)

        return Movie(
            id = safeId,
            title = safeTitle,
            description = safeOverview,
            posterUrl = safePoster,
            backdropUrl = safeBackdrop,
            videoUrl = videoUrl ?: "",
            year = safeYear,
            durationMinutes = safeDuration,
            rating = safeRating,
            genres = safeGenres,
            cast = emptyList(),
            director = "CineStream Director",
            category = safeCategory,
            featured = isFeatured ?: false,
            trending = isTrending ?: false,
            releaseDate = releaseYear ?: safeYear.toString()
        )
    }
}

data class LoginRequest(
    @Json(name = "email") val email: String? = null,
    @Json(name = "name") val name: String? = null
)

data class AdminLoginRequest(
    @Json(name = "passcode") val passcode: String
)

data class AuthResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "token") val token: String? = null,
    @Json(name = "user") val user: UserDto? = null,
    @Json(name = "error") val error: String? = null
)

data class UserDto(
    @Json(name = "userId") val userId: String,
    @Json(name = "email") val email: String,
    @Json(name = "name") val name: String = "VIP Member",
    @Json(name = "role") val role: String = "user"
)

data class WatchlistActionRequest(
    @Json(name = "movieId") val movieId: String
)

data class WatchHistoryRequest(
    @Json(name = "movieId") val movieId: String,
    @Json(name = "watchedDurationSeconds") val watchedDurationSeconds: Int,
    @Json(name = "totalDurationSeconds") val totalDurationSeconds: Int
)

data class R2UploadResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "message") val message: String? = null,
    @Json(name = "key") val key: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "sizeBytes") val sizeBytes: Long? = null,
    @Json(name = "error") val error: String? = null
)
