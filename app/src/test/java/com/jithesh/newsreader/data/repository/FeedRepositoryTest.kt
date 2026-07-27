package com.jithesh.newsreader.data.repository

import com.jithesh.newsreader.data.db.ArticleDao
import com.jithesh.newsreader.data.db.ArticleEntity
import com.jithesh.newsreader.data.db.ArticleWithFeed
import com.jithesh.newsreader.data.db.FeedDao
import com.jithesh.newsreader.data.db.FeedEntity
import com.jithesh.newsreader.data.network.FeedFetchService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

private const val RSS_MINIMAL = """<?xml version="1.0"?>
<rss version="2.0">
  <channel>
    <title>Fake Feed</title>
    <item>
      <title>Only Item</title>
      <link>https://example.com/item</link>
      <guid>item-guid</guid>
    </item>
  </channel>
</rss>
"""

private class FakeFeedDao : FeedDao {
    private val feeds = MutableStateFlow<List<FeedEntity>>(emptyList())
    private var nextId = 1L

    override fun observeFeeds(): Flow<List<FeedEntity>> = feeds

    override suspend fun getFeed(feedId: Long): FeedEntity? = feeds.value.find { it.id == feedId }

    override suspend fun getFeedByUrl(url: String): FeedEntity? = feeds.value.find { it.url == url }

    override fun observeCategories(): Flow<List<String>> = MutableStateFlow(feeds.value.map { it.category }.distinct())

    override suspend fun insert(feed: FeedEntity): Long {
        val id = nextId++
        feeds.value = feeds.value + feed.copy(id = id)
        return id
    }

    override suspend fun update(feed: FeedEntity) {
        feeds.value = feeds.value.map { if (it.id == feed.id) feed else it }
    }

    override suspend fun delete(feed: FeedEntity) {
        feeds.value = feeds.value.filterNot { it.id == feed.id }
    }

    fun seed(feed: FeedEntity): FeedEntity {
        val id = nextId++
        val seeded = feed.copy(id = id)
        feeds.value = feeds.value + seeded
        return seeded
    }
}

private class FakeArticleDao : ArticleDao {
    val inserted = mutableListOf<ArticleEntity>()
    private var nextId = 1L

    override fun observeArticlesForFeed(feedId: Long): Flow<List<ArticleEntity>> =
        MutableStateFlow(inserted.filter { it.feedId == feedId })

    override fun observeArticlesForCategories(categories: List<String>, limit: Int): Flow<List<ArticleWithFeed>> =
        MutableStateFlow(emptyList())

    override suspend fun getArticle(articleId: Long): ArticleEntity? = inserted.find { it.id == articleId }

    override suspend fun insertAll(articles: List<ArticleEntity>): List<Long> {
        val withIds = articles.map { it.copy(id = nextId++) }
        inserted += withIds
        return withIds.map { it.id }
    }

    override suspend fun setRead(articleId: Long, isRead: Boolean) {
        val idx = inserted.indexOfFirst { it.id == articleId }
        if (idx >= 0) inserted[idx] = inserted[idx].copy(isRead = isRead)
    }

    override suspend fun pruneOldest(feedId: Long, keepCount: Int) {
        val forFeed = inserted.filter { it.feedId == feedId }
            .sortedWith(compareByDescending<ArticleEntity> { it.publishedAt ?: 0L }.thenByDescending { it.dateFetched })
        val toRemove = forFeed.drop(keepCount).map { it.id }.toSet()
        inserted.removeAll { it.id in toRemove }
    }
}

private class FakeFeedFetchService : FeedFetchService {
    var callCount = 0
        private set
    var behavior: (String) -> Response<ResponseBody> = {
        Response.success(RSS_MINIMAL.toResponseBody("application/xml".toMediaType()))
    }

    override suspend fun fetchFeed(url: String): Response<ResponseBody> {
        callCount++
        return behavior(url)
    }
}

class FeedRepositoryTest {

    private lateinit var feedDao: FakeFeedDao
    private lateinit var articleDao: FakeArticleDao
    private lateinit var fetchService: FakeFeedFetchService
    private lateinit var repository: FeedRepository

    @Before
    fun setUp() {
        feedDao = FakeFeedDao()
        articleDao = FakeArticleDao()
        fetchService = FakeFeedFetchService()
        repository = FeedRepository(feedDao, articleDao, fetchService)
    }

    @Test
    fun `refreshFeed skips network call when recently fetched and not forced`() = runTest {
        val feed = feedDao.seed(
            FeedEntity(url = "https://example.com/feed", title = "Feed", category = "World", lastFetchedAt = System.currentTimeMillis()),
        )

        val result = repository.refreshFeed(feed.id, force = false)

        assertTrue(result.isSuccess)
        assertEquals(0, fetchService.callCount)
    }

