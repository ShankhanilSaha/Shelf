package com.example.shelf.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Hand-written migrations, tested against the exported schemas in MigrationTest. */
object ShelfMigrations {
    /**
     * 1 -> 2: sync bookkeeping. Every existing row is marked dirty so that the first sign-in
     * uploads the library that was built before sync existed, and logs get an update time.
     */
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `shelf_entries` ADD COLUMN `dirty` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `shelf_entries` ADD COLUMN `deleted` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE `shelf_entries` SET `dirty` = 1")
            db.execSQL("ALTER TABLE `reading_logs` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `reading_logs` ADD COLUMN `dirty` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `reading_logs` ADD COLUMN `deleted` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE `reading_logs` SET `updated_at` = `created_at`, `dirty` = 1")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `sync_state` (`uid` TEXT NOT NULL, `collection` TEXT NOT NULL, " +
                    "`cursor` INTEGER NOT NULL, `synced_at` INTEGER NOT NULL, PRIMARY KEY(`uid`, `collection`))",
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
