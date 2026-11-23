package com.example.shelf.core.data.mapper

import com.example.shelf.core.model.BookSummary
import com.example.shelf.core.model.Isbn
import com.example.shelf.core.model.WorkKeys
import com.example.shelf.core.network.model.SearchDocDto

/** Maps a search document; returns null for results we cannot show (blank title, odd key). */
internal fun SearchDocDto.toBookSummaryOrNull(): BookSummary? {
    val title = title?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val workKey = WorkKeys.fromOpenLibraryKey(key) ?: return null
    return BookSummary(
        workKey = workKey,
        title = title,
        authors = authorNames.map(String::trim).filter(String::isNotEmpty).distinct(),
        firstPublishYear = firstPublishYear?.takeIf { it > 0 },
        coverId = coverId?.takeIf { it > 0 },
        isbn = isbns.firstOrNull(Isbn::isValidIsbn13) ?: isbns.firstOrNull(Isbn::isValidIsbn10),
        pageCount = numberOfPagesMedian?.takeIf { it > 0 },
    )
}
