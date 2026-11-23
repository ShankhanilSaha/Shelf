package com.example.shelf.core.data.export

import android.content.Context
import android.net.Uri
import com.example.shelf.core.common.dispatcher.Dispatcher
import com.example.shelf.core.common.dispatcher.ShelfDispatchers
import com.example.shelf.core.common.result.ShelfError
import com.example.shelf.core.common.result.ShelfResult
import com.example.shelf.core.data.repository.LibraryRepository
import com.example.shelf.core.data.repository.ReadingLogRepository
import com.example.shelf.core.data.repository.UserDataRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Clock
import javax.inject.Inject

/** Writes the whole library and reading log as JSON to a document the user picked (SAF). */
interface LibraryExportRepository {
    suspend fun exportTo(destination: Uri): ShelfResult<ExportSummary>
}

data class ExportSummary(val books: Int, val readingLogs: Int)

internal class JsonLibraryExportRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val libraryRepository: LibraryRepository,
    private val readingLogRepository: ReadingLogRepository,
    private val userDataRepository: UserDataRepository,
    private val clock: Clock,
    @param:Dispatcher(ShelfDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : LibraryExportRepository {

    override suspend fun exportTo(destination: Uri): ShelfResult<ExportSummary> {
        val document = buildExportDocument(
            library = libraryRepository.observeLibrary().first(),
            logs = readingLogRepository.observeLogs().first(),
            goal = userDataRepository.userPreferences.first().dailyGoal,
            exportedAt = clock.instant(),
        )
        val bytes = exportJson.encodeToString(ExportDocument.serializer(), document).toByteArray()
        return withContext(ioDispatcher) {
            try {
                // "wt" truncates, so re-exporting over an existing file never leaves stale bytes.
                val stream = context.contentResolver.openOutputStream(destination, "wt")
                    ?: return@withContext ShelfResult.Failure(ShelfError.Unknown)
                stream.use { it.write(bytes) }
                ShelfResult.Success(ExportSummary(books = document.books.size, readingLogs = document.readingLogs.size))
            } catch (e: IOException) {
                ShelfResult.Failure(ShelfError.Unknown, e)
            } catch (e: SecurityException) {
                ShelfResult.Failure(ShelfError.Unknown, e)
            }
        }
    }
}
