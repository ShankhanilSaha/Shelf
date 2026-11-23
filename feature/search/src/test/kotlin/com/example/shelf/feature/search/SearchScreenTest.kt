package com.example.shelf.feature.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.shelf.core.common.result.ShelfError
import com.example.shelf.core.common.result.ShelfException
import com.example.shelf.core.model.BookSummary
import com.example.shelf.core.testing.MainDispatcherRule
import com.example.shelf.core.testing.TestData
import com.example.shelf.core.testing.repository.FakeBookRepository
import com.example.shelf.core.testing.repository.FakeSearchRepository
import com.example.shelf.core.ui.theme.ShelfTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class SearchScreenTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val composeRule = createComposeRule()

    private fun string(id: Int, vararg args: Any) = RuntimeEnvironment.getApplication().getString(id, *args)

    @Test
    fun searchFlow_withFakeRepository_showsResultsAndOpensTheTappedBook() {
        val repository = FakeSearchRepository(
            resultsFor = { listOf(TestData.summary(1, title = "Dune"), TestData.summary(2, title = "Dune Messiah")) },
        )
        var openedWorkKey: String? = null
        val viewModel = SearchViewModel(repository, FakeBookRepository())
        composeRule.setContent {
            ShelfTheme(dynamicColor = false) {
                SearchRoute(
                    onBookClick = { openedWorkKey = it },
                    onScanClick = {},
                    viewModel = viewModel,
                )
            }
        }

        composeRule.onNodeWithTag(SearchTestTags.FIELD).performTextInput("dune")
        composeRule.onNodeWithTag(SearchTestTags.FIELD).performImeAction()
        composeRule.waitUntil { composeRule.onAllNodesWithText("Dune Messiah").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Dune Messiah").performClick()

        assertEquals("OL2W", openedWorkKey)
        assertEquals(listOf("dune"), repository.searchedQueries)
    }

    @Test
    fun idle_showsRecentSearchesAndRunsTheTappedOne() {
        var tapped: String? = null
        var cleared = false
        setScreen(
            uiState = SearchUiState.Idle(listOf("piranesi", "le guin")),
            onRecentSearchClick = { tapped = it },
            onClearRecentSearches = { cleared = true },
        )

        composeRule.onNodeWithText(string(R.string.search_recent_title)).assertIsDisplayed()
        composeRule.onNodeWithText("le guin").performClick()
        composeRule.onNodeWithText(string(R.string.search_recent_clear)).performClick()

        assertEquals("le guin", tapped)
        assertEquals(true, cleared)
    }

    @Test
    fun idle_withoutRecents_showsTheEmptyStateWithScanAction() {
        var scanned = false
        setScreen(uiState = SearchUiState.Idle(emptyList()), onScanClick = { scanned = true })

        composeRule.onNodeWithText(string(R.string.search_idle_title)).assertIsDisplayed()
        composeRule.onAllNodesWithText(string(R.string.search_scan))[0].performClick()

        assertEquals(true, scanned)
    }

    @Test
    fun refreshError_showsTheMessageAndRetries() {
        setScreen(
            uiState = SearchUiState.Results("dune"),
            pagingData = PagingData.from(
                emptyList(),
                sourceLoadStates = loadStates(refresh = LoadState.Error(ShelfException(ShelfError.Network))),
            ),
        )

        composeRule.onNodeWithText(string(com.example.shelf.core.ui.R.string.ui_error_network)).assertIsDisplayed()
        composeRule.onNodeWithText(string(com.example.shelf.core.ui.R.string.ui_retry)).assertIsDisplayed()
    }

    @Test
    fun noResults_showsTheEmptyState() {
        setScreen(
            uiState = SearchUiState.Results("zzzz"),
            pagingData = PagingData.from(
                emptyList(),
                sourceLoadStates = loadStates(refresh = LoadState.NotLoading(true)),
            ),
        )

        composeRule.onNodeWithText(string(R.string.search_empty_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.search_empty_message, "zzzz")).assertIsDisplayed()
    }

    @Test
    fun appendError_showsAFooterWithRetry() {
        setScreen(
            uiState = SearchUiState.Results("dune"),
            pagingData = PagingData.from(
                listOf(TestData.summary(1, title = "Dune")),
                sourceLoadStates = loadStates(
                    refresh = LoadState.NotLoading(false),
                    append = LoadState.Error(ShelfException(ShelfError.Server)),
                ),
            ),
        )

        composeRule.onNodeWithText("Dune").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.search_append_error)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.search_retry)).assertIsDisplayed()
    }

    private fun loadStates(
        refresh: LoadState,
        append: LoadState = LoadState.NotLoading(endOfPaginationReached = true),
    ) = LoadStates(refresh = refresh, prepend = LoadState.NotLoading(endOfPaginationReached = true), append = append)

    private fun setScreen(
        uiState: SearchUiState,
        pagingData: PagingData<BookSummary> = PagingData.empty(),
        onRecentSearchClick: (String) -> Unit = {},
        onClearRecentSearches: () -> Unit = {},
        onScanClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            ShelfTheme(dynamicColor = false) {
                SearchScreen(
                    query = "",
                    uiState = uiState,
                    results = flowOf(pagingData).collectAsLazyPagingItems(),
                    onQueryChange = {},
                    onSubmit = {},
                    onClearQuery = {},
                    onRecentSearchClick = onRecentSearchClick,
                    onClearRecentSearches = onClearRecentSearches,
                    onResultClick = {},
                    onScanClick = onScanClick,
                )
            }
        }
    }
}
