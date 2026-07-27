package com.jithesh.newsreader.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "articles",
    foreignKeys = [
        ForeignKey(
            entity = FeedEntity::class,
            parentColumns = ["id"],
            childColumns = ["feedId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["feedId", "guid"], unique = true),
        Index(value = ["feedId"]),
    ],
)
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val feedId: Long,
    val guid: String,
    val title: String,
    val link: String,
    val description: String? = null,
    val contentHtml: String? = null,
    val author: String? = null,
    val publishedAt: Long? = null,
    val thumbnailUrl: String? = null,
    val isRead: Boolean = false,
    val dateFetched: Long = System.currentTimeMillis(),
)
