package com.example.helpinghand.data.model

import com.squareup.moshi.Json

data class Ingredient(
    val name: String
)

data class Meal(
    val id: Int,
    val title: String,
    @Json(name = "image") val imageUrl: String,
    @Json(name = "usedIngredients") val usedIngredients: List<Ingredient>,
    @Json(name = "missedIngredients") val missedIngredients: List<Ingredient>,
) {
    val recipeUrl: String
        get() = "https://spoonacular.com/recipes/${title.replace(' ', '-')}-${id}"
}
