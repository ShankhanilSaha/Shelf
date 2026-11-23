package com.example.shelf.core.data.repository

import com.example.shelf.core.common.time.today
import com.example.shelf.core.data.reminder.ReminderScheduler
import com.example.shelf.core.database.dao.GoalDao
import com.example.shelf.core.database.model.GoalEntity
import com.example.shelf.core.datastore.ShelfPreferencesDataSource
import com.example.shelf.core.model.DailyGoal
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.model.ThemeMode
import com.example.shelf.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalTime
import javax.inject.Inject

internal class DefaultUserDataRepository @Inject constructor(
    private val preferences: ShelfPreferencesDataSource,
    private val goalDao: GoalDao,
    private val reminderScheduler: ReminderScheduler,
    private val clock: Clock,
) : UserDataRepository {

    override val userPreferences: Flow<UserPreferences> = preferences.userPreferences

    override suspend fun setThemeMode(mode: ThemeMode) = preferences.setThemeMode(mode)

    override suspend fun setUseDynamicColor(enabled: Boolean) = preferences.setUseDynamicColor(enabled)

    override suspend fun setDefaultShelf(shelf: ShelfType) = preferences.setDefaultShelf(shelf)

    override suspend fun setOnboardingCompleted(completed: Boolean) = preferences.setOnboardingCompleted(completed)

    /** Turning the reminder on schedules it for the saved time; turning it off cancels the work. */
    override suspend fun setReminderEnabled(enabled: Boolean) {
        preferences.setReminderEnabled(enabled)
        if (enabled) {
            reminderScheduler.schedule(userPreferences.first().reminderTime)
        } else {
            reminderScheduler.cancel()
        }
    }

    /** A new time re-enqueues the unique reminder work with a freshly computed delay. */
    override suspend fun setReminderTime(time: LocalTime) {
        preferences.setReminderTime(time)
        if (userPreferences.first().reminderEnabled) reminderScheduler.schedule(time)
    }

    /**
     * DataStore holds the current goal (the setting); the Room `goals` table keeps the history,
     * so a goal changed today applies from today without rewriting past days.
     */
    override suspend fun setDailyGoal(goal: DailyGoal) {
        preferences.setDailyGoal(goal)
        goalDao.upsert(
            GoalEntity(effectiveFromEpochDay = clock.today().toEpochDay(), type = goal.type, target = goal.target),
        )
    }

    /** Called at app start: recomputes the delay (the time zone may have changed since). */
    override suspend fun refreshReminderSchedule() {
        val current = userPreferences.first()
        if (current.reminderEnabled) reminderScheduler.schedule(current.reminderTime)
    }
}
