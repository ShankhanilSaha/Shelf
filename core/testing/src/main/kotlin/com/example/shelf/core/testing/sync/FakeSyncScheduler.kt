package com.example.shelf.core.testing.sync

import com.example.shelf.core.data.sync.SyncScheduler

/** Counts sync requests instead of talking to WorkManager. */
class FakeSyncScheduler : SyncScheduler {
    var requests = 0
        private set
    var immediateRequests = 0
        private set
    var periodicScheduled = false
        private set

    override fun requestSync(immediate: Boolean) {
        requests++
        if (immediate) immediateRequests++
    }

    override fun schedulePeriodicSync() {
        periodicScheduled = true
    }

    override fun cancelSync() {
        periodicScheduled = false
    }
}
