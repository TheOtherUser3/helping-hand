package com.example.helpinghand.data.model

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Entity(tableName = "doctor_appointments")
data class DoctorAppointment(
    @PrimaryKey val id: String = "",
    val doctorName: String,
    // Store as simple string in DB: "Doctor", "Dentist", "Specialist"
    val type: String,
    // Store dates as epochDay Ints (nullable)
    val lastVisitEpochDay: Int?,
    val nextVisitEpochDay: Int?,
    // Store normalized digits-only phone
    val phoneRaw: String,
    val officeName: String,
    val intervalMonths: Int
) {

    @RequiresApi(Build.VERSION_CODES.O)
    fun getNextVisitText(): String {
        val today = LocalDate.now()
        val next = nextVisitEpochDay?.let { LocalDate.ofEpochDay(it.toLong()) }
        val last = lastVisitEpochDay?.let { LocalDate.ofEpochDay(it.toLong()) }

        if (next == null && last == null) {
            return "No visits scheduled"
        }

        if (next != null) {
            val daysUntil = ChronoUnit.DAYS.between(today, next)
            return when {
                daysUntil < 0 -> "Overdue by ${-daysUntil} days"
                daysUntil == 0L -> "Today"
                daysUntil < 7 -> "In $daysUntil days"
                daysUntil < 30 -> "In ${daysUntil / 7} weeks"
                else -> "In ${daysUntil / 30} months"
            }
        }

        if (last != null) {
            val monthsAgo = ChronoUnit.MONTHS.between(last, today)
            return "Last visit ${monthsAgo}mo ago"
        }

        return "No visits scheduled"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getFormattedNextDate(): String {
        val next = nextVisitEpochDay?.let { LocalDate.ofEpochDay(it.toLong()) }
        return next?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: "Not set"
    }

    /**
     * Display form of the phone number, with dashes added when length matches.
     * e.g. 5551234567 -> 555-123-4567
     */
    fun displayPhone(): String = formatPhoneNumber(phoneRaw)
}

/**
 * Helper to format a digits-only phone string into something friendly.
 */
fun formatPhoneNumber(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return when (digits.length) {
        10 -> "${digits.substring(0, 3)}-${digits.substring(3, 6)}-${digits.substring(6)}"
        7  -> "${digits.substring(0, 3)}-${digits.substring(3)}"
        else -> digits
    }
}
