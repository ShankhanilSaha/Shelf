package com.example.shelf.navigation

import android.content.Intent
import android.net.Uri
import com.example.shelf.feature.bookdetail.BookDetailNavKey
import com.example.shelf.feature.library.LibraryNavKey
import com.example.shelf.feature.stats.StatsNavKey
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class ShelfDeepLinksTest {
    private fun view(uri: String) = Intent(Intent.ACTION_VIEW, Uri.parse(uri))

    @Test
    fun `book link opens details above the library`() {
        val target = ShelfDeepLinks.resolve(view("shelf://book/OL27448W"))

        assertEquals(
            DeepLinkTarget(TopLevelDestination.LIBRARY, listOf(LibraryNavKey.Shelves, BookDetailNavKey("OL27448W"))),
            target,
        )
    }

    @Test
    fun `book uri builder round-trips`() {
        val target = ShelfDeepLinks.resolve(view(ShelfDeepLinks.bookUri("OL1W")))

        assertEquals(BookDetailNavKey("OL1W"), target?.backStack?.last())
    }

    @Test
    fun `log link opens the log sheet above stats`() {
        val target = ShelfDeepLinks.resolve(view("shelf://log"))

        assertEquals(
            DeepLinkTarget(TopLevelDestination.STATS, listOf(StatsNavKey.Overview, StatsNavKey.LogReading)),
            target,
        )
    }

    @Test
    fun `malformed work keys are rejected`() {
        assertNull(ShelfDeepLinks.resolve(view("shelf://book/not-a-work")))
        assertNull(ShelfDeepLinks.resolve(view("shelf://book/OL1M")))
    }

    @Test
    fun `unknown hosts, schemes and plain launches are ignored`() {
        assertNull(ShelfDeepLinks.resolve(view("shelf://author/OL1A")))
        assertNull(ShelfDeepLinks.resolve(view("https://openlibrary.org/works/OL1W")))
        assertNull(ShelfDeepLinks.resolve(Intent(Intent.ACTION_MAIN)))
        assertNull(ShelfDeepLinks.resolve(null))
    }
}
