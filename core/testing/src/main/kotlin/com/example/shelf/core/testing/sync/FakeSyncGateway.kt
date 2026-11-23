package com.example.shelf.core.testing.sync

import com.example.shelf.core.common.result.ShelfError
import com.example.shelf.core.common.result.ShelfException
import com.example.shelf.core.data.sync.ConflictPolicy
import com.example.shelf.core.data.sync.RemoteBook
import com.example.shelf.core.data.sync.RemoteLog
import com.example.shelf.core.data.sync.RemotePage
import com.example.shelf.core.data.sync.SyncGateway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * An in-memory server that behaves like the Firestore gateway: uploads follow the same
 * last-write-wins check, every write gets a new server time, and pulls return documents written
 * at or after the cursor, oldest first, in pages of [pageSize].
 */
class FakeSyncGateway(override val isAvailable: Boolean = true) : SyncGateway {
    data class Stored<T>(val value: T, val syncedAt: Long)

    private val books = mutableMapOf<String, MutableMap<String, Stored<RemoteBook>>>()
    private val logs = mutableMapOf<String, MutableMap<String, Stored<RemoteLog>>>()
    private var serverTime = 1_000L

    var offline = false
    var pageSize = 2

    /** Every pull as "collection since", e.g. "books null". */
    val pulls = mutableListOf<String>()
    var purgedBefore: Long? = null
        private set

    fun booksOf(uid: String): Map<String, RemoteBook> = books[uid].orEmpty().mapValues { it.value.value }

    fun logsOf(uid: String): Map<String, RemoteLog> = logs[uid].orEmpty().mapValues { it.value.value }

    /** A write made by another device. */
    fun putBook(uid: String, book: RemoteBook) {
        books.getOrPut(uid) { mutableMapOf() }[book.workKey] = Stored(book, ++serverTime)
    }

    /** A write made by another device. */
    fun putLog(uid: String, log: RemoteLog) {
        logs.getOrPut(uid) { mutableMapOf() }[log.id] = Stored(log, ++serverTime)
    }

    override fun pullBooks(uid: String, since: Long?): Flow<RemotePage<RemoteBook>> =
        pull("books", since) { books[uid].orEmpty().values }

    override fun pullLogs(uid: String, since: Long?): Flow<RemotePage<RemoteLog>> =
        pull("logs", since) { logs[uid].orEmpty().values }

    override suspend fun pushBooks(uid: String, books: List<RemoteBook>) {
        failIfOffline()
        val server = this.books.getOrPut(uid) { mutableMapOf() }
        val commitTime = ++serverTime
        books.forEach { book ->
            if (ConflictPolicy.localWins(book.updatedAt, server[book.workKey]?.value?.updatedAt)) {
                server[book.workKey] = Stored(book, commitTime)
            }
        }
    }

    override suspend fun pushLogs(uid: String, logs: List<RemoteLog>) {
        failIfOffline()
        val server = this.logs.getOrPut(uid) { mutableMapOf() }
        val commitTime = ++serverTime
        logs.forEach { log ->
            if (ConflictPolicy.localWins(log.updatedAt, server[log.id]?.value?.updatedAt)) {
                server[log.id] = Stored(log, commitTime)
            }
        }
    }

    override suspend fun purgeTombstones(uid: String, deletedBefore: Long) {
        failIfOffline()
        purgedBefore = deletedBefore
        books[uid]?.values?.removeAll { it.value.deleted && it.value.updatedAt < deletedBefore }
        logs[uid]?.values?.removeAll { it.value.deleted && it.value.updatedAt < deletedBefore }
    }

    private fun <T> pull(name: String, since: Long?, documents: () -> Collection<Stored<T>>) = flow {
        failIfOffline()
        pulls += "$name $since"
        documents()
            .filter { since == null || it.syncedAt >= since }
            .sortedBy { it.syncedAt }
            .chunked(pageSize)
            .forEach { page -> emit(RemotePage(page.map { it.value }, page.maxOf { it.syncedAt })) }
    }

    private fun failIfOffline() {
        if (offline) throw ShelfException(ShelfError.Network)
    }
}
