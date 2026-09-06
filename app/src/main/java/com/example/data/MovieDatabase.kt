package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.MovieDao
import com.example.data.entity.CachedMovieEntity
import com.example.data.entity.DownloadedMovieEntity
import com.example.data.entity.UploadedMovieEntity
import com.example.data.entity.WatchHistoryEntity
import com.example.data.entity.WatchlistItemEntity

@Database(
    entities = [
        WatchlistItemEntity::class,
        WatchHistoryEntity::class,
        UploadedMovieEntity::class,
        DownloadedMovieEntity::class,
        CachedMovieEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class MovieDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao

    companion object {
        @Volatile
        private var INSTANCE: MovieDatabase? = null

        fun getDatabase(context: Context): MovieDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MovieDatabase::class.java,
                    "cinestream_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
