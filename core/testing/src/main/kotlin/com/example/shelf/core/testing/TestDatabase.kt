package com.example.shelf.core.testing

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.example.shelf.core.database.ShelfDatabase
import com.example.shelf.core.database.TransactionRunner

/** An in-memory database for Robolectric tests; pass `RuntimeEnvironment.getApplication()`. */
fun inMemoryShelfDatabase(context: Context): ShelfDatabase =
    Room.inMemoryDatabaseBuilder(context, ShelfDatabase::class.java)
        .allowMainThreadQueries()
        .build()

/** The same transactional behavior as production, over the given database. */
fun ShelfDatabase.transactionRunner(): TransactionRunner = object : TransactionRunner {
    override suspend fun <R> invoke(block: suspend () -> R): R = withTransaction(block)
}
