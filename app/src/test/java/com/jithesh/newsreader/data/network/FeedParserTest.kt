package com.jithesh.newsreader.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

private const val RSS_SAMPLE = """<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" xmlns:content="http://purl.org/rss/1.0/modules/content/" xmlns:dc="http://purl.org/dc/elements/1.1/">
  <channel>
    <title>Test Feed</title>
    <link>https://example.com</link>
    <item>
      <title>First Article</title>
      <link>https://example.com/first</link>
      <guid>guid-1</guid>
      <pubDate>Mon, 27 Jul 2026 10:00:00 GMT</pubDate>
      <description>First description</description>
      <content:encoded><![CDATA[<p>Full content</p>]]></content:encoded>
      <dc:creator>Jane Doe</dc:creator>
      <enclosure url="https://example.com/first.jpg" type="image/jpeg" />
    </item>
    <item>
      <title>Second Article</title>
      <link>https://example.com/second</link>
      <description>No guid, no date</description>
    </item>
  </channel>
</rss>
"""

private const val ATOM_SAMPLE = """<?xml version="1.0" encoding="UTF-8"?>
<feed xmlns="http://www.w3.org/2005/Atom">
  <title>Atom Test Feed</title>
  <link href="https://example.org" rel="alternate" />
  <entry>
    <title>Atom Entry One</title>
    <link href="https://example.org/one" rel="alternate" />
    <id>urn:uuid:1234</id>
    <updated>2026-07-27T10:00:00Z</updated>
    <summary>Summary text</summary>
    <author><name>John Smith</name></author>
  </entry>
</feed>
"""

class FeedParserTest {

    private fun parse(xml: String): ParsedFeed =
        FeedParser.parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))

    @Test
    fun `parses RSS 2_0 feed title and item fields`() {
        val feed = parse(RSS_SAMPLE)

        assertEquals("Test Feed", feed.title)
        assertEquals("https://example.com", feed.siteLink)
        assertEquals(2, feed.articles.size)

        val first = feed.articles[0]
        assertEquals("guid-1", first.guid)
        assertEquals("First Article", first.title)
        assertEquals("https://example.com/first", first.link)
        assertEquals("First description", first.description)
        assertEquals("<p>Full content</p>", first.contentHtml)
        assertEquals("Jane Doe", first.author)
        assertEquals("Mon, 27 Jul 2026 10:00:00 GMT", first.publishedAtRaw)
        assertEquals("https://example.com/first.jpg", first.thumbnailUrl)
    }

    @Test
    fun `falls back to link as guid when guid is missing`() {
        val feed = parse(RSS_SAMPLE)
        val second = feed.articles[1]

        assertEquals("https://example.com/second", second.guid)
        assertNull(second.publishedAtRaw)
    }

    @Test
    fun `parses Atom 1_0 feed and entry fields`() {
        val feed = parse(ATOM_SAMPLE)

        assertEquals("Atom Test Feed", feed.title)
        assertEquals("https://example.org", feed.siteLink)
        assertEquals(1, feed.articles.size)

        val entry = feed.articles[0]
        assertEquals("urn:uuid:1234", entry.guid)
        assertEquals("Atom Entry One", entry.title)
        assertEquals("https://example.org/one", entry.link)
        assertEquals("Summary text", entry.description)
        assertEquals("John Smith", entry.author)
        assertEquals("2026-07-27T10:00:00Z", entry.publishedAtRaw)
    }

    @Test
    fun `never produces a blank guid even for a fully malformed item`() {
        val malformed = """<?xml version="1.0"?><rss version="2.0"><channel><item></item></channel></rss>"""
        val feed = parse(malformed)

        assertEquals(1, feed.articles.size)
        assertTrue(feed.articles[0].guid.isNotBlank())
    }
}
