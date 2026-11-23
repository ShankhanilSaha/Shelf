package com.example.shelf.feature.library

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import com.example.shelf.core.model.Book
import com.example.shelf.core.model.ShelfBook
import com.example.shelf.core.model.ShelfEntry
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.ui.preview.ScreenPreviews
import com.example.shelf.core.ui.theme.ShelfTheme
import java.time.Instant

private val now = Instant.parse("2026-11-24T10:00:00Z")

private fun book(id: Int, title: String, author: String, pages: Int?, page: Int, shelf: ShelfType) = ShelfBook(
    book = Book(workKey = "OL${id}W", title = title, authors = listOf(author), pageCount = pages, hasDetails = true),
    entry = ShelfEntry(workKey = "OL${id}W", shelf = shelf, currentPage = page, addedAt = now, updatedAt = now),
)

private val reading = listOf(
    book(1, "The Left Hand of Darkness", "Ursula K. Le Guin", pages = 304, page = 212, ShelfType.READING),
    book(
        2,
        "A Very Long Title About Reading Slowly and Deliberately Through a Winter",
        "An Author",
        512,
        40,
        ShelfType.READING,
    ),
    book(3, "Piranesi", "Susanna Clarke", pages = null, page = 30, ShelfType.READING),
)

@PreviewTest
@ScreenPreviews
@Composable
fun LibraryReadingShelf() {
    ShelfTheme(dynamicColor = false) {
        LibraryScreen(
            uiState = LibraryUiState.Success(
                selectedShelf = ShelfType.READING,
                shelves = mapOf(
                    ShelfType.WANT_TO_READ to emptyList(),
                    ShelfType.READING to reading,
                    ShelfType.FINISHED to emptyList(),
                ),
                message = null,
            ),
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

@PreviewTest
@ScreenPreviews
@Composable
fun LibraryEmpty() {
    ShelfTheme(dynamicColor = false) {
        LibraryScreen(
            uiState = LibraryUiState.Success(
                selectedShelf = ShelfType.WANT_TO_READ,
                shelves = ShelfType.entries.associateWith { emptyList() },
                message = null,
            ),
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
