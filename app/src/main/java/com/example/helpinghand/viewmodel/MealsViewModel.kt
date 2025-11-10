package com.example.helpinghand.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.helpinghand.data.database.AppDatabase
import com.example.helpinghand.data.model.Meal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import com.example.helpinghand.BuildConfig

class MealsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "helping_hand_db"
    ).build()
    private val dao = db.shoppingItemDao()

    private val _meals = MutableStateFlow<List<Meal>>(emptyList())
    val meals: StateFlow<List<Meal>> = _meals

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Spoonacular API key
    private val apiKey = BuildConfig.SPOONACULAR_API_KEY

    fun fetchMealsFromCheckedItems() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true

                // Fetch checked shopping list items
                val checkedItems = dao.getAllItemsNow()
                    .filter { it.isChecked }
                    .map { it.text.trim() }

                if (checkedItems.isEmpty()) {
                    _meals.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }

                // Build query
                val ingredients = checkedItems.joinToString(",") { it.lowercase() }
                val url = URL(
                    "https://api.spoonacular.com/recipes/findByIngredients" +
                            "?ingredients=$ingredients&number=5&ranking=2&apiKey=$apiKey"
                )
                print(url)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }

                    // ✅ FIXED: Parse directly as JSON array
                    val jsonArray = JSONArray(response)
                    val result = mutableListOf<Meal>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val used = obj.getJSONArray("usedIngredients")
                        val missed = obj.getJSONArray("missedIngredients")

                        result.add(
                            Meal(
                                id = obj.getInt("id"),
                                title = obj.getString("title"),
                                imageUrl = obj.getString("image"),
                                usedIngredients = List(used.length()) { idx ->
                                    used.getJSONObject(idx).getString("name")
                                },
                                missedIngredients = List(missed.length()) { idx ->
                                    missed.getJSONObject(idx).getString("name")
                                }
                            )
                        )
                    }

                    _meals.value = result
                } else {
                    // If the response isn’t OK, reset to empty
                    _meals.value = emptyList()
                    println("Spoonacular API returned code $responseCode")
                }

                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                _meals.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
