package com.example.shelf.feature.stats.ui

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import com.example.shelf.core.model.GoalType
import com.example.shelf.core.ui.theme.ShelfTheme
import com.example.shelf.feature.stats.DayProgress
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en")
class ChartsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun weeklyChart_hasATextAlternativeListingEveryDay() {
        val monday = LocalDate.of(2026, 11, 23)
        val days = (0L..6L).map { DayProgress(monday.plusDays(it), amount = it.toInt() * 5, met = it >= 4) }
        composeRule.setContent {
            ShelfTheme(dynamicColor = false) { WeeklyBarChart(days = days, target = 20, goalType = GoalType.PAGES) }
        }

        val description = composeRule.onNodeWithTag(StatsTestTags.WEEK_CHART)
            .fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription]
            .single()

        assertTrue(description.startsWith("Reading in the last 7 days: Monday 0 pages, Tuesday 5 pages"), description)
        assertTrue(description.endsWith("Sunday 30 pages. Daily goal 20 pages."), description)
    }

    @Test
    fun goalRing_reportsProgressToAccessibilityServices() {
        composeRule.setContent {
            ShelfTheme(dynamicColor = false) {
                GoalRing(progress = 0.6f, amount = 12, target = 20, goalType = GoalType.PAGES)
            }
        }

        composeRule.onNodeWithContentDescription("12 of 20 pages")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo(
                        0.6f,
                        0f..1f,
                    ),
                ),
            )
    }
}
