package com.jithesh.newsreader.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jithesh.newsreader.data.suggested.DefaultFeeds
import com.jithesh.newsreader.ui.common.ArticleRow
import com.jithesh.newsreader.ui.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onArticleClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val translatedTitles by viewModel.translatedTitles.collectAsStateWithLifecycle()
    val translatingIds by viewModel.translatingIds.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = modifier.fillMaxSize(),
    ) {
        if (!uiState.isLoading && uiState.isEmpty) {
            EmptyState("No articles yet. Add some feeds and pick topics in Settings to see a preview here.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                uiState.sections.forEach { section ->
                    item(key = "header_${section.category}") {
                        CategoryHeader(section.category)
                    }
                    val isNorwegianSection = section.category == DefaultFeeds.CATEGORY_NORWAY
                    items(section.articles, key = { it.id }) { article ->
                        ArticleRow(
                            title = translatedTitles[article.id] ?: article.title,
                            source = article.feedTitle,
                            publishedAt = article.publishedAt,
                            thumbnailUrl = article.thumbnailUrl,
                            isRead = article.isRead,
                            showImage = uiState.showImages,
                            unreadIndicator = Icons.Filled.Circle,
                            showTranslateAction = isNorwegianSection && translatedTitles[article.id] == null,
                            isTranslating = article.id in translatingIds,
                            onTranslateClick = { viewModel.translateTitle(article.id, article.title) },
                            onClick = {
                                viewModel.markRead(article.id)
                                onArticleClick(article.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(category: String) {
    Text(
        text = category,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
