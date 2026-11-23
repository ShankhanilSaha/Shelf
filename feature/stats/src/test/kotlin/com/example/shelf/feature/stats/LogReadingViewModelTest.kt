package com.example.shelf.feature.stats

import app.cash.turbine.test
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.testing.MainDispatcherRule
import com.example.shelf.core.testing.MutableClock
import com.example.shelf.core.testing.TestData
import com.example.shelf.core.testing.awaitUntil
import com.example.shelf.core.testing.repository.FakeLibraryRepository
import com.example.shelf.core.testing.repository.FakeReadingLogRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LogReadingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = MutableClock() // 2026-11-24
    private val logs = FakeReadingLogRepository()
    private val library = FakeLibraryRepository().apply {
        addBook(TestData.book(1, title = "Piranesi"), TestData.entry(1, shelf = ShelfType.READING))
        addBook(TestData.book(2, title = "Dune"), TestData.entry(2, shelf = ShelfType.WANT_TO_READ))
    }

    private fun TestScope.viewModel(): LogReadingViewModel {
        val viewModel = LogReadingViewModel(logs, library, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun `only books on the Reading shelf can be picked and the date defaults to today`() = runTest {
        val viewModel = viewModel()

        val editing = assertIs<LogReadingUiState.Editing>(viewModel.uiState.value)
        assertEquals(listOf("Piranesi"), editing.readingBooks.map { it.book.title })
        assertEquals(LocalDate.of(2026, 11, 24), viewModel.date)
        assertFalse(viewModel.canSave)
    }

    @Test
    fun `amounts accept digits only and either one is enough`() = runTest {
        val viewModel = viewModel()

        viewModel.onPagesChange("1x2")
        assertEquals("12", viewModel.pagesInput)
        assertTrue(viewModel.canSave)

        viewModel.onPagesChange("")
        viewModel.onMinutesChange("45")
        assertTrue(viewModel.canSave)
    }

    @Test
    fun `future dates are ignored`() = runTest {
        val viewModel = viewModel()

        viewModel.onDateChange(LocalDate.of(2026, 11, 25))
        assertEquals(LocalDate.of(2026, 11, 24), viewModel.date)

        viewModel.onDateChange(LocalDate.of(2026, 11, 20))
        assertEquals(LocalDate.of(2026, 11, 20), viewModel.date)
    }

    @Test
    fun `saving stores the log and closes the sheet`() = runTest {
        val viewModel = viewModel()
        viewModel.onBookSelected("OL1W")
        viewModel.onPagesChange("30")
        viewModel.onMinutesChange("25")
        viewModel.onDateChange(LocalDate.of(2026, 11, 23))

        viewModel.onSave()

        assertEquals(LogReadingUiState.Saved, viewModel.uiState.value)
        val saved = logs.current.single()
        assertEquals(LocalDate.of(2026, 11, 23), saved.date)
        assertEquals(30, saved.pages)
        assertEquals(25, saved.minutes)
        assertEquals("OL1W", saved.workKey)
    }

    @Test
    fun `nothing is saved without an amount`() = runTest {
        val viewModel = viewModel()

        viewModel.onSave()

        assertIs<LogReadingUiState.Editing>(viewModel.uiState.value)
        assertEquals(emptyList(), logs.current)
    }

    @Test
    fun `turbine - editing ends in saved once the log is stored`() = runTest {
        val viewModel = LogReadingViewModel(logs, library, clock)

        viewModel.uiState.test {
            val editing = awaitUntil { it is LogReadingUiState.Editing } as LogReadingUiState.Editing
            assertEquals(listOf("OL1W"), editing.readingBooks.map { it.book.workKey })

            viewModel.onPagesChange("12")
            viewModel.onSave()

            awaitUntil { it == LogReadingUiState.Saved }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
