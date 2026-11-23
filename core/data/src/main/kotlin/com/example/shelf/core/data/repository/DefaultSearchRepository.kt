package com.example.shelf.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.shelf.core.data.paging.SearchPagingSource
import com.example.shelf.core.datastore.ShelfPreferencesDataSource
import com.example.shelf.core.model.BookSummary
import com.example.shelf.core.network.OpenLibraryDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class DefaultSearchRepository @Inject constructor(
    private val network: OpenLibraryDataSource,
    private val preferences: ShelfPreferencesDataSource,
) : SearchRepository {

    override fun search(query: String): Flow<PagingData<BookSummary>> = Pager(
        // Open Library pages by page number and a fixed limit, so the first load must be one page.
        config = PagingConfig(
            pageSize = SearchPagingSource.PAGE_SIZE,
            initialLoadSize = SearchPagingSource.PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { SearchPagingSource(network, query) },
    ).flow

    override val recentSearches: Flow<List<String>> = preferences.recentSearches

    override suspend fun addRecentSearch(query: String) = preferences.addRecentSearch(query)

    override suspend fun clearRecentSearches() = preferences.clearRecentSearches()

    private companion object {
        const val PREFETCH_DISTANCE = 5
    }
}
