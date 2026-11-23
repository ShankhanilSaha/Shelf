package com.example.shelf.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.shelf.core.model.GoalType
import com.example.shelf.core.model.ShelfType

/** Cached Open Library work. Rows are only ever added or refreshed, never deleted. */
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey @ColumnInfo(name = "work_key") val workKey: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "authors") val authors: List<String>,
    @ColumnInfo(name = "first_publish_year") val firstPublishYear: Int?,
    @ColumnInfo(name = "cover_id") val coverId: Long?,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "subjects") val subjects: List<String>,
    @ColumnInfo(name = "page_count") val pageCount: Int?,
    @ColumnInfo(name = "isbn") val isbn: String?,
    /** Epoch millis of the last successful details fetch; null while only a search summary is known. */
    @ColumnInfo(name = "details_fetched_at") val detailsFetchedAt: Long?,
)

/**
 * A book in the user's library. Times are epoch millis.
 *
 * Sync bookkeeping: [dirty] marks a local change that has not been uploaded yet (every local
 * write sets it), and [deleted] turns the row into a tombstone so that a removal can be synced.
 * Tombstones are hidden from every query except the sync ones.
 */
@Entity(
    tableName = "shelf_entries",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["work_key"],
            childColumns = ["work_key"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("shelf", "updated_at")],
)
data class ShelfEntryEntity(
    @PrimaryKey @ColumnInfo(name = "work_key") val workKey: String,
    @ColumnInfo(name = "shelf") val shelf: ShelfType,
    @ColumnInfo(name = "current_page") val currentPage: Int,
    @ColumnInfo(name = "rating") val rating: Int?,
    @ColumnInfo(name = "notes") val notes: String,
    @ColumnInfo(name = "added_at") val addedAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "finished_at") val finishedAt: Long?,
    @ColumnInfo(name = "dirty", defaultValue = "0") val dirty: Boolean = true,
    @ColumnInfo(name = "deleted", defaultValue = "0") val deleted: Boolean = false,
)

/** A shelf entry together with its cached book. */
data class ShelfBookRelation(
    @Embedded val entry: ShelfEntryEntity,
    @Relation(parentColumn = "work_key", entityColumn = "work_key")
    val book: BookEntity,
)

/**
 * One reading session. [epochDay] is `LocalDate.toEpochDay()` of the local day it belongs to.
 * [dirty] and [deleted] work as in [ShelfEntryEntity].
 */
@Entity(tableName = "reading_logs", indices = [Index("epoch_day")])
data class ReadingLogEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "work_key") val workKey: String?,
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "pages") val pages: Int,
    @ColumnInfo(name = "minutes") val minutes: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = createdAt,
    @ColumnInfo(name = "dirty", defaultValue = "0") val dirty: Boolean = true,
    @ColumnInfo(name = "deleted", defaultValue = "0") val deleted: Boolean = false,
)

/**
 * Daily-goal history: the goal in force from [effectiveFromEpochDay] until the next row. Keeping
 * history means changing the goal does not rewrite whether past days met it.
 */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey @ColumnInfo(name = "effective_from_epoch_day") val effectiveFromEpochDay: Long,
    @ColumnInfo(name = "type") val type: GoalType,
    @ColumnInfo(name = "target") val target: Int,
)

/**
 * How far this device has pulled each synced collection of an account. [cursor] is the server
 * time (epoch microseconds) of the newest change pulled; [syncedAt] is when the last sync of the
 * collection finished. No row for an account means it has never been synced on this device.
 */
@Entity(tableName = "sync_state", primaryKeys = ["uid", "collection"])
data class SyncStateEntity(
    @ColumnInfo(name = "uid") val uid: String,
    @ColumnInfo(name = "collection") val collection: String,
    @ColumnInfo(name = "cursor") val cursor: Long,
    @ColumnInfo(name = "synced_at") val syncedAt: Long,
)
