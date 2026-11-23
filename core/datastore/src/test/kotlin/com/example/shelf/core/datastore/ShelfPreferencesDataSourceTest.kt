package com.example.shelf.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.shelf.core.model.DailyGoal
import com.example.shelf.core.model.GoalType
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.model.ThemeMode
import com.example.shelf.core.model.UserPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ShelfPreferencesDataSourceTest {
    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private fun TestScope.dataSource() = ShelfPreferencesDataSource(
        PreferenceDataStoreFactory.create(scope = backgroundScope) {
            tmpFolder.newFile("user.preferences_pb").also { it.delete() }
        },
    )

    @Test
    fun `defaults are returned before anything is written`() = runTest(UnconfinedTestDispatcher()) {
        assertEquals(UserPreferences(), dataSource().userPreferences.first())
    }

    @Test
    fun `every preference round-trips`() = runTest(UnconfinedTestDispatcher()) {
        val source = dataSource()

        source.setThemeMode(ThemeMode.DARK)
        source.setUseDynamicColor(false)
        source.setReminderEnabled(true)
        source.setReminderTime(LocalTime.of(7, 45))
        source.setDailyGoal(DailyGoal(GoalType.MINUTES, 45))
        source.setDefaultShelf(ShelfType.READING)
        source.setOnboardingCompleted(true)

        assertEquals(
            UserPreferences(
                themeMode = ThemeMode.DARK,
                useDynamicColor = false,
                reminderEnabled = true,
                reminderTime = LocalTime.of(7, 45),
                dailyGoal = DailyGoal(GoalType.MINUTES, 45),
                defaultShelf = ShelfType.READING,
                onboardingCompleted = true,
            ),
            source.userPreferences.first(),
        )
    }

    @Test
    fun `recent searches are newest first, de-duplicated and capped at ten`() = runTest(UnconfinedTestDispatcher()) {
        val source = dataSource()

        (1..12).forEach { source.addRecentSearch("query $it") }
        source.addRecentSearch("  QUERY 5 ")

        val recents = source.recentSearches.first()
        assertEquals(10, recents.size)
        assertEquals("QUERY 5", recents.first())
        assertEquals(1, recents.count { it.equals("query 5", ignoreCase = true) })
        assertEquals("query 12", recents[1])
    }

    @Test
    fun `blank searches are ignored and clearing empties the list`() = runTest(UnconfinedTestDispatcher()) {
        val source = dataSource()

        source.addRecentSearch("   ")
        assertEquals(emptyList(), source.recentSearches.first())

        source.addRecentSearch("dune")
        source.clearRecentSearches()
        assertEquals(emptyList(), source.recentSearches.first())
    }
}
