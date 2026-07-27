package com.jithesh.newsreader.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class DateUtilsTest {

    @Test
    fun `parses RFC-822 pubDate`() {
        val epoch = DateUtils.parseFeedDate("Mon, 27 Jul 2026 10:00:00 GMT")
        val expected = OffsetDateTime.of(2026, 7, 27, 10, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(expected, epoch)
    }

    @Test
    fun `parses ISO-8601 updated timestamp`() {
        val epoch = DateUtils.parseFeedDate("2026-07-27T10:00:00Z")
        val expected = OffsetDateTime.of(2026, 7, 27, 10, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(expected, epoch)
    }

    @Test
    fun `returns null for blank or missing input`() {
        assertNull(DateUtils.parseFeedDate(null))
        assertNull(DateUtils.parseFeedDate(""))
        assertNull(DateUtils.parseFeedDate("   "))
    }

    @Test
    fun `returns null for unparseable garbage instead of throwing`() {
        assertNull(DateUtils.parseFeedDate("not a date at all"))
    }
}
