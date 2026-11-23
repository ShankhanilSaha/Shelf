package com.example.shelf.feature.scanner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shelf.core.common.result.ShelfError
import com.example.shelf.core.common.result.ShelfResult
import com.example.shelf.core.data.repository.BookRepository
import com.example.shelf.core.data.repository.LibraryRepository
import com.example.shelf.core.data.repository.UserDataRepository
import com.example.shelf.core.model.Book
import com.example.shelf.core.model.Isbn
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.ui.text.UiText
import com.example.shelf.core.ui.text.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val libraryRepository: LibraryRepository,
    private val userDataRepository: UserDataRepository,
) : ViewModel() {

    private val state = MutableStateFlow<ScannerUiState>(ScannerUiState.Scanning())
    val uiState: StateFlow<ScannerUiState> = state.asStateFlow()

    /** Manual ISBN entry, as snapshot state for a responsive text field. */
    var manualIsbn by mutableStateOf("")
        private set

    /**
     * Called by the camera analyzer (EAN-13 and EAN-8 only). Only Bookland EAN-13 codes are ISBNs.
     * While a code is being looked up or shown, further detections are ignored.
     */
    fun onBarcodeDetected(rawValue: String) {
        val current = state.value as? ScannerUiState.Scanning ?: return
        if (Isbn.isBookBarcode(rawValue)) {
            lookUp(rawValue, fromCamera = true)
        } else if (current.hint == null) {
            state.value = ScannerUiState.Scanning(hint = UiText.Resource(R.string.scanner_not_a_book))
        }
    }

    fun onManualIsbnChange(text: String) {
        manualIsbn = text.filter { it.isDigit() || it == 'x' || it == 'X' || it == '-' || it == ' ' }.take(MAX_INPUT)
    }

    /** Returns false (and keeps the dialog open) when the text is not a valid ISBN. */
    fun onManualSubmit(): Boolean {
        val isbn = Isbn.normalize(manualIsbn) ?: return false
        lookUp(isbn, fromCamera = false)
        return true
    }

    fun onAddToShelf() {
        val found = state.value as? ScannerUiState.Found ?: return
        viewModelScope.launch {
            libraryRepository.setShelf(found.workKey, found.defaultShelf)
            state.value = found.copy(addedTo = found.defaultShelf)
        }
    }

    fun onScanAgain() {
        state.value = ScannerUiState.Scanning()
    }

    fun onRetry() {
        (state.value as? ScannerUiState.Failed)?.let { lookUp(it.isbn, fromCamera = false) }
    }

    private fun lookUp(isbn: String, fromCamera: Boolean) {
        state.value = ScannerUiState.LookingUp(isbn = isbn, fromCamera = fromCamera)
        viewModelScope.launch {
            state.value = when (val result = bookRepository.findWorkByIsbn(isbn)) {
                is ShelfResult.Success -> {
                    val workKey = result.data
                    val entry = libraryRepository.observeEntry(workKey).first()
                    ScannerUiState.Found(
                        isbn = isbn,
                        workKey = workKey,
                        book = bookRepository.observeBook(workKey).first(),
                        defaultShelf = userDataRepository.userPreferences.first().defaultShelf,
                        addedTo = entry?.shelf,
                    )
                }

                is ShelfResult.Failure -> if (result.error == ShelfError.NotFound) {
                    ScannerUiState.NotFound(isbn)
                } else {
                    ScannerUiState.Failed(isbn, result.error.toUiText())
                }
            }
        }
    }

    private companion object {
        const val MAX_INPUT = 17 // 13 digits plus hyphens
    }
}

sealed interface ScannerUiState {
    /** Camera analysis is running. [hint] explains why the last code was ignored. */
    data class Scanning(val hint: UiText? = null) : ScannerUiState

    /** A code was accepted; analysis stops while it is resolved. */
    data class LookingUp(val isbn: String, val fromCamera: Boolean) : ScannerUiState

    data class Found(
        val isbn: String,
        val workKey: String,
        val book: Book?,
        val defaultShelf: ShelfType,
        /** The shelf the book is on, if it is already in the library or was just added. */
        val addedTo: ShelfType?,
    ) : ScannerUiState

    data class NotFound(val isbn: String) : ScannerUiState

    data class Failed(val isbn: String, val message: UiText) : ScannerUiState
}
