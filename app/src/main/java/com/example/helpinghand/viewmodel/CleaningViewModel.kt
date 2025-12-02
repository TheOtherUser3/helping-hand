package com.example.helpinghand.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helpinghand.AppLogger
import com.example.helpinghand.data.dao.CleaningReminderDao
import com.example.helpinghand.data.model.CleaningReminder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class CleaningReminderViewModel(
    private val dao: CleaningReminderDao
) : ViewModel() {

    val reminders: StateFlow<List<CleaningReminder>> =
        dao.getAll()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    // For dashboard: next closest due reminder
    val nextDueReminder: StateFlow<CleaningReminder?> =
        dao.getNextDue()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                null
            )

    @RequiresApi(Build.VERSION_CODES.O)
    private fun todayEpochDay(): Int =
        LocalDate.now().toEpochDay().toInt()

    @RequiresApi(Build.VERSION_CODES.O)
    fun addReminder(name: String, intervalDays: Int) {
        val trimmedName = name.trim()

        AppLogger.d(
            AppLogger.TAG_VM,
            "addReminder called: name=\"$trimmedName\", intervalDays=$intervalDays"
        )

        if (trimmedName.isBlank() || intervalDays <= 0) {
            AppLogger.d(
                AppLogger.TAG_VM,
                "addReminder: validation failed, aborting"
            )
            return
        }

        val today = todayEpochDay()
        val nextDue = today + intervalDays

        viewModelScope.launch {
            AppLogger.d(
                AppLogger.TAG_ASYNC,
                "addReminder: coroutine started for name=\"$trimmedName\""
            )
            try {
                val reminder = CleaningReminder(
                    name = trimmedName,
                    intervalDays = intervalDays,
                    nextDueEpochDay = nextDue
                )
                AppLogger.d(
                    AppLogger.TAG_DB,
                    "addReminder: inserting reminder=$reminder"
                )
                dao.insert(reminder)
            } catch (e: Exception) {
                AppLogger.e(
                    AppLogger.TAG_DB,
                    "addReminder: FAILED for name=\"$trimmedName\" message=${e.message}",
                    e
                )
            } finally {
                AppLogger.d(
                    AppLogger.TAG_ASYNC,
                    "addReminder: coroutine finished for name=\"$trimmedName\""
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun resetCycle(item: CleaningReminder) {
        val today = todayEpochDay()
        val nextDue = today + item.intervalDays

        viewModelScope.launch {
            AppLogger.d(
                AppLogger.TAG_ASYNC,
                "resetCycle: coroutine started for id=${item.id}"
            )
            try {
                val updated = item.copy(nextDueEpochDay = nextDue)
                AppLogger.d(
                    AppLogger.TAG_DB,
                    "resetCycle: updating reminder to $updated"
                )
                dao.update(updated)
            } catch (e: Exception) {
                AppLogger.e(
                    AppLogger.TAG_DB,
                    "resetCycle: FAILED for id=${item.id} message=${e.message}",
                    e
                )
            } finally {
                AppLogger.d(
                    AppLogger.TAG_ASYNC,
                    "resetCycle: coroutine finished for id=${item.id}"
                )
            }
        }
    }

    fun deleteReminder(item: CleaningReminder) {
        viewModelScope.launch {
            AppLogger.d(
                AppLogger.TAG_ASYNC,
                "deleteReminder: coroutine started for id=${item.id}"
            )
            try {
                AppLogger.d(
                    AppLogger.TAG_DB,
                    "deleteReminder: deleting reminder=$item"
                )
                dao.delete(item)
            } catch (e: Exception) {
                AppLogger.e(
                    AppLogger.TAG_DB,
                    "deleteReminder: FAILED for id=${item.id} message=${e.message}",
                    e
                )
            } finally {
                AppLogger.d(
                    AppLogger.TAG_ASYNC,
                    "deleteReminder: coroutine finished for id=${item.id}"
                )
            }
        }
    }
}

class CleaningReminderViewModelFactory(
    private val dao: CleaningReminderDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CleaningReminderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CleaningReminderViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
