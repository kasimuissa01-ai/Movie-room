package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.UploadedMovieEntity
import com.example.data.entity.WatchHistoryEntity
import com.example.data.entity.WatchlistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {
    @Query("SELECT movieId FROM watchlist_items ORDER BY addedAt DESC")
    fun getWatchlistMovieIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist_items WHERE movieId = :movieId)")
    fun isInWatchlist(movieId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlistItem(item: WatchlistItemEntity)

    @Query("DELETE FROM watchlist_items WHERE movieId = :movieId")
    suspend fun deleteWatchlistItem(movieId: String)

    @Query("SELECT * FROM watch_history ORDER BY lastWatchedAt DESC")
    fun getWatchHistory(): Flow<List<WatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWatchProgress(history: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE movieId = :movieId")
    suspend fun deleteFromHistory(movieId: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearWatchHistory()

    @Query("SELECT * FROM uploaded_movies ORDER BY uploadedAt DESC")
    fun getUploadedMovies(): Flow<List<UploadedMovieEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUploadedMovie(movie: UploadedMovieEntity)

    @Query("DELETE FROM uploaded_movies WHERE id = :id")
    suspend fun deleteUploadedMovie(id: String)
}
