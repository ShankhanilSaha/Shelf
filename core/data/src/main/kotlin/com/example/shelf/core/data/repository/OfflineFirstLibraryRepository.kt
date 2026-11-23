package com.example.shelf.core.data.repository

import com.example.shelf.core.data.mapper.toEntity
import com.example.shelf.core.data.mapper.toShelfBook
import com.example.shelf.core.data.mapper.toShelfEntry
import com.example.shelf.core.data.sync.SyncScheduler
import com.example.shelf.core.database.TransactionRunner
import com.example.shelf.core.database.dao.BookDao
import com.example.shelf.core.database.dao.ShelfDao
import com.example.shelf.core.model.Rating
import com.example.shelf.core.model.ShelfBook
import com.example.shelf.core.model.ShelfEntry
import com.example.shelf.core.model.ShelfType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Room is the source of truth. Every write marks the row dirty (see the entity mappers) and asks
 * for a sync; removals leave a tombstone so that other devices learn about them.
 */
internal class OfflineFirstLibraryRepository @Inject constructor(
    private val shelfDao: ShelfDao,
    private val bookDao: BookDao,
    private val transaction: TransactionRunner,
    private val clock: Clock,
    private val syncScheduler: SyncScheduler,
) : LibraryRepository {

    override fun observeShelf(shelf: ShelfType): Flow<List<ShelfBook>> = shelfDao.observeShelf(shelf)
        .map { rows -> rows.map { it.toShelfBook() } }
        .distinctUntilChanged()

    override fun observeLibrary(): Flow<List<ShelfBook>> = shelfDao.observeAll()
        .map { rows -> rows.map { it.toShelfBook() } }
        .distinctUntilChanged()

    override fun observeShelfBook(workKey: String): Flow<ShelfBook?> = shelfDao.observeShelfBook(workKey)
        .map { it?.toShelfBook() }
        .distinctUntilChanged()

    override fun observeEntry(workKey: String): Flow<ShelfEntry?> = shelfDao.observeEntry(workKey)
        .map { it?.toShelfEntry() }
        .distinctUntilChanged()

    override fun observeFinishedCount(year: Int): Flow<Int> {
        val start = LocalDate.of(year, 1, 1).atStartOfDay(clock.zone).toInstant().toEpochMilli()
        val end = LocalDate.of(year + 1, 1, 1).atStartOfDay(clock.zone).toInstant().toEpochMilli()
        return shelfDao.observeFinishedCount(start, end).distinctUntilChanged()
    }

    override suspend fun setShelf(workKey: String, shelf: ShelfType): LibraryChange = synced {
        val previous = shelfDao.getEntry(workKey)?.toShelfEntry()
        val now = clock.instant()
        val base = previous ?: ShelfEntry(workKey = workKey, shelf = shelf, addedAt = now, updatedAt = now)
        val pageCount = bookDao.getBook(workKey)?.pageCount
        val finishing = shelf == ShelfType.FINISHED && previous?.shelf != ShelfType.FINISHED
        val updated = base.copy(
            shelf = shelf,
            updatedAt = now,
            finishedAt = when {
                shelf != ShelfType.FINISHED -> null
                finishing -> now
                else -> base.finishedAt
            },
            currentPage = if (finishing && pageCount != null) pageCount else base.currentPage,
        )
        shelfDao.upsert(updated.toEntity())
        LibraryChange(entry = updated, previous = previous, justFinished = finishing)
    }

    override suspend fun setCurrentPage(workKey: String, page: Int): LibraryChange = synced {
        val previous = requireNotNull(shelfDao.getEntry(workKey)?.toShelfEntry()) { "$workKey is not in the library" }
        val pageCount = bookDao.getBook(workKey)?.pageCount?.takeIf { it > 0 }
        val clamped = page.coerceIn(0, pageCount ?: Int.MAX_VALUE)
        val now = clock.instant()
        val reachedEnd = pageCount != null && clamped >= pageCount && previous.shelf != ShelfType.FINISHED
        val shelf = when {
            reachedEnd -> ShelfType.FINISHED
            previous.shelf == ShelfType.WANT_TO_READ && clamped > 0 -> ShelfType.READING
            else -> previous.shelf
        }
        val updated = previous.copy(
            currentPage = clamped,
            shelf = shelf,
            updatedAt = now,
            finishedAt = if (reachedEnd) now else previous.finishedAt,
        )
        shelfDao.upsert(updated.toEntity())
        LibraryChange(entry = updated, previous = previous, justFinished = reachedEnd)
    }

    override suspend fun setRating(workKey: String, rating: Int?) {
        require(rating == null || rating in Rating.RANGE) { "rating must be in ${Rating.RANGE}" }
        update(workKey) { it.copy(rating = rating) }
    }

    override suspend fun setNotes(workKey: String, notes: String) {
        update(workKey) { it.copy(notes = notes) }
    }

    override suspend fun remove(workKey: String): ShelfEntry? {
        val removed = transaction {
            val existing = shelfDao.getEntry(workKey) ?: return@transaction null
            shelfDao.upsert(existing.copy(deleted = true, dirty = true, updatedAt = clock.millis()))
            existing.toShelfEntry()
        }
        if (removed != null) syncScheduler.requestSync()
        return removed
    }

    override suspend fun restore(entry: ShelfEntry) = synced {
        // Undo puts the entry back as it was, but it still counts as the latest change.
        shelfDao.upsert(entry.copy(updatedAt = clock.instant()).toEntity())
    }

    private suspend fun update(workKey: String, change: (ShelfEntry) -> ShelfEntry) {
        val changed = transaction {
            val current = shelfDao.getEntry(workKey)?.toShelfEntry() ?: return@transaction false
            val updated = change(current)
            if (updated != current) shelfDao.upsert(updated.copy(updatedAt = clock.instant()).toEntity())
            updated != current
        }
        if (changed) syncScheduler.requestSync()
    }

    /** Runs a write in a transaction, then asks for a sync. */
    private suspend fun <T> synced(block: suspend () -> T): T = transaction(block).also { syncScheduler.requestSync() }
}
