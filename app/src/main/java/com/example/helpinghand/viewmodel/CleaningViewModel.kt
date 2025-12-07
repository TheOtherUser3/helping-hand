package com.example.helpinghand.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helpinghand.AppLogger
import com.example.helpinghand.data.dao.CleaningReminderDao
import com.example.helpinghand.data.household.HouseholdRepository
import com.example.helpinghand.data.model.CleaningReminder
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
import java.time.LocalDate

class CleaningReminderViewModel(
    private val dao: CleaningReminderDao,
    private val syncRepo: CleaningSyncRepository
) : ViewModel() {

    val reminders: StateFlow<List<CleaningReminder>> =
        dao.getAll()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    // For dashboard: next closest due reminder
    val nextDueReminder: StateFlow<CleaningReminder?> =
        dao.getNextDue()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                null
            )

    @RequiresApi(Build.VERSION_CODES.O)
    private fun todayEpochDay(): Int =
        LocalDate.now().toEpochDay().toInt()

    @RequiresApi(Build.VERSION_CODES.O)
    fun addReminder(name: String, intervalDays: Int) {
        val trimmedName = name.trim()

        AppLogger.d(
            AppLogger.TAG_VM,
            "addReminder called: name=\"$trimmedName\", intervalDays=$intervalDays"
        )

        if (trimmedName.isBlank() || intervalDays <= 0) {
            AppLogger.d(
                AppLogger.TAG_VM,
                "addReminder: validation failed, aborting"
            )
            return
        }

        val today = todayEpochDay()
        val nextDue = today + intervalDays

        viewModelScope.launch {
            AppLogger.d(
                AppLogger.TAG_ASYNC,
                "addReminder: coroutine started for name=\"$trimmedName\" (via CleaningSyncRepository)"
            )
            try {
                syncRepo.addReminder(
                    name = trimmedName,
                    intervalDays = intervalDays,
                    nextDueEpochDay = nextDue
                )
                AppLogger.d(
                    AppLogger.TAG_DB,
                    "addReminder: delegated to CleaningSyncRepository for \"$trimmedName\""
                )
            } catch (e: Exception) {
                AppLogger.e(
                    AppLogger.TAG_DB,
                    "addReminder: FAILED in syncRepo for name=\"$trimmedName\" message=${e.message}",
                    e
                )
            } finally {
                AppLogger.d(
                    AppLogger.TAG_ASYNC,
                    "addReminder: coroutine finished for name=\"$trimmedName\""
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun resetCycle(item: CleaningReminder) {
        val today = todayEpochDay()
        val nextDue = today + item.intervalDays

        viewModelScope.launch {
            AppLogger.d(
                AppLogger.TAG_ASYNC,
                "resetCycle: coroutine started for id=${item.id} (via CleaningSyncRepository)"
            )
            try {
                syncRepo.resetCycle(
                    item = item,
                    newNextDueEpochDay = nextDue
                )
                AppLogger.d(
                    AppLogger.TAG_DB,
                    "resetCycle: delegated to CleaningSyncRepository for id=${item.id}, nextDue=$nextDue"
                )
            } catch (e: Exception) {
                AppLogger.e(
                    AppLogger.TAG_DB,
                    "resetCycle: FAILED in syncRepo for id=${item.id} message=${e.message}",
                    e
                )
            } finally {
                AppLogger.d(
                    AppLogger.TAG_ASYNC,
                    "resetCycle: coroutine finished for id=${item.id}"
                )
            }
        }
    }

    fun deleteReminder(item: CleaningReminder) {
        viewModelScope.launch {
            AppLogger.d(
                AppLogger.TAG_ASYNC,
                "deleteReminder: coroutine started for id=${item.id} (via CleaningSyncRepository)"
            )
            try {
                syncRepo.deleteReminder(item)
                AppLogger.d(
                    AppLogger.TAG_DB,
                    "deleteReminder: delegated to CleaningSyncRepository for id=${item.id}"
                )
            } catch (e: Exception) {
                AppLogger.e(
                    AppLogger.TAG_DB,
                    "deleteReminder: FAILED in syncRepo for id=${item.id} message=${e.message}",
                    e
                )
            } finally {
                AppLogger.d(
                    AppLogger.TAG_ASYNC,
                    "deleteReminder: coroutine finished for id=${item.id}"
                )
            }
        }
    }
}

class CleaningReminderViewModelFactory(
    private val dao: CleaningReminderDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CleaningReminderViewModel::class.java)) {
            val syncRepo = CleaningSyncRepository(dao)
            @Suppress("UNCHECKED_CAST")
            return CleaningReminderViewModel(dao, syncRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

// Firebase sync for Cleaning Reminders

class CleaningSyncRepository(
    private val dao: CleaningReminderDao,
    private val householdRepo: HouseholdRepository = HouseholdRepository(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "CleaningSyncRepository"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val householdsCol = db.collection("households")

    private var listener: ListenerRegistration? = null

    init {
        AppLogger.d(TAG, "init: starting to resolve householdId for cleaning sync")
        scope.launch {
            try {
                val hid = householdRepo.getOrCreateHouseholdId()
                if (hid == null) {
                    AppLogger.e(TAG, "init: householdId is null, cannot start cleaning sync", null)
                    return@launch
                }
                AppLogger.d(TAG, "init: obtained householdId=$hid, starting listener")
                startListening(hid)
            } catch (e: Exception) {
                AppLogger.e(TAG, "init: FAILED to get householdId message=${e.message}", e)
            }
        }
    }

    private fun startListening(householdId: String) {
        AppLogger.d(TAG, "startListening: attaching snapshot listener for householdId=$householdId (cleaning)")
        listener?.remove()
        listener = householdsCol.document(householdId)
            .collection("cleaning_reminders")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    AppLogger.e(
                        TAG,
                        "startListening: snapshot error for cleaning householdId=$householdId message=${error.message}",
                        error
                    )
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    AppLogger.e(
                        TAG,
                        "startListening: snapshot is null for cleaning householdId=$householdId",
                        null
                    )
                    return@addSnapshotListener
                }

                AppLogger.d(
                    TAG,
                    "startListening: received cleaning snapshot with ${snapshot.size()} docs for householdId=$householdId"
                )

                val reminders = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: run {
                        AppLogger.e(
                            TAG,
                            "startListening: skipping cleaning doc ${doc.id} missing 'name' field",
                            null
                        )
                        return@mapNotNull null
                    }
                    val intervalDays = (doc.getLong("intervalDays") ?: 0L).toInt()
                    val nextDueEpochDay = (doc.getLong("nextDueEpochDay") ?: 0L).toInt()
                    CleaningReminder(
                        id = doc.id,
                        name = name,
                        intervalDays = intervalDays,
                        nextDueEpochDay = nextDueEpochDay
                    )
                }

                scope.launch {
                    try {
                        AppLogger.d(TAG, "startListening: replacing local cleaning reminders with ${reminders.size} items")
                        dao.deleteAll()
                        if (reminders.isNotEmpty()) {
                            dao.insertAll(reminders)
                        }
                    } catch (e: Exception) {
                        AppLogger.e(
                            TAG,
                            "startListening: FAILED to sync cleaning snapshot to Room message=${e.message}",
                            e
                        )
                    }
                }
            }
    }

    suspend fun addReminder(name: String, intervalDays: Int, nextDueEpochDay: Int) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank() || intervalDays <= 0) {
            AppLogger.d(TAG, "addReminder: validation failed inside syncRepo, aborting")
            return
        }

        AppLogger.d(
            TAG,
            "addReminder: preparing to add cleaning reminder \"$trimmedName\" intervalDays=$intervalDays nextDue=$nextDueEpochDay"
        )
        try {
            val hid = householdRepo.getOrCreateHouseholdId()
            if (hid == null) {
                AppLogger.e(TAG, "addReminder: householdId is null, aborting", null)
                return
            }

            val col = householdsCol.document(hid).collection("cleaning_reminders")
            val docRef = col.document()
            val data = mapOf(
                "name" to trimmedName,
                "intervalDays" to intervalDays,
                "nextDueEpochDay" to nextDueEpochDay
            )

            AppLogger.d(TAG, "addReminder: setting Firestore cleaning doc id=${docRef.id}")
            docRef.set(data).await()
            AppLogger.d(TAG, "addReminder: Firestore write success for cleaning id=${docRef.id}")
            // Room will update via snapshot listener
        } catch (e: Exception) {
            AppLogger.e(
                TAG,
                "addReminder: FAILED Firestore write for \"$trimmedName\" message=${e.message}",
                e
            )
            throw e
        }
    }

    suspend fun resetCycle(item: CleaningReminder, newNextDueEpochDay: Int) {
        AppLogger.d(
            TAG,
            "resetCycle: preparing to update cleaning id=${item.id} newNextDue=$newNextDueEpochDay"
        )
        try {
            val hid = householdRepo.getOrCreateHouseholdId()
            if (hid == null) {
                AppLogger.e(TAG, "resetCycle: householdId is null, aborting", null)
                return
            }

            val docRef = householdsCol
                .document(hid)
                .collection("cleaning_reminders")
                .document(item.id)

            docRef.update("nextDueEpochDay", newNextDueEpochDay).await()
            AppLogger.d(TAG, "resetCycle: Firestore update success for cleaning id=${item.id}")
        } catch (e: Exception) {
            AppLogger.e(
                TAG,
                "resetCycle: FAILED Firestore update for cleaning id=${item.id} message=${e.message}",
                e
            )
            throw e
        }
    }

    suspend fun deleteReminder(item: CleaningReminder) {
        AppLogger.d(
            TAG,
            "deleteReminder: preparing to delete cleaning id=${item.id} from Firestore"
        )
        try {
            val hid = householdRepo.getOrCreateHouseholdId()
            if (hid == null) {
                AppLogger.e(TAG, "deleteReminder: householdId is null, aborting", null)
                return
            }

            val docRef = householdsCol
                .document(hid)
                .collection("cleaning_reminders")
                .document(item.id)

            docRef.delete().await()
            AppLogger.d(TAG, "deleteReminder: Firestore delete success for cleaning id=${item.id}")
        } catch (e: Exception) {
            AppLogger.e(
                TAG,
                "deleteReminder: FAILED Firestore delete for cleaning id=${item.id} message=${e.message}",
                e
            )
            throw e
        }
    }

    fun clear() {
        AppLogger.d(TAG, "clear: removing cleaning snapshot listener")
        listener?.remove()
        listener = null
    }
}
