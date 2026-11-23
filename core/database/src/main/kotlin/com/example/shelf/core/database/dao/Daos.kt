package com.example.shelf.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.shelf.core.database.model.BookEntity
import com.example.shelf.core.database.model.GoalEntity
import com.example.shelf.core.database.model.ReadingLogEntity
import com.example.shelf.core.database.model.ShelfBookRelation
import com.example.shelf.core.database.model.ShelfEntryEntity
import com.example.shelf.core.database.model.SyncStateEntity
import com.example.shelf.core.model.ShelfType
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books WHERE work_key = :workKey")
    fun observeBook(workKey: String): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE work_key = :workKey")
    suspend fun getBook(workKey: String): BookEntity?

    @Upsert
    suspend fun upsert(book: BookEntity)

    /** Seeds the cache from a search result without overwriting fetched details. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(book: BookEntity): Long
}

/** Every read except the sync ones skips tombstones (`deleted = 1`). */
@Dao
interface ShelfDao {
    /** One query per shelf, most recently updated first. */
    @Transaction
    @Query("SELECT * FROM shelf_entries WHERE shelf = :shelf AND deleted = 0 ORDER BY updated_at DESC")
    fun observeShelf(shelf: ShelfType): Flow<List<ShelfBookRelation>>

    @Transaction
    @Query("SELECT * FROM shelf_entries WHERE deleted = 0 ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<ShelfBookRelation>>

    @Transaction
    @Query("SELECT * FROM shelf_entries WHERE work_key = :workKey AND deleted = 0")
    fun observeShelfBook(workKey: String): Flow<ShelfBookRelation?>

    @Query("SELECT * FROM shelf_entries WHERE work_key = :workKey AND deleted = 0")
    fun observeEntry(workKey: String): Flow<ShelfEntryEntity?>

    @Query("SELECT * FROM shelf_entries WHERE work_key = :workKey AND deleted = 0")
    suspend fun getEntry(workKey: String): ShelfEntryEntity?

    /** The row whatever its state, tombstone included. */
    @Query("SELECT * FROM shelf_entries WHERE work_key = :workKey")
    suspend fun getEntryForSync(workKey: String): ShelfEntryEntity?

    @Upsert
    suspend fun upsert(entry: ShelfEntryEntity)

    @Query(
        "SELECT COUNT(*) FROM shelf_entries WHERE deleted = 0 " +
            "AND shelf = 'FINISHED' AND finished_at >= :fromMillis AND finished_at < :toMillis",
    )
    fun observeFinishedCount(fromMillis: Long, toMillis: Long): Flow<Int>

    /** Local changes not uploaded yet, tombstones included, with the books they refer to. */
    @Transaction
    @Query("SELECT * FROM shelf_entries WHERE dirty = 1")
    suspend fun getDirty(): List<ShelfBookRelation>

    /** Marks an uploaded entry clean, unless it changed again while the upload ran. */
    @Query("UPDATE shelf_entries SET dirty = 0 WHERE work_key = :workKey AND updated_at = :updatedAt")
    suspend fun markClean(workKey: String, updatedAt: Long)

    @Query("UPDATE shelf_entries SET dirty = 1")
    suspend fun markAllDirty()

    /** Drops tombstones that were uploaded and are older than [before]. */
    @Query("DELETE FROM shelf_entries WHERE deleted = 1 AND dirty = 0 AND updated_at < :before")
    suspend fun purgeTombstones(before: Long): Int
}

/** Every read except the sync ones skips tombstones (`deleted = 1`). */
@Dao
interface ReadingLogDao {
    @Query("SELECT * FROM reading_logs WHERE deleted = 0 ORDER BY epoch_day DESC, created_at DESC")
    fun observeAll(): Flow<List<ReadingLogEntity>>

    @Query(
        "SELECT * FROM reading_logs WHERE deleted = 0 AND epoch_day >= :fromEpochDay " +
            "ORDER BY epoch_day DESC, created_at DESC",
    )
    fun observeSince(fromEpochDay: Long): Flow<List<ReadingLogEntity>>

    /** The row whatever its state, tombstone included. */
    @Query("SELECT * FROM reading_logs WHERE id = :id")
    suspend fun getForSync(id: String): ReadingLogEntity?

    @Upsert
    suspend fun upsert(log: ReadingLogEntity)

    /** Turns the log into a tombstone; returns 0 when there was no live log with that id. */
    @Query("UPDATE reading_logs SET deleted = 1, dirty = 1, updated_at = :now WHERE id = :id AND deleted = 0")
    suspend fun markDeleted(id: String, now: Long): Int

    @Query("SELECT * FROM reading_logs WHERE dirty = 1")
    suspend fun getDirty(): List<ReadingLogEntity>

    /** Marks an uploaded log clean, unless it changed again while the upload ran. */
    @Query("UPDATE reading_logs SET dirty = 0 WHERE id = :id AND updated_at = :updatedAt")
    suspend fun markClean(id: String, updatedAt: Long)

    @Query("UPDATE reading_logs SET dirty = 1")
    suspend fun markAllDirty()

    /** Drops tombstones that were uploaded and are older than [before]. */
    @Query("DELETE FROM reading_logs WHERE deleted = 1 AND dirty = 0 AND updated_at < :before")
    suspend fun purgeTombstones(before: Long): Int
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY effective_from_epoch_day ASC")
    fun observeHistory(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY effective_from_epoch_day DESC LIMIT 1")
    suspend fun latest(): GoalEntity?

    @Upsert
    suspend fun upsert(goal: GoalEntity)
}

@Dao
interface SyncStateDao {
    @Query("SELECT * FROM sync_state WHERE uid = :uid AND collection = :collection")
    suspend fun get(uid: String, collection: String): SyncStateEntity?

    @Upsert
    suspend fun upsert(state: SyncStateEntity)

    /** When every collection of the account was last synced on this device, or null if never. */
    @Query("SELECT MIN(synced_at) FROM sync_state WHERE uid = :uid")
    fun observeLastSyncedAt(uid: String): Flow<Long?>
}
