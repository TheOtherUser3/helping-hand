package com.example.helpinghand.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helpinghand.data.dao.ShoppingItemDao
import com.example.helpinghand.data.model.ShoppingItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingCartViewModel(
    private val dao: ShoppingItemDao
) : ViewModel() {

    // SAME as before: items is a StateFlow from DAO
    val items: StateFlow<List<ShoppingItem>> =
        dao.getAllItems()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // SAME signatures as your original code
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
