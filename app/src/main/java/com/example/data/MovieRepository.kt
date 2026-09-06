package com.example.data

import com.example.data.dao.MovieDao
import com.example.data.entity.UploadedMovieEntity
import com.example.data.entity.WatchHistoryEntity
import com.example.data.entity.WatchlistItemEntity
import com.example.model.Movie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MovieRepository(private val movieDao: MovieDao) {

    fun getUploadedMovies(): Flow<List<Movie>> {
        return movieDao.getUploadedMovies().map { entities ->
            entities.map { it.toMovie() }
        }
    }

    suspend fun saveUploadedMovie(entity: UploadedMovieEntity) {
        movieDao.insertUploadedMovie(entity)
    }

    suspend fun deleteUploadedMovie(id: String) {
        movieDao.deleteUploadedMovie(id)
    }

    fun getWatchlistMovies(resolver: ((String) -> Movie?)? = null): Flow<List<Movie>> {
        return movieDao.getWatchlistMovieIds().map { ids ->
            ids.mapNotNull { id ->
                resolver?.invoke(id) ?: SampleMovies.getMovieById(id)
            }
        }
    }

    fun isMovieInWatchlist(movieId: String): Flow<Boolean> {
        return movieDao.isInWatchlist(movieId)
    }

    suspend fun toggleWatchlist(movieId: String, currentlyInWatchlist: Boolean) {
        if (currentlyInWatchlist) {
            movieDao.deleteWatchlistItem(movieId)
        } else {
            movieDao.insertWatchlistItem(WatchlistItemEntity(movieId = movieId))
        }
    }

    fun getWatchHistory(): Flow<List<WatchHistoryEntity>> {
        return movieDao.getWatchHistory()
    }

    suspend fun recordWatchProgress(movieId: String, positionSeconds: Int, totalDurationSeconds: Int) {
        movieDao.saveWatchProgress(
            WatchHistoryEntity(
                movieId = movieId,
                positionSeconds = positionSeconds,
                totalDurationSeconds = totalDurationSeconds,
                lastWatchedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeFromHistory(movieId: String) {
        movieDao.deleteFromHistory(movieId)
    }

    suspend fun clearHistory() {
        movieDao.clearWatchHistory()
    }
}
