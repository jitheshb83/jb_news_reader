package com.jithesh.newsreader.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feeds", indices = [androidx.room.Index(value = ["url"], unique = true)])
data class FeedEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val title: String,
    val category: String,
    val siteLink: String? = null,
    val lastFetchedAt: Long? = null,
    val lastFetchError: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
)
