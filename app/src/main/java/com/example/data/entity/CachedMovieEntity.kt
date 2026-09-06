package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.CastMember
import com.example.model.Movie

/**
 * Room Database entity that caches complete basic movie metadata locally
 * so users can view movie info, descriptions, genres, ratings, and details
 * even without an active internet connection.
 */
@Entity(tableName = "cached_movies")
data class CachedMovieEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val posterUrl: String,
    val backdropUrl: String,
    val trailerUrl: String = "",
    val videoUrl: String = "",
    val year: Int = 2026,
    val durationMinutes: Int = 120,
    val rating: Float = 8.0f,
    val genresCsv: String = "",
    val director: String = "",
    val studio: String = "Movie Room Studios",
    val category: String = "Cinema",
    val quality: String = "4K Ultra HD",
    val contentRating: String = "PG-13",
    val castString: String = "",
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toMovie(): Movie {
        val parsedGenres = genresCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val parsedCast = if (castString.isNotBlank()) {
            castString.split(";;").mapNotNull { entry ->
                val parts = entry.split("||")
                if (parts.size >= 3) {
                    CastMember(parts[0], parts[1], parts[2])
                } else if (parts.size == 2) {
                    CastMember(parts[0], parts[1], "")
                } else if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                    CastMember(parts[0], "Actor", "")
                } else null
            }
        } else emptyList()

        return Movie(
            id = id,
            title = title,
            description = description,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl.ifBlank { posterUrl },
            trailerUrl = trailerUrl,
            videoUrl = videoUrl,
            year = year,
            durationMinutes = durationMinutes,
            rating = rating,
            genres = if (parsedGenres.isNotEmpty()) parsedGenres else listOf("Cinema"),
            cast = parsedCast,
            director = director.ifBlank { "Movie Room" },
            studio = studio,
            category = category.ifBlank { "Cinema" },
            featured = false,
            trending = false,
            releaseDate = year.toString(),
            quality = quality,
            contentRating = contentRating
        )
    }

    companion object {
        fun fromMovie(movie: Movie): CachedMovieEntity {
            val castEncoded = movie.cast.joinToString(";;") { "${it.name}||${it.character}||${it.avatarUrl}" }
            return CachedMovieEntity(
                id = movie.id,
                title = movie.title,
                description = movie.description,
                posterUrl = movie.posterUrl,
                backdropUrl = movie.backdropUrl,
                trailerUrl = movie.trailerUrl,
                videoUrl = movie.videoUrl,
                year = movie.year,
                durationMinutes = movie.durationMinutes,
                rating = movie.rating,
                genresCsv = movie.genres.joinToString(", "),
                director = movie.director,
                studio = movie.studio,
                category = movie.category,
                quality = movie.quality,
                contentRating = movie.contentRating,
                castString = castEncoded,
                cachedAt = System.currentTimeMillis()
            )
        }
    }
}
