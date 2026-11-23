package com.example.shelf.core.database

import com.example.shelf.core.common.dispatcher.Dispatcher
import com.example.shelf.core.common.dispatcher.ShelfDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Removes everything this app stored in its database ("remove my data from this device"). */
interface DatabaseCleaner {
    suspend fun clearAll()
}

internal class RoomDatabaseCleaner @Inject constructor(
    private val database: ShelfDatabase,
    @param:Dispatcher(ShelfDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : DatabaseCleaner {
    override suspend fun clearAll() = withContext(ioDispatcher) { database.clearAllTables() }
}
