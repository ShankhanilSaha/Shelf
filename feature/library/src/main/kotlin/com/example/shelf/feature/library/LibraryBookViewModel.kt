package com.example.shelf.feature.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shelf.core.common.dispatcher.ApplicationScope
import com.example.shelf.core.data.repository.LibraryChange
import com.example.shelf.core.data.repository.LibraryRepository
import com.example.shelf.core.model.ShelfBook
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.ui.component.UserMessage
import com.example.shelf.core.ui.text.UiText
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Edits one library book: shelf, current page, rating and notes. */
@OptIn(FlowPreview::class)
@HiltViewModel(assistedFactory = LibraryBookViewModel.Factory::class)
class LibraryBookViewModel @AssistedInject constructor(
    @Assisted navKey: LibraryNavKey.Book,
    private val libraryRepository: LibraryRepository,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val workKey = navKey.workKey

    /** Text field contents, as snapshot state so typing is never delayed. */
    var pageInput by mutableStateOf("")
        private set
    var notesInput by mutableStateOf("")
        private set

    private var persistedNotes: String? = null
    private val pendingNotes = MutableStateFlow<String?>(null)
    private val ratingPrompt = MutableStateFlow(false)
    private val message = MutableStateFlow<UserMessage?>(null)
    private val removed = MutableStateFlow(false)

    val uiState: StateFlow<LibraryBookUiState> = combine(
        libraryRepository.observeShelfBook(workKey),
        ratingPrompt,
        message,
        removed,
    ) { shelfBook, prompt, message, removed ->
        if (removed || shelfBook == null) {
            LibraryBookUiState.Closed
        } else {
            LibraryBookUiState.Success(shelfBook = shelfBook, showRatingPrompt = prompt, message = message)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), LibraryBookUiState.Loading)

    init {
        viewModelScope.launch {
            libraryRepository.observeShelfBook(workKey).first()?.let { book ->
                pageInput = book.entry.currentPage.toString()
                notesInput = book.entry.notes
                persistedNotes = book.entry.notes
            }
        }
        viewModelScope.launch {
            pendingNotes.filterNotNull().debounce(NOTES_DEBOUNCE_MILLIS).collect(::saveNotes)
        }
    }

    fun onPageInputChange(text: String) {
        pageInput = text.filter(Char::isDigit).take(MAX_PAGE_DIGITS)
    }

    fun onSavePage() {
        val page = pageInput.toIntOrNull() ?: return
        viewModelScope.launch {
            val change = libraryRepository.setCurrentPage(workKey, page)
            onChanged(change)
            message.value = UserMessage(UiText.Resource(R.string.library_progress_saved))
        }
    }

    fun onShelfSelected(shelf: ShelfType) {
        viewModelScope.launch { onChanged(libraryRepository.setShelf(workKey, shelf)) }
    }

    fun onRatingChange(rating: Int?) {
        viewModelScope.launch { libraryRepository.setRating(workKey, rating) }
    }

    fun onRatingPromptSubmit(rating: Int?) {
        ratingPrompt.value = false
        if (rating != null) onRatingChange(rating)
    }

    fun onRatingPromptDismissed() {
        ratingPrompt.value = false
    }

    /** Notes save themselves shortly after typing stops. */
    fun onNotesChange(text: String) {
        notesInput = text
        pendingNotes.value = text
    }

    fun onRemove() {
        viewModelScope.launch {
            libraryRepository.remove(workKey)
            removed.value = true
        }
    }

    fun onMessageShown() {
        message.value = null
    }

    /** Leaving within the debounce window must not lose the last keystrokes. */
    override fun onCleared() {
        val notes = notesInput
        if (notes != persistedNotes) {
            applicationScope.launch { libraryRepository.setNotes(workKey, notes) }
        }
    }

    private suspend fun saveNotes(notes: String) {
        if (notes == persistedNotes) return
        libraryRepository.setNotes(workKey, notes)
        persistedNotes = notes
    }

    /** Finishing a book (last page or moved to Finished) asks for a rating if it has none. */
    private fun onChanged(change: LibraryChange) {
        pageInput = change.entry.currentPage.toString()
        if (change.justFinished && change.entry.rating == null) ratingPrompt.value = true
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: LibraryNavKey.Book): LibraryBookViewModel
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val NOTES_DEBOUNCE_MILLIS = 500L
        const val MAX_PAGE_DIGITS = 5
    }
}

sealed interface LibraryBookUiState {
    data object Loading : LibraryBookUiState

    /** The book is no longer in the library (removed here or elsewhere): leave the screen. */
    data object Closed : LibraryBookUiState

    data class Success(val shelfBook: ShelfBook, val showRatingPrompt: Boolean, val message: UserMessage?) :
        LibraryBookUiState
}
