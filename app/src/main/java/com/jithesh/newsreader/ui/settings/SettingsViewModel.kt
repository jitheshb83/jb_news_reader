package com.jithesh.newsreader.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jithesh.newsreader.data.repository.FeedRepository
import com.jithesh.newsreader.data.settings.SettingsDefaults
import com.jithesh.newsreader.data.settings.SettingsRepository
import com.jithesh.newsreader.data.settings.ThemeMode
import com.jithesh.newsreader.data.settings.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: UserSettings = UserSettings(
        articleCount = SettingsDefaults.DEFAULT_ARTICLE_COUNT,
        topics = null,
        showImages = SettingsDefaults.DEFAULT_SHOW_IMAGES,
        themeMode = SettingsDefaults.DEFAULT_THEME_MODE,
    ),
    val availableCategories: List<String> = emptyList(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val feedRepository: FeedRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settingsFlow,
        feedRepository.observeCategories(),
    ) { settings, categories -> SettingsUiState(settings, categories) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setArticleCount(count: Int) {
        viewModelScope.launch { settingsRepository.setArticleCount(count) }
    }

    fun setTopics(topics: Set<String>) {
        viewModelScope.launch { settingsRepository.setTopics(topics) }
    }

    fun setShowImages(showImages: Boolean) {
        viewModelScope.launch { settingsRepository.setShowImages(showImages) }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(themeMode) }
    }
}
