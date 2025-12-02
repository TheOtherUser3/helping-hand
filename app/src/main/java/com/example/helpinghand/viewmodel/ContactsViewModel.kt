package com.example.helpinghand.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helpinghand.data.dao.ContactDao
import com.example.helpinghand.data.model.Contact
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val dao: ContactDao
) : ViewModel() {

    val contacts: StateFlow<List<Contact>> =
        dao.getAll()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    fun addContact(name: String, phone: String, email: String) {
        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()
        val trimmedEmail = email.trim()

        // Name required, but at least ONE of phone / email is required
        if (trimmedName.isBlank() || (trimmedPhone.isBlank() && trimmedEmail.isBlank())) {
            return
        }

        viewModelScope.launch {
            dao.insert(
                Contact(
                    name = trimmedName,
                    phone = trimmedPhone,
                    email = trimmedEmail
                )
            )
        }
    }


    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            dao.delete(contact)
        }
    }
}

class ContactsViewModelFactory(
    private val dao: ContactDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContactsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ContactsViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
