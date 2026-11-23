package com.example.shelf.feature.library

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.testing.MainDispatcherRule
import com.example.shelf.core.testing.TestData
import com.example.shelf.core.testing.awaitUntil
import com.example.shelf.core.testing.repository.FakeLibraryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val library = FakeLibraryRepository().apply {
        addBook(TestData.book(1), TestData.entry(1, shelf = ShelfType.READING))
        addBook(TestData.book(2), TestData.entry(2, shelf = ShelfType.WANT_TO_READ))
        addBook(TestData.book(3), TestData.entry(3, shelf = ShelfType.FINISHED, rating = 5))
    }
    private val savedState = SavedStateHandle()

    private fun TestScope.viewModel(): LibraryViewModel {
        val viewModel = LibraryViewModel(library, savedState)
        // Collect eagerly so uiState.value is current right after each action.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private val LibraryViewModel.success get() = assertIs<LibraryUiState.Success>(uiState.value)

    @Test
    fun `books are grouped per shelf and Reading is selected first`() = runTest {
        val viewModel = viewModel()

        assertEquals(ShelfType.READING, viewModel.success.selectedShelf)
        assertEquals(listOf("OL1W"), viewModel.success.books.map { it.book.workKey })
        assertEquals(1, viewModel.success.shelves.getValue(ShelfType.FINISHED).size)
    }

    @Test
    fun `selecting a shelf is kept in saved state`() = runTest {
        val viewModel = viewModel()

        viewModel.onShelfSelected(ShelfType.FINISHED)

        assertEquals(ShelfType.FINISHED, viewModel.success.selectedShelf)
        assertEquals(listOf("OL3W"), viewModel.success.books.map { it.book.workKey })
        assertEquals("FINISHED", savedState.get<String>("selected_shelf"))
    }

    @Test
    fun `removing shows an undo message that restores the book`() = runTest {
        val viewModel = viewModel()
        val book = viewModel.success.books.single()

        viewModel.onRemove(book)
        assertTrue(viewModel.success.books.isEmpty())
        assertEquals(
            listOf(book.book.title),
            (viewModel.success.message?.text as? com.example.shelf.core.ui.text.UiText.Resource)?.args,
        )

        viewModel.onUndoRemove()
        assertEquals(listOf("OL1W"), viewModel.success.books.map { it.book.workKey })
        assertNull(viewModel.success.message)
    }

    @Test
    fun `after the message is shown undo does nothing`() = runTest {
        val viewModel = viewModel()
        viewModel.onRemove(viewModel.success.books.single())

        viewModel.onMessageShown()
        viewModel.onUndoRemove()

        assertTrue(viewModel.success.books.isEmpty())
    }

    @Test
    fun `turbine - switching shelves emits the books of the chosen shelf`() = runTest {
        val viewModel = LibraryViewModel(library, savedState)

        viewModel.uiState.test {
            awaitUntil { it is LibraryUiState.Success }

            viewModel.onShelfSelected(ShelfType.FINISHED)

            val finished = awaitUntil { (it as? LibraryUiState.Success)?.selectedShelf == ShelfType.FINISHED }
            assertEquals(listOf("OL3W"), (finished as LibraryUiState.Success).books.map { it.book.workKey })
            cancelAndIgnoreRemainingEvents()
        }
    }
}
