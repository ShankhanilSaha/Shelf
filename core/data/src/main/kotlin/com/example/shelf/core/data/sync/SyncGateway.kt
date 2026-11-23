package com.example.shelf.core.data.sync

import com.example.shelf.core.model.ShelfType
import kotlinx.coroutines.flow.Flow

/**
 * The remote side of sync: `users/{uid}/books/{workKey}` and `users/{uid}/logs/{logId}`.
 *
 * Implementations throw `ShelfException` with a classified error; network problems are
 * `ShelfError.Network` so that the worker can retry them.
 */
interface SyncGateway {
    /** False when this build has no Firebase configuration; sync then does nothing. */
    val isAvailable: Boolean

    /** Books changed at or after the server time [since] (epoch micros), oldest first, in pages. */
    fun pullBooks(uid: String, since: Long?): Flow<RemotePage<RemoteBook>>

    /** Logs changed at or after the server time [since] (epoch micros), oldest first, in pages. */
    fun pullLogs(uid: String, since: Long?): Flow<RemotePage<RemoteLog>>

    /** Writes each book unless the server already has a newer version of it (last write wins). */
    suspend fun pushBooks(uid: String, books: List<RemoteBook>)

    /** Writes each log unless the server already has a newer version of it (last write wins). */
    suspend fun pushLogs(uid: String, logs: List<RemoteLog>)

    /** Deletes tombstones whose deletion happened before [deletedBefore] (epoch millis). */
    suspend fun purgeTombstones(uid: String, deletedBefore: Long)
}

/** One page of pulled documents and the server time (epoch micros) of the newest one. */
data class RemotePage<T>(val items: List<T>, val cursor: Long)

/** A library entry as stored remotely, with enough of the book to show it on another device. */
data class RemoteBook(
    val workKey: String,
    val title: String,
    val authors: List<String>,
    val coverId: Long?,
    val pageCount: Int?,
    val firstPublishYear: Int?,
    val shelf: ShelfType,
    val currentPage: Int,
    val rating: Int?,
    val notes: String,
    val addedAt: Long,
    val updatedAt: Long,
    val finishedAt: Long?,
    val deleted: Boolean,
)

data class RemoteLog(
    val id: String,
    val workKey: String?,
    val epochDay: Long,
    val pages: Int,
    val minutes: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean,
)
