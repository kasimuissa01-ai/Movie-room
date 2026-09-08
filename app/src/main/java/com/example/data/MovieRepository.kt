package com.example.data

import com.example.data.dao.MovieDao
import com.example.data.entity.CachedMovieEntity
import com.example.data.entity.DownloadedMovieEntity
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
        // Also cache in local Room cached_movies table
        movieDao.insertCachedMovie(CachedMovieEntity.fromMovie(entity.toMovie()))
    }

    suspend fun deleteUploadedMovie(id: String) {
        movieDao.deleteUploadedMovie(id)
        movieDao.deleteCachedMovie(id)
    }

    /**
     * Reactive stream of Watchlist movies.
     * Each item is retrieved directly from Room's local persistent storage,
     * including full title, description, genres, year, rating, and poster/backdrop art,
     * enabling complete offline access without network connection.
     */
    fun getWatchlistMovies(resolver: ((String) -> Movie?)? = null): Flow<List<Movie>> {
        return movieDao.getWatchlistItems().map { items ->
            items.map { item ->
                if (item.title.isNotBlank()) {
                    item.toMovie()
                } else {
                    resolver?.invoke(item.movieId)
                        ?: movieDao.getCachedMovieById(item.movieId)?.toMovie()
                        ?: item.toMovie()
                }
            }
        }
    }

    fun isMovieInWatchlist(movieId: String): Flow<Boolean> {
        return movieDao.isInWatchlist(movieId)
    }

    suspend fun toggleWatchlist(movie: Movie, currentlyInWatchlist: Boolean) {
        if (currentlyInWatchlist) {
            movieDao.deleteWatchlistItem(movie.id)
        } else {
            val watchlistItem = WatchlistItemEntity.fromMovie(movie)
            movieDao.insertWatchlistItem(watchlistItem)
            // Also ensure movie is present in cached_movies
            movieDao.insertCachedMovie(CachedMovieEntity.fromMovie(movie))
        }
    }

    suspend fun toggleWatchlist(movieId: String, currentlyInWatchlist: Boolean) {
        if (currentlyInWatchlist) {
            movieDao.deleteWatchlistItem(movieId)
        } else {
            val cached = movieDao.getCachedMovieById(movieId)?.toMovie()
            val watchlistItem = if (cached != null) {
                WatchlistItemEntity.fromMovie(cached)
            } else {
                WatchlistItemEntity(movieId = movieId)
            }
            movieDao.insertWatchlistItem(watchlistItem)
        }
    }

    // ==========================================
    // Local Room Movie Caching
    // ==========================================
    fun getAllCachedMovies(): Flow<List<Movie>> {
        return movieDao.getAllCachedMovies().map { list ->
            list.map { it.toMovie() }
        }
    }

    suspend fun getCachedMovieById(id: String): Movie? {
        val cached = movieDao.getCachedMovieById(id)
        if (cached != null) return cached.toMovie()

        val watchlistItem = movieDao.getWatchlistItemById(id)
        if (watchlistItem != null && watchlistItem.title.isNotBlank()) {
            return watchlistItem.toMovie()
        }
        return null
    }

    suspend fun cacheMovie(movie: Movie) {
        movieDao.insertCachedMovie(CachedMovieEntity.fromMovie(movie))
    }

    suspend fun cacheMovies(movies: List<Movie>) {
        if (movies.isEmpty()) return
        val entities = movies.map { CachedMovieEntity.fromMovie(it) }
        movieDao.insertCachedMovies(entities)
    }

    fun getCachedMoviesCountFlow(): Flow<Int> {
        return movieDao.getCachedMoviesCountFlow()
    }

    suspend fun getCachedMoviesCount(): Int {
        return movieDao.getCachedMoviesCount()
    }

    // ==========================================
    // Watch History
    // ==========================================
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

    // ==========================================
    // Offline Downloaded Movies
    // ==========================================
    fun getDownloadedMovies(): Flow<List<DownloadedMovieEntity>> {
        return movieDao.getDownloadedMovies()
    }

    fun isMovieDownloaded(movieId: String): Flow<Boolean> {
        return movieDao.isMovieDownloaded(movieId)
    }

    suspend fun deleteDownloadedMovie(movieId: String) {
        movieDao.deleteDownloadedMovie(movieId)
    }
}
