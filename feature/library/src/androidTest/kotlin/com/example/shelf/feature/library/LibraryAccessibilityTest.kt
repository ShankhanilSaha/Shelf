package com.example.shelf.feature.library

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.shelf.core.model.Book
import com.example.shelf.core.model.ShelfBook
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
class LibraryAccessibilityTest {
    private val now = Instant.parse("2026-11-24T10:00:00Z")

    private fun book(id: Int, title: String, pages: Int?, page: Int) = ShelfBook(
        book = Book(
            workKey = "OL${id}W",
            title = title,
            authors = listOf("Author $id"),
            pageCount = pages,
            hasDetails = true,
        ),
        entry = ShelfEntry(
            workKey = "OL${id}W",
            shelf = ShelfType.READING,
            currentPage = page,
            addedAt = now,
            updatedAt = now,
        ),
    )

    private fun check(state: LibraryUiState, dark: Boolean) = runComposeUiTest {
        enableAccessibilityChecks()
        setContent {
            ShelfTheme(darkTheme = dark, dynamicColor = false) {
                LibraryScreen(
                    uiState = state,
                    onShelfSelected = {},
                    onBookClick = {},
                    onRemove = {},
                    onUndoRemove = {},
                    onMessageShown = {},
                    onSearchClick = {},
                    onScanClick = {},
                )
            }
        }
        onRoot().tryPerformAccessibilityChecks()
    }

    private val readingShelf = LibraryUiState.Success(
        selectedShelf = ShelfType.READING,
        shelves = mapOf(
            ShelfType.WANT_TO_READ to emptyList(),
            ShelfType.READING to listOf(book(1, "Piranesi", 272, 30), book(2, "Dune", null, 12)),
            ShelfType.FINISHED to emptyList(),
        ),
        message = null,
    )

    @Test
    fun readingShelf_light() = check(readingShelf, dark = false)

    @Test
    fun readingShelf_dark() = check(readingShelf, dark = true)

    @Test
    fun emptyLibrary() = check(
        LibraryUiState.Success(ShelfType.WANT_TO_READ, ShelfType.entries.associateWith { emptyList() }, null),
        dark = false,
    )
}
