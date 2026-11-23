package com.example.shelf.core.data.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Used when this build has no Firebase configuration: nothing is pulled or pushed. */
internal object NoOpSyncGateway : SyncGateway {
    override val isAvailable: Boolean = false

    override fun pullBooks(uid: String, since: Long?): Flow<RemotePage<RemoteBook>> = emptyFlow()

    override fun pullLogs(uid: String, since: Long?): Flow<RemotePage<RemoteLog>> = emptyFlow()

    override suspend fun pushBooks(uid: String, books: List<RemoteBook>) = Unit

    override suspend fun pushLogs(uid: String, logs: List<RemoteLog>) = Unit

    override suspend fun purgeTombstones(uid: String, deletedBefore: Long) = Unit
}
