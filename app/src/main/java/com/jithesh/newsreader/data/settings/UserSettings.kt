package com.jithesh.newsreader.data.settings

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * [topics] is null when the user hasn't customized Home's topic selection yet — callers should
 * fall back to all categories currently present among the user's feeds (or the suggested
 * category list if there are no feeds yet). See SettingsRepository for the stored defaults.
 */
data class UserSettings(
    val articleCount: Int,
    val topics: Set<String>?,
    val showImages: Boolean,
    val themeMode: ThemeMode,
)

object SettingsDefaults {
    const val DEFAULT_ARTICLE_COUNT = 20
    const val DEFAULT_SHOW_IMAGES = true
    val DEFAULT_THEME_MODE = ThemeMode.SYSTEM
}
