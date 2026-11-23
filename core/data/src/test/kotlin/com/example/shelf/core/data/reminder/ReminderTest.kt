package com.example.shelf.core.data.reminder

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.shelf.core.common.DeepLinks
import com.example.shelf.core.model.DailyGoal
import com.example.shelf.core.model.GoalType
import com.example.shelf.core.model.UserPreferences
import com.example.shelf.core.testing.MutableClock
import com.example.shelf.core.testing.reminder.FakeReminderNotifier
import com.example.shelf.core.testing.reminder.FakeReminderScheduler
import com.example.shelf.core.testing.repository.FakeUserDataRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ReminderTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val goal = DailyGoal(GoalType.MINUTES, 15)
    private val userData = FakeUserDataRepository(
        UserPreferences(reminderEnabled = true, reminderTime = LocalTime.of(7, 30), dailyGoal = goal),
    )
    private val notifier = FakeReminderNotifier()
    private val scheduler = FakeReminderScheduler()

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
    }

    private fun worker(): ReminderWorker = TestListenableWorkerBuilder<ReminderWorker>(context)
        .setWorkerFactory(
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker = ReminderWorker(appContext, workerParameters, userData, notifier, scheduler)
            },
        )
        .build()

    @Test
    fun worker_postsTheReminderAndSchedulesTomorrows() = runTest {
        val result = worker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(listOf(goal), notifier.shown)
        assertEquals(listOf("next 07:30"), scheduler.calls)
    }

    @Test
    fun worker_keepsTheScheduleEvenWhenNotificationsAreBlocked() = runTest {
        notifier.allowed = false

        worker().doWork()

        assertEquals(emptyList(), notifier.shown)
        assertEquals(listOf("next 07:30"), scheduler.calls)
    }

    @Test
    fun worker_doesNothingOnceRemindersAreOff() = runTest {
        userData.setReminderEnabled(false)

        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertEquals(emptyList(), notifier.shown)
        assertEquals(emptyList(), scheduler.calls)
    }

    @Test
    fun scheduler_enqueuesOneUniqueWorkWithTheDelayUntilTheReminderTime() {
        val workManager = WorkManager.getInstance(context)
        val clock = MutableClock(Instant.parse("2026-03-02T08:00:00Z"), ZoneOffset.UTC)
        val scheduler = WorkManagerReminderScheduler(workManager, clock)

        scheduler.schedule(LocalTime.of(20, 0))
        val first = workManager.getWorkInfosForUniqueWork(WorkManagerReminderScheduler.WORK_NAME).get()
        assertEquals(1, first.size)
        assertEquals(WorkInfo.State.ENQUEUED, first.single().state)
        assertEquals(Duration.ofHours(12).toMillis(), first.single().initialDelayMillis)

        // Changing the time replaces the pending work instead of adding a second reminder.
        scheduler.schedule(LocalTime.of(7, 0))
        val replaced = workManager.getWorkInfosForUniqueWork(WorkManagerReminderScheduler.WORK_NAME).get()
        val enqueued = replaced.filter { it.state == WorkInfo.State.ENQUEUED }
        assertEquals(1, enqueued.size)
        assertEquals(Duration.ofHours(23).toMillis(), enqueued.single().initialDelayMillis)

        scheduler.cancel()
        val cancelled = workManager.getWorkInfosForUniqueWork(WorkManagerReminderScheduler.WORK_NAME).get()
        assertTrue(cancelled.all { it.state == WorkInfo.State.CANCELLED })
    }

    @Test
    fun notifier_postsAReminderThatOpensTheLogReadingDeepLink() {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val notifier = AndroidReminderNotifier(context)
        notifier.createChannel()

        assertTrue(notifier.showReminder(DailyGoal(GoalType.PAGES, 25)))

        val manager = context.getSystemService(NotificationManager::class.java)
        assertTrue(manager.getNotificationChannel(AndroidReminderNotifier.CHANNEL_ID) != null)
        val notification = shadowOf(manager).getNotification(AndroidReminderNotifier.NOTIFICATION_ID)
        val intent = shadowOf(notification.contentIntent).savedIntent
        assertEquals(DeepLinks.LOG_READING, intent.dataString)
        assertEquals(context.packageName, intent.`package`)
    }

    @Test
    fun notifier_doesNotPostWithoutThePermission() {
        shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val notifier = AndroidReminderNotifier(context)
        notifier.createChannel()

        assertFalse(notifier.showReminder(DailyGoal(GoalType.PAGES, 25)))
    }
}
