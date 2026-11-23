package com.example.shelf.feature.search

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.shelf.core.common.result.shelfError
import com.example.shelf.core.model.BookSummary
import com.example.shelf.core.ui.component.BookCover
import com.example.shelf.core.ui.component.EmptyState
import com.example.shelf.core.ui.component.ErrorState
import com.example.shelf.core.ui.component.LoadingState
import com.example.shelf.core.ui.icon.ShelfIcons
import com.example.shelf.core.ui.preview.ThemePreviews
import com.example.shelf.core.ui.text.asString
import com.example.shelf.core.ui.text.toUiText
import com.example.shelf.core.ui.theme.ShelfTheme
import kotlinx.coroutines.flow.flowOf

@Composable
internal fun SearchScreen(
    query: String,
    uiState: SearchUiState,
    results: LazyPagingItems<BookSummary>,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClearQuery: () -> Unit,
    onRecentSearchClick: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onResultClick: (BookSummary) -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            SearchBar(
                query = query,
                onQueryChange = onQueryChange,
                onSubmit = onSubmit,
                onClearQuery = onClearQuery,
                onScanClick = onScanClick,
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (uiState) {
                is SearchUiState.Idle -> IdleContent(
                    recentSearches = uiState.recentSearches,
                    onRecentSearchClick = onRecentSearchClick,
                    onClearRecentSearches = onClearRecentSearches,
                    onScanClick = onScanClick,
                )

                is SearchUiState.Results -> ResultsContent(
                    query = uiState.query,
                    results = results,
                    onResultClick = onResultClick,
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClearQuery: () -> Unit,
    onScanClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.search_field_label)) },
                leadingIcon = { Icon(ShelfIcons.SearchOutlined, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClearQuery) {
                            Icon(ShelfIcons.Clear, contentDescription = stringResource(R.string.search_clear_query))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSubmit()
                        focusManager.clearFocus()
                    },
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag(SearchTestTags.FIELD),
            )
            IconButton(onClick = onScanClick) {
                Icon(ShelfIcons.Scan, contentDescription = stringResource(R.string.search_scan))
            }
        }
    }
}

@Composable
private fun IdleContent(
    recentSearches: List<String>,
    onRecentSearchClick: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onScanClick: () -> Unit,
) {
    if (recentSearches.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.search_idle_title),
            message = stringResource(R.string.search_idle_message),
            actionLabel = stringResource(R.string.search_scan),
            onAction = onScanClick,
        )
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.search_recent_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                TextButton(onClick = onClearRecentSearches) {
                    Text(stringResource(R.string.search_recent_clear))
                }
            }
        }
        items(recentSearches) { recent ->
            ListItem(
                onClick = { onRecentSearchClick(recent) },
                leadingContent = { Icon(ShelfIcons.History, contentDescription = null) },
            ) {
                Text(recent, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ResultsContent(query: String, results: LazyPagingItems<BookSummary>, onResultClick: (BookSummary) -> Unit) {
    val refresh = results.loadState.refresh
    when {
        refresh is LoadState.Loading && results.itemCount == 0 -> LoadingState()

        refresh is LoadState.Error && results.itemCount == 0 -> ErrorState(
            message = refresh.error.shelfError.toUiText().asString(),
            onRetry = results::retry,
        )

        refresh is LoadState.NotLoading && results.itemCount == 0 -> EmptyState(
            title = stringResource(R.string.search_empty_title),
            message = stringResource(R.string.search_empty_message, query),
        )

        else -> LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(SearchTestTags.RESULTS),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(count = results.itemCount) { index ->
                results[index]?.let { book ->
                    SearchResultRow(book = book, onClick = { onResultClick(book) })
                }
            }
            item { AppendFooter(append = results.loadState.append, onRetry = results::retry) }
        }
    }
}

@Composable
private fun SearchResultRow(book: BookSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCover(coverId = book.coverId, title = book.title, modifier = Modifier.width(48.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = book.authors.joinToString().ifEmpty { stringResource(R.string.search_unknown_author) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            book.firstPublishYear?.let { year ->
                Text(
                    text = stringResource(R.string.search_first_published, year),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AppendFooter(append: LoadState, onRetry: () -> Unit) {
    when (append) {
        is LoadState.Loading -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        is LoadState.Error -> Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.search_append_error),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.search_retry))
                }
            }
        }

        is LoadState.NotLoading -> Unit
    }
}

/** Placeholder results while idle; reports Loading so a new query never flashes "no results". */
internal fun idlePagingData(): PagingData<BookSummary> = PagingData.empty(
    LoadStates(
        refresh = LoadState.Loading,
        prepend = LoadState.NotLoading(endOfPaginationReached = true),
        append = LoadState.NotLoading(endOfPaginationReached = true),
    ),
)

@ThemePreviews
@Composable
private fun SearchResultsPreview() {
    val books = List(4) { index ->
        BookSummary(
            workKey = "OL${index + 1}W",
            title = listOf("The Hobbit", "Dune", "Piranesi", "The Left Hand of Darkness")[index],
            authors = listOf(
                listOf("J.R.R. Tolkien"),
                listOf("Frank Herbert"),
                listOf("Susanna Clarke"),
                listOf("Ursula K. Le Guin"),
            )[index],
            firstPublishYear = listOf(1937, 1965, 2020, 1969)[index],
            coverId = null,
            isbn = null,
            pageCount = null,
        )
    }
    ShelfTheme(dynamicColor = false) {
        SearchScreen(
            query = "fantasy",
            uiState = SearchUiState.Results("fantasy"),
            results = flowOf(PagingData.from(books)).collectAsLazyPagingItems(),
            onQueryChange = {},
            onSubmit = {},
            onClearQuery = {},
            onRecentSearchClick = {},
            onClearRecentSearches = {},
            onResultClick = {},
            onScanClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchIdlePreview() {
    ShelfTheme(dynamicColor = false) {
        SearchScreen(
            query = "",
            uiState = SearchUiState.Idle(listOf("dune", "le guin", "piranesi")),
            results = flowOf(idlePagingData()).collectAsLazyPagingItems(),
            onQueryChange = {},
            onSubmit = {},
            onClearQuery = {},
            onRecentSearchClick = {},
            onClearRecentSearches = {},
            onResultClick = {},
            onScanClick = {},
        )
    }
}
