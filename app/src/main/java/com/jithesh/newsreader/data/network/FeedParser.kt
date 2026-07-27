package com.jithesh.newsreader.data.network

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory

/** Defensive cap against a pathological/malicious feed with an unbounded number of entries. */
private const val MAX_ITEMS = 500

data class ParsedArticle(
    val guid: String,
    val title: String,
    val link: String,
    val description: String?,
    val contentHtml: String?,
    val author: String?,
    val publishedAtRaw: String?,
    val thumbnailUrl: String?,
)

data class ParsedFeed(
    val title: String,
    val siteLink: String?,
    val articles: List<ParsedArticle>,
)

/**
 * Hand-rolled RSS 2.0 + Atom 1.0 parser using SAX (javax.xml.parsers), not android.util.Xml's
 * XmlPullParser — SAX is available identically on the JVM and on Android, so this can be
 * unit-tested on the local JVM without Robolectric or an emulator.
 */
object FeedParser {

    fun parse(input: InputStream): ParsedFeed {
        val handler = FeedHandler()
        newSaxParserFactory().newSAXParser().parse(input, handler)
        return ParsedFeed(
            title = handler.feedTitle.ifBlank { "Untitled feed" },
            siteLink = handler.feedLink,
            articles = handler.articles,
        )
    }

    private fun newSaxParserFactory(): SAXParserFactory {
        val factory = SAXParserFactory.newInstance()
        // Harden against XXE: these external feeds are untrusted input.
        val safeFeatures = listOf(
            XMLConstants.FEATURE_SECURE_PROCESSING to true,
            "http://xml.org/sax/features/external-general-entities" to false,
            "http://xml.org/sax/features/external-parameter-entities" to false,
            "http://apache.org/xml/features/nonvalidating/load-external-dtd" to false,
        )
        for ((feature, enabled) in safeFeatures) {
            runCatching { factory.setFeature(feature, enabled) }
        }
        return factory
    }
}

private class ArticleBuilder {
    var guid: String? = null
    var title: String = ""
    var link: String = ""
    var description: String? = null
    var contentHtml: String? = null
    var author: String? = null
    var publishedAtRaw: String? = null
    var thumbnailUrl: String? = null

    fun build(): ParsedArticle {
        val resolvedGuid = guid?.takeIf { it.isNotBlank() }
            ?: link.takeIf { it.isNotBlank() }
            ?: title.ifBlank { "untitled-${System.nanoTime()}" }
        return ParsedArticle(
            guid = resolvedGuid,
            title = title.ifBlank { "(untitled)" },
            link = link,
            description = description,
            contentHtml = contentHtml,
            author = author,
            publishedAtRaw = publishedAtRaw,
            thumbnailUrl = thumbnailUrl,
        )
    }
}

/** Namespace prefixes (content:encoded, dc:creator, media:thumbnail) are stripped, not resolved. */
private class FeedHandler : DefaultHandler() {
    private val elementStack = ArrayDeque<String>()
    private val textBuffer = StringBuilder()

    var feedTitle: String = ""
        private set
    var feedLink: String? = null
        private set
    val articles = mutableListOf<ParsedArticle>()

    private var current: ArticleBuilder? = null

    private fun tagName(qName: String) = qName.substringAfterLast(':')

    override fun startElement(uri: String?, localName: String?, qName: String, attributes: Attributes) {
        val name = tagName(qName)
        elementStack.addLast(name)
        textBuffer.setLength(0)
        when (name) {
            "item", "entry" -> current = ArticleBuilder()
            "link" -> handleLinkStart(attributes)
            "enclosure" -> handleEnclosure(attributes)
            "thumbnail" -> current?.let { if (it.thumbnailUrl == null) it.thumbnailUrl = attributes.getValue("url") }
        }
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        textBuffer.append(ch, start, length)
    }

    override fun endElement(uri: String?, localName: String?, qName: String) {
        val name = tagName(qName)
        val text = textBuffer.toString().trim()
        val parentIsAuthor = elementStack.size >= 2 && elementStack[elementStack.size - 2] == "author"
        elementStack.removeLastOrNull()

        val builder = current
        when {
            name == "item" || name == "entry" -> {
                builder?.let { if (articles.size < MAX_ITEMS) articles.add(it.build()) }
                current = null
            }
            builder != null -> applyToArticle(builder, name, text, parentIsAuthor)
            else -> applyToFeed(name, text)
        }
        textBuffer.setLength(0)
    }

    private fun applyToArticle(builder: ArticleBuilder, name: String, text: String, parentIsAuthor: Boolean) {
        when (name) {
            "title" -> builder.title = text
            "guid" -> if (text.isNotBlank()) builder.guid = text
            "id" -> if (builder.guid.isNullOrBlank() && text.isNotBlank()) builder.guid = text
            "link" -> if (builder.link.isBlank() && text.isNotBlank()) builder.link = text
            "pubDate" -> if (text.isNotBlank()) builder.publishedAtRaw = text
            "published" -> if (text.isNotBlank()) builder.publishedAtRaw = text
            "updated" -> if (builder.publishedAtRaw.isNullOrBlank() && text.isNotBlank()) builder.publishedAtRaw = text
            "description" -> builder.description = text
            "summary" -> if (builder.description.isNullOrBlank()) builder.description = text
            "encoded" -> builder.contentHtml = text
            "content" -> if (builder.contentHtml.isNullOrBlank()) builder.contentHtml = text
            "creator" -> if (text.isNotBlank()) builder.author = text
            "name" -> if (parentIsAuthor && text.isNotBlank()) builder.author = text
            "author" -> if (builder.author.isNullOrBlank() && text.isNotBlank()) builder.author = text
        }
    }

    private fun applyToFeed(name: String, text: String) {
        when (name) {
            "title" -> if (feedTitle.isBlank() && text.isNotBlank()) feedTitle = text
            "link" -> if (feedLink.isNullOrBlank() && text.isNotBlank()) feedLink = text
        }
    }

    private fun handleLinkStart(attributes: Attributes) {
        val href = attributes.getValue("href") ?: return // RSS <link> carries no href; text handled in endElement
        val rel = attributes.getValue("rel") ?: "alternate"
        val type = attributes.getValue("type").orEmpty()
        if (rel == "enclosure" && type.startsWith("image")) {
            current?.let { if (it.thumbnailUrl == null) it.thumbnailUrl = href }
            return
        }
        if (rel != "alternate") return
        val target = current
        if (target != null) {
            if (target.link.isBlank()) target.link = href
        } else if (feedLink.isNullOrBlank()) {
            feedLink = href
        }
    }

    private fun handleEnclosure(attributes: Attributes) {
        val url = attributes.getValue("url") ?: return
        val type = attributes.getValue("type").orEmpty()
        if (type.startsWith("image")) {
            current?.let { if (it.thumbnailUrl == null) it.thumbnailUrl = url }
        }
    }
}
