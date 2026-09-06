package com.example.model

data class Movie(
    val id: String,
    val title: String,
    val description: String,
    val posterUrl: String,
    val backdropUrl: String,
    val trailerUrl: String = "",
    val videoUrl: String = "",
    val year: Int,
    val durationMinutes: Int,
    val rating: Float,
    val genres: List<String>,
    val cast: List<CastMember>,
    val director: String,
    val studio: String = "CineStream Studios",
    val category: String,
    val featured: Boolean = false,
    val trending: Boolean = false,
    val releaseDate: String = "2026",
    val quality: String = "4K Ultra HD",
    val contentRating: String = "PG-13"
) {
    val durationFormatted: String
        get() {
            val hours = durationMinutes / 60
            val minutes = durationMinutes % 60
            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }
}
