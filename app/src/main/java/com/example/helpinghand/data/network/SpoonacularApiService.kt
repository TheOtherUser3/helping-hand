package com.example.helpinghand.data.network

import com.example.helpinghand.data.model.Meal
import retrofit2.http.GET
import retrofit2.http.Query

interface SpoonacularApiService {

    @GET("recipes/findByIngredients")
    suspend fun getMealsByIngredients(
        @Query("ingredients") ingredients: String,
        @Query("number") number: Int = 5,
        @Query("ranking") ranking: Int = 2,
        @Query("apiKey") apiKey: String
    ): List<Meal>
}
