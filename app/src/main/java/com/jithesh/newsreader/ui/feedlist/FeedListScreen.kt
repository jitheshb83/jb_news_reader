package com.jithesh.newsreader.ui.feedlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jithesh.newsreader.data.db.FeedEntity
import com.jithesh.newsreader.ui.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FeedListScreen(
    onFeedClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedListViewModel = hiltViewModel(),
) {
    val feeds by viewModel.feeds.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var feedPendingDelete by remember { mutableStateOf<FeedEntity?>(null) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add feed")
            }
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
        ) {
            if (feeds.isEmpty()) {
                EmptyState("No feeds yet. Tap + to add one.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(feeds, key = { it.id }) { feed ->
                        FeedRow(
                            feed = feed,
                            onClick = { onFeedClick(feed.id) },
                            onLongPress = { feedPendingDelete = feed },
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFeedDialog(
            onDismiss = { showAddDialog = false },
            onAdded = { showAddDialog = false },
            viewModel = viewModel,
        )
    }

    feedPendingDelete?.let { feed ->
        AlertDialog(
            onDismissRequest = { feedPendingDelete = null },
            title = { Text("Delete feed?") },
            text = { Text("Remove \"${feed.title}\" and its saved articles?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFeed(feed)
                    feedPendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { feedPendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeedRow(feed: FeedEntity, onClick: () -> Unit, onLongPress: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(16.dp),
    ) {
        Text(
            text = feed.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Row {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = feed.category,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            if (feed.lastFetchError != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Failed to refresh",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
    HorizontalDivider()
}
