package com.example.helpinghand.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val isChecked: Boolean = false
)
