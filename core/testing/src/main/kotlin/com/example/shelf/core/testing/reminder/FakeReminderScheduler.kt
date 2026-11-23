package com.example.shelf.core.testing.reminder

import com.example.shelf.core.data.reminder.ReminderNotifier
import com.example.shelf.core.data.reminder.ReminderScheduler
import com.example.shelf.core.model.DailyGoal
import java.time.LocalTime

/** Records scheduling calls instead of talking to WorkManager. */
class FakeReminderScheduler : ReminderScheduler {
    /** The pending reminder time, or null when nothing is scheduled. */
    var scheduledFor: LocalTime? = null
        private set
    val calls = mutableListOf<String>()

    override fun schedule(time: LocalTime) {
        calls += "schedule $time"
        scheduledFor = time
    }

    override fun scheduleNext(time: LocalTime) {
        calls += "next $time"
        scheduledFor = time
    }

    override fun cancel() {
        calls += "cancel"
        scheduledFor = null
    }
}

class FakeReminderNotifier(var allowed: Boolean = true) : ReminderNotifier {
    val shown = mutableListOf<DailyGoal>()
    var channelCreated = false
        private set

    override fun createChannel() {
        channelCreated = true
    }

    override fun showReminder(goal: DailyGoal): Boolean {
        if (allowed) shown += goal
        return allowed
    }
}
