package com.example.helpinghand.data.dao

import androidx.room.*
import com.example.helpinghand.data.model.CleaningReminder
import com.example.helpinghand.data.model.ShoppingItem
import kotlinx.coroutines.flow.Flow

@Dao
interface CleaningReminderDao {

    @Query("SELECT * FROM cleaning_reminders ORDER BY id ASC")
    fun getAll(): Flow<List<CleaningReminder>>

    @Update
    suspend fun update(item: CleaningReminder)

    @Delete
    suspend fun delete(item: CleaningReminder)

    // Next closest cleaning due (for dashboard)
    @Query("SELECT * FROM cleaning_reminders ORDER BY nextDueEpochDay ASC LIMIT 1")
    fun getNextDue(): Flow<CleaningReminder?>

    // For notifications: everything due today or earlier
    @Query("SELECT * FROM cleaning_reminders WHERE nextDueEpochDay <= :todayEpochDay")
    suspend fun getDueReminders(todayEpochDay: Int): List<CleaningReminder>

    // Firebase functions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CleaningReminder>)

    @Query("DELETE FROM cleaning_reminders")
    suspend fun deleteAll()
}
