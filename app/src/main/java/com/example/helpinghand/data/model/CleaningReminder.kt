package com.example.helpinghand.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cleaning_reminders")
data class CleaningReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val intervalDays: Int,
    val nextDueEpochDay: Int
)
