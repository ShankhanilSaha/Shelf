package com.example.shelf.core.model

import java.time.Instant

/** A book's place in the user's library. */
data class ShelfEntry(
    val workKey: String,
    val shelf: ShelfType,
    val currentPage: Int = 0,
    val rating: Int? = null,
    val notes: String = "",
    val addedAt: Instant,
    val updatedAt: Instant,
    val finishedAt: Instant? = null,
) {
    init {
        require(currentPage >= 0) { "currentPage must not be negative" }
        require(rating == null || rating in Rating.RANGE) { "rating must be in ${Rating.RANGE}" }
    }
}

/** A library entry joined with its cached book. */
data class ShelfBook(val book: Book, val entry: ShelfEntry) {
    /** Reading progress between 0 and 1, or null when the page count is unknown. */
    val progress: Float?
        get() {
            val pages = book.pageCount?.takeIf { it > 0 } ?: return null
            return (entry.currentPage.toFloat() / pages).coerceIn(0f, 1f)
        }
}

object Rating {
    const val MIN = 1
    const val MAX = 5
    val RANGE = MIN..MAX
}
