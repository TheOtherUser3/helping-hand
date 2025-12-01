package com.example.helpinghand.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helpinghand.BuildConfig
import com.example.helpinghand.data.dao.ShoppingItemDao
import com.example.helpinghand.data.model.Ingredient
import com.example.helpinghand.data.model.Meal
import com.example.helpinghand.data.model.ShoppingItem
import com.example.helpinghand.data.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MealsViewModel(
    private val dao: ShoppingItemDao
) : ViewModel() {

    private val api = NetworkModule.api
    private val apiKey = BuildConfig.SPOONACULAR_API_KEY

    private val _meals = MutableStateFlow<List<Meal>>(emptyList())
    val meals: StateFlow<List<Meal>> = _meals

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // SAME implementation as your original, just without building Room in here
    fun fetchMealsFromCheckedItems() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true

                val checkedItems = dao.getAllItemsNow()
                    .filter { it.isChecked }
                    .map { it.text.trim().lowercase() }

                if (checkedItems.isEmpty()) {
                    _meals.value = emptyList()
                    return@launch
                }

                val ingredients = checkedItems.joinToString(",")

                val result = api.getMealsByIngredients(
                    ingredients = ingredients,
                    apiKey = apiKey
                )

                val mapped = result.map { meal ->
                    Meal(
                        id = meal.id,
                        title = meal.title,
                        imageUrl = meal.imageUrl,
                        usedIngredients = meal.usedIngredients
                            .map { it.name }
                            .map { Ingredient(it) },
                        missedIngredients = meal.missedIngredients
                            .map { it.name }
                            .map { Ingredient(it) }
                    )
                }

                _meals.value = mapped

            } catch (e: Exception) {
                e.printStackTrace()
                _meals.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addMissingIngredients(meal: Meal) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existingItems = dao.getAllItemsNow()
                    .map { it.text.trim().lowercase() }
                    .toSet()

                val newItems = meal.missedIngredients
                    .map { it.name.trim() }
                    .filter { it.isNotEmpty() }
                    .filter { it.lowercase() !in existingItems }
                    .map { name ->
                        ShoppingItem(
                            text = name,
                            isChecked = false
                        )
                    }

                if (newItems.isNotEmpty()) {
                    dao.insertItems(newItems)
                }

                _meals.update { currentList ->
                    currentList.map {
                        if (it.id == meal.id) {
                            it.copy(
                                usedIngredients = it.usedIngredients + it.missedIngredients,
                                missedIngredients = emptyList()
                            )
                        } else {
                            it
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class MealsViewModelFactory(
    private val dao: ShoppingItemDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MealsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MealsViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
