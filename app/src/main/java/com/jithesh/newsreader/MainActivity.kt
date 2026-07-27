package com.jithesh.newsreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.jithesh.newsreader.ui.navigation.Destination
import com.jithesh.newsreader.ui.navigation.NewsReaderNavHost
import com.jithesh.newsreader.ui.settings.SettingsViewModel
import com.jithesh.newsreader.ui.theme.NewsReaderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

            NewsReaderTheme(themeMode = uiState.settings.themeMode) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    NewsReaderScaffold()
                }
            }
        }
    }
}

private data class BottomNavItem(val destination: Destination, val label: String, val icon: ImageVector)

private val bottomNavItems = listOf(
    BottomNavItem(Destination.Home, "Home", Icons.Filled.Home),
    BottomNavItem(Destination.Feeds, "Feeds", Icons.Filled.RssFeed),
    BottomNavItem(Destination.Settings, "Settings", Icons.Filled.Settings),
)

@Composable
private fun NewsReaderScaffold() {
    val navController: NavHostController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.destination.route,
                        onClick = {
                            navController.navigate(item.destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { paddingValues ->
        NewsReaderNavHost(navController = navController, modifier = Modifier.padding(paddingValues))
    }
}
