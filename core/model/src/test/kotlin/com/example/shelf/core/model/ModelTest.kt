package com.example.shelf.core.model

import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModelTest {
    @Test
    fun `work keys accept bare ids and Open Library keys`() {
        assertEquals("OL27448W", WorkKeys.fromOpenLibraryKey("/works/OL27448W"))
        assertEquals("OL27448W", WorkKeys.fromOpenLibraryKey("OL27448W"))
        assertEquals("/works/OL27448W", WorkKeys.toOpenLibraryKey("OL27448W"))
    }

    @Test
    fun `work keys reject edition and author keys`() {
        assertNull(WorkKeys.fromOpenLibraryKey("/books/OL7353617M"))
        assertNull(WorkKeys.fromOpenLibraryKey("/authors/OL23919A"))
        assertNull(WorkKeys.fromOpenLibraryKey("OL0W"))
        assertFalse(WorkKeys.isValid("../etc/passwd"))
    }

    @Test
    fun `page goal counts pages and minute goal counts minutes`() {
        val pages = DailyGoal(GoalType.PAGES, target = 20)
        val minutes = DailyGoal(GoalType.MINUTES, target = 30)

        assertTrue(pages.isMetBy(pages = 20, minutes = 0))
        assertFalse(pages.isMetBy(pages = 19, minutes = 120))
        assertTrue(minutes.isMetBy(pages = 0, minutes = 30))
        assertEquals(12, minutes.amountOf(pages = 99, minutes = 12))
    }

    @Test
    fun `goal target must be positive`() {
        assertFailsWith<IllegalArgumentException> { DailyGoal(GoalType.PAGES, target = 0) }
    }

    @Test
    fun `progress is current page over page count, clamped`() {
        val book = Book(workKey = "OL1W", title = "T", authors = emptyList(), pageCount = 200)
        val entry = ShelfEntry(
            workKey = "OL1W",
            shelf = ShelfType.READING,
            currentPage = 50,
            addedAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        )
        assertEquals(0.25f, ShelfBook(book, entry).progress)
        assertEquals(1f, ShelfBook(book, entry.copy(currentPage = 500)).progress)
        assertNull(ShelfBook(book.copy(pageCount = null), entry).progress)
    }

    @Test
    fun `rating outside one to five is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            ShelfEntry(
                workKey = "OL1W",
                shelf = ShelfType.FINISHED,
                rating = 6,
                addedAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            )
        }
    }

    @Test
    fun `a reading log needs pages or minutes`() {
        assertFailsWith<IllegalArgumentException> {
            ReadingLog(id = "1", date = LocalDate.of(2026, 1, 1), pages = 0, minutes = 0)
        }
    }

    @Test
    fun `cover url uses the requested size`() {
        assertEquals("https://covers.openlibrary.org/b/id/42-M.jpg", coverUrl(42))
        assertEquals("https://covers.openlibrary.org/b/id/42-L.jpg", coverUrl(42, CoverSize.LARGE))
    }
}
