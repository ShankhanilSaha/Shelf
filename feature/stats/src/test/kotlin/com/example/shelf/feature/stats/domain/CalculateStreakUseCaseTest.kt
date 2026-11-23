package com.example.shelf.feature.stats.domain

import com.example.shelf.core.model.DailyGoal
import com.example.shelf.core.model.GoalChange
import com.example.shelf.core.model.GoalType
import com.example.shelf.core.model.ReadingLog
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Written before the implementation. Dates are local calendar days (what the user saw when they
 * logged); the clock decides which day is "today".
 */
class CalculateStreakUseCaseTest {
    private val calculate = CalculateStreakUseCase()
    private val pages20 = DailyGoal(GoalType.PAGES, 20)
    private val utc = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 11, 24)

    private fun clockAt(dateTime: String, zone: ZoneId = utc): Clock =
        Clock.fixed(ZonedDateTime.of(LocalDateTime.parse(dateTime), zone).toInstant(), zone)

    private val noon = clockAt("2026-11-24T12:00")
    private var nextId = 0

    private fun log(daysAgo: Long, pages: Int = 0, minutes: Int = 0, from: LocalDate = today) =
        ReadingLog(id = "${nextId++}", date = from.minusDays(daysAgo), pages = pages, minutes = minutes)

    @Test
    fun `no logs means no streak`() {
        assertEquals(StreakSummary(current = 0, longest = 0, todayMet = false), calculate(emptyList(), pages20, noon))
    }

    @Test
    fun `meeting the goal today starts a streak of one`() {
        val summary = calculate(listOf(log(0, pages = 20)), pages20, noon)

        assertEquals(StreakSummary(current = 1, longest = 1, todayMet = true), summary)
    }

    @Test
    fun `logs on the same day are added up`() {
        val summary = calculate(listOf(log(0, pages = 12), log(0, pages = 8)), pages20, noon)

        assertEquals(1, summary.current)
    }

    @Test
    fun `a day below the goal does not count`() {
        val summary = calculate(listOf(log(0, pages = 19)), pages20, noon)

        assertEquals(StreakSummary(current = 0, longest = 0, todayMet = false), summary)
    }

    @Test
    fun `the streak stays alive until today is over`() {
        val summary = calculate(listOf(log(2, pages = 30), log(1, pages = 25)), pages20, noon)

        assertEquals(StreakSummary(current = 2, longest = 2, todayMet = false), summary)
    }

    @Test
    fun `missing yesterday and today ends the streak`() {
        val summary = calculate(listOf(log(4, pages = 30), log(3, pages = 30), log(2, pages = 30)), pages20, noon)

        assertEquals(StreakSummary(current = 0, longest = 3, todayMet = false), summary)
    }

    @Test
    fun `a gap splits runs and the longest run is remembered`() {
        val logs = listOf(5L, 4L, 3L, 1L, 0L).map { log(it, pages = 20) }

        val summary = calculate(logs, pages20, noon)

        assertEquals(StreakSummary(current = 2, longest = 3, todayMet = true), summary)
    }

    @Test
    fun `a minutes goal ignores pages`() {
        val minutes15 = DailyGoal(GoalType.MINUTES, 15)

        assertEquals(0, calculate(listOf(log(0, pages = 300, minutes = 10)), minutes15, noon).current)
        assertEquals(1, calculate(listOf(log(0, pages = 0, minutes = 15)), minutes15, noon).current)
    }

    @Test
    fun `across midnight the new day starts empty but yesterday still carries the streak`() {
        // Read at 23:59 on the 23rd and 00:01 on the 24th: two different days.
        val logs = listOf(log(1, pages = 20), log(0, pages = 5))
        val justAfterMidnight = clockAt("2026-11-24T00:01")

        assertEquals(
            StreakSummary(current = 1, longest = 1, todayMet = false),
            calculate(logs, pages20, justAfterMidnight),
        )
        val beforeMidnight = clockAt("2026-11-23T23:59")
        assertEquals(
            StreakSummary(current = 1, longest = 1, todayMet = true),
            calculate(logs.take(1), pages20, beforeMidnight),
        )
    }

    @Test
    fun `the day boundary follows the clock's time zone`() {
        // 03:00 UTC on the 25th is still the 24th in Los Angeles.
        val instant = ZonedDateTime.of(2026, 11, 25, 3, 0, 0, 0, utc).toInstant()
        val logs = listOf(log(0, pages = 20)) // read on the 24th

        val inUtc = calculate(logs, pages20, Clock.fixed(instant, utc))
        val inLosAngeles = calculate(logs, pages20, Clock.fixed(instant, ZoneId.of("America/Los_Angeles")))

        assertEquals(StreakSummary(current = 1, longest = 1, todayMet = false), inUtc)
        assertEquals(StreakSummary(current = 1, longest = 1, todayMet = true), inLosAngeles)
    }

    @Test
    fun `a log dated after today (after flying west) waits for its day`() {
        val logs = listOf(log(1, pages = 20), log(-1, pages = 20))

        val summary = calculate(logs, pages20, noon)

        assertEquals(StreakSummary(current = 1, longest = 1, todayMet = false), summary)
    }

    @Test
    fun `days are calendar days, so a daylight saving change does not create a gap`() {
        // US daylight saving ended on 2026-11-01 (a 25 hour day).
        val newYork = ZoneId.of("America/New_York")
        val nov2 = LocalDate.of(2026, 11, 2)
        val logs =
            listOf(log(2, pages = 20, from = nov2), log(1, pages = 20, from = nov2), log(0, pages = 20, from = nov2))

        val summary = calculate(logs, pages20, clockAt("2026-11-02T08:00", newYork))

        assertEquals(StreakSummary(current = 3, longest = 3, todayMet = true), summary)
    }

    @Test
    fun `raising the goal does not rewrite days that met the old goal`() {
        val goals = GoalTimeline(
            changes = listOf(
                GoalChange(effectiveFrom = today.minusDays(10), goal = DailyGoal(GoalType.PAGES, 10)),
                GoalChange(effectiveFrom = today, goal = DailyGoal(GoalType.PAGES, 50)),
            ),
        )
        val logs = listOf(log(2, pages = 12), log(1, pages = 15), log(0, pages = 20))

        val summary = calculate(logs, goals, noon)

        assertEquals(StreakSummary(current = 2, longest = 2, todayMet = false), summary)
    }

    @Test
    fun `days before the first recorded change use the initial goal`() {
        val goals = GoalTimeline(
            changes = listOf(GoalChange(effectiveFrom = today, goal = DailyGoal(GoalType.MINUTES, 30))),
            initial = DailyGoal(GoalType.PAGES, 20),
        )

        assertEquals(DailyGoal(GoalType.PAGES, 20), goals.goalOn(today.minusDays(1)))
        assertEquals(DailyGoal(GoalType.MINUTES, 30), goals.goalOn(today))
        assertEquals(DailyGoal(GoalType.MINUTES, 30), goals.goalOn(today.plusDays(3)))
    }
}
