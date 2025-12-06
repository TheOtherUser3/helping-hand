package com.example.helpinghand.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helpinghand.AppLogger
import com.example.helpinghand.data.dao.CleaningReminderDao
import com.example.helpinghand.data.dao.ShoppingItemDao
import com.example.helpinghand.data.model.CleaningReminder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    private val shoppingdao: ShoppingItemDao,
    private val cleaningdao: CleaningReminderDao
) : ViewModel() {

    // itemCount comes from dao.getCount()
    val itemCount: StateFlow<Int> =
        shoppingdao.getCount()
            .onEach { count ->
                AppLogger.d(
                    AppLogger.TAG_DB,
                    "Dashboard itemCount flow emit: count=$count"
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                0
            )

    val nextDueReminder: StateFlow<CleaningReminder?> =
        cleaningdao.getNextDue()
            .onEach { reminder ->
                AppLogger.d(
                    AppLogger.TAG_DB,
                    "Dashboard nextDueReminder emit: reminder=$reminder"
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                null
            )

    @RequiresApi(Build.VERSION_CODES.O)
    val daysUntilNextDue: StateFlow<Int?> =
        nextDueReminder
            .map { reminder ->
                if (reminder == null) {
                    AppLogger.d(
                        AppLogger.TAG_VM,
                        "daysUntilNextDue: no upcoming reminder"
                    )
                    return@map null
                }

                val today = java.time.LocalDate.now().toEpochDay()
                val days = (reminder.nextDueEpochDay - today).toInt()
                AppLogger.d(
                    AppLogger.TAG_VM,
                    "daysUntilNextDue: computed days=$days for reminderId=${reminder.id}"
                )
                days
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                null
            )
}

class DashboardViewModelFactory(
    private val shoppingdao: ShoppingItemDao,
    private val cleaningdao: CleaningReminderDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(shoppingdao, cleaningdao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
