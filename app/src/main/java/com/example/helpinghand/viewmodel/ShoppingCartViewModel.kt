package com.example.helpinghand.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.helpinghand.data.database.AppDatabase
import com.example.helpinghand.data.model.ShoppingItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingCartViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "helping_hand_db"
    ).build()
    private val dao = db.shoppingItemDao()

    val items: StateFlow<List<ShoppingItem>> =
        dao.getAllItems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addItem(name: String) = viewModelScope.launch {
        dao.insert(ShoppingItem(text = name))
    }

    fun toggleChecked(item: ShoppingItem, checked: Boolean) = viewModelScope.launch {
        dao.update(item.copy(isChecked = checked))
    }

    fun deleteChecked() = viewModelScope.launch {
        dao.deleteChecked()
    }


}
