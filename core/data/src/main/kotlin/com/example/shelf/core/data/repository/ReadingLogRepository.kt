package com.example.shelf.core.data.repository

import com.example.shelf.core.data.mapper.toEntity
import com.example.shelf.core.data.mapper.toGoalChange
import com.example.shelf.core.data.mapper.toReadingLog
import com.example.shelf.core.data.sync.SyncScheduler
import com.example.shelf.core.database.dao.GoalDao
import com.example.shelf.core.database.dao.ReadingLogDao
import com.example.shelf.core.model.GoalChange
import com.example.shelf.core.model.ReadingLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/** Reading sessions and the history of the daily goal. */
interface ReadingLogRepository {
    /** Newest first. */
    fun observeLogs(): Flow<List<ReadingLog>>

    /** Logs dated [from] or later, newest first. */
    fun observeLogsSince(from: LocalDate): Flow<List<ReadingLog>>

    /** Goal changes, oldest first. */
    fun observeGoalHistory(): Flow<List<GoalChange>>

    suspend fun addLog(date: LocalDate, pages: Int, minutes: Int, workKey: String?): ReadingLog

    suspend fun deleteLog(id: String)
}

internal class OfflineFirstReadingLogRepository @Inject constructor(
    private val readingLogDao: ReadingLogDao,
    private val goalDao: GoalDao,
    private val clock: Clock,
    private val syncScheduler: SyncScheduler,
) : ReadingLogRepository {

    override fun observeLogs(): Flow<List<ReadingLog>> = readingLogDao.observeAll()
        .map { rows -> rows.map { it.toReadingLog() } }
        .distinctUntilChanged()

    override fun observeLogsSince(from: LocalDate): Flow<List<ReadingLog>> = readingLogDao
        .observeSince(from.toEpochDay())
        .map { rows -> rows.map { it.toReadingLog() } }
        .distinctUntilChanged()

    override fun observeGoalHistory(): Flow<List<GoalChange>> = goalDao.observeHistory()
        .map { rows -> rows.map { it.toGoalChange() } }
        .distinctUntilChanged()

    override suspend fun addLog(date: LocalDate, pages: Int, minutes: Int, workKey: String?): ReadingLog {
        val log = ReadingLog(
            id = UUID.randomUUID().toString(),
            date = date,
            pages = pages,
            minutes = minutes,
            workKey = workKey,
        )
        readingLogDao.upsert(log.toEntity(createdAt = clock.instant()))
        syncScheduler.requestSync()
        return log
    }

    /** Leaves a tombstone so that the deletion reaches other devices. */
    override suspend fun deleteLog(id: String) {
        if (readingLogDao.markDeleted(id, now = clock.millis()) > 0) syncScheduler.requestSync()
    }
}
