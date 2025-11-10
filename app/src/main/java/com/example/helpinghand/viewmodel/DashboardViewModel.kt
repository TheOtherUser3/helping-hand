package com.example.helpinghand.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.helpinghand.data.database.AppDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "helping_hand_db"
    ).build()
    private val dao = db.shoppingItemDao()

    val itemCount: StateFlow<Int> = dao.getCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}
