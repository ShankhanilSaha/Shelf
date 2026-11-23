package com.example.shelf.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.shelf.core.model.DailyGoal
import com.example.shelf.core.model.GoalType
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.model.ThemeMode
import com.example.shelf.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.LocalTime
import javax.inject.Inject

/**
 * Preferences DataStore: theme, dynamic color, reminder, daily goal, default shelf, onboarding
 * flag and recent searches. Every value has a default, so a fresh install needs no setup.
 */
class ShelfPreferencesDataSource @Inject constructor(private val dataStore: DataStore<Preferences>) {

    val userPreferences: Flow<UserPreferences> = dataStore.data
        .map { it.toUserPreferences() }
        .distinctUntilChanged()

    /** Most recent first, at most [UserPreferences.MAX_RECENT_SEARCHES]. */
    val recentSearches: Flow<List<String>> = dataStore.data
        .map { decodeSearches(it[Keys.RECENT_SEARCHES]) }
        .distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME_MODE] = mode.name }

    suspend fun setUseDynamicColor(enabled: Boolean) = edit { it[Keys.DYNAMIC_COLOR] = enabled }

    suspend fun setReminderEnabled(enabled: Boolean) = edit { it[Keys.REMINDER_ENABLED] = enabled }

    suspend fun setReminderTime(time: LocalTime) = edit {
        it[Keys.REMINDER_MINUTE_OF_DAY] =
            time.toSecondOfDay() / SECONDS_PER_MINUTE
    }

    suspend fun setDailyGoal(goal: DailyGoal) = edit {
        it[Keys.GOAL_TYPE] = goal.type.name
        it[Keys.GOAL_TARGET] = goal.target
    }

    suspend fun setDefaultShelf(shelf: ShelfType) = edit { it[Keys.DEFAULT_SHELF] = shelf.name }

    suspend fun setOnboardingCompleted(completed: Boolean) = edit { it[Keys.ONBOARDING_COMPLETED] = completed }

    /** Moves [query] to the front (case-insensitive de-duplication) and keeps the newest ten. */
    suspend fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        edit { prefs ->
            val updated = (listOf(trimmed) + decodeSearches(prefs[Keys.RECENT_SEARCHES]))
                .distinctBy { it.lowercase() }
                .take(UserPreferences.MAX_RECENT_SEARCHES)
            prefs[Keys.RECENT_SEARCHES] = json.encodeToString(searchesSerializer, updated)
        }
    }

    suspend fun clearRecentSearches() = edit { it.remove(Keys.RECENT_SEARCHES) }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }

    private fun Preferences.toUserPreferences(): UserPreferences {
        val defaults = UserPreferences()
        val goalTarget = this[Keys.GOAL_TARGET]?.takeIf { it in DailyGoal.TARGET_RANGE }
        val goalType = enumOrNull<GoalType>(this[Keys.GOAL_TYPE])
        return UserPreferences(
            themeMode = enumOrNull<ThemeMode>(this[Keys.THEME_MODE]) ?: defaults.themeMode,
            useDynamicColor = this[Keys.DYNAMIC_COLOR] ?: defaults.useDynamicColor,
            reminderEnabled = this[Keys.REMINDER_ENABLED] ?: defaults.reminderEnabled,
            reminderTime = this[Keys.REMINDER_MINUTE_OF_DAY]
                ?.takeIf { it in 0 until MINUTES_PER_DAY }
                ?.let { LocalTime.ofSecondOfDay(it.toLong() * SECONDS_PER_MINUTE) }
                ?: defaults.reminderTime,
            dailyGoal = if (goalType != null &&
                goalTarget != null
            ) {
                DailyGoal(goalType, goalTarget)
            } else {
                defaults.dailyGoal
            },
            defaultShelf = enumOrNull<ShelfType>(this[Keys.DEFAULT_SHELF]) ?: defaults.defaultShelf,
            onboardingCompleted = this[Keys.ONBOARDING_COMPLETED] ?: defaults.onboardingCompleted,
        )
    }

    private fun decodeSearches(raw: String?): List<String> = if (raw == null) {
        emptyList()
    } else {
        try {
            json.decodeFromString(searchesSerializer, raw)
        } catch (_: SerializationException) {
            emptyList()
        }
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_MINUTE_OF_DAY = intPreferencesKey("reminder_minute_of_day")
        val GOAL_TYPE = stringPreferencesKey("goal_type")
        val GOAL_TARGET = intPreferencesKey("goal_target")
        val DEFAULT_SHELF = stringPreferencesKey("default_shelf")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val RECENT_SEARCHES = stringPreferencesKey("recent_searches")
    }

    private companion object {
        const val SECONDS_PER_MINUTE = 60
        const val MINUTES_PER_DAY = 24 * 60
        val json = Json { ignoreUnknownKeys = true }
        val searchesSerializer = ListSerializer(String.serializer())

        inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? = enumValues<T>().firstOrNull { it.name == name }
    }
}
