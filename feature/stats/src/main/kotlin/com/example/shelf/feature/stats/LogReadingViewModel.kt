package com.example.shelf.feature.stats

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shelf.core.common.time.today
import com.example.shelf.core.data.repository.LibraryRepository
import com.example.shelf.core.data.repository.ReadingLogRepository
import com.example.shelf.core.model.ShelfBook
import com.example.shelf.core.model.ShelfType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** The "Log reading" bottom sheet: a book from the Reading shelf, pages and/or minutes, a date. */
@HiltViewModel
class LogReadingViewModel @Inject constructor(
    private val readingLogRepository: ReadingLogRepository,
    libraryRepository: LibraryRepository,
    private val clock: Clock,
) : ViewModel() {

    // Form fields are snapshot state so the text fields update synchronously while typing.
    var selectedWorkKey by mutableStateOf<String?>(null)
        private set
    var pagesInput by mutableStateOf("")
        private set
    var minutesInput by mutableStateOf("")
        private set
    var date by mutableStateOf(clock.today())
        private set

    private val saved = MutableStateFlow(false)

    val uiState: StateFlow<LogReadingUiState> = combine(
        libraryRepository.observeShelf(ShelfType.READING),
        saved,
    ) { readingBooks, saved ->
        if (saved) LogReadingUiState.Saved else LogReadingUiState.Editing(readingBooks = readingBooks)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), LogReadingUiState.Loading)

    val canSave: Boolean
        get() = (pagesInput.toIntOrNull() ?: 0) + (minutesInput.toIntOrNull() ?: 0) > 0 && !date.isAfter(clock.today())

    fun onBookSelected(workKey: String?) {
        selectedWorkKey = workKey
    }

    fun onPagesChange(text: String) {
        pagesInput = text.filter(Char::isDigit).take(MAX_DIGITS)
    }

    fun onMinutesChange(text: String) {
        minutesInput = text.filter(Char::isDigit).take(MAX_DIGITS)
    }

    /** Future dates are rejected; a reading session cannot happen tomorrow. */
    fun onDateChange(newDate: LocalDate) {
        if (!newDate.isAfter(clock.today())) date = newDate
    }

    fun onSave() {
        if (!canSave) return
        val pages = pagesInput.toIntOrNull() ?: 0
        val minutes = minutesInput.toIntOrNull() ?: 0
        viewModelScope.launch {
            readingLogRepository.addLog(date = date, pages = pages, minutes = minutes, workKey = selectedWorkKey)
            saved.value = true
        }
    }

    fun today(): LocalDate = clock.today()

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val MAX_DIGITS = 4
    }
}

sealed interface LogReadingUiState {
    data object Loading : LogReadingUiState

    data class Editing(val readingBooks: List<ShelfBook>) : LogReadingUiState

    /** The log was stored; the sheet closes itself. */
    data object Saved : LogReadingUiState
}
