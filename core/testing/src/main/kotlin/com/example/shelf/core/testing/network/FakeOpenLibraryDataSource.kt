package com.example.shelf.core.testing.network

import com.example.shelf.core.common.result.ShelfError
import com.example.shelf.core.common.result.ShelfException
import com.example.shelf.core.network.OpenLibraryDataSource
import com.example.shelf.core.network.model.AuthorDto
import com.example.shelf.core.network.model.EditionDto
import com.example.shelf.core.network.model.SearchResponseDto
import com.example.shelf.core.network.model.WorkDto

/**
 * In-memory Open Library. Unknown ids fail with [ShelfError.NotFound]; set [offline] to make
 * every call fail like a device without a connection.
 */
class FakeOpenLibraryDataSource : OpenLibraryDataSource {
    val works = mutableMapOf<String, WorkDto>()
    val firstEditions = mutableMapOf<String, EditionDto>()
    val authors = mutableMapOf<String, AuthorDto>()
    val editionsByIsbn = mutableMapOf<String, EditionDto>()
    var searchResponse: (query: String, page: Int) -> SearchResponseDto = { _, _ -> SearchResponseDto() }

    var offline = false
    var failingEditions = false
    var failingAuthors = false
    val requests = mutableListOf<String>()

    override suspend fun search(query: String, page: Int, limit: Int): SearchResponseDto =
        call("search:$query:$page") { searchResponse(query, page) }

    override suspend fun work(workId: String): WorkDto = call("work:$workId") { works[workId] }

    override suspend fun firstEdition(workId: String): EditionDto? = call("edition:$workId", fail = failingEditions) {
        firstEditions[workId] ?: NO_EDITION
    }.takeUnless { it === NO_EDITION }

    override suspend fun author(authorId: String): AuthorDto =
        call("author:$authorId", fail = failingAuthors) { authors[authorId] }

    override suspend fun editionByIsbn(isbn: String): EditionDto = call("isbn:$isbn") { editionsByIsbn[isbn] }

    private fun <T> call(request: String, fail: Boolean = false, block: () -> T?): T {
        requests += request
        if (offline || fail) throw ShelfException(ShelfError.Network)
        return block() ?: throw ShelfException(ShelfError.NotFound)
    }

    private companion object {
        val NO_EDITION = EditionDto(key = "none")
    }
}
