package com.example.shelf.feature.stats

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.shelf.core.model.DailyGoal
import com.example.shelf.core.model.GoalType
import com.example.shelf.core.ui.theme.ShelfTheme
import com.example.shelf.feature.stats.domain.StreakSummary
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Runs the Accessibility Test Framework over the whole screen: touch target size, text and image
 * contrast on the real rendering, missing labels, duplicate clickable areas. A finding of type
 * ERROR fails the test. Needs a device or emulator on API 34+.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class StatsAccessibilityTest {
    private val today = LocalDate.of(2026, 11, 24)

    private fun check(dark: Boolean) = runComposeUiTest {
        enableAccessibilityChecks()
        setContent {
            ShelfTheme(darkTheme = dark, dynamicColor = false) {
                StatsScreen(
                    uiState = StatsUiState.Success(
                        goal = DailyGoal(GoalType.PAGES, 20),
                        todayAmount = 12,
                        streak = StreakSummary(current = 2, longest = 9, todayMet = false),
                        finishedThisYear = 7,
                        week = listOf(25, 0, 32, 20, 8, 41, 12).mapIndexed { index, amount ->
                            DayProgress(today.minusDays((6 - index).toLong()), amount, met = amount >= 20)
                        },
                        recentLogs = listOf(RecentLog("1", today, pages = 12, minutes = 0, bookTitle = "Piranesi")),
                        today = today,
                    ),
                    onLogReadingClick = {},
                    onDeleteLog = {},
                )
            }
        }
        onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun stats_light() = check(dark = false)

    @Test
    fun stats_dark() = check(dark = true)
}
