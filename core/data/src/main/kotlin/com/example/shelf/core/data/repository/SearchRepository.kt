package com.example.shelf.core.data.repository

import androidx.paging.PagingData
import com.example.shelf.core.model.BookSummary
import kotlinx.coroutines.flow.Flow

/**
 * Book search. Results are paged straight from the network and not stored: the one exception to
 * the offline-first rule, because search results are only meaningful while online.
 */
interface SearchRepository {
    fun search(query: String): Flow<PagingData<BookSummary>>

    /** Most recent first, at most ten. */
    val recentSearches: Flow<List<String>>

    suspend fun addRecentSearch(query: String)

    suspend fun clearRecentSearches()
}
