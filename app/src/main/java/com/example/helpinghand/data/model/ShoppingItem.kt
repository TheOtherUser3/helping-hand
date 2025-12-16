package com.example.helpinghand.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isChecked: Boolean = false
)

