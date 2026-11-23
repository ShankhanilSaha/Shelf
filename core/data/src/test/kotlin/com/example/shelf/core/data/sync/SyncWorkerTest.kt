package com.example.shelf.core.data.sync

import android.content.Context
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.shelf.core.data.mapper.toEntity
import com.example.shelf.core.data.repository.OfflineFirstLibraryRepository
import com.example.shelf.core.database.ShelfDatabase
import com.example.shelf.core.model.AuthState
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.testing.MutableClock
import com.example.shelf.core.testing.TestData
import com.example.shelf.core.testing.inMemoryShelfDatabase
import com.example.shelf.core.testing.repository.FakeAuthRepository
import com.example.shelf.core.testing.sync.FakeSyncGateway
import com.example.shelf.core.testing.sync.FakeSyncScheduler
import com.example.shelf.core.testing.transactionRunner
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class SyncWorkerTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private lateinit var db: ShelfDatabase
    private val clock = MutableClock()
    private val auth = FakeAuthRepository(AuthState.SignedIn(FakeAuthRepository.USER))

    @Before
    fun setUp() = runTest {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        db = inMemoryShelfDatabase(context)
        db.bookDao().upsert(TestData.summary(1).toEntity())
        OfflineFirstLibraryRepository(db.shelfDao(), db.bookDao(), db.transactionRunner(), clock, FakeSyncScheduler())
            .setShelf("OL1W", ShelfType.READING)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun worker(gateway: SyncGateway, attempt: Int = 0): SyncWorker =
        TestListenableWorkerBuilder<SyncWorker>(context)
            .setRunAttemptCount(attempt)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker = SyncWorker(
                        appContext,
                        workerParameters,
                        gateway,
                        auth,
                        LibrarySynchronizer(
                            gateway,
                            db.bookDao(),
                            db.shelfDao(),
                            db.readingLogDao(),
                            db.syncStateDao(),
                            db.transactionRunner(),
                            clock,
                        ),
                    )
                },
            )
            .build()

    @Test
    fun withoutFirebase_theWorkerDoesNothing() = runTest {
        val result = worker(NoOpSyncGateway).doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(1, db.shelfDao().getDirty().size)
    }

    @Test
    fun signedOut_theWorkerDoesNothing() = runTest {
        val gateway = FakeSyncGateway()
        auth.setState(AuthState.SignedOut)

        val result = worker(gateway).doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(gateway.pulls.isEmpty())
    }

    @Test
    fun signedIn_theWorkerSyncsTheAccount() = runTest {
        val gateway = FakeSyncGateway()

        val result = worker(gateway).doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(setOf("OL1W"), gateway.booksOf(FakeAuthRepository.USER.uid).keys)
    }

    @Test
    fun offline_theWorkerRetriesAFewTimesThenGivesUp() = runTest {
        val gateway = FakeSyncGateway().apply { offline = true }

        assertEquals(ListenableWorker.Result.retry(), worker(gateway, attempt = 0).doWork())
        assertEquals(ListenableWorker.Result.failure(), worker(gateway, attempt = 5).doWork())
    }

    @Test
    fun scheduler_enqueuesUniqueNetworkBoundWork() {
        val workManager = WorkManager.getInstance(context)
        val scheduler = WorkManagerSyncScheduler(workManager)

        scheduler.requestSync()
        scheduler.requestSync() // replaces the pending one
        scheduler.schedulePeriodicSync()

        val oneTime = workManager.getWorkInfosForUniqueWork(WorkManagerSyncScheduler.ONE_TIME_WORK_NAME).get()
        assertEquals(1, oneTime.count { it.state == WorkInfo.State.ENQUEUED })
        val pending = oneTime.single { it.state == WorkInfo.State.ENQUEUED }
        assertEquals(NetworkType.CONNECTED, pending.constraints.requiredNetworkType)
        assertEquals(WorkManagerSyncScheduler.DEBOUNCE.toMillis(), pending.initialDelayMillis)
        val periodic = workManager.getWorkInfosForUniqueWork(WorkManagerSyncScheduler.PERIODIC_WORK_NAME).get().single()
        assertEquals(Duration.ofHours(6).toMillis(), periodic.periodicityInfo?.repeatIntervalMillis)
        assertEquals(NetworkType.CONNECTED, periodic.constraints.requiredNetworkType)

        scheduler.cancelSync()

        assertTrue(
            workManager.getWorkInfosForUniqueWork(WorkManagerSyncScheduler.PERIODIC_WORK_NAME).get()
                .all { it.state == WorkInfo.State.CANCELLED },
        )
        assertTrue(
            workManager.getWorkInfosForUniqueWork(WorkManagerSyncScheduler.ONE_TIME_WORK_NAME).get()
                .all { it.state == WorkInfo.State.CANCELLED },
        )
    }

    @Test
    fun scheduler_runsRightAwayAfterSignIn() {
        val workManager = WorkManager.getInstance(context)

        WorkManagerSyncScheduler(workManager).requestSync(immediate = true)

        val info = workManager.getWorkInfosForUniqueWork(WorkManagerSyncScheduler.ONE_TIME_WORK_NAME).get().single()
        assertEquals(0, info.initialDelayMillis)
    }
}
