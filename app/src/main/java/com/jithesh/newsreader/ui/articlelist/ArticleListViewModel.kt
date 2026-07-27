package com.jithesh.newsreader.ui.articlelist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jithesh.newsreader.data.db.ArticleEntity
import com.jithesh.newsreader.data.repository.FeedRepository
import com.jithesh.newsreader.data.suggested.DefaultFeeds
import com.jithesh.newsreader.data.translation.NorwegianTranslator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArticleListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val feedRepository: FeedRepository,
    private val translator: NorwegianTranslator,
) : ViewModel() {

    private val feedId: Long = checkNotNull(savedStateHandle["feedId"])

    val articles: StateFlow<List<ArticleEntity>> = feedRepository.observeArticlesForFeed(feedId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _feedTitle = MutableStateFlow("")
    val feedTitle: StateFlow<String> = _feedTitle.asStateFlow()

    private val _isNorwegian = MutableStateFlow(false)
    val isNorwegian: StateFlow<Boolean> = _isNorwegian.asStateFlow()

    private val _translatedTitles = MutableStateFlow<Map<Long, String>>(emptyMap())
    val translatedTitles: StateFlow<Map<Long, String>> = _translatedTitles.asStateFlow()

    private val _translatingIds = MutableStateFlow<Set<Long>>(emptySet())
    val translatingIds: StateFlow<Set<Long>> = _translatingIds.asStateFlow()

    init {
        viewModelScope.launch {
            val feed = feedRepository.getFeed(feedId)
            _feedTitle.value = feed?.title.orEmpty()
            _isNorwegian.value = feed?.category == DefaultFeeds.CATEGORY_NORWAY
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            feedRepository.refreshFeed(feedId, force = true)
            _isRefreshing.value = false
        }
    }

    fun markRead(articleId: Long, isRead: Boolean) {
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
