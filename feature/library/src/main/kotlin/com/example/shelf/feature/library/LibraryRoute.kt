package com.example.shelf.feature.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Entry point of the library feature: the shelves, or one book's page. */
@Composable
fun LibraryRoute(
    navKey: LibraryNavKey,
    onBookClick: (workKey: String) -> Unit,
    onOpenDetails: (workKey: String) -> Unit,
    onSearchClick: () -> Unit,
    onScanClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (navKey) {
        LibraryNavKey.Shelves -> ShelvesRoute(
            onBookClick = onBookClick,
            onSearchClick = onSearchClick,
            onScanClick = onScanClick,
            modifier = modifier,
        )

        is LibraryNavKey.Book -> LibraryBookRoute(
            navKey = navKey,
            onOpenDetails = onOpenDetails,
            onBack = onBack,
            modifier = modifier,
        )
    }
}

@Composable
internal fun ShelvesRoute(
    onBookClick: (workKey: String) -> Unit,
    onSearchClick: () -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LibraryScreen(
        uiState = uiState,
        onShelfSelected = viewModel::onShelfSelected,
        onBookClick = onBookClick,
        onRemove = viewModel::onRemove,
        onUndoRemove = viewModel::onUndoRemove,
        onMessageShown = viewModel::onMessageShown,
        onSearchClick = onSearchClick,
        onScanClick = onScanClick,
        modifier = modifier,
    )
}

@Composable
internal fun LibraryBookRoute(
    navKey: LibraryNavKey.Book,
    onOpenDetails: (workKey: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryBookViewModel = hiltViewModel<LibraryBookViewModel, LibraryBookViewModel.Factory>(
        creationCallback = { factory -> factory.create(navKey) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LibraryBookScreen(
        uiState = uiState,
        pageInput = viewModel.pageInput,
        notesInput = viewModel.notesInput,
        onPageInputChange = viewModel::onPageInputChange,
        onSavePage = viewModel::onSavePage,
        onShelfSelected = viewModel::onShelfSelected,
        onRatingChange = viewModel::onRatingChange,
        onRatingPromptSubmit = viewModel::onRatingPromptSubmit,
        onRatingPromptDismissed = viewModel::onRatingPromptDismissed,
        onNotesChange = viewModel::onNotesChange,
        onRemove = viewModel::onRemove,
        onMessageShown = viewModel::onMessageShown,
        onOpenDetails = { onOpenDetails(navKey.workKey) },
        onBack = onBack,
        modifier = modifier,
    )
}
