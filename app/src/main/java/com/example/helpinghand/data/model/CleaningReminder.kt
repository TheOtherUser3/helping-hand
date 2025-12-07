package com.example.helpinghand.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cleaning_reminders")
data class CleaningReminder(
    @PrimaryKey val id: String = "",
    val name: String,
    val intervalDays: Int,
    val nextDueEpochDay: Int
)
