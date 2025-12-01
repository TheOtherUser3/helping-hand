package com.example.helpinghand

import android.app.Application
import androidx.room.Room
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.helpinghand.data.database.AppDatabase
import com.example.helpinghand.work.CleaningReminderWorker
import java.util.concurrent.TimeUnit

class HelpingHandApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "helping_hand_db"
        )
            .fallbackToDestructiveMigration()
            .build()

        scheduleCleaningReminderWorker()
    }

    private fun scheduleCleaningReminderWorker() {
        val request = PeriodicWorkRequestBuilder<CleaningReminderWorker>(
            1, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "cleaning_reminder_daily",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
