package com.example.shelf.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.shelf.core.model.ShelfBook
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.ui.component.BookCover
import com.example.shelf.core.ui.component.LoadingState
import com.example.shelf.core.ui.component.RatingInput
import com.example.shelf.core.ui.component.UserMessageEffect
import com.example.shelf.core.ui.icon.ShelfIcons
import com.example.shelf.core.ui.text.labelRes
import com.example.shelf.core.ui.R as UiR

private const val PERCENT = 100

@Composable
internal fun LibraryBookScreen(
    uiState: LibraryBookUiState,
    pageInput: String,
    notesInput: String,
    onPageInputChange: (String) -> Unit,
    onSavePage: () -> Unit,
    onShelfSelected: (ShelfType) -> Unit,
    onRatingChange: (Int?) -> Unit,
    onRatingPromptSubmit: (Int?) -> Unit,
    onRatingPromptDismissed: () -> Unit,
    onNotesChange: (String) -> Unit,
    onRemove: () -> Unit,
    onMessageShown: () -> Unit,
    onOpenDetails: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Leaving is state-driven: once the book is gone from the library this screen closes itself.
    LaunchedEffect(uiState) {
        if (uiState == LibraryBookUiState.Closed) onBack()
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val success = uiState as? LibraryBookUiState.Success
    UserMessageEffect(message = success?.message, hostState = snackbarHostState, onShown = onMessageShown)
    var confirmRemoval by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = success?.shelfBook?.book?.title.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(ShelfIcons.Back, contentDescription = stringResource(UiR.string.ui_navigate_up))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (success == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LibraryBookContent(
                shelfBook = success.shelfBook,
                pageInput = pageInput,
                notesInput = notesInput,
                onPageInputChange = onPageInputChange,
                onSavePage = onSavePage,
                onShelfSelected = onShelfSelected,
                onRatingChange = onRatingChange,
                onNotesChange = onNotesChange,
                onOpenDetails = onOpenDetails,
                onRemoveClick = { confirmRemoval = true },
            )
        }
        if (success.showRatingPrompt) {
            RatingPrompt(
                title = success.shelfBook.book.title,
                onSubmit = onRatingPromptSubmit,
                onDismiss = onRatingPromptDismissed,
            )
        }
        if (confirmRemoval) {
            AlertDialog(
                onDismissRequest = { confirmRemoval = false },
                title = { Text(stringResource(R.string.library_remove_title)) },
                text = { Text(stringResource(R.string.library_remove_message, success.shelfBook.book.title)) },
                confirmButton = {
                    TextButton(onClick = {
                        confirmRemoval = false
                        onRemove()
                    }) { Text(stringResource(R.string.library_remove)) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmRemoval = false }) { Text(stringResource(UiR.string.ui_cancel)) }
                },
            )
        }
    }
}

@Composable
private fun LibraryBookContent(
    shelfBook: ShelfBook,
    pageInput: String,
    notesInput: String,
    onPageInputChange: (String) -> Unit,
    onSavePage: () -> Unit,
    onShelfSelected: (ShelfType) -> Unit,
    onRatingChange: (Int?) -> Unit,
    onNotesChange: (String) -> Unit,
    onOpenDetails: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BookCover(coverId = shelfBook.book.coverId, title = shelfBook.book.title, modifier = Modifier.width(88.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(shelfBook.book.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = shelfBook.book.authors.joinToString().ifEmpty {
                        stringResource(UiR.string.ui_unknown_author)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onOpenDetails) { Text(stringResource(R.string.library_view_details)) }
            }
        }

        Section(title = stringResource(R.string.library_section_shelf)) {
            ShelfChooser(selected = shelfBook.entry.shelf, onShelfSelected = onShelfSelected)
        }

        Section(title = stringResource(R.string.library_section_progress)) {
            ProgressEditor(
                shelfBook = shelfBook,
                pageInput = pageInput,
                onPageInputChange = onPageInputChange,
                onSavePage = onSavePage,
            )
        }

        Section(title = stringResource(UiR.string.ui_rating_label)) {
            RatingInput(rating = shelfBook.entry.rating, onRatingChange = onRatingChange)
        }

        Section(title = stringResource(R.string.library_section_notes)) {
            OutlinedTextField(
                value = notesInput,
                onValueChange = onNotesChange,
                label = { Text(stringResource(R.string.library_notes_label)) },
                supportingText = { Text(stringResource(R.string.library_notes_hint)) },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        OutlinedButton(onClick = onRemoveClick) {
            Icon(ShelfIcons.Delete, contentDescription = null)
            Text(stringResource(R.string.library_remove), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/** Shelf picker as wrapping chips (they never truncate at large font scales), exposed as radio buttons. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShelfChooser(selected: ShelfType, onShelfSelected: (ShelfType) -> Unit) {
    FlowRow(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ShelfType.entries.forEach { shelf ->
            FilterChip(
                selected = shelf == selected,
                onClick = { onShelfSelected(shelf) },
                label = { Text(stringResource(shelf.labelRes)) },
                leadingIcon = if (shelf == selected) {
                    { Icon(ShelfIcons.Check, contentDescription = null) }
                } else {
                    null
                },
                modifier = Modifier.semantics { role = Role.RadioButton },
            )
        }
    }
}

@Composable
private fun ProgressEditor(
    shelfBook: ShelfBook,
    pageInput: String,
    onPageInputChange: (String) -> Unit,
    onSavePage: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val pageCount = shelfBook.book.pageCount
    val progress = shelfBook.progress
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (progress != null && pageCount != null) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Text(
                text = stringResource(
                    R.string.library_progress_summary,
                    shelfBook.entry.currentPage,
                    pageCount,
                    (progress * PERCENT).toInt(),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = pageInput,
                onValueChange = onPageInputChange,
                label = { Text(stringResource(R.string.library_current_page)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    onSavePage()
                    focusManager.clearFocus()
                }),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onSavePage, enabled = pageInput.isNotEmpty()) {
                Text(stringResource(R.string.library_update_page))
            }
        }
    }
}

@Composable
private fun RatingPrompt(title: String, onSubmit: (Int?) -> Unit, onDismiss: () -> Unit) {
    var rating by rememberSaveable { mutableStateOf<Int?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.library_rate_title, title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.library_rate_message))
                RatingInput(rating = rating, onRatingChange = { rating = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSubmit(rating)
            }, enabled = rating != null) { Text(stringResource(UiR.string.ui_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.library_rate_later)) }
        },
    )
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}
