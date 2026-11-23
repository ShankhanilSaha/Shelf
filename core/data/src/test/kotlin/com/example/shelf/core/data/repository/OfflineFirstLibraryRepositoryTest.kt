package com.example.shelf.core.data.repository

import app.cash.turbine.test
import com.example.shelf.core.data.mapper.toEntity
import com.example.shelf.core.database.ShelfDatabase
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.testing.MutableClock
import com.example.shelf.core.testing.TestData
import com.example.shelf.core.testing.inMemoryShelfDatabase
import com.example.shelf.core.testing.sync.FakeSyncScheduler
import com.example.shelf.core.testing.transactionRunner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class OfflineFirstLibraryRepositoryTest {
    private lateinit var db: ShelfDatabase
    private val clock = MutableClock()
    private val syncScheduler = FakeSyncScheduler()
    private lateinit var repository: OfflineFirstLibraryRepository

    @Before
    fun setUp() = runTest {
        db = inMemoryShelfDatabase(RuntimeEnvironment.getApplication())
        repository =
            OfflineFirstLibraryRepository(db.shelfDao(), db.bookDao(), db.transactionRunner(), clock, syncScheduler)
        (1..3).forEach { db.bookDao().upsert(TestData.summary(it, pageCount = 200).toEntity()) }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `every change is marked for upload and asks for a sync`() = runTest {
        repository.setShelf("OL1W", ShelfType.READING)
        repository.setCurrentPage("OL1W", 12)
        repository.setRating("OL1W", 4)
        repository.setNotes("OL1W", "Good")
        repository.setNotes("OL1W", "Good") // no change, no sync

        assertEquals(4, syncScheduler.requests)
        assertTrue(db.shelfDao().getEntryForSync("OL1W")!!.dirty)
    }

    @Test
    fun `removing leaves a tombstone for sync and undo brings the book back`() = runTest {
        repository.setShelf("OL1W", ShelfType.READING)
        clock.advanceBy(Duration.ofMinutes(1))

        val removed = repository.remove("OL1W")!!

        assertNull(repository.observeEntry("OL1W").first())
        val tombstone = db.shelfDao().getEntryForSync("OL1W")!!
        assertTrue(tombstone.deleted)
        assertTrue(tombstone.dirty)
        assertEquals(clock.millis(), tombstone.updatedAt)
        assertEquals(2, syncScheduler.requests)
        assertNull(repository.remove("OL1W")) // already gone

        repository.restore(removed)

        assertEquals(ShelfType.READING, repository.observeEntry("OL1W").first()?.shelf)
        assertFalse(db.shelfDao().getEntryForSync("OL1W")!!.deleted)
        assertEquals(3, syncScheduler.requests)
    }

    @Test
    fun `adding a book creates an entry on that shelf`() = runTest {
        val change = repository.setShelf("OL1W", ShelfType.WANT_TO_READ)

        assertNull(change.previous)
        assertFalse(change.justFinished)
        val entry = repository.observeEntry("OL1W").first()!!
        assertEquals(ShelfType.WANT_TO_READ, entry.shelf)
        assertEquals(clock.now, entry.addedAt)
    }

    @Test
    fun `each shelf lists its books most recently updated first`() = runTest {
        repository.setShelf("OL1W", ShelfType.READING)
        clock.advanceBy(Duration.ofMinutes(1))
        repository.setShelf("OL2W", ShelfType.READING)
        clock.advanceBy(Duration.ofMinutes(1))
        repository.setShelf("OL3W", ShelfType.FINISHED)

        repository.observeShelf(ShelfType.READING).test {
            assertEquals(listOf("OL2W", "OL1W"), awaitItem().map { it.book.workKey })
            clock.advanceBy(Duration.ofMinutes(1))
            repository.setCurrentPage("OL1W", 10)
            assertEquals(listOf("OL1W", "OL2W"), awaitItem().map { it.book.workKey })
        }
    }

    @Test
    fun `moving to finished stamps the date and completes the pages, moving away clears it`() = runTest {
        repository.setShelf("OL1W", ShelfType.READING)
        clock.advanceBy(Duration.ofDays(1))

        val finished = repository.setShelf("OL1W", ShelfType.FINISHED)

        assertTrue(finished.justFinished)
        assertEquals(clock.now, finished.entry.finishedAt)
        assertEquals(200, finished.entry.currentPage)
        assertEquals(ShelfType.READING, finished.previous?.shelf)

        val reread = repository.setShelf("OL1W", ShelfType.READING)
        assertFalse(reread.justFinished)
        assertNull(reread.entry.finishedAt)
    }

    @Test
    fun `progress moves want-to-read to reading and the last page to finished`() = runTest {
        repository.setShelf("OL1W", ShelfType.WANT_TO_READ)

        val started = repository.setCurrentPage("OL1W", 12)
        assertEquals(ShelfType.READING, started.entry.shelf)
        assertFalse(started.justFinished)

        val done = repository.setCurrentPage("OL1W", 999)
        assertEquals(200, done.entry.currentPage)
        assertEquals(ShelfType.FINISHED, done.entry.shelf)
        assertTrue(done.justFinished)
        assertEquals(clock.now, done.entry.finishedAt)
    }

    @Test
    fun `negative pages clamp to zero`() = runTest {
        repository.setShelf("OL1W", ShelfType.READING)

        assertEquals(0, repository.setCurrentPage("OL1W", -5).entry.currentPage)
    }

    @Test
    fun `progress for a book outside the library is rejected`() = runTest {
        assertFailsWith<IllegalArgumentException> { repository.setCurrentPage("OL1W", 3) }
    }

    @Test
    fun `rating and notes are saved and validated`() = runTest {
        repository.setShelf("OL1W", ShelfType.FINISHED)

        repository.setRating("OL1W", 4)
        repository.setNotes("OL1W", "Loved the ending.")

        val entry = repository.observeEntry("OL1W").first()!!
        assertEquals(4, entry.rating)
        assertEquals("Loved the ending.", entry.notes)
        assertFailsWith<IllegalArgumentException> { repository.setRating("OL1W", 6) }
    }

    @Test
    fun `remove returns the entry and restore puts it back`() = runTest {
        repository.setShelf("OL1W", ShelfType.READING)
        repository.setCurrentPage("OL1W", 50)

        val removed = repository.remove("OL1W")!!
        assertNull(repository.observeEntry("OL1W").first())
        assertNull(repository.remove("OL1W"))

        repository.restore(removed)
        val restored = repository.observeEntry("OL1W").first()!!
        assertEquals(50, restored.currentPage)
        assertEquals(ShelfType.READING, restored.shelf)
    }

    @Test
    fun `books finished this year are counted in the local calendar year`() = runTest {
        clock.now = Instant.parse("2026-12-31T23:30:00Z")
        repository.setShelf("OL1W", ShelfType.FINISHED)
        clock.now = Instant.parse("2027-01-01T00:30:00Z")
        repository.setShelf("OL2W", ShelfType.FINISHED)

        assertEquals(1, repository.observeFinishedCount(2026).first())
        assertEquals(1, repository.observeFinishedCount(2027).first())
        assertEquals(2, repository.observeLibrary().first().size)
    }
}
