package com.example.shelf.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shelf.core.data.repository.LibraryRepository
import com.example.shelf.core.model.ShelfBook
import com.example.shelf.core.model.ShelfEntry
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.ui.component.UserMessage
import com.example.shelf.core.ui.text.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.shelf.core.ui.R as UiR

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val selectedShelf = savedStateHandle.getStateFlow(KEY_SELECTED_SHELF, ShelfType.READING.name)
        .map { name -> ShelfType.entries.firstOrNull { it.name == name } ?: ShelfType.READING }
    private val message = MutableStateFlow<UserMessage?>(null)
    private var lastRemoved: ShelfEntry? = null

    val uiState: StateFlow<LibraryUiState> = combine(
        libraryRepository.observeShelf(ShelfType.WANT_TO_READ),
        libraryRepository.observeShelf(ShelfType.READING),
        libraryRepository.observeShelf(ShelfType.FINISHED),
        selectedShelf,
        message,
    ) { wantToRead, reading, finished, selected, message ->
        LibraryUiState.Success(
            selectedShelf = selected,
            shelves = mapOf(
                ShelfType.WANT_TO_READ to wantToRead,
                ShelfType.READING to reading,
                ShelfType.FINISHED to finished,
            ),
            message = message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), LibraryUiState.Loading)

    fun onShelfSelected(shelf: ShelfType) {
        savedStateHandle[KEY_SELECTED_SHELF] = shelf.name
    }

    /** Swipe to remove; the snackbar offers Undo. */
    fun onRemove(book: ShelfBook) {
        viewModelScope.launch {
            lastRemoved = libraryRepository.remove(book.book.workKey) ?: return@launch
            message.value = UserMessage(
                text = UiText.Resource(R.string.library_removed, listOf(book.book.title)),
                actionLabel = UiText.Resource(UiR.string.ui_undo),
            )
        }
    }

    fun onUndoRemove() {
        val entry = lastRemoved ?: return
        lastRemoved = null
        message.value = null
        viewModelScope.launch { libraryRepository.restore(entry) }
    }

    fun onMessageShown() {
        message.value = null
        lastRemoved = null
    }

    private companion object {
        const val KEY_SELECTED_SHELF = "selected_shelf"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

sealed interface LibraryUiState {
    data object Loading : LibraryUiState

    data class Success(
        val selectedShelf: ShelfType,
        val shelves: Map<ShelfType, List<ShelfBook>>,
        val message: UserMessage?,
    ) : LibraryUiState {
        val books: List<ShelfBook> get() = shelves[selectedShelf].orEmpty()

        val isLibraryEmpty: Boolean get() = shelves.values.all { it.isEmpty() }
    }
}
