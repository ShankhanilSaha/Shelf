package com.example.shelf.core.data.sync

import com.example.shelf.core.data.mapper.toEntity
import com.example.shelf.core.data.repository.OfflineFirstLibraryRepository
import com.example.shelf.core.data.repository.OfflineFirstReadingLogRepository
import com.example.shelf.core.database.ShelfDatabase
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.testing.MutableClock
import com.example.shelf.core.testing.TestData
import com.example.shelf.core.testing.inMemoryShelfDatabase
import com.example.shelf.core.testing.sync.FakeSyncGateway
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
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class LibrarySynchronizerTest {
    private lateinit var db: ShelfDatabase
    private val clock = MutableClock()
    private val gateway = FakeSyncGateway()
    private lateinit var library: OfflineFirstLibraryRepository
    private lateinit var logs: OfflineFirstReadingLogRepository
    private lateinit var synchronizer: LibrarySynchronizer

    private val now get() = clock.millis()

    @Before
    fun setUp() = runTest {
        db = inMemoryShelfDatabase(RuntimeEnvironment.getApplication())
        val scheduler = FakeSyncScheduler()
        library = OfflineFirstLibraryRepository(db.shelfDao(), db.bookDao(), db.transactionRunner(), clock, scheduler)
        logs = OfflineFirstReadingLogRepository(db.readingLogDao(), db.goalDao(), clock, scheduler)
        synchronizer = LibrarySynchronizer(
            gateway = gateway,
            bookDao = db.bookDao(),
            shelfDao = db.shelfDao(),
            readingLogDao = db.readingLogDao(),
            syncStateDao = db.syncStateDao(),
            transaction = db.transactionRunner(),
            clock = clock,
        )
        (1..3).forEach { db.bookDao().upsert(TestData.summary(it).toEntity()) }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun remoteBook(
        id: Int,
        updatedAt: Long,
        shelf: ShelfType = ShelfType.READING,
        currentPage: Int = 10,
        deleted: Boolean = false,
    ) = RemoteBook(
        workKey = "OL${id}W",
        title = "Remote book $id",
        authors = listOf("Remote author"),
        coverId = 7,
        pageCount = 320,
        firstPublishYear = 1999,
        shelf = shelf,
        currentPage = currentPage,
        rating = null,
        notes = "",
        addedAt = updatedAt,
        updatedAt = updatedAt,
        finishedAt = null,
        deleted = deleted,
    )

    @Test
    fun `the first sync uploads the library and the reading log`() = runTest {
        library.setShelf("OL1W", ShelfType.READING)
        val log = logs.addLog(LocalDate.of(2026, 11, 24), pages = 25, minutes = 0, workKey = "OL1W")

        synchronizer.sync("u1")

        val uploaded = gateway.booksOf("u1").getValue("OL1W")
        assertEquals(ShelfType.READING, uploaded.shelf)
        assertEquals("Book 1", uploaded.title)
        assertEquals(25, gateway.logsOf("u1").getValue(log.id).pages)
        assertTrue(db.shelfDao().getDirty().isEmpty())
        assertTrue(db.readingLogDao().getDirty().isEmpty())
        assertEquals(now, db.syncStateDao().observeLastSyncedAt("u1").first())
    }

    @Test
    fun `books from another device appear in the library, paged`() = runTest {
        gateway.pageSize = 2
        (4..8).forEach { gateway.putBook("u1", remoteBook(it, updatedAt = now - 1_000)) }

        synchronizer.sync("u1")

        val shelf = library.observeShelf(ShelfType.READING).first()
        assertEquals(5, shelf.size)
        val book = db.bookDao().getBook("OL4W")!!
        assertEquals("Remote book 4", book.title)
        assertNull(book.detailsFetchedAt) // details are fetched when it is opened
        assertTrue(db.shelfDao().getDirty().isEmpty()) // nothing to send back
    }

    @Test
    fun `the newer edit wins in both directions`() = runTest {
        library.setShelf("OL1W", ShelfType.WANT_TO_READ) // local edit at now
        library.setShelf("OL2W", ShelfType.WANT_TO_READ)
        gateway.putBook("u1", remoteBook(1, updatedAt = now + 60_000, shelf = ShelfType.FINISHED)) // newer
        gateway.putBook("u1", remoteBook(2, updatedAt = now - 60_000, shelf = ShelfType.FINISHED)) // older

        synchronizer.sync("u1")

        assertEquals(ShelfType.FINISHED, library.observeEntry("OL1W").first()?.shelf)
        assertEquals(ShelfType.WANT_TO_READ, library.observeEntry("OL2W").first()?.shelf)
        assertEquals(ShelfType.FINISHED, gateway.booksOf("u1").getValue("OL1W").shelf)
        assertEquals(ShelfType.WANT_TO_READ, gateway.booksOf("u1").getValue("OL2W").shelf)
    }

    @Test
    fun `a removal is uploaded as a tombstone`() = runTest {
        library.setShelf("OL1W", ShelfType.READING)
        synchronizer.sync("u1")
        clock.advanceBy(Duration.ofMinutes(1))

        library.remove("OL1W")
        synchronizer.sync("u1")

        val tombstone = gateway.booksOf("u1").getValue("OL1W")
        assertTrue(tombstone.deleted)
        assertEquals(now, tombstone.updatedAt)
    }

    @Test
    fun `a newer remote tombstone removes the book here`() = runTest {
        library.setShelf("OL1W", ShelfType.READING)
        synchronizer.sync("u1")
        gateway.putBook("u1", remoteBook(1, updatedAt = now + 60_000, deleted = true))

        synchronizer.sync("u1")

        assertNull(library.observeEntry("OL1W").first())
        assertTrue(library.observeLibrary().first().isEmpty())
    }

    @Test
    fun `an edit made after a remote deletion brings the book back everywhere`() = runTest {
        gateway.putBook("u1", remoteBook(1, updatedAt = now - 60_000, deleted = true))
        library.setShelf("OL1W", ShelfType.READING) // edited later, on this device

        synchronizer.sync("u1")

        assertNotNull(library.observeEntry("OL1W").first())
        assertFalse(gateway.booksOf("u1").getValue("OL1W").deleted)
    }

    @Test
    fun `remote deletions of logs apply and local ones upload`() = runTest {
        val kept = logs.addLog(LocalDate.of(2026, 11, 23), pages = 10, minutes = 0, workKey = null)
        val removed = logs.addLog(LocalDate.of(2026, 11, 24), pages = 20, minutes = 0, workKey = null)
        synchronizer.sync("u1")
        clock.advanceBy(Duration.ofMinutes(1))
        logs.deleteLog(removed.id)
        gateway.putLog("u1", gateway.logsOf("u1").getValue(kept.id).copy(deleted = true, updatedAt = now + 1))

        synchronizer.sync("u1")

        assertTrue(logs.observeLogs().first().isEmpty())
        assertTrue(gateway.logsOf("u1").getValue(removed.id).deleted)
    }

    @Test
    fun `later syncs only pull what changed since the cursor`() = runTest {
        gateway.putBook("u1", remoteBook(1, updatedAt = now))
        synchronizer.sync("u1")
        val cursor = db.syncStateDao().get("u1", LibrarySynchronizer.BOOKS)!!.cursor

        synchronizer.sync("u1")

        assertEquals(listOf("books null", "books $cursor"), gateway.pulls.filter { it.startsWith("books") })
    }

    @Test
    fun `tombstones older than thirty days are dropped on both sides`() = runTest {
        library.setShelf("OL1W", ShelfType.READING)
        library.remove("OL1W")
        synchronizer.sync("u1") // uploads the tombstone
        clock.advanceBy(Duration.ofDays(31))

        synchronizer.sync("u1")

        assertNull(db.shelfDao().getEntryForSync("OL1W"))
        assertEquals(ConflictPolicy.tombstoneCutoff(now), gateway.purgedBefore)
        assertTrue(gateway.booksOf("u1").isEmpty())
    }

    @Test
    fun `the first sync of another account uploads everything to it too`() = runTest {
        library.setShelf("OL1W", ShelfType.READING)
        synchronizer.sync("u1")

        synchronizer.sync("u2")

        assertEquals(setOf("OL1W"), gateway.booksOf("u2").keys)
    }

    @Test
    fun `a failed sync keeps the changes for the next attempt`() = runTest {
        library.setShelf("OL1W", ShelfType.READING)
        gateway.offline = true

        runCatching { synchronizer.sync("u1") }

        assertEquals(1, db.shelfDao().getDirty().size)
        gateway.offline = false
        synchronizer.sync("u1")
        assertEquals(setOf("OL1W"), gateway.booksOf("u1").keys)
    }
}
