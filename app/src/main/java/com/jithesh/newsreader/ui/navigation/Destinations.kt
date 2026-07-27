package com.jithesh.newsreader.ui.navigation

sealed class Destination(val route: String) {
    object Home : Destination("home")
    object Feeds : Destination("feeds")
    object Settings : Destination("settings")

    object ArticleList : Destination("articleList/{feedId}") {
        fun createRoute(feedId: Long) = "articleList/$feedId"
    }

    object ArticleDetail : Destination("articleDetail/{articleId}") {
        fun createRoute(articleId: Long) = "articleDetail/$articleId"
    }
}
