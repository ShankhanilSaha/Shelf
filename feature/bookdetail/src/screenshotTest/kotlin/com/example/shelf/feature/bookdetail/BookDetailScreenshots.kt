package com.example.shelf.feature.bookdetail

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import com.example.shelf.core.model.Book
import com.example.shelf.core.model.ShelfEntry
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.ui.preview.ScreenPreviews
import com.example.shelf.core.ui.theme.ShelfTheme
import java.time.Instant

private val book = Book(
    workKey = "OL27513W",
    title = "The Fellowship of the Ring",
    authors = listOf("J.R.R. Tolkien"),
    firstPublishYear = 1954,
    description = "In a sleepy village in the Shire, young Frodo Baggins finds himself faced with an immense " +
        "task, as his elderly cousin Bilbo entrusts the Ring to his care.",
    subjects = listOf("Fantasy fiction", "Elves", "Dwarves", "Middle Earth (Imaginary place)", "Quests"),
    pageCount = 538,
    hasDetails = true,
)

@PreviewTest
@ScreenPreviews
@Composable
fun BookDetailNotInLibrary() {
    ShelfTheme(dynamicColor = false) {
        BookDetailScreen(
            uiState = BookDetailUiState.Success(
                book = book,
                entry = null,
                defaultShelf = ShelfType.WANT_TO_READ,
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

@PreviewTest
@ScreenPreviews
@Composable
fun BookDetailOnReadingShelf() {
    val now = Instant.parse("2026-11-24T10:00:00Z")
    ShelfTheme(dynamicColor = false) {
        BookDetailScreen(
            uiState = BookDetailUiState.Success(
                book = book,
                entry = ShelfEntry(
                    workKey = book.workKey,
                    shelf = ShelfType.READING,
                    currentPage = 120,
                    addedAt = now,
                    updatedAt = now,
                ),
                defaultShelf = ShelfType.WANT_TO_READ,
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
