package com.example.helpinghand.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.helpinghand.BuildConfig
import com.example.helpinghand.data.database.AppDatabase
import com.example.helpinghand.data.model.Ingredient
import com.example.helpinghand.data.network.NetworkModule
import com.example.helpinghand.data.model.Meal
import com.example.helpinghand.data.model.ShoppingItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update

class MealsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "helping_hand_db"
    ).build()

    private val dao = db.shoppingItemDao()
    private val api = NetworkModule.api
    private val apiKey = BuildConfig.SPOONACULAR_API_KEY

    private val _meals = MutableStateFlow<List<Meal>>(emptyList())
    val meals: StateFlow<List<Meal>> = _meals

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

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

                // Retrofit call
                val result = api.getMealsByIngredients(
                    ingredients = ingredients,
                    apiKey = apiKey
                )

                // Convert ingredient objects to just names for display
                val mapped = result.map { meal ->
                    Meal(
                        id = meal.id,
                        title = meal.title,
                        imageUrl = meal.imageUrl,
                        usedIngredients = meal.usedIngredients.map { it.name }.map { Ingredient(it) },
                        missedIngredients = meal.missedIngredients.map { it.name }.map { Ingredient(it) }
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
                // 1. Get current items in the shopping list
                val existingItems = dao.getAllItemsNow()
                    .map { it.text.trim().lowercase() }
                    .toSet()

                // 2. Build list of new ShoppingItem entries that are not already present
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

                // 3. Update the local meal state so UI moves missing -> used
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
