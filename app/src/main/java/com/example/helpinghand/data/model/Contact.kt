package com.example.helpinghand.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey val id: String = "",
    val name: String,
    val phone: String,
    val email: String = ""
) {
    fun displayPhone(): String = formatPhoneNumber(phone)
}
