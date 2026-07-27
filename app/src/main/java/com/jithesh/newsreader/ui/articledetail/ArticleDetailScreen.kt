package com.jithesh.newsreader.ui.articledetail

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.jithesh.newsreader.ui.common.formatPublishedAt
import com.jithesh.newsreader.util.stripHtml

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArticleDetailViewModel = hiltViewModel(),
) {
    val article by viewModel.article.collectAsStateWithLifecycle()
    val translation by viewModel.translation.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Article") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    article?.let { current ->
                        if (translation.isNorwegian) {
                            IconButton(onClick = viewModel::translate, enabled = !translation.isTranslating) {
                                if (translation.isTranslating) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Filled.Translate, contentDescription = "Translate to English")
                                }
                            }
                        }
                        IconButton(onClick = { viewModel.toggleRead() }) {
                            Icon(
                                imageVector = if (current.isRead) Icons.Filled.MarkEmailUnread else Icons.Filled.Check,
                                contentDescription = if (current.isRead) "Mark as unread" else "Mark as read",
                            )
                        }
                        IconButton(onClick = {
                            val uri = current.link.toUri()
                            if (uri.scheme == "http" || uri.scheme == "https") {
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }
                        }) {
                            Icon(Icons.Filled.OpenInBrowser, contentDescription = "Open in browser")
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        val current = article
        if (current == null) {
            return@Scaffold
        }
        val showingTranslation = translation.showingTranslation && translation.translatedTitle != null
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                text = if (showingTranslation) translation.translatedTitle!! else current.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            val meta = listOfNotNull(
                current.author,
                formatPublishedAt(current.publishedAt),
            ).joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (translation.translatedTitle != null || translation.error != null) {
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    when {
                        translation.error != null -> Text(
                            text = translation.error!!,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        showingTranslation -> {
                            Text(
                                text = "Translated from Norwegian",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(onClick = viewModel::showOriginal) { Text("Show original") }
                        }
                        else -> TextButton(onClick = viewModel::translate) { Text("Show translation") }
                    }
                }
            }
            if (!current.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = current.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }
            val body = current.contentHtml ?: current.description
            val displayBody = if (showingTranslation) translation.translatedBody else body?.let(::stripHtml)
            if (!displayBody.isNullOrBlank()) {
                Text(
                    text = displayBody,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}
