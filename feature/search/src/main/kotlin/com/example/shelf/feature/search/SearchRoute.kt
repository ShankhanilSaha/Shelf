package com.example.shelf.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems

/** Entry point of the search feature. */
@Composable
fun SearchRoute(
    onBookClick: (workKey: String) -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val results = viewModel.results.collectAsLazyPagingItems()
    SearchScreen(
        query = viewModel.query,
        uiState = uiState,
        results = results,
        onQueryChange = viewModel::onQueryChange,
        onSubmit = viewModel::onSubmit,
        onClearQuery = viewModel::onClearQuery,
        onRecentSearchClick = viewModel::onRecentSearchClick,
        onClearRecentSearches = viewModel::onClearRecentSearches,
        onResultClick = { book ->
            viewModel.onResultClick(book)
            onBookClick(book.workKey)
        },
        onScanClick = onScanClick,
        modifier = modifier,
    )
}
