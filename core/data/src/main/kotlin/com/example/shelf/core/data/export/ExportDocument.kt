package com.example.shelf.core.data.export

import com.example.shelf.core.model.DailyGoal
import com.example.shelf.core.model.ReadingLog
import com.example.shelf.core.model.ShelfBook
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant

/** The exported file. `format` and `version` let a future importer recognise it. */
@Serializable
internal data class ExportDocument(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAt: String,
    val dailyGoal: ExportGoal,
    val books: List<ExportBook>,
    val readingLogs: List<ExportLog>,
) {
    companion object {
        const val FORMAT = "shelf-library"
        const val VERSION = 1
    }
}

@Serializable
internal data class ExportGoal(val type: String, val target: Int)

@Serializable
internal data class ExportBook(
    val workKey: String,
    val title: String,
    val authors: List<String>,
    val isbn: String?,
    val pageCount: Int?,
    val shelf: String,
    val currentPage: Int,
    val rating: Int?,
    val notes: String,
    val addedAt: String,
    val updatedAt: String,
    val finishedAt: String?,
)

@Serializable
internal data class ExportLog(val id: String, val date: String, val pages: Int, val minutes: Int, val workKey: String?)

internal fun buildExportDocument(
    library: List<ShelfBook>,
    logs: List<ReadingLog>,
    goal: DailyGoal,
    exportedAt: Instant,
): ExportDocument = ExportDocument(
    exportedAt = exportedAt.toString(),
    dailyGoal = ExportGoal(type = goal.type.name, target = goal.target),
    books = library.map { (book, entry) ->
        ExportBook(
            workKey = book.workKey,
            title = book.title,
            authors = book.authors,
            isbn = book.isbn,
            pageCount = book.pageCount,
            shelf = entry.shelf.name,
            currentPage = entry.currentPage,
            rating = entry.rating,
            notes = entry.notes,
            addedAt = entry.addedAt.toString(),
            updatedAt = entry.updatedAt.toString(),
            finishedAt = entry.finishedAt?.toString(),
        )
    },
    readingLogs = logs.map { log ->
        ExportLog(
            id = log.id,
            date = log.date.toString(),
            pages = log.pages,
            minutes = log.minutes,
            workKey = log.workKey,
        )
    },
)

internal val exportJson = Json {
    prettyPrint = true
    encodeDefaults = true
}
