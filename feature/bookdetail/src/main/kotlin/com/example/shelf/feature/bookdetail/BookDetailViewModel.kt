package com.example.shelf.feature.bookdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shelf.core.common.result.ShelfError
import com.example.shelf.core.common.result.ShelfResult
import com.example.shelf.core.data.repository.BookRepository
import com.example.shelf.core.data.repository.LibraryRepository
import com.example.shelf.core.data.repository.UserDataRepository
import com.example.shelf.core.model.Book
import com.example.shelf.core.model.ShelfEntry
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.ui.component.UserMessage
import com.example.shelf.core.ui.text.UiText
import com.example.shelf.core.ui.text.labelRes
import com.example.shelf.core.ui.text.toUiText
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.shelf.core.ui.R as UiR

@HiltViewModel(assistedFactory = BookDetailViewModel.Factory::class)
class BookDetailViewModel @AssistedInject constructor(
    @Assisted navKey: BookDetailNavKey,
    private val bookRepository: BookRepository,
    private val libraryRepository: LibraryRepository,
    userDataRepository: UserDataRepository,
) : ViewModel() {

    private val workKey = navKey.workKey
    private val refresh = MutableStateFlow<RefreshState>(RefreshState.InProgress)
    private val message = MutableStateFlow<UserMessage?>(null)

    /** What Undo restores: the entry before the last change, or null to remove the book again. */
    private var undoTarget: UndoTarget? = null

    val uiState: StateFlow<BookDetailUiState> = combine(
        bookRepository.observeBook(workKey),
        libraryRepository.observeEntry(workKey),
        userDataRepository.userPreferences.map { it.defaultShelf }.distinctUntilChanged(),
        refresh,
        message,
    ) { book, entry, defaultShelf, refresh, message ->
        when {
            book != null -> BookDetailUiState.Success(
                book = book,
                entry = entry,
                defaultShelf = defaultShelf,
                isRefreshing = refresh == RefreshState.InProgress,
                message = message,
            )

            refresh is RefreshState.Failed -> BookDetailUiState.Error(refresh.error.toUiText())

            else -> BookDetailUiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), BookDetailUiState.Loading)

    init {
        refresh()
    }

    /** Shows the cache immediately and refreshes it from Open Library in the background. */
    fun refresh() {
        viewModelScope.launch {
            refresh.value = RefreshState.InProgress
            when (val result = bookRepository.refreshBook(workKey)) {
                is ShelfResult.Success -> refresh.value = RefreshState.Done

                is ShelfResult.Failure -> {
                    refresh.value = RefreshState.Failed(result.error)
                    // Only mention the failure when the cached copy is incomplete.
                    val cached = bookRepository.observeBook(workKey).first()
                    if (cached != null && !cached.hasDetails) {
                        message.value = UserMessage(UiText.Resource(R.string.bookdetail_refresh_failed))
                    }
                }
            }
        }
    }

    fun onShelfSelected(shelf: ShelfType) {
        viewModelScope.launch {
            val change = libraryRepository.setShelf(workKey, shelf)
            undoTarget = UndoTarget(change.previous)
            val text = if (change.previous == null) R.string.bookdetail_added_to else R.string.bookdetail_moved_to
            message.value = UserMessage(
                text = UiText.Resource(text, listOf(UiText.Resource(shelf.labelRes))),
                actionLabel = UiText.Resource(UiR.string.ui_undo),
            )
        }
    }

    fun onRemoveFromLibrary() {
        viewModelScope.launch {
            val removed = libraryRepository.remove(workKey) ?: return@launch
            undoTarget = UndoTarget(removed)
            message.value = UserMessage(
                text = UiText.Resource(R.string.bookdetail_removed),
                actionLabel = UiText.Resource(UiR.string.ui_undo),
            )
        }
    }

    fun onUndo() {
        val target = undoTarget ?: return
        undoTarget = null
        message.value = null
        viewModelScope.launch {
            val previous = target.previous
            if (previous == null) libraryRepository.remove(workKey) else libraryRepository.restore(previous)
        }
    }

    fun onMessageShown() {
        message.value = null
        undoTarget = null
    }

    private data class UndoTarget(val previous: ShelfEntry?)

    private sealed interface RefreshState {
        data object InProgress : RefreshState

        data object Done : RefreshState

        data class Failed(val error: ShelfError) : RefreshState
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: BookDetailNavKey): BookDetailViewModel
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

sealed interface BookDetailUiState {
    data object Loading : BookDetailUiState

    /** Nothing cached and the fetch failed. */
    data class Error(val message: UiText) : BookDetailUiState

    data class Success(
        val book: Book,
        /** The library entry, or null when the book is not on a shelf. */
        val entry: ShelfEntry?,
        /** Shelf used by the one-tap "Add" button. */
        val defaultShelf: ShelfType,
        val isRefreshing: Boolean,
        val message: UserMessage?,
    ) : BookDetailUiState
}
