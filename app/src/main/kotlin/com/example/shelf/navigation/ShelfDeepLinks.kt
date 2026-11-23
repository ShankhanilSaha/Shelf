package com.example.shelf.navigation

import android.content.Intent
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.deeplink.DeepLinkRequest
import androidx.navigation3.runtime.deeplink.DeepLinkUri
import androidx.navigation3.runtime.deeplink.UriDeepLinkMatcher
import androidx.navigation3.runtime.deeplink.invoke
import com.example.shelf.core.common.DeepLinks
import com.example.shelf.core.model.WorkKeys
import com.example.shelf.feature.bookdetail.BookDetailNavKey
import com.example.shelf.feature.library.LibraryNavKey
import com.example.shelf.feature.stats.StatsNavKey

/** Where a deep link lands: a tab and the full back stack inside it (root first). */
data class DeepLinkTarget(val topLevel: TopLevelDestination, val backStack: List<NavKey>)

/**
 * Deep links handled by [com.example.shelf.MainActivity]:
 * - `shelf://book/{workKey}` opens a book's details on top of the library.
 * - `shelf://log` opens the "Log reading" sheet above stats (used by the reminder notification).
 */
object ShelfDeepLinks {
    private val bookMatcher = UriDeepLinkMatcher(DeepLinkUri(DeepLinks.BOOK_PATTERN), BookDetailNavKey.serializer())
    private val logMatcher = UriDeepLinkMatcher(DeepLinkUri(DeepLinks.LOG_READING), StatsNavKey.LogReading.serializer())

    fun bookUri(workKey: String): String = DeepLinks.book(workKey)

    fun resolve(intent: Intent?): DeepLinkTarget? {
        if (intent?.data == null) return null
        val request = DeepLinkRequest(intent)
        val bookKey = bookMatcher.match(request)?.key
        return when {
            // Never pass arbitrary text from outside the app on to the network layer.
            bookKey != null -> bookKey.takeIf { WorkKeys.isValid(it.workKey) }?.let { key ->
                DeepLinkTarget(TopLevelDestination.LIBRARY, listOf(LibraryNavKey.Shelves, key))
            }

            logMatcher.match(request) != null ->
                DeepLinkTarget(TopLevelDestination.STATS, listOf(StatsNavKey.Overview, StatsNavKey.LogReading))

            else -> null
        }
    }
}
