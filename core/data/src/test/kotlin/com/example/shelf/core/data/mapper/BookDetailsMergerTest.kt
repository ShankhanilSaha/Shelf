package com.example.shelf.core.data.mapper

import com.example.shelf.core.database.model.BookEntity
import com.example.shelf.core.network.model.EditionDto
import com.example.shelf.core.network.model.KeyRefDto
import com.example.shelf.core.network.model.WorkDto
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BookDetailsMergerTest {
    private val cachedSummary = BookEntity(
        workKey = "OL1W",
        title = "Cached title",
        authors = listOf("Cached Author"),
        firstPublishYear = 1965,
        coverId = 7,
        description = null,
        subjects = emptyList(),
        pageCount = 412,
        isbn = "9780441013593",
        detailsFetchedAt = null,
    )

    @Test
    fun `fresh details override the cached summary`() {
        val merged = mergeBookDetails(
            workKey = "OL1W",
            existing = cachedSummary,
            work = WorkDto(
                key = "/works/OL1W",
                title = "Dune",
                description = JsonPrimitive("Spice."),
                subjects = listOf("Science fiction", "science fiction", " Deserts "),
                covers = listOf(-1, 99),
                firstPublishDate = "August 1965",
            ),
            edition = EditionDto(numberOfPages = 604),
            authorNames = listOf("Frank Herbert"),
            fetchedAtMillis = 5,
        )

        assertEquals("Dune", merged.title)
        assertEquals(listOf("Frank Herbert"), merged.authors)
        assertEquals(1965, merged.firstPublishYear)
        assertEquals(99L, merged.coverId)
        assertEquals("Spice.", merged.description)
        assertEquals(listOf("Science fiction", "Deserts"), merged.subjects)
        assertEquals(604, merged.pageCount)
        assertEquals("9780441013593", merged.isbn)
        assertEquals(5L, merged.detailsFetchedAt)
    }

    @Test
    fun `missing pieces never erase what was cached`() {
        val merged = mergeBookDetails(
            workKey = "OL1W",
            existing = cachedSummary,
            work = WorkDto(key = "/works/OL1W", title = "  "),
            edition = null,
            authorNames = emptyList(),
            fetchedAtMillis = 5,
        )

        assertEquals("Cached title", merged.title)
        assertEquals(listOf("Cached Author"), merged.authors)
        assertEquals(1965, merged.firstPublishYear)
        assertEquals(7L, merged.coverId)
        assertEquals(412, merged.pageCount)
    }

    @Test
    fun `without a cache the edition fills the gaps`() {
        val merged = mergeBookDetails(
            workKey = "OL1W",
            existing = null,
            work = WorkDto(key = "/works/OL1W"),
            edition = EditionDto(
                title = "Edition title",
                publishDate = "c1999",
                covers = listOf(-1, 3),
                isbn13 = listOf("bad", "9780306406157"),
            ),
            authorNames = emptyList(),
            fetchedAtMillis = 1,
        )

        assertEquals("Edition title", merged.title)
        // The edition's date is when that printing came out, not the work's first publication.
        assertNull(merged.firstPublishYear)
        assertEquals(3L, merged.coverId)
        assertEquals("9780306406157", merged.isbn)
        assertNull(merged.description)
    }

    @Test
    fun `the earliest claimed first publication wins`() {
        val cached = cachedSummary.copy(firstPublishYear = 1954)
        val work = WorkDto(key = "/works/OL1W", firstPublishDate = "November 12, 1972")

        val fromSearch =
            mergeBookDetails("OL1W", cached, work, edition = null, authorNames = emptyList(), fetchedAtMillis = 1)
        val fromIndex = mergeBookDetails(
            workKey = "OL1W",
            existing = null,
            work = work,
            edition = EditionDto(publishDate = "1991"),
            authorNames = emptyList(),
            fetchedAtMillis = 1,
            indexedFirstPublishYear = 1954,
        )
        val workOnly =
            mergeBookDetails("OL1W", null, work, edition = null, authorNames = emptyList(), fetchedAtMillis = 1)

        assertEquals(1954, fromSearch.firstPublishYear)
        assertEquals(1954, fromIndex.firstPublishYear)
        assertEquals(1972, workOnly.firstPublishYear)
    }

    @Test
    fun `markdown in descriptions becomes plain text`() {
        val markdown = """
            **From the *New York Times* bestselling author of *Jonathan Strange & Mr. Norrell*.**

            Piranesi's house is no _ordinary_ building ([source][1]). See [the site](https://example.com).

            ----------
            [1]: https://www.example.com/piranesi
        """.trimIndent()

        assertEquals(
            "From the New York Times bestselling author of Jonathan Strange & Mr. Norrell.\n\n" +
                "Piranesi's house is no ordinary building (source). See the site.",
            markdown.toPlainDescription(),
        )
        assertEquals("A plain description stays as it is.", "A plain description stays as it is.".toPlainDescription())
    }

    @Test
    fun `catalogue tags are not shown as subjects`() {
        val merged = mergeBookDetails(
            workKey = "OL1W",
            existing = null,
            work = WorkDto(
                key = "/works/OL1W",
                subjects = listOf(
                    "form:novel",
                    "genre:fantasy",
                    "nyt:combined-print-and-e-book-fiction=2020-10-04",
                    "Fantasy fiction",
                ),
            ),
            edition = null,
            authorNames = emptyList(),
            fetchedAtMillis = 1,
        )

        assertEquals(listOf("Fantasy fiction"), merged.subjects)
    }

    @Test
    fun `years are parsed from free text`() {
        assertEquals(1938, parseYear("January 1938"))
        assertEquals(2021, parseYear("2021-03-04"))
        assertEquals(1999, parseYear("c1999"))
        assertNull(parseYear("123456"))
        assertNull(parseYear("unknown"))
        assertNull(parseYear(null))
    }

    @Test
    fun `an edition found by isbn seeds a minimal row`() {
        val seed = EditionDto(
            title = "The Fellowship of the Ring",
            publishDate = "1991",
            numberOfPages = 416,
            covers = listOf(8311423, -1),
            works = listOf(KeyRefDto("/works/OL27513W")),
        ).toSeedEntity("OL27513W", "9780261103573")

        assertEquals("The Fellowship of the Ring", seed.title)
        assertEquals(416, seed.pageCount)
        assertEquals(8311423L, seed.coverId)
        assertNull(seed.firstPublishYear)
        assertNull(seed.detailsFetchedAt)
    }
}
