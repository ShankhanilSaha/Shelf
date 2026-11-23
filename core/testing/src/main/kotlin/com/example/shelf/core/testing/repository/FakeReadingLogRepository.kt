package com.example.shelf.core.testing.repository

import com.example.shelf.core.data.repository.ReadingLogRepository
import com.example.shelf.core.model.GoalChange
import com.example.shelf.core.model.ReadingLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class FakeReadingLogRepository : ReadingLogRepository {
    private val logs = MutableStateFlow<List<ReadingLog>>(emptyList())
    val goalHistory = MutableStateFlow<List<GoalChange>>(emptyList())
    private var nextId = 0

    fun setLogs(value: List<ReadingLog>) {
        logs.value = value
    }

    val current: List<ReadingLog> get() = logs.value

    override fun observeLogs(): Flow<List<ReadingLog>> = logs.map { list -> list.sortedByDescending { it.date } }

    override fun observeLogsSince(from: LocalDate): Flow<List<ReadingLog>> =
        logs.map { list -> list.filter { !it.date.isBefore(from) }.sortedByDescending { it.date } }

    override fun observeGoalHistory(): Flow<List<GoalChange>> = goalHistory

    override suspend fun addLog(date: LocalDate, pages: Int, minutes: Int, workKey: String?): ReadingLog {
        val log = ReadingLog(id = "log-${nextId++}", date = date, pages = pages, minutes = minutes, workKey = workKey)
        logs.value = logs.value + log
        return log
    }

    override suspend fun deleteLog(id: String) {
        logs.value = logs.value.filterNot { it.id == id }
    }
}
