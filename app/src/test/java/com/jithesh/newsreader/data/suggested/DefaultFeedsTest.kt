package com.jithesh.newsreader.data.suggested

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultFeedsTest {

    @Test
    fun `every suggested feed has a well-formed url and non-blank name and known category`() {
        DefaultFeeds.FEEDS.forEach { feed ->
            assertTrue("URL should be http(s): ${feed.url}", feed.url.startsWith("http://") || feed.url.startsWith("https://"))
            assertFalse("Name should not be blank for ${feed.url}", feed.name.isBlank())
            assertTrue(
                "Category '${feed.category}' should be one of the known categories",
                feed.category in DefaultFeeds.ALL_CATEGORIES,
            )
        }
    }

    @Test
    fun `every category except General has at least one suggested feed`() {
        val categoriesWithFeeds = DefaultFeeds.FEEDS.map { it.category }.toSet()
        DefaultFeeds.ALL_CATEGORIES.filter { it != DefaultFeeds.CATEGORY_GENERAL }.forEach { category ->
            assertTrue("Expected at least one feed for category $category", category in categoriesWithFeeds)
        }
    }

    @Test
    fun `no duplicate feed urls`() {
        val urls = DefaultFeeds.FEEDS.map { it.url }
        assertTrue("Found duplicate URLs in DefaultFeeds", urls.size == urls.toSet().size)
    }
}
