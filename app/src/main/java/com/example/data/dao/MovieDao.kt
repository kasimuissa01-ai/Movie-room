package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.CachedMovieEntity
import com.example.data.entity.DownloadedMovieEntity
import com.example.data.entity.UploadedMovieEntity
import com.example.data.entity.WatchHistoryEntity
import com.example.data.entity.WatchlistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {

    // ==========================================
    // Watchlist Operations (Saved Movies)
    // ==========================================
    @Query("SELECT * FROM watchlist_items ORDER BY addedAt DESC")
    fun getWatchlistItems(): Flow<List<WatchlistItemEntity>>

    @Query("SELECT * FROM watchlist_items ORDER BY addedAt DESC")
    suspend fun getWatchlistItemsDirect(): List<WatchlistItemEntity>

    @Query("SELECT * FROM watchlist_items WHERE movieId = :movieId LIMIT 1")
    suspend fun getWatchlistItemById(movieId: String): WatchlistItemEntity?

    @Query("SELECT movieId FROM watchlist_items ORDER BY addedAt DESC")
    fun getWatchlistMovieIds(): Flow<List<String>>

    @Query("SELECT movieId FROM watchlist_items ORDER BY addedAt DESC")
    suspend fun getWatchlistMovieIdsDirect(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist_items WHERE movieId = :movieId)")
    fun isInWatchlist(movieId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlistItem(item: WatchlistItemEntity)

    @Query("DELETE FROM watchlist_items WHERE movieId = :movieId")
    suspend fun deleteWatchlistItem(movieId: String)

    @Query("DELETE FROM watchlist_items")
    suspend fun clearWatchlist()

    // ==========================================
    // General Cached Movies (Offline Basic Info)
    // ==========================================
    @Query("SELECT * FROM cached_movies ORDER BY cachedAt DESC")
    fun getAllCachedMovies(): Flow<List<CachedMovieEntity>>

    @Query("SELECT * FROM cached_movies WHERE id = :id LIMIT 1")
    suspend fun getCachedMovieById(id: String): CachedMovieEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedMovie(movie: CachedMovieEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedMovies(movies: List<CachedMovieEntity>)

    @Query("DELETE FROM cached_movies WHERE id = :id")
    suspend fun deleteCachedMovie(id: String)

    @Query("SELECT COUNT(*) FROM cached_movies")
    fun getCachedMoviesCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cached_movies")
    suspend fun getCachedMoviesCount(): Int

    // ==========================================
    // Watch History Operations
    // ==========================================
    @Query("SELECT * FROM watch_history ORDER BY lastWatchedAt DESC")
    fun getWatchHistory(): Flow<List<WatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWatchProgress(history: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE movieId = :movieId")
    suspend fun deleteFromHistory(movieId: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearWatchHistory()

    // ==========================================
    // Admin Uploaded Movies (Cloudflare R2)
    // ==========================================
    @Query("SELECT * FROM uploaded_movies ORDER BY uploadedAt DESC")
    fun getUploadedMovies(): Flow<List<UploadedMovieEntity>>

    @Query("SELECT * FROM uploaded_movies ORDER BY uploadedAt DESC")
    suspend fun getUploadedMoviesDirect(): List<UploadedMovieEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUploadedMovie(movie: UploadedMovieEntity)

    @Query("DELETE FROM uploaded_movies WHERE id = :id")
    suspend fun deleteUploadedMovie(id: String)

    // ==========================================
    // Offline Downloaded Movies
    // ==========================================
    @Query("SELECT * FROM downloaded_movies ORDER BY downloadedAt DESC")
    fun getDownloadedMovies(): Flow<List<DownloadedMovieEntity>>

    @Query("SELECT * FROM downloaded_movies WHERE movieId = :movieId LIMIT 1")
    fun getDownloadedMovie(movieId: String): Flow<DownloadedMovieEntity?>

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_movies WHERE movieId = :movieId AND downloadStatus = 'COMPLETED')")
    fun isMovieDownloaded(movieId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedMovie(movie: DownloadedMovieEntity)

    @Query("UPDATE downloaded_movies SET downloadProgress = :progress, downloadStatus = :status WHERE movieId = :movieId")
    suspend fun updateDownloadProgress(movieId: String, progress: Int, status: String)

    @Query("DELETE FROM downloaded_movies WHERE movieId = :movieId")
    suspend fun deleteDownloadedMovie(movieId: String)

    @Query("DELETE FROM downloaded_movies")
    suspend fun clearAllDownloads()
}
