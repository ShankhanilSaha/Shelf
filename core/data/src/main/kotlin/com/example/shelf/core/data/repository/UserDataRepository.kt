package com.example.shelf.core.data.repository

import com.example.shelf.core.model.DailyGoal
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.model.ThemeMode
import com.example.shelf.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

/** App settings, backed by Preferences DataStore. */
interface UserDataRepository {
    val userPreferences: Flow<UserPreferences>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setUseDynamicColor(enabled: Boolean)

    suspend fun setDefaultShelf(shelf: ShelfType)

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun setReminderEnabled(enabled: Boolean)

    suspend fun setReminderTime(time: LocalTime)

    suspend fun setDailyGoal(goal: DailyGoal)

    /** Re-arms the daily reminder if it is enabled; called when the app starts. */
    suspend fun refreshReminderSchedule()
}
