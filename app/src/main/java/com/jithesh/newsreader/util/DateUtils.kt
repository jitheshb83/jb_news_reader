package com.jithesh.newsreader.util

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Parses RSS 2.0 `pubDate` (RFC-822, e.g. "Mon, 27 Jul 2026 10:00:00 GMT")
 * and Atom `updated`/`published` (ISO-8601, e.g. "2026-07-27T10:00:00Z") timestamps.
 * Real-world feeds are inconsistent, so this returns null instead of throwing on anything unparseable.
 */
object DateUtils {

    fun parseFeedDate(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        return parseRfc822(trimmed) ?: parseIso8601(trimmed)
    }

    private fun parseRfc822(value: String): Long? = try {
        OffsetDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
    } catch (e: DateTimeParseException) {
        null
    }

    private fun parseIso8601(value: String): Long? = try {
        OffsetDateTime.parse(value).toInstant().toEpochMilli()
    } catch (e: DateTimeParseException) {
        null
    }
}
