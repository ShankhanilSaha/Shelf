package com.example.shelf.core.data.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.shelf.core.common.DeepLinks
import com.example.shelf.core.data.R
import com.example.shelf.core.model.DailyGoal
import com.example.shelf.core.model.GoalType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Posts the daily reading reminder. */
interface ReminderNotifier {
    /** Creates the notification channel; safe to call on every app start. */
    fun createChannel()

    /** Returns false when notifications are blocked or the permission is missing. */
    fun showReminder(goal: DailyGoal): Boolean
}

internal class AndroidReminderNotifier @Inject constructor(@param:ApplicationContext private val context: Context) :
    ReminderNotifier {

    override fun createChannel() {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(context.getString(R.string.data_reminder_channel_name))
            .setDescription(context.getString(R.string.data_reminder_channel_description))
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    // The permission is checked right above notify(); lint cannot follow the early return.
    @SuppressLint("MissingPermission")
    override fun showReminder(goal: DailyGoal): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled() || !hasPermission()) return false

        // Tapping opens shelf://log, the "Log reading" sheet.
        val intent = Intent(Intent.ACTION_VIEW, DeepLinks.LOG_READING.toUri()).setPackage(context.packageName)
        val contentIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val textRes = when (goal.type) {
            GoalType.PAGES -> R.plurals.data_reminder_text_pages
            GoalType.MINUTES -> R.plurals.data_reminder_text_minutes
        }
        val text = context.resources.getQuantityString(textRes, goal.target, goal.target)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.data_ic_notification)
            .setContentTitle(context.getString(R.string.data_reminder_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
        return true
    }

    private fun hasPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    companion object {
        const val CHANNEL_ID = "reading_reminders"
        const val NOTIFICATION_ID = 1001
        private const val REQUEST_CODE = 1
    }
}