    @Test
    fun `refreshFeed fetches when never fetched before, even without force`() = runTest {
        val feed = feedDao.seed(
            FeedEntity(url = "https://example.com/feed", title = "Feed", category = "World", lastFetchedAt = null),
        )

        repository.refreshFeed(feed.id, force = false)

        assertEquals(1, fetchService.callCount)
    }

    @Test
    fun `refreshFeed fetches when last fetch was outside the throttle window`() = runTest {
        val staleTimestamp = System.currentTimeMillis() - FeedRepository.MIN_REFRESH_INTERVAL_MS - 1_000
        val feed = feedDao.seed(
            FeedEntity(url = "https://example.com/feed", title = "Feed", category = "World", lastFetchedAt = staleTimestamp),
        )

        repository.refreshFeed(feed.id, force = false)

        assertEquals(1, fetchService.callCount)
    }

    @Test
    fun `refreshFeed with force bypasses the throttle`() = runTest {
        val feed = feedDao.seed(
            FeedEntity(url = "https://example.com/feed", title = "Feed", category = "World", lastFetchedAt = System.currentTimeMillis()),
        )

        repository.refreshFeed(feed.id, force = true)

        assertEquals(1, fetchService.callCount)
    }

    @Test
    fun `successful refresh stores articles and clears any previous error`() = runTest {
        val feed = feedDao.seed(
            FeedEntity(url = "https://example.com/feed", title = "Feed", category = "World", lastFetchError = "old error"),
        )

        val result = repository.refreshFeed(feed.id, force = true)

        assertTrue(result.isSuccess)
        assertEquals(1, articleDao.inserted.size)
        assertEquals("Only Item", articleDao.inserted[0].title)
        val updatedFeed = feedDao.getFeed(feed.id)
        assertNotNull(updatedFeed?.lastFetchedAt)
        assertNull(updatedFeed?.lastFetchError)
    }

    @Test
    fun `malformed XML fails gracefully instead of crashing`() = runTest {
        fetchService.behavior = {
            Response.success("<rss><channel><title>Broken<".toResponseBody("application/xml".toMediaType()))
        }
        val feed = feedDao.seed(FeedEntity(url = "https://example.com/feed", title = "Feed", category = "World"))

        // Must not throw — a malformed feed is expected, untrusted input, not a programming error.
        val result = repository.refreshFeed(feed.id, force = true)

        assertTrue(result.isFailure)
        assertNotNull(feedDao.getFeed(feed.id)?.lastFetchError)
    }

    @Test
    fun `refreshAllFeeds continues past one feed's failure to fetch the rest`() = runTest {
        val badFeed = feedDao.seed(FeedEntity(url = "https://bad.example.com/feed", title = "Bad", category = "World"))
        val goodFeed = feedDao.seed(FeedEntity(url = "https://good.example.com/feed", title = "Good", category = "World"))
        fetchService.behavior = { url ->
            if (url.contains("bad")) throw java.io.IOException("boom") else Response.success(RSS_MINIMAL.toResponseBody("application/xml".toMediaType()))
        }

        repository.refreshAllFeeds(force = true)

        assertNotNull(feedDao.getFeed(badFeed.id)?.lastFetchError)
        assertNull(feedDao.getFeed(goodFeed.id)?.lastFetchError)
        assertEquals(1, articleDao.inserted.count { it.feedId == goodFeed.id })
    }

    @Test
    fun `refresh prunes articles beyond MAX_ARTICLES_PER_FEED`() = runTest {
        val feed = feedDao.seed(FeedEntity(url = "https://example.com/feed", title = "Feed", category = "World"))
        val manyItemsRss = buildString {
            append("<rss version=\"2.0\"><channel><title>Big Feed</title>")
            repeat(FeedRepository.MAX_ARTICLES_PER_FEED + 50) { i ->
                append("<item><title>Item $i</title><link>https://example.com/$i</link><guid>g$i</guid></item>")
            }
            append("</channel></rss>")
        }
        fetchService.behavior = { Response.success(manyItemsRss.toResponseBody("application/xml".toMediaType())) }

        repository.refreshFeed(feed.id, force = true)

        assertEquals(FeedRepository.MAX_ARTICLES_PER_FEED, articleDao.inserted.count { it.feedId == feed.id })
    }

    @Test
    fun `HTTP error response records lastFetchError and fails`() = runTest {
        fetchService.behavior = {
            Response.error(500, "".toResponseBody("application/xml".toMediaType()))
        }
        val feed = feedDao.seed(FeedEntity(url = "https://example.com/feed", title = "Feed", category = "World"))

        val result = repository.refreshFeed(feed.id, force = true)

        assertTrue(result.isFailure)
        assertEquals("HTTP 500", feedDao.getFeed(feed.id)?.lastFetchError)
    }

    @Test
    fun `addFeed rejects a feed that is already added`() = runTest {
        feedDao.seed(FeedEntity(url = "https://example.com/feed", title = "Feed", category = "World"))

        val result = repository.addFeed("https://example.com/feed", "World")

        assertTrue(result.isFailure)
    }
}
