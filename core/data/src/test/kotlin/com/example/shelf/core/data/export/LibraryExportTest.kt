package com.example.shelf.core.data.export

import android.net.Uri
import com.example.shelf.core.common.result.ShelfResult
import com.example.shelf.core.model.DailyGoal
import com.example.shelf.core.model.GoalType
import com.example.shelf.core.model.ReadingLog
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.model.UserPreferences
import com.example.shelf.core.testing.MutableClock
import com.example.shelf.core.testing.TestData
import com.example.shelf.core.testing.repository.FakeLibraryRepository
import com.example.shelf.core.testing.repository.FakeReadingLogRepository
import com.example.shelf.core.testing.repository.FakeUserDataRepository
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class LibraryExportTest {
    private val context = RuntimeEnvironment.getApplication()
    private val library = FakeLibraryRepository().apply {
        addBook(
            TestData.book(1, title = "Dune"),
            TestData.entry(1, shelf = ShelfType.FINISHED, rating = 5, notes = "Spice"),
        )
        addBook(TestData.book(2, title = "Piranesi"), TestData.entry(2, shelf = ShelfType.READING, currentPage = 40))
    }
    private val logs = FakeReadingLogRepository().apply {
        setLogs(
            listOf(ReadingLog(id = "a", date = LocalDate.of(2026, 11, 23), pages = 30, minutes = 0, workKey = "OL2W")),
        )
    }
    private val userData = FakeUserDataRepository(UserPreferences(dailyGoal = DailyGoal(GoalType.PAGES, 25)))

    @Test
    fun exportWritesTheLibraryAndLogAsJsonToTheChosenDocument() = runTest {
        val uri = Uri.parse("content://com.example.documents/document/export.json")
        val sink = ByteArrayOutputStream()
        shadowOf(context.contentResolver).registerOutputStream(uri, sink)
        val repository = JsonLibraryExportRepository(
            context = context,
            libraryRepository = library,
            readingLogRepository = logs,
            userDataRepository = userData,
            clock = MutableClock(),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        val result = repository.exportTo(uri)

        assertEquals(ShelfResult.Success(ExportSummary(books = 2, readingLogs = 1)), result)
        val json = Json.parseToJsonElement(sink.toString(Charsets.UTF_8)).jsonObject
        assertEquals("shelf-library", json.getValue("format").jsonPrimitive.content)
        assertEquals(25, json.getValue("dailyGoal").jsonObject.getValue("target").jsonPrimitive.int)
        val books = json.getValue("books").jsonArray.map { it.jsonObject }
        val dune = books.single { it.getValue("title").jsonPrimitive.content == "Dune" }
        assertEquals("FINISHED", dune.getValue("shelf").jsonPrimitive.content)
        assertEquals("Spice", dune.getValue("notes").jsonPrimitive.content)
        assertEquals(
            "2026-11-23",
            json.getValue("readingLogs").jsonArray.single().jsonObject.getValue("date").jsonPrimitive.content,
        )
    }
}
