// Test data: arbitrary example values are the point of these builders.
@file:Suppress("MagicNumber")

package com.example.shelf.core.testing

import com.example.shelf.core.model.Book
import com.example.shelf.core.model.BookSummary
import com.example.shelf.core.model.ShelfBook
import com.example.shelf.core.model.ShelfEntry
import com.example.shelf.core.model.ShelfType
import java.time.Instant

/** Builders with sensible defaults so tests only spell out what they care about. */
object TestData {
    val NOW: Instant = Instant.parse("2026-11-24T10:15:30Z")

    fun summary(
        id: Int = 1,
        title: String = "Book $id",
        authors: List<String> = listOf("Author $id"),
        firstPublishYear: Int? = 2000 + id,
        coverId: Long? = null,
        pageCount: Int? = 300,
    ) = BookSummary(
        workKey = "OL${id}W",
        title = title,
        authors = authors,
        firstPublishYear = firstPublishYear,
        coverId = coverId,
        isbn = null,
        pageCount = pageCount,
    )

    fun book(
        id: Int = 1,
        title: String = "Book $id",
        authors: List<String> = listOf("Author $id"),
        pageCount: Int? = 300,
        description: String? = "Description of book $id.",
        subjects: List<String> = listOf("Fiction"),
        hasDetails: Boolean = true,
    ) = Book(
        workKey = "OL${id}W",
        title = title,
        authors = authors,
        firstPublishYear = 2000 + id,
        coverId = null,
        description = description,
        subjects = subjects,
        pageCount = pageCount,
        hasDetails = hasDetails,
    )

    fun entry(
        id: Int = 1,
        shelf: ShelfType = ShelfType.WANT_TO_READ,
        currentPage: Int = 0,
        rating: Int? = null,
        notes: String = "",
        updatedAt: Instant = NOW,
        finishedAt: Instant? = null,
    ) = ShelfEntry(
        workKey = "OL${id}W",
        shelf = shelf,
        currentPage = currentPage,
        rating = rating,
        notes = notes,
        addedAt = updatedAt,
        updatedAt = updatedAt,
        finishedAt = finishedAt,
    )

    fun shelfBook(id: Int = 1, shelf: ShelfType = ShelfType.WANT_TO_READ, currentPage: Int = 0) =
        ShelfBook(book = book(id), entry = entry(id, shelf = shelf, currentPage = currentPage))
}
