package com.example.shelf.feature.library

import app.cash.turbine.test
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.testing.MainDispatcherRule
import com.example.shelf.core.testing.TestData
import com.example.shelf.core.testing.awaitUntil
import com.example.shelf.core.testing.repository.FakeLibraryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryBookViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private val library = FakeLibraryRepository()

    private fun TestScope.viewModel(): LibraryBookViewModel {
        val viewModel = LibraryBookViewModel(LibraryNavKey.Book("OL1W"), library, backgroundScope)
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        return viewModel
    }

    private val LibraryBookViewModel.success get() = assertIs<LibraryBookUiState.Success>(uiState.value)

    @Test
    fun `inputs start from the saved entry`() = runTest(dispatcher) {
        library.addBook(TestData.book(1), TestData.entry(1, shelf = ShelfType.READING, currentPage = 42, notes = "Hi"))

        val viewModel = viewModel()

        assertEquals("42", viewModel.pageInput)
        assertEquals("Hi", viewModel.notesInput)
        assertEquals(42, viewModel.success.shelfBook.entry.currentPage)
    }

    @Test
    fun `page input accepts digits only`() = runTest(dispatcher) {
        library.addBook(TestData.book(1), TestData.entry(1, shelf = ShelfType.READING))
        val viewModel = viewModel()

        viewModel.onPageInputChange("1a2-3")

        assertEquals("123", viewModel.pageInput)
    }

    @Test
    fun `saving progress starts the book and confirms with a message`() = runTest(dispatcher) {
        library.addBook(TestData.book(1, pageCount = 300), TestData.entry(1, shelf = ShelfType.WANT_TO_READ))
        val viewModel = viewModel()

        viewModel.onPageInputChange("50")
        viewModel.onSavePage()
        runCurrent()

        assertEquals(ShelfType.READING, viewModel.success.shelfBook.entry.shelf)
        assertEquals(
            R.string.library_progress_saved,
            (viewModel.success.message?.text as? com.example.shelf.core.ui.text.UiText.Resource)?.id,
        )
        assertFalse(viewModel.success.showRatingPrompt)
    }

    @Test
    fun `reaching the last page finishes the book and asks for a rating`() = runTest(dispatcher) {
        library.addBook(
            TestData.book(1, pageCount = 300),
            TestData.entry(1, shelf = ShelfType.READING, currentPage = 280),
        )
        val viewModel = viewModel()

        viewModel.onPageInputChange("400")
        viewModel.onSavePage()
        runCurrent()

        assertEquals(ShelfType.FINISHED, viewModel.success.shelfBook.entry.shelf)
        assertEquals("300", viewModel.pageInput)
        assertTrue(viewModel.success.showRatingPrompt)

        viewModel.onRatingPromptSubmit(4)
        runCurrent()
        assertFalse(viewModel.success.showRatingPrompt)
        assertEquals(4, viewModel.success.shelfBook.entry.rating)
    }

    @Test
    fun `moving to finished asks for a rating only when there is none`() = runTest(dispatcher) {
        library.addBook(TestData.book(1), TestData.entry(1, shelf = ShelfType.READING))
        library.addBook(TestData.book(2), TestData.entry(2, shelf = ShelfType.READING, rating = 3))
        val viewModel = viewModel()

        viewModel.onShelfSelected(ShelfType.FINISHED)
        runCurrent()
        assertTrue(viewModel.success.showRatingPrompt)
        viewModel.onRatingPromptDismissed()
        runCurrent()
        assertFalse(viewModel.success.showRatingPrompt)

        val rated = LibraryBookViewModel(LibraryNavKey.Book("OL2W"), library, backgroundScope)
        backgroundScope.launch { rated.uiState.collect {} }
        runCurrent()
        rated.onShelfSelected(ShelfType.FINISHED)
        runCurrent()
        assertFalse(assertIs<LibraryBookUiState.Success>(rated.uiState.value).showRatingPrompt)
    }

    @Test
    fun `notes are saved after typing pauses`() = runTest(dispatcher) {
        library.addBook(TestData.book(1), TestData.entry(1, shelf = ShelfType.READING))
        val viewModel = viewModel()

        viewModel.onNotesChange("Great")
        viewModel.onNotesChange("Great opening")
        advanceTimeBy(400)
        runCurrent()
        assertEquals("", library.entry("OL1W")?.notes)

        advanceTimeBy(200)
        runCurrent()
        assertEquals("Great opening", library.entry("OL1W")?.notes)
    }

    @Test
    fun `removing closes the screen and a missing book closes it too`() = runTest(dispatcher) {
        library.addBook(TestData.book(1), TestData.entry(1, shelf = ShelfType.READING))
        val viewModel = viewModel()

        viewModel.onRemove()
        runCurrent()

        assertEquals(LibraryBookUiState.Closed, viewModel.uiState.value)
        assertEquals(null, library.entry("OL1W"))
    }

    @Test
    fun `turbine - removing the book closes the screen`() = runTest {
        library.addBook(TestData.book(1), TestData.entry(1, shelf = ShelfType.READING))
        val viewModel = LibraryBookViewModel(LibraryNavKey.Book("OL1W"), library, backgroundScope)

        viewModel.uiState.test {
            awaitUntil { it is LibraryBookUiState.Success }

            viewModel.onRemove()

            awaitUntil { it == LibraryBookUiState.Closed }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
