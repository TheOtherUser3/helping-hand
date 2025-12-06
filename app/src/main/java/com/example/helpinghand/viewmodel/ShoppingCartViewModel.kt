package com.example.helpinghand.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helpinghand.AppLogger
import com.example.helpinghand.data.dao.ShoppingItemDao
import com.example.helpinghand.data.model.ShoppingItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingCartViewModel(
    private val dao: ShoppingItemDao
) : ViewModel() {

    // items is a StateFlow from DAO
    val items: StateFlow<List<ShoppingItem>> =
        dao.getAllItems()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    fun addItem(name: String) = viewModelScope.launch {
        val trimmed = name.trim()
        AppLogger.d(
            AppLogger.TAG_VM,
            "addItem called: name=\"$trimmed\""
        )
        if (trimmed.isBlank()) {
            AppLogger.d(
                AppLogger.TAG_VM,
                "addItem: blank name, aborting"
            )
            return@launch
        }

        AppLogger.d(
            AppLogger.TAG_ASYNC,
            "addItem: coroutine started for name=\"$trimmed\""
        )
        try {
            val item = ShoppingItem(text = trimmed)
            AppLogger.d(
                AppLogger.TAG_DB,
                "addItem: inserting item=$item"
            )
            dao.insert(item)
        } catch (e: Exception) {
            AppLogger.e(
                AppLogger.TAG_DB,
                "addItem: FAILED for name=\"$trimmed\" message=${e.message}",
                e
            )
        } finally {
            AppLogger.d(
                AppLogger.TAG_ASYNC,
                "addItem: coroutine finished for name=\"$trimmed\""
            )
        }
    }

    fun toggleChecked(item: ShoppingItem, checked: Boolean) = viewModelScope.launch {
        AppLogger.d(
            AppLogger.TAG_ASYNC,
            "toggleChecked: coroutine started for id=${item.id}, checked=$checked"
        )
        try {
            val updated = item.copy(isChecked = checked)
            AppLogger.d(
                AppLogger.TAG_DB,
                "toggleChecked: updating item to $updated"
            )
            dao.update(updated)
        } catch (e: Exception) {
            AppLogger.e(
                AppLogger.TAG_DB,
                "toggleChecked: FAILED for id=${item.id} message=${e.message}",
                e
            )
        } finally {
            AppLogger.d(
                AppLogger.TAG_ASYNC,
                "toggleChecked: coroutine finished for id=${item.id}"
            )
        }
    }

    fun deleteChecked() = viewModelScope.launch {
        AppLogger.d(
            AppLogger.TAG_ASYNC,
            "deleteChecked: coroutine started"
        )
        try {
            AppLogger.d(
                AppLogger.TAG_DB,
                "deleteChecked: deleting checked items in DB"
            )
            dao.deleteChecked()
        } catch (e: Exception) {
            AppLogger.e(
                AppLogger.TAG_DB,
                "deleteChecked: FAILED message=${e.message}",
                e
            )
        } finally {
            AppLogger.d(
                AppLogger.TAG_ASYNC,
                "deleteChecked: coroutine finished"
            )
        }
    }
}

class ShoppingCartViewModelFactory(
    private val dao: ShoppingItemDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppingCartViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShoppingCartViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
