package com.example.shelf.feature.bookdetail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.shelf.core.model.Book
import com.example.shelf.core.model.CoverSize
import com.example.shelf.core.model.ShelfEntry
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.ui.component.BookCover
import com.example.shelf.core.ui.component.ErrorState
import com.example.shelf.core.ui.component.LoadingState
import com.example.shelf.core.ui.component.UserMessage
import com.example.shelf.core.ui.component.UserMessageEffect
import com.example.shelf.core.ui.icon.ShelfIcons
import com.example.shelf.core.ui.preview.ThemePreviews
import com.example.shelf.core.ui.text.asString
import com.example.shelf.core.ui.text.labelRes
import com.example.shelf.core.ui.theme.ShelfTheme
import com.example.shelf.core.ui.R as UiR

private const val COLLAPSED_DESCRIPTION_LINES = 6

@Composable
internal fun BookDetailScreen(
    uiState: BookDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onShelfSelected: (ShelfType) -> Unit,
    onRemoveFromLibrary: () -> Unit,
    onUndo: () -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    UserMessageEffect(
        message = (uiState as? BookDetailUiState.Success)?.message,
        hostState = snackbarHostState,
        onShown = onMessageShown,
        onAction = onUndo,
    )
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(ShelfIcons.Back, contentDescription = stringResource(UiR.string.ui_navigate_up))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (uiState) {
            BookDetailUiState.Loading -> LoadingState(Modifier.padding(padding))

            is BookDetailUiState.Error -> ErrorState(
                message = uiState.message.asString(),
                onRetry = onRetry,
                modifier = Modifier.padding(padding),
            )

            is BookDetailUiState.Success -> BookDetailContent(
                state = uiState,
                contentPadding = padding,
                onShelfSelected = onShelfSelected,
                onRemoveFromLibrary = onRemoveFromLibrary,
            )
        }
    }
}

@Composable
private fun BookDetailContent(
    state: BookDetailUiState.Success,
    contentPadding: PaddingValues,
    onShelfSelected: (ShelfType) -> Unit,
    onRemoveFromLibrary: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { BookHeader(book = state.book) }
            item {
                ShelfSplitButton(
                    entry = state.entry,
                    defaultShelf = state.defaultShelf,
                    onShelfSelected = onShelfSelected,
                    onRemoveFromLibrary = onRemoveFromLibrary,
                )
            }
            if (state.isRefreshing && !state.book.hasDetails) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }
            state.book.description?.let { description ->
                item { DescriptionSection(description = description) }
            }
            if (state.book.subjects.isNotEmpty()) {
                item { SubjectsSection(subjects = state.book.subjects) }
            }
            if (state.book.hasDetails && state.book.description == null && state.book.subjects.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.bookdetail_no_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun BookHeader(book: Book) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        BookCover(
            coverId = book.coverId,
            title = book.title,
            size = CoverSize.LARGE,
            modifier = Modifier.width(120.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = book.authors.joinToString().ifEmpty { stringResource(UiR.string.ui_unknown_author) },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            book.firstPublishYear?.let { year ->
                Text(
                    text = stringResource(UiR.string.ui_published_in, year),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            book.pageCount?.let { pages ->
                Text(
                    text = pluralStringResource(UiR.plurals.ui_page_count, pages, pages),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Split button: the leading half adds the book to the default shelf in one tap (or, for a book
 * already in the library, shows its shelf and opens the menu); the trailing half opens the menu
 * of all shelves plus "Remove from library".
 */
@Composable
private fun ShelfSplitButton(
    entry: ShelfEntry?,
    defaultShelf: ShelfType,
    onShelfSelected: (ShelfType) -> Unit,
    onRemoveFromLibrary: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val chooseShelf = stringResource(R.string.bookdetail_choose_shelf)
    Box {
        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.LeadingButton(
                    onClick = { if (entry == null) onShelfSelected(defaultShelf) else menuExpanded = true },
                ) {
                    Icon(
                        imageVector = if (entry == null) ShelfIcons.Add else ShelfIcons.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (entry == null) {
                            stringResource(R.string.bookdetail_add_to, stringResource(defaultShelf.labelRes))
                        } else {
                            stringResource(R.string.bookdetail_on_shelf, stringResource(entry.shelf.labelRes))
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            trailingButton = {
                SplitButtonDefaults.TrailingButton(
                    checked = menuExpanded,
                    onCheckedChange = { menuExpanded = it },
                    modifier = Modifier.semantics { contentDescription = chooseShelf },
                ) {
                    Icon(
                        imageVector = ShelfIcons.Dropdown,
                        contentDescription = null,
                        modifier = Modifier
                            .size(SplitButtonDefaults.TrailingIconSize)
                            .rotate(if (menuExpanded) 180f else 0f),
                    )
                }
            },
        )
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            ShelfType.entries.forEach { shelf ->
                val current = entry?.shelf == shelf
                DropdownMenuItem(
                    text = { Text(stringResource(shelf.labelRes)) },
                    leadingIcon = { if (current) Icon(ShelfIcons.Check, contentDescription = null) },
                    enabled = !current,
                    onClick = {
                        menuExpanded = false
                        onShelfSelected(shelf)
                    },
                )
            }
            if (entry != null) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.bookdetail_remove)) },
                    leadingIcon = { Icon(ShelfIcons.Delete, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onRemoveFromLibrary()
                    },
                )
            }
        }
    }
}

@Composable
private fun DescriptionSection(description: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.animateContentSize()) {
        SectionTitle(stringResource(R.string.bookdetail_about))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_DESCRIPTION_LINES,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = { expanded = !expanded }) {
            Text(stringResource(if (expanded) R.string.bookdetail_show_less else R.string.bookdetail_show_more))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectsSection(subjects: List<String>) {
    Column {
        SectionTitle(stringResource(R.string.bookdetail_subjects))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            subjects.forEach { subject ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Text(
                        text = subject,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .padding(bottom = 8.dp)
            .semantics { heading() },
    )
}

@ThemePreviews
@Composable
private fun BookDetailPreview() {
    ShelfTheme(dynamicColor = false) {
        BookDetailScreen(
            uiState = BookDetailUiState.Success(
                book = Book(
                    workKey = "OL27482W",
                    title = "The Hobbit",
                    authors = listOf("J.R.R. Tolkien"),
                    firstPublishYear = 1937,
                    description = "Bilbo Baggins is a hobbit who enjoys a comfortable, unambitious life, " +
                        "rarely travelling further than the pantry of his hobbit-hole.",
                    subjects = listOf("Fantasy", "Dragons", "Adventure"),
                    pageCount = 310,
                    hasDetails = true,
                ),
                entry = null,
                defaultShelf = ShelfType.WANT_TO_READ,
                isRefreshing = false,
                message = null as UserMessage?,
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
