package com.example.helpinghand

import android.app.Application
import androidx.room.Room
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.helpinghand.data.database.AppDatabase
import com.example.helpinghand.data.database.SettingsRepository
import com.example.helpinghand.data.database.settingsDataStore   // <<< IMPORTANT
import com.example.helpinghand.work.CleaningReminderWorker
import java.util.concurrent.TimeUnit

class HelpingHandApp : Application() {

    lateinit var database: AppDatabase
        private set

    // settings repository using DataStore
    lateinit var settingsRepository: SettingsRepository
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

        // ✅ Pass the DataStore, not the Context
        settingsRepository = SettingsRepository(applicationContext.settingsDataStore)

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
