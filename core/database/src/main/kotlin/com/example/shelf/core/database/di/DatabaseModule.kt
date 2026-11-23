package com.example.shelf.core.database.di

import android.content.Context
import androidx.room.Room
import com.example.shelf.core.database.DatabaseCleaner
import com.example.shelf.core.database.RoomDatabaseCleaner
import com.example.shelf.core.database.RoomTransactionRunner
import com.example.shelf.core.database.ShelfDatabase
import com.example.shelf.core.database.ShelfMigrations
import com.example.shelf.core.database.TransactionRunner
import com.example.shelf.core.database.dao.BookDao
import com.example.shelf.core.database.dao.GoalDao
import com.example.shelf.core.database.dao.ReadingLogDao
import com.example.shelf.core.database.dao.ShelfDao
import com.example.shelf.core.database.dao.SyncStateDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    // Copying the migration array once at startup costs nothing measurable.
    @Suppress("SpreadOperator")
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ShelfDatabase =
        Room.databaseBuilder(context, ShelfDatabase::class.java, ShelfDatabase.NAME)
            .addMigrations(*ShelfMigrations.ALL)
            .build()

    @Provides
    fun provideBookDao(database: ShelfDatabase): BookDao = database.bookDao()

    @Provides
    fun provideShelfDao(database: ShelfDatabase): ShelfDao = database.shelfDao()

    @Provides
    fun provideReadingLogDao(database: ShelfDatabase): ReadingLogDao = database.readingLogDao()

    @Provides
    fun provideGoalDao(database: ShelfDatabase): GoalDao = database.goalDao()

    @Provides
    fun provideSyncStateDao(database: ShelfDatabase): SyncStateDao = database.syncStateDao()

    @Provides
    fun provideTransactionRunner(database: ShelfDatabase): TransactionRunner = RoomTransactionRunner(database)

    @Provides
    fun provideDatabaseCleaner(cleaner: RoomDatabaseCleaner): DatabaseCleaner = cleaner
}
