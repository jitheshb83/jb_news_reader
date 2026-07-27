package com.jithesh.newsreader.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.jithesh.newsreader.ui.articledetail.ArticleDetailScreen
import com.jithesh.newsreader.ui.articlelist.ArticleListScreen
import com.jithesh.newsreader.ui.feedlist.FeedListScreen
import com.jithesh.newsreader.ui.home.HomeScreen
import com.jithesh.newsreader.ui.settings.SettingsScreen

@Composable
fun NewsReaderNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Home.route,
        modifier = modifier,
    ) {
        composable(Destination.Home.route) {
            HomeScreen(onArticleClick = { articleId ->
                navController.navigate(Destination.ArticleDetail.createRoute(articleId))
            })
        }
        composable(Destination.Feeds.route) {
            FeedListScreen(onFeedClick = { feedId ->
                navController.navigate(Destination.ArticleList.createRoute(feedId))
            })
        }
        composable(Destination.Settings.route) {
            SettingsScreen()
        }
        composable(
            route = Destination.ArticleList.route,
            arguments = listOf(navArgument("feedId") { type = NavType.LongType }),
        ) {
            ArticleListScreen(
                onArticleClick = { articleId ->
                    navController.navigate(Destination.ArticleDetail.createRoute(articleId))
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Destination.ArticleDetail.route,
            arguments = listOf(navArgument("articleId") { type = NavType.LongType }),
        ) {
            ArticleDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
