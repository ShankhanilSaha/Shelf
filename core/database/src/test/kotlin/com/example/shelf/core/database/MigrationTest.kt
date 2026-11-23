package com.example.shelf.core.database

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.example.shelf.core.model.ShelfType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Builds a database exactly as version 1 created it (from the exported schema), migrates it by
 * opening it with Room, which also validates the result against the current entities, and checks
 * that the data survived. The Room Gradle plugin only exposes schemas to instrumented tests, so
 * this does what MigrationTestHelper does from the schema files directly.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    private val context = RuntimeEnvironment.getApplication()
    private val name = "migration-test.db"
    private var database: ShelfDatabase? = null

    @Before
    fun setUp() {
        context.deleteDatabase(name)
    }

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(name)
    }

    @Test
    fun migrate1To2_keepsTheLibraryAndMarksItForTheFirstUpload() = runTest {
        createVersion(1) { db ->
            db.execSQL(
                "INSERT INTO books (work_key, title, authors, first_publish_year, cover_id, description, " +
                    "subjects, page_count, isbn, details_fetched_at) " +
                    "VALUES ('OL1W', 'Dune', '[\"Frank Herbert\"]', 1965, 42, NULL, '[]', 604, NULL, NULL)",
            )
            db.execSQL(
                "INSERT INTO shelf_entries (work_key, shelf, current_page, rating, notes, added_at, updated_at, " +
                    "finished_at) VALUES ('OL1W', 'READING', 120, NULL, 'Spice', 1000, 2000, NULL)",
            )
            db.execSQL(
                "INSERT INTO reading_logs (id, work_key, epoch_day, pages, minutes, created_at) " +
                    "VALUES ('log-1', 'OL1W', 20000, 25, 0, 3000)",
            )
        }

        val migrated = Room.databaseBuilder(context, ShelfDatabase::class.java, name)
            .addMigrations(*ShelfMigrations.ALL)
            .allowMainThreadQueries()
            .build()
            .also { database = it }

        val entry = migrated.shelfDao().getEntryForSync("OL1W")!!
        assertEquals(ShelfType.READING, entry.shelf)
        assertEquals(120, entry.currentPage)
        assertEquals("Spice", entry.notes)
        assertTrue(entry.dirty)
        assertEquals(false, entry.deleted)
        assertEquals(listOf("Frank Herbert"), migrated.bookDao().getBook("OL1W")?.authors)

        val log = migrated.readingLogDao().getForSync("log-1")!!
        assertEquals(3000, log.updatedAt)
        assertTrue(log.dirty)
        assertEquals(1, migrated.readingLogDao().observeAll().first().size)
        assertEquals(null, migrated.syncStateDao().get("anyone", "books"))
    }

    /** Creates the tables, indices and Room bookkeeping of [version] from its exported schema. */
    private fun createVersion(version: Int, seed: (SQLiteDatabase) -> Unit) {
        val schema = JSONObject(File("schemas/${ShelfDatabase::class.qualifiedName}/$version.json").readText())
            .getJSONObject("database")
        val entities = schema.getJSONArray("entities")
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name).apply { parentFile?.mkdirs() }, null)
            .use { db ->
                for (i in 0 until entities.length()) {
                    val entity = entities.getJSONObject(i)
                    val table = entity.getString("tableName")
                    db.execSQL(entity.getString("createSql").replace(TABLE_NAME, table))
                    val indices = entity.optJSONArray("indices") ?: continue
                    for (j in 0 until indices.length()) {
                        db.execSQL(indices.getJSONObject(j).getString("createSql").replace(TABLE_NAME, table))
                    }
                }
                val setup = schema.getJSONArray("setupQueries")
                for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
                seed(db)
                db.version = version
            }
    }

    private companion object {
        const val TABLE_NAME = "\${TABLE_NAME}"
    }
}
