package com.jithesh.newsreader.ui.articlelist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jithesh.newsreader.ui.common.ArticleRow
import com.jithesh.newsreader.ui.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(
    onArticleClick: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArticleListViewModel = hiltViewModel(),
) {
    val articles by viewModel.articles.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val feedTitle by viewModel.feedTitle.collectAsStateWithLifecycle()
    val isNorwegian by viewModel.isNorwegian.collectAsStateWithLifecycle()
    val translatedTitles by viewModel.translatedTitles.collectAsStateWithLifecycle()
    val translatingIds by viewModel.translatingIds.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(feedTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
        ) {
            if (articles.isEmpty()) {
                EmptyState("No articles yet. Pull to refresh.", modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(articles, key = { it.id }) { article ->
                        ArticleRow(
                            title = translatedTitles[article.id] ?: article.title,
                            source = feedTitle,
                            publishedAt = article.publishedAt,
                            thumbnailUrl = article.thumbnailUrl,
                            isRead = article.isRead,
                            showImage = true,
                            unreadIndicator = Icons.Filled.Circle,
                            showTranslateAction = isNorwegian && translatedTitles[article.id] == null,
                            isTranslating = article.id in translatingIds,
                            onTranslateClick = { viewModel.translateTitle(article.id, article.title) },
                            onClick = {
                                viewModel.markRead(article.id, true)
                                onArticleClick(article.id)
                            },
                        )
                    }
                }
            }
        }
    }
}
