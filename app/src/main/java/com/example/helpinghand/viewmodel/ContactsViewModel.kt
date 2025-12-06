package com.example.helpinghand.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helpinghand.AppLogger
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

        AppLogger.d(
            AppLogger.TAG_VM,
            "addContact called: name=\"$trimmedName\", phone=\"$trimmedPhone\", email=\"$trimmedEmail\""
        )

        // Name required, but at least ONE of phone / email is required
        if (trimmedName.isBlank() || (trimmedPhone.isBlank() && trimmedEmail.isBlank())) {
            AppLogger.d(
                AppLogger.TAG_VM,
                "addContact: validation failed, aborting"
            )
            return
        }

        viewModelScope.launch {
            AppLogger.d(
                AppLogger.TAG_ASYNC,
                "addContact: coroutine started for name=\"$trimmedName\""
            )
            try {
                val contact = Contact(
                    name = trimmedName,
                    phone = trimmedPhone,
                    email = trimmedEmail
                )
                AppLogger.d(
                    AppLogger.TAG_DB,
                    "addContact: inserting contact=$contact"
                )
                dao.insert(contact)
            } catch (e: Exception) {
                AppLogger.e(
                    AppLogger.TAG_DB,
                    "addContact: FAILED for name=\"$trimmedName\" message=${e.message}",
                    e
                )
            } finally {
                AppLogger.d(
                    AppLogger.TAG_ASYNC,
                    "addContact: coroutine finished for name=\"$trimmedName\""
                )
            }
        }
    }


    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            AppLogger.d(
                AppLogger.TAG_ASYNC,
                "deleteContact: coroutine started for id=${contact.id}"
            )
            try {
                AppLogger.d(
                    AppLogger.TAG_DB,
                    "deleteContact: deleting contact=$contact"
                )
                dao.delete(contact)
            } catch (e: Exception) {
                AppLogger.e(
                    AppLogger.TAG_DB,
                    "deleteContact: FAILED for id=${contact.id} message=${e.message}",
                    e
                )
            } finally {
                AppLogger.d(
                    AppLogger.TAG_ASYNC,
                    "deleteContact: coroutine finished for id=${contact.id}"
                )
            }
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
