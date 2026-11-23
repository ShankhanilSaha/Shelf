package com.example.shelf.core.data.repository

import com.example.shelf.core.model.ShelfBook
import com.example.shelf.core.model.ShelfEntry
import com.example.shelf.core.model.ShelfType
import kotlinx.coroutines.flow.Flow

/** The user's library: which books are on which shelf, progress, rating and notes. */
interface LibraryRepository {
    /** Books on [shelf], most recently updated first. */
    fun observeShelf(shelf: ShelfType): Flow<List<ShelfBook>>

    fun observeLibrary(): Flow<List<ShelfBook>>

    fun observeShelfBook(workKey: String): Flow<ShelfBook?>

    fun observeEntry(workKey: String): Flow<ShelfEntry?>

    /** Number of books finished in [year] (local calendar year). */
    fun observeFinishedCount(year: Int): Flow<Int>

    /**
     * Adds the book to [shelf] or moves it there. Moving to [ShelfType.FINISHED] stamps
     * `finishedAt`; moving away from it clears the stamp. The book must already be cached.
     */
    suspend fun setShelf(workKey: String, shelf: ShelfType): LibraryChange

    /**
     * Records the current page. Starting a "want to read" book moves it to Reading; reaching
     * the last page moves it to Finished.
     */
    suspend fun setCurrentPage(workKey: String, page: Int): LibraryChange

    suspend fun setRating(workKey: String, rating: Int?)

    suspend fun setNotes(workKey: String, notes: String)

    /** Removes the book from the library and returns the removed entry, for undo. */
    suspend fun remove(workKey: String): ShelfEntry?

    /** Puts back an entry returned by [remove] or the state before a change (undo). */
    suspend fun restore(entry: ShelfEntry)
}

/**
 * Result of a library write.
 * @property previous the entry before the write (null if the book was not in the library), for undo
 * @property justFinished the write moved the book to Finished, so the UI may ask for a rating
 */
data class LibraryChange(val entry: ShelfEntry, val previous: ShelfEntry?, val justFinished: Boolean)
