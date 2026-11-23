package com.example.shelf.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.shelf.core.model.Book
import com.example.shelf.core.model.ShelfBook
import com.example.shelf.core.model.ShelfEntry
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.ui.component.BookCover
import com.example.shelf.core.ui.component.EmptyState
import com.example.shelf.core.ui.component.LabeledExtendedFab
import com.example.shelf.core.ui.component.LoadingState
import com.example.shelf.core.ui.component.RatingDisplay
import com.example.shelf.core.ui.component.UserMessageEffect
import com.example.shelf.core.ui.icon.ShelfIcons
import com.example.shelf.core.ui.preview.ThemePreviews
import com.example.shelf.core.ui.text.labelRes
import com.example.shelf.core.ui.theme.ShelfTheme
import java.time.Instant
import com.example.shelf.core.ui.R as UiR

@Composable
internal fun LibraryScreen(
    uiState: LibraryUiState,
    onShelfSelected: (ShelfType) -> Unit,
    onBookClick: (workKey: String) -> Unit,
    onRemove: (ShelfBook) -> Unit,
    onUndoRemove: () -> Unit,
    onMessageShown: () -> Unit,
    onSearchClick: () -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    UserMessageEffect(
        message = (uiState as? LibraryUiState.Success)?.message,
        hostState = snackbarHostState,
        onShown = onMessageShown,
        onAction = onUndoRemove,
    )
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.library_title)) },
                actions = {
                    IconButton(onClick = onScanClick) {
                        Icon(ShelfIcons.Scan, contentDescription = stringResource(R.string.library_scan))
                    }
                },
            )
        },
        floatingActionButton = {
            LabeledExtendedFab(
                text = stringResource(R.string.library_add_book),
                icon = ShelfIcons.Add,
                onClick = onSearchClick,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (uiState) {
            LibraryUiState.Loading -> LoadingState(Modifier.padding(padding))

            is LibraryUiState.Success -> Column(modifier = Modifier.padding(padding)) {
                ShelfTabs(state = uiState, onShelfSelected = onShelfSelected)
                if (uiState.books.isEmpty()) {
                    ShelfEmptyState(shelf = uiState.selectedShelf, onSearchClick = onSearchClick)
                } else {
                    ShelfList(books = uiState.books, onBookClick = onBookClick, onRemove = onRemove)
                }
            }
        }
    }
}

@Composable
private fun ShelfTabs(state: LibraryUiState.Success, onShelfSelected: (ShelfType) -> Unit) {
    PrimaryTabRow(selectedTabIndex = state.selectedShelf.ordinal) {
        ShelfType.entries.forEach { shelf ->
            Tab(
                selected = shelf == state.selectedShelf,
                onClick = { onShelfSelected(shelf) },
                text = {
                    Text(
                        text = stringResource(
                            R.string.library_tab,
                            stringResource(shelf.labelRes),
                            state.shelves[shelf].orEmpty().size,
                        ),
                        textAlign = TextAlign.Center,
                    )
                },
            )
        }
    }
}

@Composable
private fun ShelfEmptyState(shelf: ShelfType, onSearchClick: () -> Unit) {
    val (title, message) = when (shelf) {
        ShelfType.WANT_TO_READ -> R.string.library_empty_want_title to R.string.library_empty_want_message
        ShelfType.READING -> R.string.library_empty_reading_title to R.string.library_empty_reading_message
        ShelfType.FINISHED -> R.string.library_empty_finished_title to R.string.library_empty_finished_message
    }
    EmptyState(
        title = stringResource(title),
        message = stringResource(message),
        actionLabel = stringResource(R.string.library_find_book),
        onAction = onSearchClick,
    )
}

@Composable
private fun ShelfList(books: List<ShelfBook>, onBookClick: (String) -> Unit, onRemove: (ShelfBook) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(LibraryTestTags.LIST),
        // Leaves room for the extended FAB over the last row.
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
    ) {
        items(books, key = { it.book.workKey }) { book ->
            // A fresh swipe state whenever the entry changes, so an undone removal is not still swiped.
            key(book.entry.updatedAt) {
                SwipeToRemove(onRemove = { onRemove(book) }) {
                    ShelfBookRow(book = book, onClick = {
                        onBookClick(book.book.workKey)
                    }, onRemove = { onRemove(book) })
                }
            }
        }
    }
}

@Composable
private fun SwipeToRemove(onRemove: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        onDismiss = { value -> if (value == SwipeToDismissBoxValue.EndToStart) onRemove() },
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = ShelfIcons.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        content()
    }
}

@Composable
private fun ShelfBookRow(book: ShelfBook, onClick: () -> Unit, onRemove: () -> Unit) {
    val removeLabel = stringResource(R.string.library_remove)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .heightIn(min = 88.dp)
            .clickable(onClick = onClick)
            // Swiping is not available to every user; offer removal as an accessibility action too.
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(removeLabel) {
                        onRemove()
                        true
                    },
                )
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCover(coverId = book.book.coverId, title = book.book.title, modifier = Modifier.width(56.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = book.book.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = book.book.authors.joinToString().ifEmpty { stringResource(UiR.string.ui_unknown_author) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            when (book.entry.shelf) {
                ShelfType.READING -> ReadingProgress(book)
                ShelfType.FINISHED -> RatingDisplay(rating = book.entry.rating)
                ShelfType.WANT_TO_READ -> Unit
            }
        }
    }
}

@Composable
private fun ReadingProgress(book: ShelfBook) {
    val pageCount = book.book.pageCount
    val progress = book.progress
    if (progress != null && pageCount != null) {
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Text(
            text = stringResource(R.string.library_page_of, book.entry.currentPage, pageCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Text(
            text = pluralStringResource(R.plurals.library_on_page, book.entry.currentPage, book.entry.currentPage),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@ThemePreviews
@Composable
private fun LibraryScreenPreview() {
    val books = listOf(
        ShelfBook(
            book = Book("OL1W", "Piranesi", listOf("Susanna Clarke"), pageCount = 272),
            entry = ShelfEntry(
                workKey = "OL1W",
                shelf = ShelfType.READING,
                currentPage = 120,
                addedAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ),
        ),
    )
    ShelfTheme(dynamicColor = false) {
        LibraryScreen(
            uiState = LibraryUiState.Success(
                selectedShelf = ShelfType.READING,
                shelves = mapOf(ShelfType.READING to books),
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
