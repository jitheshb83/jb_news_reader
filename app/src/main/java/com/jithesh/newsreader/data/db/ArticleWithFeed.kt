package com.jithesh.newsreader.data.db

/** Projection used by Home: an article joined with its parent feed's title/category. */
data class ArticleWithFeed(
    val id: Long,
    val feedId: Long,
    val guid: String,
    val title: String,
    val link: String,
    val description: String?,
    val contentHtml: String?,
    val author: String?,
    val publishedAt: Long?,
    val thumbnailUrl: String?,
    val isRead: Boolean,
    val dateFetched: Long,
    val feedTitle: String,
    val feedCategory: String,
)
