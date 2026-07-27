package com.jithesh.newsreader.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jithesh.newsreader.data.db.ArticleWithFeed
import com.jithesh.newsreader.data.repository.FeedRepository
import com.jithesh.newsreader.data.settings.SettingsRepository
import com.jithesh.newsreader.data.suggested.DefaultFeeds
import com.jithesh.newsreader.data.translation.NorwegianTranslator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategorySection(
    val category: String,
    val articles: List<ArticleWithFeed>,
)

data class HomeUiState(
    val sections: List<CategorySection> = emptyList(),
    val showImages: Boolean = true,
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean get() = sections.all { it.articles.isEmpty() }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val settingsRepository: SettingsRepository,
    private val translator: NorwegianTranslator,
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _translatedTitles = MutableStateFlow<Map<Long, String>>(emptyMap())
    val translatedTitles: StateFlow<Map<Long, String>> = _translatedTitles.asStateFlow()

    private val _translatingIds = MutableStateFlow<Set<Long>>(emptySet())
    val translatingIds: StateFlow<Set<Long>> = _translatingIds.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        settingsRepository.settingsFlow,
        feedRepository.observeCategories(),
    ) { settings, knownCategories -> settings to knownCategories }
        .flatMapLatest { (settings, knownCategories) ->
            val effectiveTopics: List<String> = settings.topics?.takeIf { it.isNotEmpty() }?.toList()
                ?: knownCategories.takeIf { it.isNotEmpty() }
                ?: DefaultFeeds.ALL_CATEGORIES
            feedRepository.observeArticlesForCategories(effectiveTopics, settings.articleCount)
                .map { articles ->
                    // groupBy preserves first-seen order, and articles arrive newest-first,
                    // so sections naturally land in "most recently active category first" order.
                    val sections = articles.groupBy { it.feedCategory }
                        .map { (category, articlesInCategory) -> CategorySection(category, articlesInCategory) }
                    HomeUiState(sections = sections, showImages = settings.showImages, isLoading = false)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        // Quiet app-open refresh: respects the throttle, so this is a no-op network-wise
        // if feeds were already fetched recently.
        viewModelScope.launch { feedRepository.refreshAllFeeds(force = false) }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            feedRepository.refreshAllFeeds(force = true)
            _isRefreshing.value = false
        }
    }

    fun markRead(articleId: Long, isRead: Boolean = true) {
        viewModelScope.launch { feedRepository.setArticleRead(articleId, isRead) }
    }

    /** Translates and caches a single article's title; a no-op if already translated or in flight. */
    fun translateTitle(articleId: Long, title: String) {
        if (_translatedTitles.value.containsKey(articleId) || articleId in _translatingIds.value) return
        viewModelScope.launch {
            _translatingIds.value = _translatingIds.value + articleId
            translator.translate(title).onSuccess { translated ->
                _translatedTitles.value = _translatedTitles.value + (articleId to translated)
            }
            _translatingIds.value = _translatingIds.value - articleId
        }
    }
}
