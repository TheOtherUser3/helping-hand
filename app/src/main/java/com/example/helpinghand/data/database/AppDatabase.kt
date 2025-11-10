package com.example.helpinghand.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.helpinghand.data.dao.ShoppingItemDao
import com.example.helpinghand.data.entity.ShoppingItem

@Database(entities = [ShoppingItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shoppingItemDao(): ShoppingItemDao
}
