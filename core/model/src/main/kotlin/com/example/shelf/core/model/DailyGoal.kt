package com.example.shelf.core.model

import java.time.LocalDate

enum class GoalType {
    PAGES,
    MINUTES,
}

/** How much the user wants to read per day. */
data class DailyGoal(val type: GoalType, val target: Int) {
    init {
        require(target in TARGET_RANGE) { "target must be in $TARGET_RANGE" }
    }

    /** The amount that counts towards this goal. */
    fun amountOf(pages: Int, minutes: Int): Int = when (type) {
        GoalType.PAGES -> pages
        GoalType.MINUTES -> minutes
    }

    fun isMetBy(pages: Int, minutes: Int): Boolean = amountOf(pages, minutes) >= target

    companion object {
        val TARGET_RANGE = 1..1_000
        val Default = DailyGoal(type = GoalType.PAGES, target = 20)
    }
}

/** A goal that applies from [effectiveFrom] until the next change. */
data class GoalChange(val effectiveFrom: LocalDate, val goal: DailyGoal)
