package com.jithesh.newsreader.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private object Keys {
    val ARTICLE_COUNT = intPreferencesKey("home_article_count")
    val TOPICS = stringSetPreferencesKey("home_topics")
    val SHOW_IMAGES = booleanPreferencesKey("show_images")
    val THEME_MODE = stringPreferencesKey("theme_mode")
}

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val settingsFlow: Flow<UserSettings> = dataStore.data.map { prefs -> prefs.toUserSettings() }

    suspend fun setArticleCount(count: Int) {
        dataStore.edit { it[Keys.ARTICLE_COUNT] = count }
    }

    suspend fun setTopics(topics: Set<String>) {
        dataStore.edit { it[Keys.TOPICS] = topics }
    }

    suspend fun setShowImages(showImages: Boolean) {
        dataStore.edit { it[Keys.SHOW_IMAGES] = showImages }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = themeMode.name }
    }

    private fun Preferences.toUserSettings(): UserSettings = UserSettings(
        articleCount = this[Keys.ARTICLE_COUNT] ?: SettingsDefaults.DEFAULT_ARTICLE_COUNT,
        topics = this[Keys.TOPICS],
        showImages = this[Keys.SHOW_IMAGES] ?: SettingsDefaults.DEFAULT_SHOW_IMAGES,
        themeMode = this[Keys.THEME_MODE]?.let { stored ->
            runCatching { ThemeMode.valueOf(stored) }.getOrDefault(SettingsDefaults.DEFAULT_THEME_MODE)
        } ?: SettingsDefaults.DEFAULT_THEME_MODE,
    )
}
