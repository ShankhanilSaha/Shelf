package com.example.shelf.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.shelf.core.database.dao.BookDao
import com.example.shelf.core.database.dao.GoalDao
import com.example.shelf.core.database.dao.ReadingLogDao
import com.example.shelf.core.database.dao.ShelfDao
import com.example.shelf.core.database.dao.SyncStateDao
import com.example.shelf.core.database.model.BookEntity
import com.example.shelf.core.database.model.GoalEntity
import com.example.shelf.core.database.model.ReadingLogEntity
import com.example.shelf.core.database.model.ShelfEntryEntity
import com.example.shelf.core.database.model.SyncStateEntity

/**
 * Schema history lives in core/database/schemas (exported JSON, one file per version); every
 * version change needs a migration in [ShelfMigrations] and a test in MigrationTest.
 */
@Database(
    entities = [
        BookEntity::class,
        ShelfEntryEntity::class,
        ReadingLogEntity::class,
        GoalEntity::class,
        SyncStateEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ShelfDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    abstract fun shelfDao(): ShelfDao

    abstract fun readingLogDao(): ReadingLogDao

    abstract fun goalDao(): GoalDao

    abstract fun syncStateDao(): SyncStateDao

    companion object {
        const val NAME = "shelf.db"
    }
}
