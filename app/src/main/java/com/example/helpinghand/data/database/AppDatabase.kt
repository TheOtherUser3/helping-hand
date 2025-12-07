package com.example.helpinghand.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.helpinghand.data.dao.CleaningReminderDao
import com.example.helpinghand.data.dao.ContactDao
import com.example.helpinghand.data.dao.DoctorAppointmentDao
import com.example.helpinghand.data.dao.ShoppingItemDao
import com.example.helpinghand.data.model.CleaningReminder
import com.example.helpinghand.data.model.Contact
import com.example.helpinghand.data.model.ShoppingItem
import com.example.helpinghand.data.model.DoctorAppointment



@Database(entities = [
    ShoppingItem::class,
    CleaningReminder::class,
    Contact::class,
    DoctorAppointment::class
                     ],
    version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun cleaningReminderDao(): CleaningReminderDao

    abstract fun contactDao(): ContactDao
    abstract fun doctorAppointmentDao(): DoctorAppointmentDao
}
