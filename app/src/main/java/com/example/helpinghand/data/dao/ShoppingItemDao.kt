package com.example.helpinghand.data.dao

import androidx.room.*
import com.example.helpinghand.data.model.CleaningReminder
import com.example.helpinghand.data.model.ShoppingItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingItemDao {
    @Query("SELECT * FROM shopping_items ORDER BY id DESC")
    fun getAllItems(): Flow<List<ShoppingItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ShoppingItem)

    @Update
    suspend fun update(item: ShoppingItem)

    @Delete
    suspend fun delete(item: ShoppingItem)

    @Query("DELETE FROM shopping_items WHERE isChecked = 1")
    suspend fun deleteChecked()

    // Synchronous version of getAllItems (for API call)
    @Query("SELECT * FROM shopping_items")
    fun getAllItemsNow(): List<ShoppingItem>
  
    @Query("SELECT COUNT(*) FROM shopping_items")
    fun getCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItems(items: List<ShoppingItem>)

    // Firebase functions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ShoppingItem>)

    @Query("DELETE FROM shopping_items")
    suspend fun deleteAll()
}
