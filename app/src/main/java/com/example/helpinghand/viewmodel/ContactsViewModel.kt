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
            val syncRepo = ContactsSyncRepository(dao)
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

    fun setHouseholdId(newHouseholdId: String?) {
        if (newHouseholdId == cachedHouseholdId) return
        listener?.remove()
        listener = null
        cachedHouseholdId = newHouseholdId
        if (newHouseholdId != null) startListening(newHouseholdId)
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

        private const val MAX_NAME_CHARS = 60
        private const val MAX_EMAIL_CHARS = 120
        private const val MAX_PHONE_DIGITS = 10
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val householdsCol = db.collection("households")

    private var listener: ListenerRegistration? = null
    @Volatile private var cachedHouseholdId: String? = null

    init {
        AppLogger.d(TAG, "init: attempting to start contacts sync listener")
        scope.launch {
            try {
                val hid = ensureHouseholdAndListener()
                if (hid == null) {
                    AppLogger.e(
                        TAG,
                        "init: householdId is null, contacts listener not started (will retry on next operation)",
                        null
                    )
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "init: FAILED ensureHouseholdAndListener message=${e.message}", e)
            }
        }
    }

    private suspend fun resolveHouseholdId(): String? {
        val existing = cachedHouseholdId
        if (existing != null) return existing

        AppLogger.d(TAG, "resolveHouseholdId: cache miss, calling HouseholdRepository.getOrCreateHouseholdId()")
        return try {
            val hid = householdRepo.getOrCreateHouseholdId()
            if (hid == null) {
                AppLogger.e(TAG, "resolveHouseholdId: returned null householdId", null)
                null
            } else {
                cachedHouseholdId = hid
                AppLogger.d(TAG, "resolveHouseholdId: resolved householdId=$hid")
                hid
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "resolveHouseholdId: FAILED message=${e.message}", e)
            null
        }
    }

    private suspend fun ensureHouseholdAndListener(): String? {
        val hid = resolveHouseholdId() ?: return null
        if (listener != null) return hid

        AppLogger.d(TAG, "ensureHouseholdAndListener: attaching listener for householdId=$hid (contacts)")
        startListening(hid)
        return hid
    }

    private fun startListening(householdId: String) {
        AppLogger.d(TAG, "startListening: attaching snapshot listener for householdId=$householdId (contacts)")
        listener?.remove()
        listener = householdsCol.document(householdId)
            .collection("contacts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    AppLogger.e(
                        TAG,
                        "startListening: snapshot error for contacts householdId=$householdId message=${error.message}",
                        error
                    )
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    AppLogger.e(TAG, "startListening: snapshot is null for contacts householdId=$householdId", null)
                    return@addSnapshotListener
                }

                AppLogger.d(TAG, "startListening: received contacts snapshot with ${snapshot.size()} docs for householdId=$householdId")

                val contacts = snapshot.documents.mapNotNull { doc ->
                    val rawName = doc.getString("name") ?: run {
                        AppLogger.e(TAG, "startListening: skipping contact doc ${doc.id} missing 'name' field", null)
                        return@mapNotNull null
                    }

                    val name = rawName.trim().take(MAX_NAME_CHARS)
                    if (name.isBlank()) {
                        AppLogger.e(TAG, "startListening: skipping contact doc ${doc.id} blank 'name' after trim", null)
                        return@mapNotNull null
                    }

                    val phone = normalizeUsPhone(doc.getString("phone") ?: "")
                    val email = (doc.getString("email") ?: "").trim().take(MAX_EMAIL_CHARS)

                    Contact(
                        id = doc.id,
                        name = name,
                        phone = phone,
                        email = email
                    )
                }

                scope.launch {
                    try {
                        AppLogger.d(TAG, "startListening: replacing local contacts with ${contacts.size} items")
                        dao.deleteAll()
                        if (contacts.isNotEmpty()) dao.insertAll(contacts)
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "startListening: FAILED to sync contacts snapshot to Room message=${e.message}", e)
                    }
                }
            }
    }

    suspend fun addContact(name: String, phone: String, email: String) {
        val safeName = name.trim().take(MAX_NAME_CHARS)
        val safeEmail = email.trim().take(MAX_EMAIL_CHARS)
        val safePhone = normalizeUsPhone(phone)

        if (safeName.isBlank() || (safePhone.isBlank() && safeEmail.isBlank())) {
            AppLogger.d(TAG, "addContact: validation failed inside syncRepo, aborting")
            return
        }

        AppLogger.d(TAG, "addContact: preparing to add contact \"$safeName\" to Firestore")

        try {
            val hid = ensureHouseholdAndListener()
            if (hid == null) {
                AppLogger.e(TAG, "addContact: householdId is null, aborting", null)
                return
            }

            val col = householdsCol.document(hid).collection("contacts")
            val docRef = col.document()
            val data = mapOf(
                "name" to safeName,
                "phone" to safePhone,  // digits-only stored
                "email" to safeEmail
            )

            AppLogger.d(TAG, "addContact: setting Firestore contact doc id=${docRef.id}")
            docRef.set(data).await()
            AppLogger.d(TAG, "addContact: Firestore write success for contact id=${docRef.id}")
        } catch (e: Exception) {
            AppLogger.e(TAG, "addContact: FAILED Firestore write for \"$safeName\" message=${e.message}", e)
            throw e
        }
    }

    suspend fun deleteContact(contact: Contact) {
        AppLogger.d(TAG, "deleteContact: preparing to delete contact id=${contact.id} from Firestore")

        try {
            val hid = ensureHouseholdAndListener()
            if (hid == null) {
                AppLogger.e(TAG, "deleteContact: householdId is null, aborting", null)
                return
            }

            val docRef = householdsCol.document(hid)
                .collection("contacts")
                .document(contact.id)

            docRef.delete().await()
            AppLogger.d(TAG, "deleteContact: Firestore delete success for id=${contact.id}")
        } catch (e: Exception) {
            AppLogger.e(TAG, "deleteContact: FAILED Firestore delete for id=${contact.id} message=${e.message}", e)
            throw e
        }
    }

    fun clear() {
        AppLogger.d(TAG, "clear: removing contacts snapshot listener and resetting household cache")
        listener?.remove()
        listener = null
        cachedHouseholdId = null
    }

    private fun normalizeUsPhone(input: String): String {
        val digits = input.filter { it.isDigit() }
        if (digits.isBlank()) return ""

        val normalized = if (digits.length == 11 && digits.startsWith("1")) digits.drop(1) else digits
        return normalized.take(MAX_PHONE_DIGITS)
    }

    fun setHouseholdId(newHouseholdId: String?) {
        val normalized = newHouseholdId?.trim()?.takeIf { it.isNotBlank() }

        // No change, do nothing
        if (normalized == cachedHouseholdId) return

        AppLogger.d(TAG, "setHouseholdId: switching contacts sync from $cachedHouseholdId -> $normalized")

        // Kill the old listener
        listener?.remove()
        listener = null

        // Update cache
        cachedHouseholdId = normalized

        // Attach new listener if we have an id
        if (normalized != null) {
            startListening(normalized)
        }
    }

}
