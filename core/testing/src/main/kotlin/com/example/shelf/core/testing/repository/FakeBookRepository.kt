package com.example.shelf.core.testing.repository

import com.example.shelf.core.common.result.ShelfError
import com.example.shelf.core.common.result.ShelfResult
import com.example.shelf.core.data.repository.BookRepository
import com.example.shelf.core.model.Book
import com.example.shelf.core.model.BookSummary
import com.example.shelf.core.model.toBook
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [BookRepository]; [refreshResult] decides what a refresh does. */
class FakeBookRepository : BookRepository {
    private val books = MutableStateFlow<Map<String, Book>>(emptyMap())

    /** Book stored when a refresh succeeds, keyed by work key. */
    val remoteBooks = mutableMapOf<String, Book>()
    val isbnToWorkKey = mutableMapOf<String, String>()
    var refreshError: ShelfError? = null
    val refreshedKeys = mutableListOf<String>()
    val cachedSummaries = mutableListOf<BookSummary>()

    fun setCached(book: Book) {
        books.value = books.value + (book.workKey to book)
    }

    override fun observeBook(workKey: String): Flow<Book?> = books.map { it[workKey] }

    override suspend fun refreshBook(workKey: String): ShelfResult<Unit> {
        refreshedKeys += workKey
        refreshError?.let { return ShelfResult.Failure(it) }
        val remote = remoteBooks[workKey] ?: return ShelfResult.Failure(ShelfError.NotFound)
        setCached(remote)
        return ShelfResult.Success(Unit)
    }

    override suspend fun cacheSummary(summary: BookSummary) {
        cachedSummaries += summary
        if (books.value[summary.workKey] == null) setCached(summary.toBook())
    }

    override suspend fun findWorkByIsbn(isbn: String): ShelfResult<String> {
        refreshError?.let { return ShelfResult.Failure(it) }
        return isbnToWorkKey[isbn]?.let { ShelfResult.Success(it) } ?: ShelfResult.Failure(ShelfError.NotFound)
    }
}
