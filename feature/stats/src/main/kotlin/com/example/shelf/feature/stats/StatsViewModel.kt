package com.example.shelf.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shelf.core.common.time.durationUntilNext
import com.example.shelf.core.common.time.today
import com.example.shelf.core.data.repository.LibraryRepository
import com.example.shelf.core.data.repository.ReadingLogRepository
import com.example.shelf.core.data.repository.UserDataRepository
import com.example.shelf.core.model.DailyGoal
import com.example.shelf.core.model.GoalChange
import com.example.shelf.core.model.ReadingLog
import com.example.shelf.feature.stats.domain.CalculateStreakUseCase
import com.example.shelf.feature.stats.domain.GoalTimeline
import com.example.shelf.feature.stats.domain.StreakSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val readingLogRepository: ReadingLogRepository,
    private val libraryRepository: LibraryRepository,
    private val userDataRepository: UserDataRepository,
    private val calculateStreak: CalculateStreakUseCase,
    private val clock: Clock,
) : ViewModel() {

    /** Today's date now, and again right after every local midnight, so the screen rolls over. */
    private val today: Flow<LocalDate> = flow {
        while (true) {
            emit(clock.today())
            delay(clock.durationUntilNext(LocalTime.MIDNIGHT).toMillis() + MIDNIGHT_SLACK_MILLIS)
        }
    }.distinctUntilChanged()

    val uiState: StateFlow<StatsUiState> = today.flatMapLatest { day ->
        combine(
            readingLogRepository.observeLogs(),
            readingLogRepository.observeGoalHistory(),
            userDataRepository.userPreferences.map { it.dailyGoal }.distinctUntilChanged(),
            libraryRepository.observeFinishedCount(day.year),
            libraryRepository.observeLibrary().map { books -> books.associate { it.book.workKey to it.book.title } },
        ) { logs, history, goal, finished, titles ->
            buildState(day, logs, history, goal, finished, titles)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), StatsUiState.Loading)

    fun onDeleteLog(id: String) {
        viewModelScope.launch { readingLogRepository.deleteLog(id) }
    }

    private fun buildState(
        today: LocalDate,
        logs: List<ReadingLog>,
        history: List<GoalChange>,
        goal: DailyGoal,
        finishedThisYear: Int,
        titles: Map<String, String>,
    ): StatsUiState.Success {
        val goals = GoalTimeline.of(history, current = goal, today = today)
        val byDay = logs.groupBy { it.date }
        fun amountOn(day: LocalDate) = byDay[day].orEmpty().let { dayLogs ->
            goal.amountOf(pages = dayLogs.sumOf { it.pages }, minutes = dayLogs.sumOf { it.minutes })
        }
        fun metOn(day: LocalDate) = byDay[day].orEmpty().let { dayLogs ->
            goals.goalOn(day).isMetBy(pages = dayLogs.sumOf { it.pages }, minutes = dayLogs.sumOf { it.minutes })
        }
        return StatsUiState.Success(
            goal = goal,
            todayAmount = amountOn(today),
            streak = calculateStreak(logs, goals, clock),
            finishedThisYear = finishedThisYear,
            week = (DAYS_IN_CHART - 1 downTo 0).map { offset ->
                val day = today.minusDays(offset.toLong())
                DayProgress(date = day, amount = amountOn(day), met = metOn(day))
            },
            recentLogs = logs.take(RECENT_LOGS).map { log ->
                RecentLog(
                    id = log.id,
                    date = log.date,
                    pages = log.pages,
                    minutes = log.minutes,
                    bookTitle = log.workKey?.let(titles::get),
                )
            },
            today = today,
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val DAYS_IN_CHART = 7
        const val RECENT_LOGS = 5
        const val MIDNIGHT_SLACK_MILLIS = 1_000L
    }
}

sealed interface StatsUiState {
    data object Loading : StatsUiState

    data class Success(
        val goal: DailyGoal,
        /** Today's amount in the goal's unit (pages or minutes). */
        val todayAmount: Int,
        val streak: StreakSummary,
        val finishedThisYear: Int,
        /** The last seven days, oldest first; the last entry is today. */
        val week: List<DayProgress>,
        val recentLogs: List<RecentLog>,
        val today: LocalDate,
    ) : StatsUiState {
        val todayProgress: Float get() = (todayAmount.toFloat() / goal.target).coerceIn(0f, 1f)
    }
}

data class DayProgress(val date: LocalDate, val amount: Int, val met: Boolean)

data class RecentLog(val id: String, val date: LocalDate, val pages: Int, val minutes: Int, val bookTitle: String?)
