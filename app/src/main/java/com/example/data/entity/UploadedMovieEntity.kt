package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.CastMember
import com.example.model.Movie

@Entity(tableName = "uploaded_movies")
data class UploadedMovieEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val posterUrl: String,
    val backdropUrl: String,
    val videoUrl: String,
    val trailerUrl: String = "",
    val year: Int = 2026,
    val durationMinutes: Int = 120,
    val rating: Float = 8.5f,
    val genresString: String = "Action, Sci-Fi",
    val director: String = "Admin Studio",
    val studio: String = "Cloudflare R2 Cinema",
    val category: String = "Admin Uploads",
    val quality: String = "4K Ultra HD",
    val contentRating: String = "PG-13",
    val uploadedAt: Long = System.currentTimeMillis(),
    val r2StorageKey: String = ""
) {
    fun toMovie(): Movie {
        val parsedGenres = genresString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return Movie(
            id = id,
            title = title,
            description = description,
            posterUrl = posterUrl.ifBlank { "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&q=80" },
            backdropUrl = backdropUrl.ifBlank { posterUrl.ifBlank { "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&q=80" } },
            trailerUrl = trailerUrl.ifBlank { videoUrl },
            videoUrl = videoUrl,
            year = year,
            durationMinutes = durationMinutes,
            rating = rating,
            genres = if (parsedGenres.isNotEmpty()) parsedGenres else listOf("Cinema"),
            cast = listOf(
                CastMember("Featured Creator", "Director / Producer", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&q=80"),
                CastMember("Cloudflare R2", "High Speed CDN", "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=200&q=80")
            ),
            director = director,
            studio = studio,
            category = category,
            featured = true,
            trending = true,
            releaseDate = year.toString(),
            quality = quality,
            contentRating = contentRating
        )
    }
}
