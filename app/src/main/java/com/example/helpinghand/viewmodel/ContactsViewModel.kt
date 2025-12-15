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
        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()
        val trimmedEmail = email.trim()

        AppLogger.d(
            AppLogger.TAG_VM,
            "addContact called: name=\"$trimmedName\", phone=\"$trimmedPhone\", email=\"$trimmedEmail\""
        )

        if (trimmedName.isBlank() || (trimmedPhone.isBlank() && trimmedEmail.isBlank())) {
            AppLogger.d(AppLogger.TAG_VM, "addContact: validation failed, aborting")
            return
        }

        viewModelScope.launch {
            AppLogger.d(
                AppLogger.TAG_ASYNC,
                "addContact: coroutine started for name=\"$trimmedName\" (via ContactsSyncRepository)"
            )
            try {
                syncRepo.addContact(
                    name = trimmedName,
                    phone = trimmedPhone,
                    email = trimmedEmail
                )
                AppLogger.d(
                    AppLogger.TAG_DB,
                    "addContact: delegated to ContactsSyncRepository for \"$trimmedName\""
                )
            } catch (e: Exception) {
                AppLogger.e(
                    AppLogger.TAG_DB,
                    "addContact: FAILED in syncRepo for name=\"$trimmedName\" message=${e.message}",
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

    init {
        AppLogger.d(TAG, "init: attempting to start contacts sync listener")
        scope.launch {
            try {
                val hid = ensureHouseholdAndListener()
                if (hid == null) {
                    AppLogger.e(TAG, "init: householdId is null, contacts listener not started (will retry on next operation)", null)
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
                    val name = doc.getString("name") ?: run {
                        AppLogger.e(TAG, "startListening: skipping contact doc ${doc.id} missing 'name' field", null)
                        return@mapNotNull null
                    }
                    val phone = doc.getString("phone") ?: ""
                    val email = doc.getString("email") ?: ""
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
        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()
        val trimmedEmail = email.trim()

        if (trimmedName.isBlank() || (trimmedPhone.isBlank() && trimmedEmail.isBlank())) {
            AppLogger.d(TAG, "addContact: validation failed inside syncRepo, aborting")
            return
        }

        AppLogger.d(TAG, "addContact: preparing to add contact \"$trimmedName\" to Firestore")

        try {
            val hid = ensureHouseholdAndListener()
            if (hid == null) {
                AppLogger.e(TAG, "addContact: householdId is null, aborting", null)
                return
            }

            val col = householdsCol.document(hid).collection("contacts")
            val docRef = col.document()
            val data = mapOf(
                "name" to trimmedName,
                "phone" to trimmedPhone,
                "email" to trimmedEmail
            )

            AppLogger.d(TAG, "addContact: setting Firestore contact doc id=${docRef.id}")
            docRef.set(data).await()
            AppLogger.d(TAG, "addContact: Firestore write success for contact id=${docRef.id}")
        } catch (e: Exception) {
            AppLogger.e(TAG, "addContact: FAILED Firestore write for \"$trimmedName\" message=${e.message}", e)
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
}
