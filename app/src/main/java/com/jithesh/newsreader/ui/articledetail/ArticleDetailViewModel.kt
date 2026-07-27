package com.jithesh.newsreader.ui.articledetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jithesh.newsreader.data.db.ArticleEntity
import com.jithesh.newsreader.data.repository.FeedRepository
import com.jithesh.newsreader.data.suggested.DefaultFeeds
import com.jithesh.newsreader.data.translation.NorwegianTranslator
import com.jithesh.newsreader.util.stripHtml
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TranslationUiState(
    val isNorwegian: Boolean = false,
    val isTranslating: Boolean = false,
    val translatedTitle: String? = null,
    val translatedBody: String? = null,
    val error: String? = null,
    val showingTranslation: Boolean = false,
)

@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val feedRepository: FeedRepository,
    private val translator: NorwegianTranslator,
) : ViewModel() {

    private val articleId: Long = checkNotNull(savedStateHandle["articleId"])

    private val _article = MutableStateFlow<ArticleEntity?>(null)
    val article: StateFlow<ArticleEntity?> = _article.asStateFlow()

    private val _translation = MutableStateFlow(TranslationUiState())
    val translation: StateFlow<TranslationUiState> = _translation.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = feedRepository.getArticle(articleId) ?: return@launch
            val current = if (!loaded.isRead) {
                feedRepository.setArticleRead(articleId, true)
                loaded.copy(isRead = true)
            } else {
                loaded
            }
            _article.value = current

            val feed = feedRepository.getFeed(current.feedId)
            if (feed?.category == DefaultFeeds.CATEGORY_NORWAY) {
                _translation.value = _translation.value.copy(isNorwegian = true)
            }
        }
    }

    fun toggleRead() {
        val current = _article.value ?: return
        val newState = !current.isRead
        viewModelScope.launch {
            feedRepository.setArticleRead(articleId, newState)
            _article.value = current.copy(isRead = newState)
        }
    }

    /** Translates on first call and caches the result; subsequent calls just re-show it. */
    fun translate() {
        val article = _article.value ?: return
        val state = _translation.value
        if (state.translatedTitle != null) {
            _translation.value = state.copy(showingTranslation = true)
            return
        }
        viewModelScope.launch {
            _translation.value = _translation.value.copy(isTranslating = true, error = null)
            val bodyPlain = stripHtml(article.contentHtml ?: article.description.orEmpty())
            val titleResult = translator.translate(article.title)
            val bodyResult = translator.translate(bodyPlain)
            _translation.value = if (titleResult.isSuccess && bodyResult.isSuccess) {
                _translation.value.copy(
                    isTranslating = false,
                    translatedTitle = titleResult.getOrNull(),
                    translatedBody = bodyResult.getOrNull(),
                    showingTranslation = true,
                )
            } else {
                _translation.value.copy(
                    isTranslating = false,
                    error = titleResult.exceptionOrNull()?.message
                        ?: bodyResult.exceptionOrNull()?.message
                        ?: "Couldn't translate this article",
                )
            }
        }
    }

    fun showOriginal() {
        _translation.value = _translation.value.copy(showingTranslation = false)
    }
}
