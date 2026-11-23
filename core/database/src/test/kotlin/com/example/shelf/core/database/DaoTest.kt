package com.example.shelf.core.database

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import app.cash.turbine.test
import com.example.shelf.core.database.model.BookEntity
import com.example.shelf.core.database.model.GoalEntity
import com.example.shelf.core.database.model.ReadingLogEntity
import com.example.shelf.core.database.model.ShelfEntryEntity
import com.example.shelf.core.database.model.SyncStateEntity
import com.example.shelf.core.model.GoalType
import com.example.shelf.core.model.ShelfType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class DaoTest {
    private lateinit var db: ShelfDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), ShelfDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun book(id: Int, pages: Int? = 300, fetchedAt: Long? = null) = BookEntity(
        workKey = "OL${id}W",
        title = "Book $id",
        authors = listOf("Author $id", "Co-author"),
        firstPublishYear = 2000,
        coverId = 10L + id,
        description = null,
        subjects = listOf("Fiction", "Space"),
        pageCount = pages,
        isbn = null,
        detailsFetchedAt = fetchedAt,
    )

    private fun entry(id: Int, shelf: ShelfType, updatedAt: Long, finishedAt: Long? = null) = ShelfEntryEntity(
        workKey = "OL${id}W",
        shelf = shelf,
        currentPage = 0,
        rating = null,
        notes = "",
        addedAt = updatedAt,
        updatedAt = updatedAt,
        finishedAt = finishedAt,
    )

    @Test
    fun bookLists_roundTripThroughTheConverter() = runTest {
        db.bookDao().upsert(book(1))

        val stored = db.bookDao().getBook("OL1W")

        assertEquals(listOf("Author 1", "Co-author"), stored?.authors)
        assertEquals(listOf("Fiction", "Space"), stored?.subjects)
    }

    @Test
    fun insertIfAbsent_neverOverwritesFetchedDetails() = runTest {
        db.bookDao().upsert(book(1, fetchedAt = 99).copy(description = "Full details"))

        val rowId = db.bookDao().insertIfAbsent(book(1).copy(title = "Search title"))

        assertEquals(-1, rowId)
        assertEquals("Full details", db.bookDao().getBook("OL1W")?.description)
    }

    @Test
    fun observeBook_emitsUpdates() = runTest {
        db.bookDao().observeBook("OL1W").test {
            assertNull(awaitItem())
            db.bookDao().upsert(book(1))
            assertEquals("Book 1", awaitItem()?.title)
            db.bookDao().upsert(book(1).copy(title = "Renamed"))
            assertEquals("Renamed", awaitItem()?.title)
        }
    }

    @Test
    fun observeShelf_filtersByShelfAndSortsByLastUpdate() = runTest {
        (1..3).forEach { db.bookDao().upsert(book(it)) }
        db.shelfDao().upsert(entry(1, ShelfType.READING, updatedAt = 100))
        db.shelfDao().upsert(entry(2, ShelfType.READING, updatedAt = 300))
        db.shelfDao().upsert(entry(3, ShelfType.FINISHED, updatedAt = 200))

        val reading = db.shelfDao().observeShelf(ShelfType.READING).first()

        assertEquals(listOf("OL2W", "OL1W"), reading.map { it.entry.workKey })
        assertEquals("Book 2", reading.first().book.title)
        assertEquals(3, db.shelfDao().observeAll().first().size)
    }

    @Test
    fun shelfEntry_requiresACachedBook() = runTest {
        assertFailsWith<SQLiteConstraintException> {
            db.shelfDao().upsert(entry(42, ShelfType.READING, updatedAt = 1))
        }
    }

    @Test
    fun tombstones_areHiddenFromEveryReadButTheSyncOnes() = runTest {
        (1..2).forEach { db.bookDao().upsert(book(it)) }
        db.shelfDao().upsert(entry(1, ShelfType.FINISHED, updatedAt = 1, finishedAt = 1))
        db.shelfDao().upsert(entry(2, ShelfType.FINISHED, updatedAt = 2, finishedAt = 1).copy(deleted = true))

        assertEquals(listOf("OL1W"), db.shelfDao().observeShelf(ShelfType.FINISHED).first().map { it.entry.workKey })
        assertEquals(1, db.shelfDao().observeAll().first().size)
        assertNull(db.shelfDao().getEntry("OL2W"))
        assertNull(db.shelfDao().observeEntry("OL2W").first())
        assertNull(db.shelfDao().observeShelfBook("OL2W").first())
        assertEquals(1, db.shelfDao().observeFinishedCount(fromMillis = 0, toMillis = 10).first())
        assertEquals(true, db.shelfDao().getEntryForSync("OL2W")?.deleted)
        assertEquals("Book 2", db.bookDao().getBook("OL2W")?.title)
    }

    @Test
    fun dirtyEntries_areMarkedCleanOnlyIfUnchangedSinceTheUpload() = runTest {
        (1..2).forEach { db.bookDao().upsert(book(it)) }
        db.shelfDao().upsert(entry(1, ShelfType.READING, updatedAt = 10))
        db.shelfDao().upsert(entry(2, ShelfType.READING, updatedAt = 10).copy(dirty = false))

        assertEquals(listOf("OL1W"), db.shelfDao().getDirty().map { it.entry.workKey })
        db.shelfDao().markClean("OL1W", updatedAt = 9) // changed again while uploading
        assertEquals(1, db.shelfDao().getDirty().size)
        db.shelfDao().markClean("OL1W", updatedAt = 10)
        assertEquals(0, db.shelfDao().getDirty().size)

        db.shelfDao().markAllDirty()
        assertEquals(2, db.shelfDao().getDirty().size)
    }

    @Test
    fun purge_dropsOnlyOldTombstonesThatWereUploaded() = runTest {
        (1..3).forEach { db.bookDao().upsert(book(it)) }
        db.shelfDao().upsert(entry(1, ShelfType.READING, updatedAt = 10).copy(deleted = true, dirty = false))
        db.shelfDao().upsert(entry(2, ShelfType.READING, updatedAt = 10).copy(deleted = true, dirty = true))
        db.shelfDao().upsert(entry(3, ShelfType.READING, updatedAt = 50).copy(deleted = true, dirty = false))

        assertEquals(1, db.shelfDao().purgeTombstones(before = 20))

        assertNull(db.shelfDao().getEntryForSync("OL1W"))
        assertEquals(true, db.shelfDao().getEntryForSync("OL2W")?.deleted)
        assertEquals(true, db.shelfDao().getEntryForSync("OL3W")?.deleted)
    }

    @Test
    fun finishedCount_countsOnlyFinishedBooksInsideTheRange() = runTest {
        (1..4).forEach { db.bookDao().upsert(book(it)) }
        db.shelfDao().upsert(entry(1, ShelfType.FINISHED, updatedAt = 1, finishedAt = 1_000))
        db.shelfDao().upsert(entry(2, ShelfType.FINISHED, updatedAt = 1, finishedAt = 1_999))
        db.shelfDao().upsert(entry(3, ShelfType.FINISHED, updatedAt = 1, finishedAt = 2_000))
        db.shelfDao().upsert(entry(4, ShelfType.READING, updatedAt = 1, finishedAt = 1_500))

        assertEquals(2, db.shelfDao().observeFinishedCount(fromMillis = 1_000, toMillis = 2_000).first())
    }

    @Test
    fun readingLogs_areFilteredByDayAndNewestFirst() = runTest {
        val dao = db.readingLogDao()
        dao.upsert(ReadingLogEntity("a", null, epochDay = 10, pages = 5, minutes = 0, createdAt = 1))
        dao.upsert(ReadingLogEntity("b", "OL1W", epochDay = 12, pages = 0, minutes = 20, createdAt = 2))
        dao.upsert(ReadingLogEntity("c", null, epochDay = 12, pages = 3, minutes = 0, createdAt = 3))

        assertEquals(listOf("c", "b"), dao.observeSince(fromEpochDay = 11).first().map { it.id })
        assertEquals(1, dao.markDeleted("a", now = 50))
        assertEquals(0, dao.markDeleted("a", now = 60))
        assertEquals(2, dao.observeAll().first().size)

        val tombstone = dao.getForSync("a")
        assertEquals(true, tombstone?.deleted)
        assertEquals(50, tombstone?.updatedAt)
        assertEquals(true, tombstone?.dirty)
    }

    @Test
    fun syncState_isKeptPerAccountAndCollection() = runTest {
        val dao = db.syncStateDao()
        dao.upsert(SyncStateEntity(uid = "u1", collection = "books", cursor = 5, syncedAt = 100))
        dao.upsert(SyncStateEntity(uid = "u1", collection = "logs", cursor = 7, syncedAt = 90))
        dao.upsert(SyncStateEntity(uid = "u2", collection = "books", cursor = 1, syncedAt = 10))

        assertEquals(5, dao.get("u1", "books")?.cursor)
        assertNull(dao.get("u2", "logs"))
        assertEquals(90, dao.observeLastSyncedAt("u1").first())
        assertNull(dao.observeLastSyncedAt("u3").first())
    }

    @Test
    fun goals_keepHistoryInOrder() = runTest {
        val dao = db.goalDao()
        dao.upsert(GoalEntity(effectiveFromEpochDay = 20, type = GoalType.MINUTES, target = 30))
        dao.upsert(GoalEntity(effectiveFromEpochDay = 10, type = GoalType.PAGES, target = 20))
        dao.upsert(GoalEntity(effectiveFromEpochDay = 20, type = GoalType.MINUTES, target = 45))

        assertEquals(listOf(10L, 20L), dao.observeHistory().first().map { it.effectiveFromEpochDay })
        assertEquals(45, dao.latest()?.target)
    }
}
