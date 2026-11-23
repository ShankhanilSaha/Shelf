package com.example.shelf.core.data.repository

import com.example.shelf.core.database.ShelfDatabase
import com.example.shelf.core.database.model.GoalEntity
import com.example.shelf.core.model.DailyGoal
import com.example.shelf.core.model.GoalType
import com.example.shelf.core.testing.MutableClock
import com.example.shelf.core.testing.inMemoryShelfDatabase
import com.example.shelf.core.testing.sync.FakeSyncScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ReadingLogRepositoryTest {
    private lateinit var db: ShelfDatabase
    private val syncScheduler = FakeSyncScheduler()
    private lateinit var repository: OfflineFirstReadingLogRepository

    @Before
    fun setUp() {
        db = inMemoryShelfDatabase(RuntimeEnvironment.getApplication())
        repository = OfflineFirstReadingLogRepository(db.readingLogDao(), db.goalDao(), MutableClock(), syncScheduler)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `logs are stored by local day and filtered by date`() = runTest {
        val monday = LocalDate.of(2026, 11, 23)
        repository.addLog(monday.minusDays(3), pages = 10, minutes = 0, workKey = null)
        val recent = repository.addLog(monday, pages = 0, minutes = 25, workKey = "OL1W")

        assertEquals(2, repository.observeLogs().first().size)
        assertEquals(listOf(recent), repository.observeLogsSince(monday.minusDays(1)).first())

        repository.deleteLog(recent.id)
        assertEquals(1, repository.observeLogs().first().size)
    }

    @Test
    fun `adding and deleting ask for a sync and deleting leaves a tombstone`() = runTest {
        val log = repository.addLog(LocalDate.of(2026, 11, 24), pages = 5, minutes = 0, workKey = null)
        repository.deleteLog(log.id)
        repository.deleteLog(log.id) // already deleted: nothing to sync

        assertEquals(2, syncScheduler.requests)
        val tombstone = db.readingLogDao().getForSync(log.id)!!
        assertTrue(tombstone.deleted)
        assertTrue(tombstone.dirty)
    }

    @Test
    fun `goal history is returned oldest first`() = runTest {
        db.goalDao().upsert(GoalEntity(effectiveFromEpochDay = 200, type = GoalType.MINUTES, target = 30))
        db.goalDao().upsert(GoalEntity(effectiveFromEpochDay = 100, type = GoalType.PAGES, target = 20))

        val history = repository.observeGoalHistory().first()

        assertEquals(listOf(LocalDate.ofEpochDay(100), LocalDate.ofEpochDay(200)), history.map { it.effectiveFrom })
        assertEquals(DailyGoal(GoalType.MINUTES, 30), history.last().goal)
    }
}
