package com.example.shelf.feature.bookdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Entry point of the book detail feature. */
@Composable
fun BookDetailRoute(
    navKey: BookDetailNavKey,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookDetailViewModel = hiltViewModel<BookDetailViewModel, BookDetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(navKey) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BookDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::refresh,
        onShelfSelected = viewModel::onShelfSelected,
        onRemoveFromLibrary = viewModel::onRemoveFromLibrary,
        onUndo = viewModel::onUndo,
        onMessageShown = viewModel::onMessageShown,
        modifier = modifier,
    )
}
