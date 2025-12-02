package com.example.helpinghand.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
        if (name.isBlank() || intervalDays <= 0) return

        val today = todayEpochDay()
        val nextDue = today + intervalDays

        viewModelScope.launch {
            dao.insert(
                CleaningReminder(
                    name = name,
                    intervalDays = intervalDays,
                    nextDueEpochDay = nextDue
                )
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun resetCycle(item: CleaningReminder) {
        val today = todayEpochDay()
        val nextDue = today + item.intervalDays

        viewModelScope.launch {
            dao.update(
                item.copy(nextDueEpochDay = nextDue)
            )
        }
    }

    fun deleteReminder(item: CleaningReminder) {
        viewModelScope.launch {
            dao.delete(item)
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
