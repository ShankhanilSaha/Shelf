package com.example.shelf.core.data.repository

import com.example.shelf.core.common.result.ShelfError
import com.example.shelf.core.common.result.ShelfException
import com.example.shelf.core.common.result.ShelfResult
import com.example.shelf.core.data.mapper.mergeBookDetails
import com.example.shelf.core.data.mapper.toBook
import com.example.shelf.core.data.mapper.toEntity
import com.example.shelf.core.data.mapper.toSeedEntity
import com.example.shelf.core.database.dao.BookDao
import com.example.shelf.core.model.Book
import com.example.shelf.core.model.BookSummary
import com.example.shelf.core.model.WorkKeys
import com.example.shelf.core.network.OpenLibraryDataSource
import com.example.shelf.core.network.model.EditionDto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

internal class OfflineFirstBookRepository @Inject constructor(
    private val bookDao: BookDao,
    private val network: OpenLibraryDataSource,
    private val clock: Clock,
) : BookRepository {

    override fun observeBook(workKey: String): Flow<Book?> = bookDao.observeBook(workKey)
        .map { it?.toBook() }
        .distinctUntilChanged()

    override suspend fun refreshBook(workKey: String): ShelfResult<Unit> = try {
        // The work is required; the edition (page count), author names and year are best effort.
        val existing = bookDao.getBook(workKey)
        val work = network.work(workKey)
        val details = coroutineScope {
            val edition = async { bestEffort { network.firstEdition(workKey) } }
            // Books that came from search already have the index's year; others (scanned, deep
            // links) ask the index, because the work record's own date is often a reprint's.
            val indexedYear = async {
                if (existing?.firstPublishYear != null) null else bestEffort { indexedFirstPublishYear(workKey) }
            }
            val authors = work.authorKeys
                .take(MAX_AUTHORS)
                .map { key -> async { bestEffort { network.author(key.removePrefix(AUTHOR_PREFIX)).displayName } } }
            FetchedDetails(edition.await(), authors.awaitAll().filterNotNull().distinct(), indexedYear.await())
        }
        val merged = mergeBookDetails(
            workKey = workKey,
            existing = existing,
            work = work,
            edition = details.edition,
            authorNames = details.authorNames,
            fetchedAtMillis = clock.millis(),
            indexedFirstPublishYear = details.indexedYear,
        )
        bookDao.upsert(merged)
        ShelfResult.Success(Unit)
    } catch (e: ShelfException) {
        ShelfResult.Failure(e.error, e)
    }

    private suspend fun indexedFirstPublishYear(workKey: String): Int? =
        network.search(query = "key:/works/$workKey", page = 1, limit = 1)
            .docs
            .firstOrNull { WorkKeys.fromOpenLibraryKey(it.key) == workKey }
            ?.firstPublishYear

    private class FetchedDetails(val edition: EditionDto?, val authorNames: List<String>, val indexedYear: Int?)

    override suspend fun cacheSummary(summary: BookSummary) {
        bookDao.insertIfAbsent(summary.toEntity())
    }

    override suspend fun findWorkByIsbn(isbn: String): ShelfResult<String> = try {
        val edition: EditionDto = network.editionByIsbn(isbn)
        val workKey = edition.works.firstNotNullOfOrNull { WorkKeys.fromOpenLibraryKey(it.key) }
        if (workKey == null) {
            ShelfResult.Failure(ShelfError.NotFound)
        } else {
            bookDao.insertIfAbsent(edition.toSeedEntity(workKey, isbn))
            ShelfResult.Success(workKey)
        }
    } catch (e: ShelfException) {
        ShelfResult.Failure(e.error, e)
    }

    private suspend fun <T> bestEffort(block: suspend () -> T): T? = try {
        block()
    } catch (_: ShelfException) {
        null
    }

    private companion object {
        const val MAX_AUTHORS = 3
        const val AUTHOR_PREFIX = "/authors/"
    }
}
