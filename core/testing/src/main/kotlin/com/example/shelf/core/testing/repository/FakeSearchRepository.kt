package com.example.shelf.core.testing.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.testing.asPagingSourceFactory
import com.example.shelf.core.data.repository.SearchRepository
import com.example.shelf.core.model.BookSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [SearchRepository]. By default every query returns [resultsFor]; tests can replace
 * [pagingFlowFor] to simulate load states and errors.
 */
class FakeSearchRepository(var resultsFor: (String) -> List<BookSummary> = { emptyList() }) : SearchRepository {
    val searchedQueries = mutableListOf<String>()
    private val recents = MutableStateFlow<List<String>>(emptyList())

    /**
     * A real [Pager] over an in-memory source, like production. (A static `PagingData.from(list)`
     * passed through `cachedIn` never reports load states, which hangs `asSnapshot`.)
     */
    var pagingFlowFor: (String) -> Flow<PagingData<BookSummary>> = { query ->
        Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = resultsFor(query).asPagingSourceFactory(),
        ).flow
    }

    override fun search(query: String): Flow<PagingData<BookSummary>> {
        searchedQueries += query
        return pagingFlowFor(query)
    }

    override val recentSearches: Flow<List<String>> = recents

    override suspend fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        recents.value = (listOf(trimmed) + recents.value).distinctBy { it.lowercase() }.take(MAX_RECENTS)
    }

    override suspend fun clearRecentSearches() {
        recents.value = emptyList()
    }

    fun setRecentSearches(searches: List<String>) {
        recents.value = searches
    }

    private companion object {
        const val MAX_RECENTS = 10
        const val PAGE_SIZE = 20
    }
}
