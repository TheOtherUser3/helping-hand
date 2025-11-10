package com.example.helpinghand.data.model

data class Meal(
    val id: Int,
    val title: String,
    val imageUrl: String,
    val usedIngredients: List<String>,
    val missedIngredients: List<String>
)
