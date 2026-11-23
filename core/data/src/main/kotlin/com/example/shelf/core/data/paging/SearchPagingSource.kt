package com.example.shelf.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.shelf.core.common.result.ShelfException
import com.example.shelf.core.data.mapper.toBookSummaryOrNull
import com.example.shelf.core.model.BookSummary
import com.example.shelf.core.network.OpenLibraryDataSource

/** Pages through `GET /search.json?q=…&page=n&limit=20`. Pages are 1-based. */
internal class SearchPagingSource(private val network: OpenLibraryDataSource, private val query: String) :
    PagingSource<Int, BookSummary>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, BookSummary> {
        val page = params.key ?: FIRST_PAGE
        return try {
            val response = network.search(query = query, page = page, limit = PAGE_SIZE)
            // Count raw documents, not mapped ones: skipped results still occupy their slot.
            val seen = (page - 1) * PAGE_SIZE + response.docs.size
            LoadResult.Page(
                data = response.docs.mapNotNull { it.toBookSummaryOrNull() },
                prevKey = if (page == FIRST_PAGE) null else page - 1,
                nextKey = if (response.docs.isEmpty() || seen >= response.numFound) null else page + 1,
            )
        } catch (e: ShelfException) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, BookSummary>): Int? {
        val anchor = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchor) ?: return null
        return page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
    }

    companion object {
        const val FIRST_PAGE = 1
        const val PAGE_SIZE = 20
    }
}
