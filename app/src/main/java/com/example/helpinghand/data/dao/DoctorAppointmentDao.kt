package com.example.helpinghand.data.dao

import androidx.room.*
import com.example.helpinghand.data.model.DoctorAppointment
import kotlinx.coroutines.flow.Flow

@Dao
interface DoctorAppointmentDao {

    @Query("SELECT * FROM doctor_appointments ORDER BY doctorName")
    fun getAll(): Flow<List<DoctorAppointment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appointment: DoctorAppointment)

    @Update
    suspend fun update(appointment: DoctorAppointment)

    @Delete
    suspend fun delete(appointment: DoctorAppointment)
}
