package com.example.shelf.feature.bookdetail

import app.cash.turbine.test
import com.example.shelf.core.common.result.ShelfError
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.model.UserPreferences
import com.example.shelf.core.testing.MainDispatcherRule
import com.example.shelf.core.testing.TestData
import com.example.shelf.core.testing.awaitUntil
import com.example.shelf.core.testing.repository.FakeBookRepository
import com.example.shelf.core.testing.repository.FakeLibraryRepository
import com.example.shelf.core.testing.repository.FakeUserDataRepository
import com.example.shelf.core.ui.text.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class BookDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val books = FakeBookRepository()
    private val library = FakeLibraryRepository()
    private val userData = FakeUserDataRepository(UserPreferences(defaultShelf = ShelfType.READING))
    private val fullBook = TestData.book(1, title = "Dune")

    private fun TestScope.viewModel(): BookDetailViewModel {
        val viewModel = BookDetailViewModel(BookDetailNavKey("OL1W"), books, library, userData)
        // Collect eagerly so uiState.value is current right after each action.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private val BookDetailViewModel.success get() = assertIs<BookDetailUiState.Success>(uiState.value)

    @Test
    fun `a refreshed book is shown with the default shelf`() = runTest {
        books.remoteBooks["OL1W"] = fullBook
        library.addBook(fullBook)

        val viewModel = viewModel()

        assertEquals("Dune", viewModel.success.book.title)
        assertEquals(ShelfType.READING, viewModel.success.defaultShelf)
        assertNull(viewModel.success.entry)
        assertEquals(listOf("OL1W"), books.refreshedKeys)
    }

    @Test
    fun `nothing cached and offline shows an error that can be retried`() = runTest {
        books.refreshError = ShelfError.Network
        val viewModel = viewModel()

        assertEquals(
            BookDetailUiState.Error(UiText.Resource(com.example.shelf.core.ui.R.string.ui_error_network)),
            viewModel.uiState.value,
        )

        books.refreshError = null
        books.remoteBooks["OL1W"] = fullBook
        viewModel.refresh()
        assertEquals("Dune", viewModel.success.book.title)
    }

    @Test
    fun `a cached summary stays visible offline with a message`() = runTest {
        books.setCached(TestData.book(1, hasDetails = false))
        books.refreshError = ShelfError.Network

        val viewModel = viewModel()

        assertEquals(UiText.Resource(R.string.bookdetail_refresh_failed), viewModel.success.message?.text)
    }

    @Test
    fun `fully cached details stay visible offline without nagging`() = runTest {
        books.setCached(fullBook)
        books.refreshError = ShelfError.Network

        val viewModel = viewModel()

        assertNull(viewModel.success.message)
    }

    @Test
    fun `adding to a shelf offers undo, which removes the book again`() = runTest {
        books.remoteBooks["OL1W"] = fullBook
        library.addBook(fullBook)
        val viewModel = viewModel()

        viewModel.onShelfSelected(ShelfType.WANT_TO_READ)
        assertEquals(ShelfType.WANT_TO_READ, viewModel.success.entry?.shelf)
        assertNotNull(viewModel.success.message?.actionLabel)

        viewModel.onUndo()
        assertNull(viewModel.success.entry)
        assertNull(viewModel.success.message)
    }

    @Test
    fun `moving shelves can be undone back to the previous shelf`() = runTest {
        books.remoteBooks["OL1W"] = fullBook
        library.addBook(fullBook, TestData.entry(1, shelf = ShelfType.READING, currentPage = 40))
        val viewModel = viewModel()

        viewModel.onShelfSelected(ShelfType.FINISHED)
        assertEquals(ShelfType.FINISHED, viewModel.success.entry?.shelf)

        viewModel.onUndo()
        assertEquals(ShelfType.READING, viewModel.success.entry?.shelf)
        assertEquals(40, viewModel.success.entry?.currentPage)
    }

    @Test
    fun `removing can be undone, but not after the message was dismissed`() = runTest {
        books.remoteBooks["OL1W"] = fullBook
        library.addBook(fullBook, TestData.entry(1, shelf = ShelfType.READING))
        val viewModel = viewModel()

        viewModel.onRemoveFromLibrary()
        assertNull(viewModel.success.entry)
        viewModel.onUndo()
        assertEquals(ShelfType.READING, viewModel.success.entry?.shelf)

        viewModel.onRemoveFromLibrary()
        viewModel.onMessageShown()
        viewModel.onUndo()
        assertNull(viewModel.success.entry)
    }

    @Test
    fun `turbine - the cached book shows first, then adding puts it on the default shelf`() = runTest {
        books.setCached(fullBook)
        val viewModel = BookDetailViewModel(BookDetailNavKey("OL1W"), books, library, userData)

        viewModel.uiState.test {
            val shown = awaitUntil { it is BookDetailUiState.Success } as BookDetailUiState.Success
            assertEquals("Dune", shown.book.title)
            assertNull(shown.entry)

            viewModel.onShelfSelected(ShelfType.READING)

            val added = awaitUntil { (it as? BookDetailUiState.Success)?.entry != null } as BookDetailUiState.Success
            assertEquals(ShelfType.READING, added.entry?.shelf)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
