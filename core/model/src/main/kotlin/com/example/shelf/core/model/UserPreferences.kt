package com.example.shelf.core.model

import java.time.LocalTime

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime = DEFAULT_REMINDER_TIME,
    val dailyGoal: DailyGoal = DailyGoal.Default,
    val defaultShelf: ShelfType = ShelfType.WANT_TO_READ,
    val onboardingCompleted: Boolean = false,
) {
    companion object {
        val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.of(20, 0)
        const val MAX_RECENT_SEARCHES = 10
    }
}
