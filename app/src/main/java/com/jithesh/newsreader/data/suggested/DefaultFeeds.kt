package com.jithesh.newsreader.data.suggested

data class SuggestedFeed(
    val category: String,
    val name: String,
    val url: String,
)

/**
 * Curated starter feeds shown as quick-add chips in AddFeedDialog, grouped by category.
 * URLs below are fetch-verified (curl, 200 + valid RSS/Atom body) as of 2026-07-27. Feed
 * URLs still drift over time — if one 404s later, swap in an alternate source for that
 * outlet/category.
 */
object DefaultFeeds {

    const val CATEGORY_NORWAY = "Norway"
    const val CATEGORY_WORLD = "World"
    const val CATEGORY_INDIA = "India"
    const val CATEGORY_AI = "AI"
    const val CATEGORY_SOFTWARE_DEVELOPMENT = "Software Development"
    const val CATEGORY_CLOUD = "Cloud"
    const val CATEGORY_TECHNOLOGY_NEWS = "Technology News"
    const val CATEGORY_CYBER_SECURITY = "Cyber Security"
    const val CATEGORY_INNOVATION = "Innovation"
    const val CATEGORY_TRENDING = "Trending"
    const val CATEGORY_GENERAL = "General"

    val ALL_CATEGORIES = listOf(
        CATEGORY_NORWAY,
        CATEGORY_WORLD,
        CATEGORY_INDIA,
        CATEGORY_AI,
        CATEGORY_SOFTWARE_DEVELOPMENT,
        CATEGORY_CLOUD,
        CATEGORY_TECHNOLOGY_NEWS,
        CATEGORY_CYBER_SECURITY,
        CATEGORY_INNOVATION,
        CATEGORY_TRENDING,
        CATEGORY_GENERAL,
    )

    val FEEDS = listOf(
        SuggestedFeed(CATEGORY_NORWAY, "NRK", "https://www.nrk.no/toppsaker.rss"),
        SuggestedFeed(CATEGORY_NORWAY, "VG", "https://www.vg.no/rss/feed/?format=rss"),
        SuggestedFeed(CATEGORY_NORWAY, "NRK Østlandssendingen (Oslo region)", "https://www.nrk.no/ostlandssendingen/toppsaker.rss"),
        SuggestedFeed(CATEGORY_NORWAY, "Moss Avis (Moss local)", "https://www.moss-avis.no/service/rss"),

        SuggestedFeed(CATEGORY_WORLD, "BBC News World", "https://feeds.bbci.co.uk/news/world/rss.xml"),
        SuggestedFeed(CATEGORY_WORLD, "Al Jazeera", "https://www.aljazeera.com/xml/rss/all.xml"),

        SuggestedFeed(CATEGORY_INDIA, "The Hindu National", "https://www.thehindu.com/news/national/feeder/default.rss"),
        SuggestedFeed(CATEGORY_INDIA, "Times of India Top Stories", "https://timesofindia.indiatimes.com/rssfeedstopstories.cms"),
        SuggestedFeed(CATEGORY_INDIA, "Daijiworld (Mangalore, English)", "https://www.daijiworld.com/rssfeed/rssfeed.xml"),
        SuggestedFeed(CATEGORY_INDIA, "Mangalorean.com (Mangalore, English)", "https://www.mangalorean.com/feed/"),

        SuggestedFeed(CATEGORY_AI, "MIT Technology Review – AI", "https://www.technologyreview.com/topic/artificial-intelligence/feed"),
        SuggestedFeed(CATEGORY_AI, "Google AI Blog", "https://blog.google/technology/ai/rss/"),

        SuggestedFeed(CATEGORY_SOFTWARE_DEVELOPMENT, "InfoQ", "https://feed.infoq.com/"),
        SuggestedFeed(CATEGORY_SOFTWARE_DEVELOPMENT, "Martin Fowler's Blog", "https://martinfowler.com/feed.atom"),

        SuggestedFeed(CATEGORY_CLOUD, "AWS News Blog", "https://aws.amazon.com/blogs/aws/feed/"),
        SuggestedFeed(CATEGORY_CLOUD, "Google Cloud Blog", "https://cloud.google.com/blog/rss/"),

        SuggestedFeed(CATEGORY_TECHNOLOGY_NEWS, "The Verge", "https://www.theverge.com/rss/index.xml"),
        SuggestedFeed(CATEGORY_TECHNOLOGY_NEWS, "Ars Technica", "https://feeds.arstechnica.com/arstechnica/index"),
        SuggestedFeed(CATEGORY_TECHNOLOGY_NEWS, "TechCrunch", "https://techcrunch.com/feed/"),

        SuggestedFeed(CATEGORY_CYBER_SECURITY, "Krebs on Security", "https://krebsonsecurity.com/feed/"),
        SuggestedFeed(CATEGORY_CYBER_SECURITY, "The Hacker News", "https://feeds.feedburner.com/TheHackersNews"),

        SuggestedFeed(CATEGORY_INNOVATION, "MIT Technology Review", "https://www.technologyreview.com/feed/"),
        SuggestedFeed(CATEGORY_INNOVATION, "Fast Company", "https://www.fastcompany.com/latest/rss"),

        SuggestedFeed(CATEGORY_TRENDING, "Hacker News Front Page", "https://news.ycombinator.com/rss"),
        SuggestedFeed(CATEGORY_TRENDING, "Google News Top Stories", "https://news.google.com/rss"),
    )
}
