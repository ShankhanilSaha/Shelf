package com.example.shelf.feature.stats

import app.cash.turbine.test
import com.example.shelf.core.model.DailyGoal
import com.example.shelf.core.model.GoalType
import com.example.shelf.core.model.ReadingLog
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.model.UserPreferences
import com.example.shelf.core.testing.MainDispatcherRule
import com.example.shelf.core.testing.MutableClock
import com.example.shelf.core.testing.TestData
import com.example.shelf.core.testing.awaitUntil
import com.example.shelf.core.testing.repository.FakeLibraryRepository
import com.example.shelf.core.testing.repository.FakeReadingLogRepository
import com.example.shelf.core.testing.repository.FakeUserDataRepository
import com.example.shelf.feature.stats.domain.CalculateStreakUseCase
import com.example.shelf.feature.stats.domain.StreakSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = MutableClock() // 2026-11-24, UTC
    private val today = LocalDate.of(2026, 11, 24)
    private val logs = FakeReadingLogRepository()
    private val library = FakeLibraryRepository { clock.now }.apply {
        addBook(TestData.book(1, title = "Piranesi"), TestData.entry(1, shelf = ShelfType.READING))
        addBook(TestData.book(2), TestData.entry(2, shelf = ShelfType.FINISHED, finishedAt = clock.now))
    }
    private val userData = FakeUserDataRepository(UserPreferences(dailyGoal = DailyGoal(GoalType.PAGES, 20)))

    private fun TestScope.viewModel(): StatsViewModel {
        val viewModel = StatsViewModel(logs, library, userData, CalculateStreakUseCase(), clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private val StatsViewModel.success get() = assertIs<StatsUiState.Success>(uiState.value)

    private fun log(id: String, daysAgo: Long, pages: Int, workKey: String? = null) =
        ReadingLog(id = id, date = today.minusDays(daysAgo), pages = pages, minutes = 0, workKey = workKey)

    @Test
    fun `today's progress, streaks and the week are derived from the logs`() = runTest {
        logs.setLogs(listOf(log("a", 0, 12, "OL1W"), log("b", 1, 25), log("c", 2, 20), log("d", 9, 40)))

        val state = viewModel().success

        assertEquals(12, state.todayAmount)
        assertEquals(0.6f, state.todayProgress)
        assertEquals(StreakSummary(current = 2, longest = 2, todayMet = false), state.streak)
        assertEquals(7, state.week.size)
        assertEquals(today, state.week.last().date)
        assertEquals(listOf(0, 0, 0, 0, 20, 25, 12), state.week.map { it.amount })
        assertEquals(listOf(false, false, false, false, true, true, false), state.week.map { it.met })
        assertEquals(1, state.finishedThisYear)
    }

    @Test
    fun `recent sessions show the book title when it is known`() = runTest {
        logs.setLogs(listOf(log("a", 0, 12, "OL1W"), log("b", 1, 25, "OL404W")))

        val recent = viewModel().success.recentLogs

        assertEquals(listOf("Piranesi", null), recent.map { it.bookTitle })
    }

    @Test
    fun `a minutes goal measures minutes`() = runTest {
        userData.setDailyGoal(DailyGoal(GoalType.MINUTES, 30))
        logs.setLogs(listOf(ReadingLog("a", today, pages = 50, minutes = 30)))

        val state = viewModel().success

        assertEquals(30, state.todayAmount)
        assertEquals(true, state.streak.todayMet)
    }

    @Test
    fun `deleting a session updates the stats`() = runTest {
        logs.setLogs(listOf(log("a", 0, 25)))
        val viewModel = viewModel()
        assertEquals(1, viewModel.success.streak.current)

        viewModel.onDeleteLog("a")

        assertEquals(0, viewModel.success.streak.current)
        assertEquals(emptyList(), viewModel.success.recentLogs)
    }

    @Test
    fun `turbine - a new log updates today's progress and the streak`() = runTest {
        val viewModel = StatsViewModel(logs, library, userData, CalculateStreakUseCase(), clock)

        viewModel.uiState.test {
            val empty = awaitUntil { it is StatsUiState.Success } as StatsUiState.Success
            assertEquals(0, empty.todayAmount)

            logs.addLog(today, pages = 25, minutes = 0, workKey = null)

            val logged = awaitUntil { (it as? StatsUiState.Success)?.todayAmount == 25 } as StatsUiState.Success
            assertEquals(1, logged.streak.current)
            assertTrue(logged.streak.todayMet)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
