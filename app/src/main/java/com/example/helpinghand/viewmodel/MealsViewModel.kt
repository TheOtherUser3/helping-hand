package com.example.helpinghand.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helpinghand.BuildConfig
import com.example.helpinghand.AppLogger
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

    fun fetchMealsFromCheckedItems() {
        viewModelScope.launch(Dispatchers.IO) {
            AppLogger.d(AppLogger.TAG_ASYNC, "fetchMealsFromCheckedItems: coroutine started")

            try {
                _isLoading.value = true

                // DB LOGGING: read from Room
                AppLogger.d(AppLogger.TAG_DB, "fetchMealsFromCheckedItems: loading items from DB")
                val checkedItems = dao.getAllItemsNow()
                    .filter { it.isChecked }
                    .map { it.text.trim().lowercase() }

                AppLogger.d(
                    AppLogger.TAG_DB,
                    "fetchMealsFromCheckedItems: ${checkedItems.size} checked items = $checkedItems"
                )

                if (checkedItems.isEmpty()) {
                    AppLogger.d(
                        AppLogger.TAG_VM,
                        "fetchMealsFromCheckedItems: no checked items, clearing meals"
                    )
                    _meals.value = emptyList()
                    return@launch
                }

                val ingredients = checkedItems.joinToString(",")

                // API LOGGING: before call
                AppLogger.d(
                    AppLogger.TAG_API,
                    "fetchMealsFromCheckedItems: calling Spoonacular with ingredients=\"$ingredients\""
                )

                val result = api.getMealsByIngredients(
                    ingredients = ingredients,
                    apiKey = apiKey
                )

                AppLogger.d(
                    AppLogger.TAG_API,
                    "fetchMealsFromCheckedItems: API success, ${result.size} meals returned"
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

                AppLogger.d(
                    AppLogger.TAG_VM,
                    "fetchMealsFromCheckedItems: updating _meals with ${mapped.size} mapped meals"
                )
                _meals.value = mapped

            } catch (e: Exception) {
                // CRASH LOGGING: detailed error
                AppLogger.e(
                    AppLogger.TAG_API,
                    "fetchMealsFromCheckedItems: FAILED, message=${e.message}",
                    e
                )
                _meals.value = emptyList()
            } finally {
                _isLoading.value = false
                AppLogger.d(AppLogger.TAG_ASYNC, "fetchMealsFromCheckedItems: coroutine finished")
            }
        }
    }

    fun addMissingIngredients(meal: Meal) {
        viewModelScope.launch(Dispatchers.IO) {
            AppLogger.d(
                AppLogger.TAG_ASYNC,
                "addMissingIngredients: started for mealId=${meal.id}"
            )

            try {
                AppLogger.d(AppLogger.TAG_DB, "addMissingIngredients: reading existing items from DB")
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

                AppLogger.d(
                    AppLogger.TAG_DB,
                    "addMissingIngredients: ${newItems.size} new items to insert: $newItems"
                )

                if (newItems.isNotEmpty()) {
                    dao.insertItems(newItems)
                    AppLogger.d(AppLogger.TAG_DB, "addMissingIngredients: inserted items into DB")
                }

                _meals.update { currentList ->
                    val updated = currentList.map {
                        if (it.id == meal.id) {
                            it.copy(
                                usedIngredients = it.usedIngredients + it.missedIngredients,
                                missedIngredients = emptyList()
                            )
                        } else {
                            it
                        }
                    }

                    AppLogger.d(
                        AppLogger.TAG_VM,
                        "addMissingIngredients: updated _meals for mealId=${meal.id}"
                    )
                    updated
                }
            } catch (e: Exception) {
                AppLogger.e(
                    AppLogger.TAG_DB,
                    "addMissingIngredients: FAILED for mealId=${meal.id}, message=${e.message}",
                    e
                )
            } finally {
                AppLogger.d(
                    AppLogger.TAG_ASYNC,
                    "addMissingIngredients: coroutine finished for mealId=${meal.id}"
                )
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
