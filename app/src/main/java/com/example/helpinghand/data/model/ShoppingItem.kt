package com.example.helpinghand.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey val id: String = "",
    val text: String,
    val isChecked: Boolean = false
)

