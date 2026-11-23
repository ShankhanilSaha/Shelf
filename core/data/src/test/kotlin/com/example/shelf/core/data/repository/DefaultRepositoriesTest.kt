package com.example.shelf.core.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.paging.testing.asSnapshot
import com.example.shelf.core.database.ShelfDatabase
import com.example.shelf.core.datastore.ShelfPreferencesDataSource
import com.example.shelf.core.model.DailyGoal
import com.example.shelf.core.model.GoalType
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.model.ThemeMode
import com.example.shelf.core.network.model.SearchDocDto
import com.example.shelf.core.network.model.SearchResponseDto
import com.example.shelf.core.testing.MutableClock
import com.example.shelf.core.testing.inMemoryShelfDatabase
import com.example.shelf.core.testing.network.FakeOpenLibraryDataSource
import com.example.shelf.core.testing.reminder.FakeReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.Duration
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class DefaultRepositoriesTest {
    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private lateinit var db: ShelfDatabase
    private val clock = MutableClock()

    @Before
    fun setUp() {
        db = inMemoryShelfDatabase(RuntimeEnvironment.getApplication())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun TestScope.preferences() = ShelfPreferencesDataSource(
        PreferenceDataStoreFactory.create(scope = backgroundScope) {
            tmpFolder.newFile("prefs.preferences_pb").also { it.delete() }
        },
    )

    @Test
    fun `search repository pages results from the network and keeps recents`() = runTest {
        val network = FakeOpenLibraryDataSource()
        network.searchResponse = { query, _ ->
            SearchResponseDto(
                numFound = 2,
                docs = listOf(SearchDocDto("/works/OL1W", "$query 1"), SearchDocDto("/works/OL2W", "$query 2")),
            )
        }
        val repository = DefaultSearchRepository(network, preferences())

        val titles = repository.search("dune").asSnapshot().map { it.title }
        repository.addRecentSearch("dune")

        assertEquals(listOf("dune 1", "dune 2"), titles)
        assertEquals(listOf("dune"), repository.recentSearches.first())
        repository.clearRecentSearches()
        assertEquals(emptyList(), repository.recentSearches.first())
    }

    @Test
    fun `user data repository stores settings and the goal history`() = runTest {
        val scheduler = FakeReminderScheduler()
        val repository = DefaultUserDataRepository(preferences(), db.goalDao(), scheduler, clock)

        repository.setThemeMode(ThemeMode.DARK)
        repository.setUseDynamicColor(false)
        repository.setDefaultShelf(ShelfType.READING)
        repository.setOnboardingCompleted(true)
        repository.setReminderEnabled(true)
        repository.setReminderTime(LocalTime.of(21, 15))
        repository.setDailyGoal(DailyGoal(GoalType.PAGES, 30))
        clock.advanceBy(Duration.ofDays(2))
        repository.setDailyGoal(DailyGoal(GoalType.MINUTES, 25))

        val prefs = repository.userPreferences.first()
        assertEquals(ThemeMode.DARK, prefs.themeMode)
        assertEquals(false, prefs.useDynamicColor)
        assertEquals(ShelfType.READING, prefs.defaultShelf)
        assertEquals(true, prefs.onboardingCompleted)
        assertEquals(true, prefs.reminderEnabled)
        assertEquals(LocalTime.of(21, 15), prefs.reminderTime)
        assertEquals(DailyGoal(GoalType.MINUTES, 25), prefs.dailyGoal)
        assertEquals(listOf(30, 25), db.goalDao().observeHistory().first().map { it.target })
    }

    @Test
    fun `reminder settings schedule, reschedule and cancel the daily work`() = runTest {
        val scheduler = FakeReminderScheduler()
        val repository = DefaultUserDataRepository(preferences(), db.goalDao(), scheduler, clock)

        repository.setReminderTime(LocalTime.of(6, 45))
        assertEquals(emptyList(), scheduler.calls)

        repository.setReminderEnabled(true)
        assertEquals(LocalTime.of(6, 45), scheduler.scheduledFor)

        repository.setReminderTime(LocalTime.of(22, 0))
        repository.refreshReminderSchedule()
        assertEquals(listOf("schedule 06:45", "schedule 22:00", "schedule 22:00"), scheduler.calls)

        repository.setReminderEnabled(false)
        assertNull(scheduler.scheduledFor)
    }
}
