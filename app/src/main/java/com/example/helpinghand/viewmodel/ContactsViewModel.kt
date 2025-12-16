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

/* =========================
           ViewModel
   ========================= */

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
        if (!sanitized.isValid) return

        viewModelScope.launch {
            syncRepo.addContact(
                name = sanitized.name,
                phone = sanitized.phone,
                email = sanitized.email
            )
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            syncRepo.deleteContact(contact)
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncRepo.clear()
    }

    private data class SanitizedContact(
        val name: String,
        val phone: String,
        val email: String
    ) {
        val isValid = name.isNotBlank() && (phone.isNotBlank() || email.isNotBlank())
    }

    private fun sanitizeInputs(name: String, phone: String, email: String): SanitizedContact {
        return SanitizedContact(
            name.trim().take(60),
            normalizeUsPhone(phone),
            email.trim().take(120)
        )
    }

    private fun normalizeUsPhone(input: String): String {
        val digits = input.filter { it.isDigit() }
        if (digits.isBlank()) return ""
        val normalized = if (digits.length == 11 && digits.startsWith("1")) digits.drop(1) else digits
        return normalized.take(10)
    }

    fun onHouseholdIdChanged(newHouseholdId: String?) {
        syncRepo.setHouseholdId(newHouseholdId)
    }
}

class ContactsViewModelFactory(
    private val dao: ContactDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContactsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ContactsViewModel(dao, ContactsSyncRepository(dao)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/* =========================
   Sync Repository
   ========================= */

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

    /* ---------- Household resolution ---------- */

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

    /* ---------- SNAPSHOT LISTENER ---------- */

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

                val fromCache = snapshot.metadata.isFromCache

                scope.launch {
                    try {
                        if (fromCache) {
                            dao.insertAll(contacts)
                        } else {
                            dao.deleteAll()
                            if (contacts.isNotEmpty()) dao.insertAll(contacts)
                        }
                    } catch (e: Exception) {
                        AppLogger.e(
                            TAG,
                            "startListening FAILED (fromCache=$fromCache)",
                            e
                        )
                    }
                }
            }
    }

    /* ---------- MUTATIONS ---------- */

    suspend fun addContact(name: String, phone: String, email: String) {
        val id = UUID.randomUUID().toString()
        val contact = Contact(id, name, phone, email)

        // Local first
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
        // Local first
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

    /* ---------- Lifecycle ---------- */

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
