package com.example.shelf.feature.scanner

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
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ScannerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val books = FakeBookRepository().apply {
        isbnToWorkKey["9780261103573"] = "OL1W"
        isbnToWorkKey["0306406152"] = "OL2W"
        setCached(TestData.book(1, title = "The Fellowship of the Ring"))
    }
    private val library = FakeLibraryRepository().apply {
        addBook(TestData.book(1, title = "The Fellowship of the Ring"))
        addBook(TestData.book(2), TestData.entry(2, shelf = ShelfType.FINISHED))
    }
    private val userData = FakeUserDataRepository(UserPreferences(defaultShelf = ShelfType.READING))
    private val viewModel by lazy { ScannerViewModel(books, library, userData) }

    @Test
    fun `a book barcode is looked up and shown with the default shelf`() {
        viewModel.onBarcodeDetected("9780261103573")

        val found = assertIs<ScannerUiState.Found>(viewModel.uiState.value)
        assertEquals("OL1W", found.workKey)
        assertEquals("The Fellowship of the Ring", found.book?.title)
        assertEquals(ShelfType.READING, found.defaultShelf)
        assertEquals(null, found.addedTo)
    }

    @Test
    fun `other barcodes are ignored with a hint`() {
        viewModel.onBarcodeDetected("4006381333931") // a valid EAN-13 that is not a book
        assertEquals(ScannerUiState.Scanning(UiText.Resource(R.string.scanner_not_a_book)), viewModel.uiState.value)

        viewModel.onBarcodeDetected("96385074") // EAN-8
        assertIs<ScannerUiState.Scanning>(viewModel.uiState.value)
    }

    @Test
    fun `once a code is accepted further detections are ignored`() {
        viewModel.onBarcodeDetected("9780261103573")
        viewModel.onBarcodeDetected("9780306406157")

        assertEquals("OL1W", assertIs<ScannerUiState.Found>(viewModel.uiState.value).workKey)
    }

    @Test
    fun `an unknown isbn says so and scanning can resume`() {
        viewModel.onBarcodeDetected("9780306406157")
        assertEquals(ScannerUiState.NotFound("9780306406157"), viewModel.uiState.value)

        viewModel.onScanAgain()
        assertEquals(ScannerUiState.Scanning(), viewModel.uiState.value)
    }

    @Test
    fun `an offline lookup can be retried`() {
        books.refreshError = ShelfError.Network
        viewModel.onBarcodeDetected("9780261103573")
        assertIs<ScannerUiState.Failed>(viewModel.uiState.value)

        books.refreshError = null
        viewModel.onRetry()
        assertIs<ScannerUiState.Found>(viewModel.uiState.value)
    }

    @Test
    fun `manual entry accepts a valid ISBN-10 and rejects nonsense`() {
        viewModel.onManualIsbnChange("12345")
        assertFalse(viewModel.onManualSubmit())

        viewModel.onManualIsbnChange("0-306-40615-2abc")
        assertEquals("0-306-40615-2", viewModel.manualIsbn)
        assertTrue(viewModel.onManualSubmit())
        val found = assertIs<ScannerUiState.Found>(viewModel.uiState.value)
        assertEquals("OL2W", found.workKey)
        assertEquals(ShelfType.FINISHED, found.addedTo)
    }

    @Test
    fun `adding puts the book on the default shelf`() {
        viewModel.onBarcodeDetected("9780261103573")

        viewModel.onAddToShelf()

        assertEquals(ShelfType.READING, assertIs<ScannerUiState.Found>(viewModel.uiState.value).addedTo)
        assertEquals(ShelfType.READING, library.entry("OL1W")?.shelf)
    }

    @Test
    fun `turbine - a scanned isbn goes from scanning through lookup to found`() = runTest {
        viewModel.uiState.test {
            assertEquals(ScannerUiState.Scanning(), awaitItem())

            viewModel.onBarcodeDetected("9780261103573")

            val found = awaitUntil { it is ScannerUiState.Found } as ScannerUiState.Found
            assertEquals("OL1W", found.workKey)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
