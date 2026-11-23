package com.example.shelf.core.testing.repository

import com.example.shelf.core.data.repository.LibraryChange
import com.example.shelf.core.data.repository.LibraryRepository
import com.example.shelf.core.model.Book
import com.example.shelf.core.model.ShelfBook
import com.example.shelf.core.model.ShelfEntry
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.testing.TestData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneOffset

/**
 * In-memory [LibraryRepository] that follows the production rules (finishing stamps, automatic
 * moves), so ViewModel tests exercise realistic behavior.
 */
class FakeLibraryRepository(private val now: () -> Instant = { TestData.NOW }) : LibraryRepository {
    private val books = mutableMapOf<String, Book>()
    private val entries = MutableStateFlow<Map<String, ShelfEntry>>(emptyMap())

    fun addBook(book: Book, entry: ShelfEntry? = null) {
        books[book.workKey] = book
        if (entry != null) entries.value = entries.value + (entry.workKey to entry)
    }

    fun entry(workKey: String): ShelfEntry? = entries.value[workKey]

    private fun shelfBooks(): List<ShelfBook> = entries.value.values
        .mapNotNull { entry -> books[entry.workKey]?.let { ShelfBook(it, entry) } }
        .sortedByDescending { it.entry.updatedAt }

    override fun observeShelf(shelf: ShelfType): Flow<List<ShelfBook>> =
        entries.map { shelfBooks().filter { it.entry.shelf == shelf } }

    override fun observeLibrary(): Flow<List<ShelfBook>> = entries.map { shelfBooks() }

    override fun observeShelfBook(workKey: String): Flow<ShelfBook?> =
        entries.map { shelfBooks().firstOrNull { it.book.workKey == workKey } }

    override fun observeEntry(workKey: String): Flow<ShelfEntry?> = entries.map { it[workKey] }

    override fun observeFinishedCount(year: Int): Flow<Int> = entries.map { map ->
        map.values.count { it.finishedAt?.atZone(ZoneOffset.UTC)?.year == year }
    }

    override suspend fun setShelf(workKey: String, shelf: ShelfType): LibraryChange {
        val previous = entries.value[workKey]
        val time = now()
        val finishing = shelf == ShelfType.FINISHED && previous?.shelf != ShelfType.FINISHED
        val base = previous ?: ShelfEntry(workKey = workKey, shelf = shelf, addedAt = time, updatedAt = time)
        val updated = base.copy(
            shelf = shelf,
            updatedAt = time,
            finishedAt = if (shelf == ShelfType.FINISHED) base.finishedAt ?: time else null,
            currentPage = if (finishing) books[workKey]?.pageCount ?: base.currentPage else base.currentPage,
        )
        put(updated)
        return LibraryChange(updated, previous, justFinished = finishing)
    }

    override suspend fun setCurrentPage(workKey: String, page: Int): LibraryChange {
        val previous = checkNotNull(entries.value[workKey])
        val pageCount = books[workKey]?.pageCount
        val clamped = page.coerceIn(0, pageCount ?: Int.MAX_VALUE)
        val reachedEnd = pageCount != null && clamped >= pageCount && previous.shelf != ShelfType.FINISHED
        val shelf = when {
            reachedEnd -> ShelfType.FINISHED
            previous.shelf == ShelfType.WANT_TO_READ && clamped > 0 -> ShelfType.READING
            else -> previous.shelf
        }
        val updated = previous.copy(
            currentPage = clamped,
            shelf = shelf,
            updatedAt = now(),
            finishedAt = if (reachedEnd) now() else previous.finishedAt,
        )
        put(updated)
        return LibraryChange(updated, previous, justFinished = reachedEnd)
    }

    override suspend fun setRating(workKey: String, rating: Int?) {
        entries.value[workKey]?.let { put(it.copy(rating = rating, updatedAt = now())) }
    }

    override suspend fun setNotes(workKey: String, notes: String) {
        entries.value[workKey]?.let { put(it.copy(notes = notes, updatedAt = now())) }
    }

    override suspend fun remove(workKey: String): ShelfEntry? {
        val existing = entries.value[workKey] ?: return null
        entries.value = entries.value - workKey
        return existing
    }

    override suspend fun restore(entry: ShelfEntry) = put(entry)

    private fun put(entry: ShelfEntry) {
        entries.value = entries.value + (entry.workKey to entry)
    }
}
