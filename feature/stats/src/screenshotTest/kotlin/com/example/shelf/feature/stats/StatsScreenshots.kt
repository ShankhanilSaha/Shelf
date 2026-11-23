package com.example.shelf.feature.stats

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import com.example.shelf.core.model.DailyGoal
import com.example.shelf.core.model.GoalType
import com.example.shelf.core.ui.preview.ScreenPreviews
import com.example.shelf.core.ui.theme.ShelfTheme
import com.example.shelf.feature.stats.domain.StreakSummary
import java.time.LocalDate

private val today = LocalDate.of(2026, 11, 24)
private val amounts = listOf(25, 0, 32, 20, 8, 41, 12)

@PreviewTest
@ScreenPreviews
@Composable
fun StatsWithAWeekOfReading() {
    ShelfTheme(dynamicColor = false) {
        StatsScreen(
            uiState = StatsUiState.Success(
                goal = DailyGoal(GoalType.PAGES, 20),
                todayAmount = 12,
                streak = StreakSummary(current = 2, longest = 9, todayMet = false),
                finishedThisYear = 7,
                week = amounts.mapIndexed { index, amount ->
                    DayProgress(date = today.minusDays((6 - index).toLong()), amount = amount, met = amount >= 20)
                },
                recentLogs = listOf(
                    RecentLog("1", today, pages = 12, minutes = 0, bookTitle = "The Left Hand of Darkness"),
                    RecentLog("2", today.minusDays(1), pages = 41, minutes = 50, bookTitle = null),
                ),
                today = today,
            ),
            onLogReadingClick = {},
            onDeleteLog = {},
        )
    }
}
