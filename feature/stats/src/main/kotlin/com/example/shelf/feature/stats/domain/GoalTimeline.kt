package com.example.shelf.feature.stats.domain

import com.example.shelf.core.model.DailyGoal
import com.example.shelf.core.model.GoalChange
import java.time.LocalDate

/**
 * Which daily goal applied on a given day. A change applies from its `effectiveFrom` day until
 * the next change; days before the first recorded change use [initial] (the app default, since
 * every change is recorded).
 */
class GoalTimeline(changes: List<GoalChange> = emptyList(), private val initial: DailyGoal = DailyGoal.Default) {
    private val ordered = changes.sortedBy { it.effectiveFrom }

    fun goalOn(date: LocalDate): DailyGoal = ordered.lastOrNull { !it.effectiveFrom.isAfter(date) }?.goal ?: initial

    companion object {
        /** The same goal on every day. */
        fun constant(goal: DailyGoal) = GoalTimeline(initial = goal)

        /**
         * History from the database plus the current goal from settings, applied from [today]. If
         * the latest recorded change already matches the current goal nothing is added.
         */
        fun of(history: List<GoalChange>, current: DailyGoal, today: LocalDate): GoalTimeline {
            val latest = history.maxByOrNull { it.effectiveFrom }
            val changes = if (latest?.goal == current) history else history + GoalChange(today, current)
            return GoalTimeline(changes)
        }
    }
}
