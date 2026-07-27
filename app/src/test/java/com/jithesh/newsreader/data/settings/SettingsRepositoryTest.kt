package com.jithesh.newsreader.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    private lateinit var tempFile: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        tempFile = File.createTempFile("test_settings", ".preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = TestScope(UnconfinedTestDispatcher()),
            produceFile = { tempFile },
        )
        repository = SettingsRepository(dataStore)
    }

    @After
    fun tearDown() {
        tempFile.delete()
    }

    @Test
    fun `defaults are returned when nothing has been written`() = runTest {
        val settings = repository.settingsFlow.first()

        assertEquals(SettingsDefaults.DEFAULT_ARTICLE_COUNT, settings.articleCount)
        assertNull(settings.topics)
        assertEquals(SettingsDefaults.DEFAULT_SHOW_IMAGES, settings.showImages)
        assertEquals(SettingsDefaults.DEFAULT_THEME_MODE, settings.themeMode)
    }

    @Test
    fun `setArticleCount round-trips`() = runTest {
        repository.setArticleCount(35)
        assertEquals(35, repository.settingsFlow.first().articleCount)
    }

    @Test
    fun `setTopics round-trips and is no longer null`() = runTest {
        repository.setTopics(setOf("World", "AI"))
        assertEquals(setOf("World", "AI"), repository.settingsFlow.first().topics)
    }

    @Test
    fun `setShowImages round-trips`() = runTest {
        repository.setShowImages(false)
        assertEquals(false, repository.settingsFlow.first().showImages)
    }

    @Test
    fun `setThemeMode round-trips`() = runTest {
        repository.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repository.settingsFlow.first().themeMode)
    }
}
