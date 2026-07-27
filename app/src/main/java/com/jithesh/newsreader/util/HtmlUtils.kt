package com.jithesh.newsreader.util

fun stripHtml(html: String): String =
    html.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()
