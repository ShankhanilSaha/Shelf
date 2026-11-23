package com.example.shelf.core.data.reminder

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.shelf.core.common.time.durationUntilNext
import java.time.Clock
import java.time.LocalTime
import javax.inject.Inject

/**
 * Schedules the daily reminder as one unique, one-time [ReminderWorker] whose initial delay is
 * the time until the next occurrence of the chosen wall-clock time. The worker re-arms itself
 * after posting, so the reminder follows daylight-saving changes (exact timing is not required).
 */
interface ReminderScheduler {
    /** (Re)schedules the reminder for the next [time], replacing any pending one. */
    fun schedule(time: LocalTime)

    /** Used by the running worker: queues the next reminder after the current run completes. */
    fun scheduleNext(time: LocalTime)

    fun cancel()
}

internal class WorkManagerReminderScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val clock: Clock,
) : ReminderScheduler {

    override fun schedule(time: LocalTime) = enqueue(time, ExistingWorkPolicy.REPLACE)

    // REPLACE from inside the running worker would cancel that very worker; appending chains the
    // next run after it, and its delay starts counting when the current run finishes.
    override fun scheduleNext(time: LocalTime) = enqueue(time, ExistingWorkPolicy.APPEND_OR_REPLACE)

    override fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    private fun enqueue(time: LocalTime, policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(clock.durationUntilNext(time))
            .addTag(WORK_NAME)
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, policy, request)
    }

    companion object {
        const val WORK_NAME = "shelf-daily-reminder"
    }
}
