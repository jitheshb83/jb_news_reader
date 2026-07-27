package com.jithesh.newsreader.ui.feedlist

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jithesh.newsreader.data.suggested.DefaultFeeds
import kotlinx.coroutines.launch

@Composable
fun AddFeedDialog(
    onDismiss: () -> Unit,
    onAdded: () -> Unit,
    viewModel: FeedListViewModel,
) {
    var url by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(DefaultFeeds.CATEGORY_GENERAL) }
    var isSubmitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit(submitUrl: String, submitCategory: String) {
        if (isSubmitting || submitUrl.isBlank()) return
        isSubmitting = true
        error = null
        scope.launch {
            viewModel.addFeed(submitUrl, submitCategory)
                .onSuccess { onAdded() }
                .onFailure { error = it.message ?: "Couldn't add feed" }
            isSubmitting = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add feed") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Suggested feeds", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                DefaultFeeds.ALL_CATEGORIES.forEach { cat ->
                    val feedsInCategory = DefaultFeeds.FEEDS.filter { it.category == cat }
                    if (feedsInCategory.isNotEmpty()) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                        ) {
                            feedsInCategory.forEach { suggestion ->
                                AssistChip(
                                    onClick = { submit(suggestion.url, suggestion.category) },
                                    label = { Text(suggestion.name) },
                                    modifier = Modifier.padding(end = 4.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Text("Or add a custom URL", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Feed URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { categoryExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(category, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose category")
                    }
                    DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        DefaultFeeds.ALL_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                },
                            )
                        }
                    }
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { submit(url, category) }, enabled = url.isNotBlank() && !isSubmitting) {
                Text(if (isSubmitting) "Adding…" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
