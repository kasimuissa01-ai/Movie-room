package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Movie

@Entity(tableName = "downloaded_movies")
data class DownloadedMovieEntity(
    @PrimaryKey
    val movieId: String,
    val title: String,
    val posterUrl: String,
    val backdropUrl: String,
    val localFilePath: String,
    val remoteVideoUrl: String,
    val fileSizeBytes: Long = 0L,
    val fileSizeFormatted: String = "45 MB",
    val durationFormatted: String = "2h 10m",
    val durationMinutes: Int = 130,
    val quality: String = "1080p Full HD",
    val year: Int = 2024,
    val genresCsv: String = "Action, Sci-Fi",
    val description: String = "",
    val downloadStatus: String = "COMPLETED", // QUEUED, DOWNLOADING, COMPLETED, FAILED
    val downloadProgress: Int = 100,
    val downloadedAt: Long = System.currentTimeMillis()
) {
    fun toMovie(): Movie {
        val genreList = genresCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
        return Movie(
            id = movieId,
            title = title,
            description = description,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            videoUrl = localFilePath.ifBlank { remoteVideoUrl },
            rating = 8.5f,
            quality = quality,
            year = year,
            durationMinutes = durationMinutes,
            genres = if (genreList.isNotEmpty()) genreList else listOf("Offline Download"),
            director = "Offline Movie",
            cast = emptyList(),
            contentRating = "PG-13",
            category = "downloads",
            featured = false,
            trending = false
        )
    }
}
