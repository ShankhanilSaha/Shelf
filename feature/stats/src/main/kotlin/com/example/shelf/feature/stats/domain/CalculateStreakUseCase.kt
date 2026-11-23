package com.example.shelf.feature.stats.domain

import com.example.shelf.core.model.DailyGoal
import com.example.shelf.core.model.ReadingLog
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * @property current consecutive days meeting the goal, ending today, or ending yesterday while
 *   today is still in progress
 * @property longest the longest run ever, up to and including today
 * @property todayMet whether today already counts
 */
data class StreakSummary(val current: Int, val longest: Int, val todayMet: Boolean)

/**
 * Pure streak calculation. Logs carry the local calendar day they belong to; [Clock] decides
 * which day is today, so time-zone changes move "today" but never move a logged day. Days after
 * today (possible right after flying west) are ignored until they arrive. Daylight-saving changes
 * cannot create gaps because only calendar days are compared.
 */
class CalculateStreakUseCase @Inject constructor() {

    operator fun invoke(logs: List<ReadingLog>, goal: DailyGoal, clock: Clock): StreakSummary =
        invoke(logs, GoalTimeline.constant(goal), clock)

    operator fun invoke(logs: List<ReadingLog>, goals: GoalTimeline, clock: Clock): StreakSummary {
        val today = LocalDate.now(clock)
        val metDays: Set<LocalDate> = logs
            .filter { !it.date.isAfter(today) }
            .groupBy { it.date }
            .filter { (date, dayLogs) ->
                goals.goalOn(date).isMetBy(pages = dayLogs.sumOf { it.pages }, minutes = dayLogs.sumOf { it.minutes })
            }
            .keys

        val todayMet = today in metDays
        var current = 0
        var cursor = if (todayMet) today else today.minusDays(1)
        while (cursor in metDays) {
            current++
            cursor = cursor.minusDays(1)
        }

        return StreakSummary(current = current, longest = longestRun(metDays), todayMet = todayMet)
    }

    private fun longestRun(days: Set<LocalDate>): Int {
        var longest = 0
        var run = 0
        var previous: LocalDate? = null
        for (day in days.sorted()) {
            run = if (previous != null && day == previous.plusDays(1)) run + 1 else 1
            longest = maxOf(longest, run)
            previous = day
        }
        return longest
    }
}
