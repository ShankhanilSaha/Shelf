package com.example.shelf.core.data.sync

import com.example.shelf.core.database.TransactionRunner
import com.example.shelf.core.database.dao.BookDao
import com.example.shelf.core.database.dao.ReadingLogDao
import com.example.shelf.core.database.dao.ShelfDao
import com.example.shelf.core.database.dao.SyncStateDao
import com.example.shelf.core.database.model.SyncStateEntity
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import javax.inject.Inject

/**
 * One sync of the signed-in account: pull what changed remotely since the last cursor, apply it
 * with [ConflictPolicy], upload local changes, then drop old tombstones on both sides.
 *
 * Every step can be interrupted and repeated safely: pulled pages are applied together with
 * their cursor in one transaction, an upload only marks rows clean if they did not change
 * meanwhile, and applying a document twice changes nothing.
 */
class LibrarySynchronizer @Inject internal constructor(
    private val gateway: SyncGateway,
    private val bookDao: BookDao,
    private val shelfDao: ShelfDao,
    private val readingLogDao: ReadingLogDao,
    private val syncStateDao: SyncStateDao,
    private val transaction: TransactionRunner,
    private val clock: Clock,
) {
    suspend fun sync(uid: String) {
        // The first sync of an account on this device merges everything stored locally into it,
        // including what was added while signed out or under another account.
        if (COLLECTIONS.all { syncStateDao.get(uid, it) == null }) {
            transaction {
                shelfDao.markAllDirty()
                readingLogDao.markAllDirty()
            }
        }
        pull(uid, BOOKS, gateway::pullBooks) { applyRemoteBook(it) }
        pull(uid, LOGS, gateway::pullLogs) { applyRemoteLog(it) }
        pushBooks(uid)
        pushLogs(uid)
        purgeTombstones(uid)
        markSynced(uid)
    }

    private suspend fun <T> pull(
        uid: String,
        collection: String,
        fetch: (uid: String, since: Long?) -> Flow<RemotePage<T>>,
        apply: suspend (T) -> Unit,
    ) {
        val state = syncStateDao.get(uid, collection)
        var cursor = state?.cursor
        fetch(uid, cursor).collect { page ->
            transaction {
                page.items.forEach { apply(it) }
                val newCursor = maxOf(cursor ?: 0, page.cursor)
                syncStateDao.upsert(
                    SyncStateEntity(uid, collection, cursor = newCursor, syncedAt = state?.syncedAt ?: 0),
                )
                cursor = newCursor
            }
        }
    }

    private suspend fun applyRemoteBook(remote: RemoteBook) {
        val local = shelfDao.getEntryForSync(remote.workKey)
        if (!ConflictPolicy.remoteWins(local?.updatedAt, local?.dirty == true, remote.updatedAt)) return
        if (local == null && remote.deleted) return // nothing here to delete
        bookDao.insertIfAbsent(remote.toBookSeed())
        shelfDao.upsert(remote.toShelfEntryEntity())
    }

    private suspend fun applyRemoteLog(remote: RemoteLog) {
        val local = readingLogDao.getForSync(remote.id)
        if (!ConflictPolicy.remoteWins(local?.updatedAt, local?.dirty == true, remote.updatedAt)) return
        if (local == null && remote.deleted) return
        readingLogDao.upsert(remote.toEntity())
    }

    private suspend fun pushBooks(uid: String) {
        val dirty = shelfDao.getDirty()
        if (dirty.isEmpty()) return
        gateway.pushBooks(uid, dirty.map { it.toRemoteBook() })
        transaction { dirty.forEach { shelfDao.markClean(it.entry.workKey, it.entry.updatedAt) } }
    }

    private suspend fun pushLogs(uid: String) {
        val dirty = readingLogDao.getDirty()
        if (dirty.isEmpty()) return
        gateway.pushLogs(uid, dirty.map { it.toRemoteLog() })
        transaction { dirty.forEach { readingLogDao.markClean(it.id, it.updatedAt) } }
    }

    private suspend fun purgeTombstones(uid: String) {
        val cutoff = ConflictPolicy.tombstoneCutoff(clock.millis())
        transaction {
            shelfDao.purgeTombstones(cutoff)
            readingLogDao.purgeTombstones(cutoff)
        }
        gateway.purgeTombstones(uid, cutoff)
    }

    private suspend fun markSynced(uid: String) {
        val now = clock.millis()
        transaction {
            COLLECTIONS.forEach { collection ->
                val cursor = syncStateDao.get(uid, collection)?.cursor ?: 0
                syncStateDao.upsert(SyncStateEntity(uid, collection, cursor = cursor, syncedAt = now))
            }
        }
    }

    companion object {
        const val BOOKS = "books"
        const val LOGS = "logs"
        private val COLLECTIONS = listOf(BOOKS, LOGS)
    }
}
