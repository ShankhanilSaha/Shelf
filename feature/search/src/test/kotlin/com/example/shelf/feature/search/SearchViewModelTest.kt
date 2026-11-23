package com.example.shelf.feature.search

import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import com.example.shelf.core.testing.MainDispatcherRule
import com.example.shelf.core.testing.TestData
import com.example.shelf.core.testing.repository.FakeBookRepository
import com.example.shelf.core.testing.repository.FakeSearchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private val repository = FakeSearchRepository(
        resultsFor = { query ->
            listOf(TestData.summary(1, title = "$query one"), TestData.summary(2, title = "$query two"))
        },
    )

    private val bookRepository = FakeBookRepository()

    // Created after the rule has installed the test Main dispatcher (field initializers run before rules).
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        viewModel = SearchViewModel(repository, bookRepository)
    }

    /** Keeps both outputs subscribed, as the screen does. */
    private fun TestScope.collectOutputs() {
        backgroundScope.launch { viewModel.uiState.collect {} }
        backgroundScope.launch { viewModel.results.collect {} }
        runCurrent()
    }

    @Test
    fun `starts idle and shows recent searches`() = runTest(dispatcher) {
        repository.setRecentSearches(listOf("dune", "piranesi"))

        viewModel.uiState.test {
            assertEquals(SearchUiState.Idle(emptyList()), awaitItem())
            assertEquals(SearchUiState.Idle(listOf("dune", "piranesi")), awaitItem())
        }
    }

    @Test
    fun `typing is debounced for 400 ms and searches only the final query`() = runTest(dispatcher) {
        collectOutputs()

        listOf("d", "du", "dun", "dune").forEach(viewModel::onQueryChange)
        advanceTimeBy(SearchViewModel.DEBOUNCE_MILLIS - 1)
        runCurrent()
        assertEquals(SearchUiState.Idle(emptyList()), viewModel.uiState.value)
        assertEquals(emptyList(), repository.searchedQueries)

        advanceTimeBy(2)
        runCurrent()
        assertEquals(SearchUiState.Results("dune"), viewModel.uiState.value)
        assertEquals(listOf("dune"), repository.searchedQueries)
    }

    @Test
    fun `whitespace is trimmed before searching`() = runTest(dispatcher) {
        collectOutputs()

        viewModel.onQueryChange("   dune  ")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MILLIS + 1)
        runCurrent()

        assertEquals(SearchUiState.Results("dune"), viewModel.uiState.value)
        assertEquals("   dune  ", viewModel.query)
        assertEquals(listOf("dune"), repository.searchedQueries)
    }

    @Test
    fun `fewer than two characters never search`() = runTest(dispatcher) {
        collectOutputs()

        viewModel.onQueryChange(" d ")
        viewModel.onSubmit()
        advanceTimeBy(SearchViewModel.DEBOUNCE_MILLIS * 2)
        runCurrent()

        assertEquals(SearchUiState.Idle(emptyList()), viewModel.uiState.value)
        assertEquals(emptyList(), repository.searchedQueries)
    }

    @Test
    fun `clearing the field returns to idle without waiting`() = runTest(dispatcher) {
        collectOutputs()
        viewModel.onQueryChange("dune")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MILLIS + 1)
        runCurrent()

        viewModel.onClearQuery()
        runCurrent()

        assertEquals("", viewModel.query)
        assertEquals(SearchUiState.Idle(emptyList()), viewModel.uiState.value)
    }

    @Test
    fun `submitting searches immediately and remembers the query`() = runTest(dispatcher) {
        collectOutputs()

        viewModel.onQueryChange("dune")
        viewModel.onSubmit()
        runCurrent()

        assertEquals(SearchUiState.Results("dune"), viewModel.uiState.value)
        assertEquals(listOf("dune"), repository.recentSearches.first())

        // The debounced value arriving later is the same query, so no second request is made.
        advanceTimeBy(SearchViewModel.DEBOUNCE_MILLIS + 1)
        runCurrent()
        assertEquals(listOf("dune"), repository.searchedQueries)
    }

    @Test
    fun `a new query cancels the previous search`() = runTest(dispatcher) {
        val cancelled = mutableListOf<String>()
        repository.pagingFlowFor = { query ->
            flow {
                emit(PagingData.from(listOf(TestData.summary(1, title = query))))
                try {
                    awaitCancellation()
                } finally {
                    cancelled += query
                }
            }
        }
        collectOutputs()

        viewModel.onQueryChange("dune")
        viewModel.onSubmit()
        runCurrent()
        viewModel.onQueryChange("piranesi")
        viewModel.onSubmit()
        runCurrent()

        assertEquals(listOf("dune", "piranesi"), repository.searchedQueries)
        assertEquals(listOf("dune"), cancelled)
    }

    @Test
    fun `results come from the repository for the active query`() = runTest(dispatcher) {
        collectOutputs()
        viewModel.onQueryChange("dune")
        viewModel.onSubmit()
        runCurrent()

        val titles = viewModel.results.asSnapshot().map { it.title }

        assertEquals(listOf("dune one", "dune two"), titles)
    }

    @Test
    fun `tapping a recent search runs it`() = runTest(dispatcher) {
        collectOutputs()

        viewModel.onRecentSearchClick("piranesi")
        runCurrent()

        assertEquals("piranesi", viewModel.query)
        assertEquals(SearchUiState.Results("piranesi"), viewModel.uiState.value)
    }

    @Test
    fun `opening a result remembers the query and recents can be cleared`() = runTest(dispatcher) {
        collectOutputs()
        viewModel.onQueryChange("dune")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MILLIS + 1)
        runCurrent()

        viewModel.onResultClick(TestData.summary(7))
        runCurrent()
        assertEquals(listOf("dune"), repository.recentSearches.first())
        assertEquals(listOf("OL7W"), bookRepository.cachedSummaries.map { it.workKey })

        viewModel.onClearRecentSearches()
        runCurrent()
        assertEquals(emptyList(), repository.recentSearches.first())
    }
}
