package com.example.shelf.core.data.sync

import com.example.shelf.core.common.result.ShelfError
import com.example.shelf.core.common.result.ShelfException
import com.example.shelf.core.model.ShelfType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Firestore implementation: `users/{uid}/books/{workKey}` and `users/{uid}/logs/{logId}`.
 *
 * Each document carries the edit time `updatedAt` (device clock, used by [ConflictPolicy]) and
 * the server time `syncedAt` it was last written. Pulls page through `syncedAt`, so clock skew
 * between devices cannot hide a change from the cursor. Documents are mapped by hand, which
 * keeps R8 free of reflection rules.
 */
internal class FirebaseSyncGateway @Inject constructor(private val firestore: FirebaseFirestore) : SyncGateway {
    override val isAvailable: Boolean = true

    override fun pullBooks(uid: String, since: Long?): Flow<RemotePage<RemoteBook>> =
        pull(collection(uid, LibrarySynchronizer.BOOKS), since) { it.toRemoteBook() }

    override fun pullLogs(uid: String, since: Long?): Flow<RemotePage<RemoteLog>> =
        pull(collection(uid, LibrarySynchronizer.LOGS), since) { it.toRemoteLog() }

    override suspend fun pushBooks(uid: String, books: List<RemoteBook>) = push(
        collection = collection(uid, LibrarySynchronizer.BOOKS),
        items = books,
        id = RemoteBook::workKey,
        updatedAt = RemoteBook::updatedAt,
        toDocument = { it.toDocument() },
    )

    override suspend fun pushLogs(uid: String, logs: List<RemoteLog>) = push(
        collection = collection(uid, LibrarySynchronizer.LOGS),
        items = logs,
        id = RemoteLog::id,
        updatedAt = RemoteLog::updatedAt,
        toDocument = { it.toDocument() },
    )

    override suspend fun purgeTombstones(uid: String, deletedBefore: Long) {
        for (name in listOf(LibrarySynchronizer.BOOKS, LibrarySynchronizer.LOGS)) {
            // Only tombstones have `deletedAt`, so this single-field range needs no composite index.
            val expired = call {
                collection(
                    uid,
                    name,
                ).whereLessThan(DELETED_AT, deletedBefore).limit(PAGE_SIZE).get(Source.SERVER).await()
            }
            if (expired.isEmpty) continue
            val batch = firestore.batch()
            expired.documents.forEach { batch.delete(it.reference) }
            call { batch.commit().await() }
        }
    }

    private fun collection(uid: String, name: String): CollectionReference =
        firestore.collection(USERS).document(uid).collection(name)

    private fun <T> pull(
        collection: CollectionReference,
        since: Long?,
        parse: (DocumentSnapshot) -> T?,
    ): Flow<RemotePage<T>> = flow {
        // ">=" rather than ">": documents written in one batch share a server time, so a page
        // boundary must not skip the rest of them. Seeing a document twice is harmless.
        val filtered: Query = since?.let { collection.whereGreaterThanOrEqualTo(SYNCED_AT, it.toTimestamp()) }
            ?: collection
        val base = filtered.orderBy(SYNCED_AT).limit(PAGE_SIZE)
        var after: DocumentSnapshot? = null
        do {
            val query = after?.let { base.startAfter(it) } ?: base
            val documents = call { query.get(Source.SERVER).await() }.documents
            if (documents.isEmpty()) break
            val cursor = documents.mapNotNull { it.getTimestamp(SYNCED_AT)?.toMicros() }.maxOrNull() ?: since ?: 0
            emit(RemotePage(documents.mapNotNull(parse), cursor))
            after = documents.last()
        } while (documents.size.toLong() == PAGE_SIZE)
    }

    private suspend fun <T> push(
        collection: CollectionReference,
        items: List<T>,
        id: (T) -> String,
        updatedAt: (T) -> Long,
        toDocument: (T) -> Map<String, Any?>,
    ) {
        for (chunk in items.chunked(TRANSACTION_SIZE)) {
            call {
                firestore.runTransaction { transaction ->
                    val refs = chunk.map { collection.document(id(it)) }
                    // A transaction reads everything before it writes; it retries on contention.
                    val serverUpdatedAt = refs.map { transaction.get(it).getLong(UPDATED_AT) }
                    chunk.forEachIndexed { index, item ->
                        if (ConflictPolicy.localWins(updatedAt(item), serverUpdatedAt[index])) {
                            transaction.set(refs[index], toDocument(item))
                        }
                    }
                }.await()
            }
        }
    }

