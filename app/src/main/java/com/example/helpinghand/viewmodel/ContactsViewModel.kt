package com.example.helpinghand.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helpinghand.AppLogger
import com.example.helpinghand.data.dao.ContactDao
import com.example.helpinghand.data.household.HouseholdRepository
import com.example.helpinghand.data.model.Contact
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ContactsViewModel(
    private val dao: ContactDao,
    private val syncRepo: ContactsSyncRepository
) : ViewModel() {

    val contacts: StateFlow<List<Contact>> =
        dao.getAll()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    fun addContact(name: String, phone: String, email: String) {
        val sanitized = sanitizeInputs(name, phone, email)

        AppLogger.d(
            AppLogger.TAG_VM,
            "addContact called: name=\"${sanitized.name}\", phone=\"${sanitized.phone}\", email=\"${sanitized.email}\""
        )

        if (!sanitized.isValid) {
            AppLogger.d(AppLogger.TAG_VM, "addContact: validation failed, aborting")
            return
        }

        viewModelScope.launch {
            AppLogger.d(
                AppLogger.TAG_ASYNC,
                "addContact: coroutine started for name=\"${sanitized.name}\" (via ContactsSyncRepository)"
            )
            try {
                syncRepo.addContact(
                    name = sanitized.name,
                    phone = sanitized.phone,
                    email = sanitized.email
                )
                AppLogger.d(
                    AppLogger.TAG_DB,
                    "addContact: delegated to ContactsSyncRepository for \"${sanitized.name}\""
                )
            } catch (e: Exception) {
                AppLogger.e(
                    AppLogger.TAG_DB,
                    "addContact: FAILED in syncRepo for name=\"${sanitized.name}\" message=${e.message}",
                    e
                )
            } finally {
                AppLogger.d(
                    AppLogger.TAG_ASYNC,
                    "addContact: coroutine finished for name=\"${sanitized.name}\""
                )
            }
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            AppLogger.d(
                AppLogger.TAG_ASYNC,
                "deleteContact: coroutine started for id=${contact.id} (via ContactsSyncRepository)"
            )
            try {
                syncRepo.deleteContact(contact)
                AppLogger.d(
                    AppLogger.TAG_DB,
                    "deleteContact: delegated to ContactsSyncRepository for id=${contact.id}"
                )
            } catch (e: Exception) {
                AppLogger.e(
                    AppLogger.TAG_DB,
                    "deleteContact: FAILED in syncRepo for id=${contact.id} message=${e.message}",
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

    override fun onCleared() {
        super.onCleared()
        try {
            AppLogger.d(AppLogger.TAG_VM, "ContactsViewModel.onCleared: clearing sync listener")
            syncRepo.clear()
        } catch (e: Exception) {
            AppLogger.e(
                AppLogger.TAG_VM,
                "ContactsViewModel.onCleared: FAILED to clear sync listener message=${e.message}",
                e
            )
        }
    }

    private data class SanitizedContact(
        val name: String,
        val phone: String,
        val email: String
    ) {
        val isValid: Boolean =
            name.isNotBlank() && (phone.isNotBlank() || email.isNotBlank())
    }

    private fun sanitizeInputs(name: String, phone: String, email: String): SanitizedContact {
        val safeName = name.trim().take(MAX_NAME_CHARS)
        val safeEmail = email.trim().take(MAX_EMAIL_CHARS)
        val safePhone = normalizeUsPhone(phone)
        return SanitizedContact(
            name = safeName,
            phone = safePhone,
            email = safeEmail
        )
    }

    /**
     * Accepts:
     * - 6666666666
     * - 666-666-6666
     * - spaces too
     * Stores: digits-only, coerced to 10 digits (US-style).
     */
    private fun normalizeUsPhone(input: String): String {
        val digits = input.filter { it.isDigit() }
        if (digits.isBlank()) return ""

        val normalized = if (digits.length == 11 && digits.startsWith("1")) digits.drop(1) else digits
        return normalized.take(MAX_PHONE_DIGITS)
    }

    fun onHouseholdIdChanged(newHouseholdId: String?) {
        AppLogger.d(AppLogger.TAG_VM, "ContactsViewModel: onHouseholdIdChanged -> $newHouseholdId")
        syncRepo.setHouseholdId(newHouseholdId)
    }


    companion object {
        private const val MAX_NAME_CHARS = 60
        private const val MAX_EMAIL_CHARS = 120
        private const val MAX_PHONE_DIGITS = 10
    }
}

class ContactsViewModelFactory(
    private val dao: ContactDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContactsViewModel::class.java)) {
            val syncRepo = ContactsSyncRepository(dao)
            @Suppress("UNCHECKED_CAST")
            return ContactsViewModel(dao, syncRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

// Firebase sync for Contacts

class ContactsSyncRepository(
    private val dao: ContactDao,
    private val householdRepo: HouseholdRepository = HouseholdRepository(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "ContactsSyncRepository"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val householdsCol = db.collection("households")

    private var listener: ListenerRegistration? = null
    @Volatile private var cachedHouseholdId: String? = null

    private suspend fun ensureHousehold(): String? {
        cachedHouseholdId?.let { return it }
        return try {
            householdRepo.getOrCreateHouseholdId()?.also {
                cachedHouseholdId = it
                startListening(it)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "ensureHousehold FAILED", e)
            null
        }
    }

    private fun startListening(householdId: String) {
        listener?.remove()
        listener = householdsCol.document(householdId)
            .collection("contacts")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val contacts = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    Contact(
                        id = doc.id,
                        name = name,
                        phone = doc.getString("phone") ?: "",
                        email = doc.getString("email") ?: ""
                    )
                }

                scope.launch {
                    try {
                        dao.insertAll(contacts)
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "listener upsert FAILED", e)
                    }
                }
            }
    }

    suspend fun addContact(name: String, phone: String, email: String) {
        val id = UUID.randomUUID().toString()
        val contact = Contact(id, name, phone, email)

        // LOCAL FIRST
        dao.insertAll(listOf(contact))

        // Best-effort Firestore
        val hid = ensureHousehold() ?: return
        try {
            householdsCol.document(hid)
                .collection("contacts")
                .document(id)
                .set(
                    mapOf(
                        "name" to name,
                        "phone" to phone,
                        "email" to email
                    )
                )
                .await()
        } catch (e: Exception) {
            AppLogger.e(TAG, "addContact Firestore FAILED", e)
        }
    }

    suspend fun deleteContact(contact: Contact) {
        // LOCAL FIRST
        dao.delete(contact)

        val hid = ensureHousehold() ?: return
        try {
            householdsCol.document(hid)
                .collection("contacts")
                .document(contact.id)
                .delete()
                .await()
        } catch (e: Exception) {
            AppLogger.e(TAG, "deleteContact Firestore FAILED", e)
        }
    }

    fun clear() {
        listener?.remove()
        listener = null
        cachedHouseholdId = null
    }

    fun setHouseholdId(newHouseholdId: String?) {
        if (newHouseholdId == cachedHouseholdId) return
        listener?.remove()
        listener = null
        cachedHouseholdId = newHouseholdId
        if (newHouseholdId != null) startListening(newHouseholdId)
    }
}

