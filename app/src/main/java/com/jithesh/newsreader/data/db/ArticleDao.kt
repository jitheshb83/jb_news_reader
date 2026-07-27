package com.jithesh.newsreader.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    @Query("SELECT * FROM articles WHERE feedId = :feedId ORDER BY publishedAt DESC, dateFetched DESC")
    fun observeArticlesForFeed(feedId: Long): Flow<List<ArticleEntity>>

    @Query(
        """
        SELECT articles.*, feeds.title AS feedTitle, feeds.category AS feedCategory
        FROM articles
        INNER JOIN feeds ON feeds.id = articles.feedId
        WHERE feeds.category IN (:categories)
        ORDER BY articles.publishedAt DESC, articles.dateFetched DESC
        LIMIT :limit
        """,
    )
    fun observeArticlesForCategories(categories: List<String>, limit: Int): Flow<List<ArticleWithFeed>>

    @Query("SELECT * FROM articles WHERE id = :articleId")
    suspend fun getArticle(articleId: Long): ArticleEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(articles: List<ArticleEntity>): List<Long>

    @Query("UPDATE articles SET isRead = :isRead WHERE id = :articleId")
    suspend fun setRead(articleId: Long, isRead: Boolean)

    /** Bounds per-feed storage: keeps only the [keepCount] newest articles for [feedId]. */
    @Query(
        """
        DELETE FROM articles WHERE feedId = :feedId AND id NOT IN (
            SELECT id FROM articles WHERE feedId = :feedId
            ORDER BY publishedAt DESC, dateFetched DESC
            LIMIT :keepCount
        )
        """,
    )
    suspend fun pruneOldest(feedId: Long, keepCount: Int)
}
