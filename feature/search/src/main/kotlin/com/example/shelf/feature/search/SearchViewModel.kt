package com.example.shelf.feature.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.shelf.core.data.repository.BookRepository
import com.example.shelf.core.data.repository.SearchRepository
import com.example.shelf.core.model.BookSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val bookRepository: BookRepository,
) : ViewModel() {

    /**
     * The text field's content. Kept as Compose snapshot state rather than a Flow so the field
     * updates synchronously while typing (Google's guidance for TextField state in a ViewModel).
     */
    var query by mutableStateOf("")
        private set

    private val typedQueries = MutableStateFlow("")
    private val submittedQueries = MutableSharedFlow<String>(extraBufferCapacity = 1)

    /**
     * The query actually searched for: trimmed, at least [MIN_QUERY_LENGTH] characters, and
     * debounced by [DEBOUNCE_MILLIS] while typing. Submitting from the keyboard skips the wait;
     * clearing the field returns to the idle state immediately.
     */
    private val activeQuery: StateFlow<String?> = merge(
        typedQueries.debounce { text -> if (text.trim().length < MIN_QUERY_LENGTH) 0L else DEBOUNCE_MILLIS },
        submittedQueries,
    )
        .map { text -> text.trim().takeIf { it.length >= MIN_QUERY_LENGTH } }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** `flatMapLatest` cancels the previous query's paging (and its in-flight request). */
    val results: Flow<PagingData<BookSummary>> = activeQuery
        .flatMapLatest { query -> if (query == null) flowOf(idlePagingData()) else searchRepository.search(query) }
        .cachedIn(viewModelScope)

    val uiState: StateFlow<SearchUiState> = combine(activeQuery, searchRepository.recentSearches) { query, recents ->
        if (query == null) SearchUiState.Idle(recentSearches = recents) else SearchUiState.Results(query = query)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SearchUiState.Idle(recentSearches = emptyList()),
    )

    fun onQueryChange(text: String) {
        query = text
        typedQueries.value = text
    }

    /** Keyboard "search" action: search now and remember the query. */
    fun onSubmit() {
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) return
        submittedQueries.tryEmit(trimmed)
        remember(trimmed)
    }

    fun onClearQuery() = onQueryChange("")

    fun onRecentSearchClick(recent: String) {
        onQueryChange(recent)
        onSubmit()
    }

    fun onClearRecentSearches() {
        viewModelScope.launch { searchRepository.clearRecentSearches() }
    }

    /**
     * Opening a result counts as a successful search worth remembering. The result also seeds the
     * book cache, so the detail screen can show title and cover before its own request finishes.
     */
    fun onResultClick(book: BookSummary) {
        activeQuery.value?.let(::remember)
        viewModelScope.launch { bookRepository.cacheSummary(book) }
    }

    private fun remember(query: String) {
        viewModelScope.launch { searchRepository.addRecentSearch(query) }
    }

    companion object {
        const val MIN_QUERY_LENGTH = 2
        const val DEBOUNCE_MILLIS = 400L
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

sealed interface SearchUiState {
    /** No query yet (or under two characters): show recent searches. */
    data class Idle(val recentSearches: List<String>) : SearchUiState

    /** Results for [query]; loading, empty and error states come from the paging load states. */
    data class Results(val query: String) : SearchUiState
}
