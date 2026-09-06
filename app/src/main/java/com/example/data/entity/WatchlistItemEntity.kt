package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist_items")
data class WatchlistItemEntity(
    @PrimaryKey
    val movieId: String,
    val addedAt: Long = System.currentTimeMillis()
)
