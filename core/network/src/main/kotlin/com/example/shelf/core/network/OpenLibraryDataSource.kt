package com.example.shelf.core.network

import com.example.shelf.core.common.result.ShelfException
import com.example.shelf.core.network.model.AuthorDto
import com.example.shelf.core.network.model.EditionDto
import com.example.shelf.core.network.model.SearchResponseDto
import com.example.shelf.core.network.model.WorkDto
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * Network access used by the data layer. Every failure is thrown as a [ShelfException] whose
 * error is already classified (offline, not found, server problem).
 */
interface OpenLibraryDataSource {
    suspend fun search(query: String, page: Int, limit: Int): SearchResponseDto

    suspend fun work(workId: String): WorkDto

    /** The first edition listed for a work (used for the page count), or null if it has none. */
    suspend fun firstEdition(workId: String): EditionDto?

    suspend fun author(authorId: String): AuthorDto

    suspend fun editionByIsbn(isbn: String): EditionDto
}

internal class RetrofitOpenLibraryDataSource @Inject constructor(private val api: OpenLibraryApi) :
    OpenLibraryDataSource {
    override suspend fun search(query: String, page: Int, limit: Int): SearchResponseDto =
        classified { api.search(query = query, page = page, limit = limit) }

    override suspend fun work(workId: String): WorkDto = classified { api.work(workId) }

    override suspend fun firstEdition(workId: String): EditionDto? =
        classified { api.editions(workId, limit = 1).entries.firstOrNull() }

    override suspend fun author(authorId: String): AuthorDto = classified { api.author(authorId) }

    override suspend fun editionByIsbn(isbn: String): EditionDto = classified { api.editionByIsbn(isbn) }

    // The one place that catches everything: anything thrown by Retrofit, OkHttp or the JSON
    // converter becomes a classified ShelfException. Cancellation must keep propagating.
    @Suppress("TooGenericExceptionCaught")
    private suspend inline fun <T> classified(block: () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw ShelfException(e.toShelfError(), e)
    }
}
