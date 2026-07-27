package com.jithesh.newsreader.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jithesh.newsreader.data.settings.ThemeMode
import kotlin.math.roundToInt

private fun toggleTopic(current: Set<String>, topic: String): Set<String> =
    if (topic in current) current - topic else current + topic

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    var sliderValue by remember(settings.articleCount) { mutableStateOf(settings.articleCount.toFloat()) }
    val selectedTopics = settings.topics ?: uiState.availableCategories.toSet()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Home feed", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        Text("Number of articles: ${sliderValue.roundToInt()}", style = MaterialTheme.typography.titleMedium)
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { viewModel.setArticleCount(sliderValue.roundToInt()) },
            valueRange = 5f..50f,
            steps = 8,
        )

        Spacer(Modifier.height(24.dp))
        Text("Topics shown on Home", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (uiState.availableCategories.isEmpty()) {
            Text(
                text = "Add some feeds first to choose topics.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            uiState.availableCategories.forEach { category ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setTopics(toggleTopic(selectedTopics, category)) },
                ) {
                    Checkbox(
                        checked = category in selectedTopics,
                        onCheckedChange = { viewModel.setTopics(toggleTopic(selectedTopics, category)) },
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(category)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Show images in article list",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = settings.showImages, onCheckedChange = { viewModel.setShowImages(it) })
        }

        Spacer(Modifier.height(24.dp))
        Text("Theme", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        ThemeMode.entries.forEach { mode ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setThemeMode(mode) },
            ) {
                RadioButton(selected = settings.themeMode == mode, onClick = { viewModel.setThemeMode(mode) })
                Spacer(Modifier.width(8.dp))
                Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
            }
        }
    }
}
