package com.jithesh.newsreader.ui.feedlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jithesh.newsreader.data.db.FeedEntity
import com.jithesh.newsreader.data.repository.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedListViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
) : ViewModel() {

    val feeds: StateFlow<List<FeedEntity>> = feedRepository.observeFeeds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    suspend fun addFeed(url: String, category: String): Result<Long> =
        feedRepository.addFeed(url, category)

    fun deleteFeed(feed: FeedEntity) {
        viewModelScope.launch { feedRepository.deleteFeed(feed) }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            feedRepository.refreshAllFeeds(force = true)
            _isRefreshing.value = false
        }
    }
}
