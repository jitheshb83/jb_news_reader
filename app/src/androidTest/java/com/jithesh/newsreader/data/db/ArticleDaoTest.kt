package com.jithesh.newsreader.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArticleDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var feedDao: FeedDao
    private lateinit var articleDao: ArticleDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        feedDao = db.feedDao()
        articleDao = db.articleDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun observeArticlesForCategories_filtersByCategoryAndRespectsLimit() = runTest {
        val worldFeedId = feedDao.insert(FeedEntity(url = "https://a.com/rss", title = "World Feed", category = "World"))
        val aiFeedId = feedDao.insert(FeedEntity(url = "https://b.com/rss", title = "AI Feed", category = "AI"))
        val indiaFeedId = feedDao.insert(FeedEntity(url = "https://c.com/rss", title = "India Feed", category = "India"))

        articleDao.insertAll(
            listOf(
                ArticleEntity(feedId = worldFeedId, guid = "w1", title = "World 1", link = "https://a.com/1", publishedAt = 3_000),
                ArticleEntity(feedId = worldFeedId, guid = "w2", title = "World 2", link = "https://a.com/2", publishedAt = 1_000),
                ArticleEntity(feedId = aiFeedId, guid = "a1", title = "AI 1", link = "https://b.com/1", publishedAt = 2_000),
                ArticleEntity(feedId = indiaFeedId, guid = "i1", title = "India 1", link = "https://c.com/1", publishedAt = 4_000),
            ),
        )

        val result = articleDao.observeArticlesForCategories(listOf("World", "AI"), limit = 10).first()

        assertEquals(3, result.size)
        assertEquals(setOf("World 1", "World 2", "AI 1"), result.map { it.title }.toSet())
        // newest first
        assertEquals(listOf("World 1", "AI 1", "World 2"), result.map { it.title })
    }

    @Test
    fun observeArticlesForCategories_respectsLimit() = runTest {
        val feedId = feedDao.insert(FeedEntity(url = "https://a.com/rss", title = "Feed", category = "World"))
        articleDao.insertAll(
            (1..5).map { i ->
                ArticleEntity(feedId = feedId, guid = "g$i", title = "Article $i", link = "https://a.com/$i", publishedAt = i.toLong())
            },
        )

        val result = articleDao.observeArticlesForCategories(listOf("World"), limit = 2).first()

        assertEquals(2, result.size)
    }

    @Test
    fun insertAll_ignoresDuplicateGuidForSameFeed() = runTest {
        val feedId = feedDao.insert(FeedEntity(url = "https://a.com/rss", title = "Feed", category = "World"))
        articleDao.insertAll(listOf(ArticleEntity(feedId = feedId, guid = "dup", title = "Original", link = "https://a.com/1")))
        articleDao.insertAll(listOf(ArticleEntity(feedId = feedId, guid = "dup", title = "Should be ignored", link = "https://a.com/1")))

        val articles = articleDao.observeArticlesForFeed(feedId).first()

        assertEquals(1, articles.size)
        assertEquals("Original", articles[0].title)
    }

    @Test
    fun deletingFeed_cascadesToItsArticles() = runTest {
        val feed = FeedEntity(url = "https://a.com/rss", title = "Feed", category = "World")
        val feedId = feedDao.insert(feed)
        articleDao.insertAll(listOf(ArticleEntity(feedId = feedId, guid = "g1", title = "A", link = "https://a.com/1")))

        feedDao.delete(feed.copy(id = feedId))

        assertEquals(0, articleDao.observeArticlesForFeed(feedId).first().size)
    }
}
