package com.example.shelf.core.data.repository

import com.example.shelf.core.common.result.ShelfResult
import com.example.shelf.core.model.Book
import com.example.shelf.core.model.BookSummary
import kotlinx.coroutines.flow.Flow

/**
 * Book details. The UI observes the Room cache ([observeBook]); [refreshBook] fetches from Open
 * Library and writes into the cache, so a viewed book opens instantly, also offline.
 */
interface BookRepository {
    fun observeBook(workKey: String): Flow<Book?>

    /** Fetches the work, its first edition (page count) and author names into the cache. */
    suspend fun refreshBook(workKey: String): ShelfResult<Unit>

    /** Seeds the cache from a search result (never overwrites fetched details). */
    suspend fun cacheSummary(summary: BookSummary)

    /** Resolves an ISBN to an Open Library work key and seeds the cache with the edition. */
    suspend fun findWorkByIsbn(isbn: String): ShelfResult<String>
}