    private suspend fun <T> call(block: suspend () -> T): T = try {
        block()
    } catch (e: FirebaseFirestoreException) {
        throw ShelfException(e.code.toShelfError(), e)
    }

    private companion object {
        const val USERS = "users"
        const val UPDATED_AT = "updatedAt"
        const val SYNCED_AT = "syncedAt"
        const val DELETED = "deleted"
        const val DELETED_AT = "deletedAt"
        const val PAGE_SIZE = 200L
        const val TRANSACTION_SIZE = 100
    }

    private fun RemoteBook.toDocument(): Map<String, Any?> = buildMap {
        put("title", title)
        put("authors", authors)
        put("coverId", coverId)
        put("pageCount", pageCount)
        put("firstPublishYear", firstPublishYear)
        put("shelf", shelf.name)
        put("currentPage", currentPage)
        put("rating", rating)
        put("notes", notes)
        put("addedAt", addedAt)
        put("finishedAt", finishedAt)
        putSyncFields(updatedAt, deleted)
    }

    private fun RemoteLog.toDocument(): Map<String, Any?> = buildMap {
        put("workKey", workKey)
        put("epochDay", epochDay)
        put("pages", pages)
        put("minutes", minutes)
        put("createdAt", createdAt)
        putSyncFields(updatedAt, deleted)
    }

    private fun MutableMap<String, Any?>.putSyncFields(updatedAt: Long, deleted: Boolean) {
        put(UPDATED_AT, updatedAt)
        put(DELETED, deleted)
        if (deleted) put(DELETED_AT, updatedAt)
        put(SYNCED_AT, FieldValue.serverTimestamp())
    }

    /** Null for a document this version cannot read (written by a newer app, or corrupt). */
    private fun DocumentSnapshot.toRemoteBook(): RemoteBook? = runCatching {
        RemoteBook(
            workKey = id,
            title = getString("title") ?: id,
            authors = (get("authors") as? List<*>)?.filterIsInstance<String>().orEmpty(),
            coverId = getLong("coverId"),
            pageCount = getLong("pageCount")?.toInt(),
            firstPublishYear = getLong("firstPublishYear")?.toInt(),
            shelf = ShelfType.valueOf(checkNotNull(getString("shelf"))),
            currentPage = getLong("currentPage")?.toInt() ?: 0,
            rating = getLong("rating")?.toInt(),
            notes = getString("notes").orEmpty(),
            addedAt = getLong("addedAt") ?: 0,
            updatedAt = checkNotNull(getLong(UPDATED_AT)),
            finishedAt = getLong("finishedAt"),
            deleted = getBoolean(DELETED) == true,
        )
    }.getOrNull()

    private fun DocumentSnapshot.toRemoteLog(): RemoteLog? = runCatching {
        RemoteLog(
            id = id,
            workKey = getString("workKey"),
            epochDay = checkNotNull(getLong("epochDay")),
            pages = getLong("pages")?.toInt() ?: 0,
            minutes = getLong("minutes")?.toInt() ?: 0,
            createdAt = getLong("createdAt") ?: 0,
            updatedAt = checkNotNull(getLong(UPDATED_AT)),
            deleted = getBoolean(DELETED) == true,
        )
    }.getOrNull()
}

private const val MICROS_PER_SECOND = 1_000_000L
private const val NANOS_PER_MICRO = 1_000

/** Server timestamps have microsecond precision, so micros round-trip exactly. */
private fun Timestamp.toMicros(): Long = seconds * MICROS_PER_SECOND + nanoseconds / NANOS_PER_MICRO

private fun Long.toTimestamp(): Timestamp =
    Timestamp(this / MICROS_PER_SECOND, ((this % MICROS_PER_SECOND) * NANOS_PER_MICRO).toInt())

private fun FirebaseFirestoreException.Code.toShelfError(): ShelfError = when (this) {
    FirebaseFirestoreException.Code.UNAVAILABLE,
    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
    FirebaseFirestoreException.Code.ABORTED,
    FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED,
    FirebaseFirestoreException.Code.CANCELLED,
    -> ShelfError.Network

    FirebaseFirestoreException.Code.UNAUTHENTICATED,
    FirebaseFirestoreException.Code.PERMISSION_DENIED,
    -> ShelfError.Unavailable

    else -> ShelfError.Server
}
