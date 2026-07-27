package com.jithesh.newsreader.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.jithesh.newsreader.data.db.ArticleDao
import com.jithesh.newsreader.data.db.ArticleEntity
import com.jithesh.newsreader.data.db.ArticleWithFeed
import com.jithesh.newsreader.data.db.FeedDao
import com.jithesh.newsreader.data.db.FeedEntity
import com.jithesh.newsreader.data.network.FeedFetchService
import com.jithesh.newsreader.data.repository.FeedRepository
import com.jithesh.newsreader.data.settings.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import okhttp3.ResponseBody
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.File

/** Minimal look-and-feel check: the key controls (article count, theme options) render. */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class NoopFeedDao : FeedDao {
        override fun observeFeeds(): Flow<List<FeedEntity>> = MutableStateFlow(emptyList())
        override suspend fun getFeed(feedId: Long): FeedEntity? = null
        override suspend fun getFeedByUrl(url: String): FeedEntity? = null
        override fun observeCategories(): Flow<List<String>> = MutableStateFlow(listOf("World", "AI"))
        override suspend fun insert(feed: FeedEntity): Long = 1L
        override suspend fun update(feed: FeedEntity) {}
        override suspend fun delete(feed: FeedEntity) {}
    }

    private class NoopArticleDao : ArticleDao {
        override fun observeArticlesForFeed(feedId: Long): Flow<List<ArticleEntity>> = MutableStateFlow(emptyList())
        override fun observeArticlesForCategories(categories: List<String>, limit: Int): Flow<List<ArticleWithFeed>> =
            MutableStateFlow(emptyList())
        override suspend fun getArticle(articleId: Long): ArticleEntity? = null
        override suspend fun insertAll(articles: List<ArticleEntity>): List<Long> = emptyList()
        override suspend fun setRead(articleId: Long, isRead: Boolean) {}
        override suspend fun pruneOldest(feedId: Long, keepCount: Int) {}
    }

    private class NoopFeedFetchService : FeedFetchService {
        override suspend fun fetchFeed(url: String): Response<ResponseBody> = Response.success(null)
    }

    private fun testDataStore(): DataStore<Preferences> {
        val file = File.createTempFile("settings_ui_test", ".preferences_pb")
        file.deleteOnExit()
        return PreferenceDataStoreFactory.create(
            scope = TestScope(UnconfinedTestDispatcher()),
            produceFile = { file },
        )
    }

    @Test
    fun keyControls_areVisible() {
        val viewModel = SettingsViewModel(
            settingsRepository = SettingsRepository(testDataStore()),
            feedRepository = FeedRepository(NoopFeedDao(), NoopArticleDao(), NoopFeedFetchService()),
        )

        composeRule.setContent {
            SettingsScreen(viewModel = viewModel)
        }

        composeRule.onNodeWithText("Home feed").assertExists()
        composeRule.onNodeWithText("Show images in article list").assertExists()
        composeRule.onNodeWithText("Theme").assertExists()
        composeRule.onNodeWithText("System").assertExists()
        composeRule.onNodeWithText("Light").assertExists()
        composeRule.onNodeWithText("Dark").assertExists()
        composeRule.onNodeWithText("World").assertExists()
        composeRule.onNodeWithText("AI").assertExists()
    }
}
