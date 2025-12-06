package com.example.helpinghand.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.helpinghand.HelpingHandApp
import com.example.helpinghand.MainActivity
import com.example.helpinghand.R
import com.example.helpinghand.data.model.CleaningReminder
import java.time.LocalDate
import com.example.helpinghand.AppLogger


class CleaningReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        AppLogger.d(AppLogger.TAG_ASYNC, "CleaningReminderWorker.doWork started")

        return try {
            // Get DAO from Application
            val app = applicationContext as HelpingHandApp
            val dao = app.database.cleaningReminderDao()

            val todayEpochDay = LocalDate.now().toEpochDay().toInt()
            AppLogger.d(
                AppLogger.TAG_DB,
                "CleaningReminderWorker: querying due reminders for day=$todayEpochDay"
            )

            val dueReminders: List<CleaningReminder> = dao.getDueReminders(todayEpochDay)

            AppLogger.d(
                AppLogger.TAG_DB,
                "CleaningReminderWorker: found ${dueReminders.size} due reminders"
            )

            if (dueReminders.isNotEmpty()) {
                AppLogger.d(
                    AppLogger.TAG_VM,
                    "CleaningReminderWorker: showing notification for first=${dueReminders.first().name}"
                )
                showDueNotification(dueReminders)
            }

            AppLogger.d(AppLogger.TAG_ASYNC, "CleaningReminderWorker.doWork completed successfully")
            Result.success()
        } catch (e: Exception) {
            AppLogger.e(
                AppLogger.TAG_ASYNC,
                "CleaningReminderWorker.doWork FAILED: ${e.message}",
                e
            )
            Result.failure()
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showDueNotification(dueReminders: List<CleaningReminder>) {
        val context = applicationContext
        val channelId = "cleaning_reminders_channel"

        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Cleaning reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when cleaning tasks are due"
            }
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // task names in the text
        val firstName = dueReminders.first().name
        val contentText = if (dueReminders.size == 1) {
            "$firstName is due today."
        } else {
            "$firstName and ${dueReminders.size - 1} more tasks are due today."
        }

        AppLogger.d(
            AppLogger.TAG_VM,
            "CleaningReminderWorker: building notification with contentText=\"$contentText\""
        )

        // Tap takes you to app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Cleaning reminder")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Single summary notification id
        NotificationManagerCompat.from(context).notify(1001, notification)
    }
}
