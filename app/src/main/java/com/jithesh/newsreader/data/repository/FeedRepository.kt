package com.jithesh.newsreader.data.repository

import com.jithesh.newsreader.data.db.ArticleDao
import com.jithesh.newsreader.data.db.ArticleEntity
import com.jithesh.newsreader.data.db.ArticleWithFeed
import com.jithesh.newsreader.data.db.FeedDao
import com.jithesh.newsreader.data.db.FeedEntity
import com.jithesh.newsreader.data.network.FeedFetchService
import com.jithesh.newsreader.data.network.FeedParser
import com.jithesh.newsreader.util.DateUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedRepository @Inject constructor(
    private val feedDao: FeedDao,
    private val articleDao: ArticleDao,
    private val feedFetchService: FeedFetchService,
) {
    companion object {
        val MIN_REFRESH_INTERVAL_MS = java.util.concurrent.TimeUnit.MINUTES.toMillis(15)

        /** Bounds per-feed storage growth — old articles beyond this are pruned after each refresh. */
        const val MAX_ARTICLES_PER_FEED = 200
    }

    fun observeFeeds(): Flow<List<FeedEntity>> = feedDao.observeFeeds()

    fun observeCategories(): Flow<List<String>> = feedDao.observeCategories()

    fun observeArticlesForFeed(feedId: Long): Flow<List<ArticleEntity>> =
        articleDao.observeArticlesForFeed(feedId)

    fun observeArticlesForCategories(categories: List<String>, limit: Int): Flow<List<ArticleWithFeed>> =
        if (categories.isEmpty()) flowOf(emptyList()) else articleDao.observeArticlesForCategories(categories, limit)

    suspend fun getArticle(articleId: Long): ArticleEntity? = articleDao.getArticle(articleId)

    suspend fun getFeed(feedId: Long): FeedEntity? = feedDao.getFeed(feedId)

    suspend fun addFeed(url: String, category: String): Result<Long> {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) return Result.failure(IllegalArgumentException("Feed URL cannot be blank"))
        feedDao.getFeedByUrl(trimmedUrl)?.let {
            return Result.failure(IllegalStateException("This feed has already been added"))
        }
        val feedId = feedDao.insert(FeedEntity(url = trimmedUrl, title = trimmedUrl, category = category))
        refreshFeed(feedId, force = true)
        return Result.success(feedId)
    }

    suspend fun deleteFeed(feed: FeedEntity) {
        feedDao.delete(feed)
    }

    suspend fun setArticleRead(articleId: Long, isRead: Boolean) {
        articleDao.setRead(articleId, isRead)
    }

    suspend fun refreshAllFeeds(force: Boolean) = supervisorScope {
        // supervisorScope: one feed failing to fetch/parse must not cancel the others' in-flight requests.
        feedDao.observeFeeds().first()
            .map { feed -> async { refreshFeed(feed.id, force) } }
            .forEach { it.await() }
    }

    /**
     * Skips the network fetch (and just leaves whatever's already in Room) if the feed was
     * fetched within [MIN_REFRESH_INTERVAL_MS] and [force] is false — avoids re-downloading
     * feeds the user just looked at. OkHttp's own disk cache (see NetworkModule) additionally
     * turns an unchanged fetch that does go through into a cheap 304 rather than a full body.
     */
    suspend fun refreshFeed(feedId: Long, force: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        val feed = feedDao.getFeed(feedId) ?: return@withContext Result.failure(IllegalArgumentException("Feed $feedId not found"))
        val now = System.currentTimeMillis()
        val lastFetchedAt = feed.lastFetchedAt
        if (!force && lastFetchedAt != null && now - lastFetchedAt < MIN_REFRESH_INTERVAL_MS) {
            return@withContext Result.success(Unit)
        }

        try {
            val response = feedFetchService.fetchFeed(feed.url)
            if (!response.isSuccessful) {
                feedDao.update(feed.copy(lastFetchError = "HTTP ${response.code()}"))
                return@withContext Result.failure(IOException("HTTP ${response.code()} fetching ${feed.url}"))
            }
            val body = response.body() ?: return@withContext Result.failure(IOException("Empty response body for ${feed.url}"))
            val parsed = body.byteStream().use { FeedParser.parse(it) }

            feedDao.update(
                feed.copy(
                    title = parsed.title.takeIf { it.isNotBlank() } ?: feed.title,
                    siteLink = parsed.siteLink ?: feed.siteLink,
                    lastFetchedAt = now,
                    lastFetchError = null,
                ),
            )
            val articles = parsed.articles.map { article ->
                ArticleEntity(
                    feedId = feedId,
                    guid = article.guid,
                    title = article.title,
                    link = article.link,
                    description = article.description,
                    contentHtml = article.contentHtml,
                    author = article.author,
                    publishedAt = DateUtils.parseFeedDate(article.publishedAtRaw),
                    thumbnailUrl = article.thumbnailUrl,
                )
            }
            articleDao.insertAll(articles)
            articleDao.pruneOldest(feedId, MAX_ARTICLES_PER_FEED)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Deliberately broad: this is untrusted network content (HTTP + hand-rolled XML
            // parsing) being processed for potentially many feeds in parallel. A single
            // malformed/unreachable feed must fail gracefully (Result.failure + lastFetchError)
            // rather than propagate out of viewModelScope and crash the whole app.
            feedDao.update(feed.copy(lastFetchError = e.message ?: "Failed to refresh"))
            Result.failure(e)
        }
    }
}
