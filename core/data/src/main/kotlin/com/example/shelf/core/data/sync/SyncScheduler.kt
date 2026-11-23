package com.example.shelf.core.data.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import javax.inject.Inject

/** Schedules [SyncWorker]: on demand after local writes and sign-in, and periodically. */
interface SyncScheduler {
    /**
     * Syncs soon. Requests made in quick succession collapse into one run a few seconds after
     * the last; [immediate] skips that wait (sign-in).
     */
    fun requestSync(immediate: Boolean = false)

    /** Pulls remote changes every few hours while signed in. Keeps an existing schedule. */
    fun schedulePeriodicSync()

    fun cancelSync()
}

internal class WorkManagerSyncScheduler @Inject constructor(private val workManager: WorkManager) : SyncScheduler {

    override fun requestSync(immediate: Boolean) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(NETWORK)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF)
            .setInitialDelay(if (immediate) Duration.ZERO else DEBOUNCE)
            .addTag(TAG)
            .build()
        // REPLACE turns a burst of edits into one run. Interrupting a running sync loses nothing:
        // every step is idempotent and the cursor only moves together with the applied page.
        workManager.enqueueUniqueWork(ONE_TIME_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    override fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(PERIOD)
            .setConstraints(NETWORK)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF)
            .addTag(TAG)
            .build()
        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    override fun cancelSync() {
        workManager.cancelUniqueWork(ONE_TIME_WORK_NAME)
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    companion object {
        const val ONE_TIME_WORK_NAME = "shelf-sync"
        const val PERIODIC_WORK_NAME = "shelf-sync-periodic"
        const val TAG = "shelf-sync"
        val DEBOUNCE: Duration = Duration.ofSeconds(5)
        val BACKOFF: Duration = Duration.ofSeconds(30)
        val PERIOD: Duration = Duration.ofHours(6)
        private val NETWORK = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    }
}
