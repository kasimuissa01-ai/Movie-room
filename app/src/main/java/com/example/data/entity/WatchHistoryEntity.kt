package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey
    val movieId: String,
    val positionSeconds: Int,
    val totalDurationSeconds: Int,
    val lastWatchedAt: Long = System.currentTimeMillis()
)
