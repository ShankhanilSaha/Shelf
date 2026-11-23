package com.example.shelf.core.testing.repository

import com.example.shelf.core.data.repository.UserDataRepository
import com.example.shelf.core.model.DailyGoal
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.model.ThemeMode
import com.example.shelf.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalTime

class FakeUserDataRepository(initial: UserPreferences = UserPreferences()) : UserDataRepository {
    private val state = MutableStateFlow(initial)

    val current: UserPreferences get() = state.value

    override val userPreferences: Flow<UserPreferences> = state

    override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }

    override suspend fun setUseDynamicColor(enabled: Boolean) = state.update { it.copy(useDynamicColor = enabled) }

    override suspend fun setDefaultShelf(shelf: ShelfType) = state.update { it.copy(defaultShelf = shelf) }

    override suspend fun setOnboardingCompleted(completed: Boolean) =
        state.update { it.copy(onboardingCompleted = completed) }

    override suspend fun setReminderEnabled(enabled: Boolean) = state.update { it.copy(reminderEnabled = enabled) }

    override suspend fun setReminderTime(time: LocalTime) = state.update { it.copy(reminderTime = time) }

    override suspend fun setDailyGoal(goal: DailyGoal) = state.update { it.copy(dailyGoal = goal) }

    var reminderRefreshes = 0
        private set

    override suspend fun refreshReminderSchedule() {
        reminderRefreshes++
    }
}
