package com.example.shelf.core.data.mapper

import com.example.shelf.core.model.BookSummary
import com.example.shelf.core.network.model.SearchDocDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SearchMappersTest {
    @Test
    fun `maps a complete document`() {
        val doc = SearchDocDto(
            key = "/works/OL27482W",
            title = " The Hobbit ",
            authorNames = listOf("J.R.R. Tolkien", " ", "J.R.R. Tolkien"),
            firstPublishYear = 1937,
            coverId = 14627509,
            isbns = listOf("0261102214", "9780261103573"),
            numberOfPagesMedian = 310,
        )

        assertEquals(
            BookSummary(
                workKey = "OL27482W",
                title = "The Hobbit",
                authors = listOf("J.R.R. Tolkien"),
                firstPublishYear = 1937,
                coverId = 14627509,
                isbn = "9780261103573",
                pageCount = 310,
            ),
            doc.toBookSummaryOrNull(),
        )
    }

    @Test
    fun `blank or missing titles are skipped`() {
        assertNull(SearchDocDto(key = "/works/OL1W", title = "  ").toBookSummaryOrNull())
        assertNull(SearchDocDto(key = "/works/OL1W", title = null).toBookSummaryOrNull())
    }

    @Test
    fun `non-work keys are skipped`() {
        assertNull(SearchDocDto(key = "/books/OL1M", title = "Edition").toBookSummaryOrNull())
    }

    @Test
    fun `invalid numbers become null and ISBN-10 is the fallback`() {
        val summary = SearchDocDto(
            key = "/works/OL1W",
            title = "T",
            firstPublishYear = 0,
            coverId = -1,
            isbns = listOf("not-an-isbn", "0306406152"),
            numberOfPagesMedian = 0,
        ).toBookSummaryOrNull()

        assertNull(summary?.firstPublishYear)
        assertNull(summary?.coverId)
        assertNull(summary?.pageCount)
        assertEquals("0306406152", summary?.isbn)
    }
}
