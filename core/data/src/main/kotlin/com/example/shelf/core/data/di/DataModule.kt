package com.example.shelf.core.data.di

import android.content.Context
import androidx.work.WorkManager
import com.example.shelf.core.data.export.JsonLibraryExportRepository
import com.example.shelf.core.data.export.LibraryExportRepository
import com.example.shelf.core.data.reminder.AndroidReminderNotifier
import com.example.shelf.core.data.reminder.ReminderNotifier
import com.example.shelf.core.data.reminder.ReminderScheduler
import com.example.shelf.core.data.reminder.WorkManagerReminderScheduler
import com.example.shelf.core.data.repository.BookRepository
import com.example.shelf.core.data.repository.DefaultSearchRepository
import com.example.shelf.core.data.repository.DefaultUserDataRepository
import com.example.shelf.core.data.repository.LibraryRepository
import com.example.shelf.core.data.repository.OfflineFirstBookRepository
import com.example.shelf.core.data.repository.OfflineFirstLibraryRepository
import com.example.shelf.core.data.repository.OfflineFirstReadingLogRepository
import com.example.shelf.core.data.repository.ReadingLogRepository
import com.example.shelf.core.data.repository.SearchRepository
import com.example.shelf.core.data.repository.UserDataRepository
import com.example.shelf.core.data.sync.SyncScheduler
import com.example.shelf.core.data.sync.WorkManagerSyncScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface DataModule {
    @Binds
    fun bindSearchRepository(impl: DefaultSearchRepository): SearchRepository

    @Binds
    fun bindBookRepository(impl: OfflineFirstBookRepository): BookRepository

    @Binds
    fun bindLibraryRepository(impl: OfflineFirstLibraryRepository): LibraryRepository

    @Binds
    fun bindReadingLogRepository(impl: OfflineFirstReadingLogRepository): ReadingLogRepository

    @Binds
    fun bindUserDataRepository(impl: DefaultUserDataRepository): UserDataRepository

    @Binds
    fun bindLibraryExportRepository(impl: JsonLibraryExportRepository): LibraryExportRepository

    @Binds
    fun bindReminderScheduler(impl: WorkManagerReminderScheduler): ReminderScheduler

    @Binds
    fun bindReminderNotifier(impl: AndroidReminderNotifier): ReminderNotifier

    @Binds
    fun bindSyncScheduler(impl: WorkManagerSyncScheduler): SyncScheduler
}

@Module
@InstallIn(SingletonComponent::class)
internal object WorkModule {
    /** WorkManager is initialised on demand with Hilt's worker factory (see ShelfApplication). */
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)
}
