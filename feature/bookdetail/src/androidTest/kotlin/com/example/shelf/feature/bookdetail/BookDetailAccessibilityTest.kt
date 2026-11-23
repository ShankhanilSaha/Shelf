package com.example.shelf.feature.bookdetail

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.shelf.core.model.Book
import com.example.shelf.core.model.ShelfEntry
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.ui.theme.ShelfTheme
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Runs the Accessibility Test Framework over the whole screen: touch target size, text and image
 * contrast on the real rendering, missing labels, duplicate clickable areas. A finding of type
 * ERROR fails the test. Needs a device or emulator on API 34+.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BookDetailAccessibilityTest {
    private val book = Book(
        workKey = "OL1W",
        title = "The Fellowship of the Ring",
        authors = listOf("J.R.R. Tolkien"),
        firstPublishYear = 1954,
        description = "Frodo Baggins is entrusted with the Ring.",
        subjects = listOf("Fantasy fiction", "Quests"),
        pageCount = 538,
        hasDetails = true,
    )

    private fun check(entry: ShelfEntry?, dark: Boolean) = runComposeUiTest {
        enableAccessibilityChecks()
        setContent {
            ShelfTheme(darkTheme = dark, dynamicColor = false) {
                BookDetailScreen(
                    uiState = BookDetailUiState.Success(
                        book,
                        entry,
                        ShelfType.WANT_TO_READ,
                        isRefreshing = false,
                        message = null,
                    ),
                    onBack = {},
                    onRetry = {},
                    onShelfSelected = {},
                    onRemoveFromLibrary = {},
                    onUndo = {},
                    onMessageShown = {},
                )
            }
        }
        onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun notInLibrary() = check(entry = null, dark = false)

    @Test
    fun onAShelf_dark() {
        val now = Instant.parse("2026-11-24T10:00:00Z")
        check(ShelfEntry(workKey = "OL1W", shelf = ShelfType.READING, addedAt = now, updatedAt = now), dark = true)
    }
}
