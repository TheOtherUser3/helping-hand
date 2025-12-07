package com.example.helpinghand

import android.app.Application
import androidx.room.Room
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.helpinghand.data.database.AppDatabase
import com.example.helpinghand.data.database.SettingsRepository
import com.example.helpinghand.data.database.settingsDataStore
import com.example.helpinghand.work.CleaningReminderWorker
import com.google.firebase.FirebaseApp
import java.util.concurrent.TimeUnit

class HelpingHandApp : Application() {

    lateinit var database: AppDatabase
        private set

    // settings repository using DataStore
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // 1) Initialize Firebase BEFORE any FirebaseAuth / Firestore calls
        FirebaseApp.initializeApp(this)
        AppLogger.d(
            AppLogger.TAG_VM,
            "HelpingHandApp.onCreate - FirebaseApp initialized"
        )

        // 2) Initialize Room database
        AppLogger.d(
            AppLogger.TAG_VM,
            "HelpingHandApp.onCreate - initializing database and settingsRepository"
        )

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            if (BuildConfig.DEBUG && isRunningTests()) {
                "helping_hand_test_db"
            } else {
                "helping_hand_db"
            }
        )
            .fallbackToDestructiveMigration()
            .build()

        // 3) Initialize DataStore-backed settings repository
        settingsRepository = SettingsRepository(applicationContext.settingsDataStore)

        // 4) Schedule background worker
        scheduleCleaningReminderWorker()
    }

    private fun scheduleCleaningReminderWorker() {
        AppLogger.d(
            AppLogger.TAG_ASYNC,
            "scheduleCleaningReminderWorker: scheduling daily CleaningReminderWorker"
        )

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

private fun isRunningTests(): Boolean {
    return try {
        Class.forName("androidx.test.platform.app.InstrumentationRegistry")
        true
    } catch (e: ClassNotFoundException) {
        false
    }
}
