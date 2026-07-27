package com.jithesh.newsreader.ui.feedlist

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.jithesh.newsreader.data.db.ArticleDao
import com.jithesh.newsreader.data.db.ArticleEntity
import com.jithesh.newsreader.data.db.ArticleWithFeed
import com.jithesh.newsreader.data.db.FeedDao
import com.jithesh.newsreader.data.db.FeedEntity
import com.jithesh.newsreader.data.network.FeedFetchService
import com.jithesh.newsreader.data.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.ResponseBody
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

/** Minimal look-and-feel checks: does the right thing render for empty vs. populated feed lists. */
class FeedListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class StubFeedDao(initial: List<FeedEntity>) : FeedDao {
        val feeds = MutableStateFlow(initial)
        override fun observeFeeds(): Flow<List<FeedEntity>> = feeds
        override suspend fun getFeed(feedId: Long) = feeds.value.find { it.id == feedId }
        override suspend fun getFeedByUrl(url: String) = feeds.value.find { it.url == url }
        override fun observeCategories(): Flow<List<String>> = MutableStateFlow(feeds.value.map { it.category }.distinct())
        override suspend fun insert(feed: FeedEntity): Long = 1L
        override suspend fun update(feed: FeedEntity) {}
        override suspend fun delete(feed: FeedEntity) {}
    }

    private class StubArticleDao : ArticleDao {
        override fun observeArticlesForFeed(feedId: Long): Flow<List<ArticleEntity>> = MutableStateFlow(emptyList())
        override fun observeArticlesForCategories(categories: List<String>, limit: Int): Flow<List<ArticleWithFeed>> =
            MutableStateFlow(emptyList())
        override suspend fun getArticle(articleId: Long): ArticleEntity? = null
        override suspend fun insertAll(articles: List<ArticleEntity>): List<Long> = emptyList()
        override suspend fun setRead(articleId: Long, isRead: Boolean) {}
        override suspend fun pruneOldest(feedId: Long, keepCount: Int) {}
    }

    private class StubFeedFetchService : FeedFetchService {
        override suspend fun fetchFeed(url: String): Response<ResponseBody> = Response.success(null)
    }

    private fun repositoryWithFeeds(feeds: List<FeedEntity>) =
        FeedRepository(StubFeedDao(feeds), StubArticleDao(), StubFeedFetchService())

    @Test
    fun emptyState_showsAddFeedPrompt() {
        composeRule.setContent {
            FeedListScreen(
                onFeedClick = {},
                viewModel = FeedListViewModel(repositoryWithFeeds(emptyList())),
            )
        }

        composeRule.onNodeWithText("No feeds yet. Tap + to add one.").assertExists()
    }

    @Test
    fun populatedList_showsFeedTitleAndCategory() {
        val feed = FeedEntity(id = 1, url = "https://example.com/rss", title = "Example News", category = "World")
        composeRule.setContent {
            FeedListScreen(
                onFeedClick = {},
                viewModel = FeedListViewModel(repositoryWithFeeds(listOf(feed))),
            )
        }

        composeRule.onNodeWithText("Example News").assertExists()
        composeRule.onNodeWithText("World").assertExists()
    }
}
