package com.example.shelf.core.database

import androidx.room.withTransaction
import javax.inject.Inject

/** Runs a read-modify-write sequence atomically. */
interface TransactionRunner {
    suspend operator fun <R> invoke(block: suspend () -> R): R
}

internal class RoomTransactionRunner @Inject constructor(private val database: ShelfDatabase) : TransactionRunner {
    override suspend fun <R> invoke(block: suspend () -> R): R = database.withTransaction(block)
}
