package com.example.shelf.core.data.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.shelf.core.data.repository.UserDataRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/** Posts the daily reminder, then schedules tomorrow's. Does nothing once reminders are off. */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val userDataRepository: UserDataRepository,
    private val notifier: ReminderNotifier,
    private val scheduler: ReminderScheduler,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val preferences = userDataRepository.userPreferences.first()
        if (!preferences.reminderEnabled) return Result.success()
        notifier.showReminder(preferences.dailyGoal)
        scheduler.scheduleNext(preferences.reminderTime)
        return Result.success()
    }
}
