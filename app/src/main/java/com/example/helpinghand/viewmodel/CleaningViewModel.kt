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
import java.util.UUID

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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun todayEpochDay(): Int = LocalDate.now().toEpochDay().toInt()

    @RequiresApi(Build.VERSION_CODES.O)
    fun addReminder(
        name: String,
        intervalDays: Int,
        assignedToUid: String? = null,
        assignedToName: String? = null
    ) {
        val trimmedName = name.trim()

        AppLogger.d(
            AppLogger.TAG_VM,
            "addReminder called: name=\"$trimmedName\", intervalDays=$intervalDays, assignedToUid=$assignedToUid, assignedToName=$assignedToName"
        )

        if (trimmedName.isBlank() || intervalDays <= 0) {
            AppLogger.d(AppLogger.TAG_VM, "addReminder: validation failed, aborting")
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
                    nextDueEpochDay = nextDue,
                    assignedToUid = assignedToUid,
                    assignedToName = assignedToName
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
                syncRepo.resetCycle(item = item, newNextDueEpochDay = nextDue)
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

    fun reassignReminder(
        item: CleaningReminder,
        assignedToUid: String?,
        assignedToName: String?
    ) {
        AppLogger.d(
            AppLogger.TAG_VM,
            "reassignReminder called: id=${item.id}, assignedToUid=$assignedToUid, assignedToName=$assignedToName"
        )

        viewModelScope.launch {
            AppLogger.d(
                AppLogger.TAG_ASYNC,
                "reassignReminder: coroutine started for id=${item.id} (via CleaningSyncRepository)"
            )
            try {
                syncRepo.updateAssignee(
                    reminderId = item.id,
                    assignedToUid = assignedToUid,
                    assignedToName = assignedToName
                )
                AppLogger.d(
                    AppLogger.TAG_DB,
                    "reassignReminder: delegated to CleaningSyncRepository for id=${item.id}"
                )
            } catch (e: Exception) {
                AppLogger.e(
                    AppLogger.TAG_DB,
                    "reassignReminder: FAILED in syncRepo for id=${item.id} message=${e.message}",
                    e
                )
            } finally {
                AppLogger.d(
                    AppLogger.TAG_ASYNC,
                    "reassignReminder: coroutine finished for id=${item.id}"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            AppLogger.d(AppLogger.TAG_VM, "CleaningReminderViewModel.onCleared: clearing sync listener")
            syncRepo.clear()
        } catch (e: Exception) {
            AppLogger.e(
                AppLogger.TAG_VM,
                "CleaningReminderViewModel.onCleared: FAILED to clear sync listener message=${e.message}",
                e
            )
        }
    }

    fun onHouseholdIdChanged(newHouseholdId: String?) {
        AppLogger.d(AppLogger.TAG_VM, "CleaningReminderViewModel: onHouseholdIdChanged -> $newHouseholdId")
        syncRepo.setHouseholdId(newHouseholdId)
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
            .collection("cleaning_reminders")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val reminders = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    CleaningReminder(
                        id = doc.id,
                        name = name,
                        intervalDays = (doc.getLong("intervalDays") ?: 0).toInt(),
                        nextDueEpochDay = (doc.getLong("nextDueEpochDay") ?: 0).toInt(),
                        assignedToUid = doc.getString("assignedToUid"),
                        assignedToName = doc.getString("assignedToName")
                    )
                }

                // 🔑 FIX: UPSERT ONLY
                scope.launch {
                    try {
                        dao.insertAll(reminders)
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "listener upsert FAILED", e)
                    }
                }
            }
    }

    suspend fun addReminder(
        name: String,
        intervalDays: Int,
        nextDueEpochDay: Int,
        assignedToUid: String?,
        assignedToName: String?
    ) {
        val id = UUID.randomUUID().toString()
        val reminder = CleaningReminder(
            id = id,
            name = name,
            intervalDays = intervalDays,
            nextDueEpochDay = nextDueEpochDay,
            assignedToUid = assignedToUid,
            assignedToName = assignedToName
        )

        // ✅ LOCAL FIRST
        dao.insertAll(listOf(reminder))

        val hid = ensureHousehold() ?: return
        try {
            householdsCol.document(hid)
                .collection("cleaning_reminders")
                .document(id)
                .set(
                    mapOf(
                        "name" to name,
                        "intervalDays" to intervalDays,
                        "nextDueEpochDay" to nextDueEpochDay,
                        "assignedToUid" to assignedToUid,
                        "assignedToName" to assignedToName
                    )
                )
                .await()
        } catch (e: Exception) {
            AppLogger.e(TAG, "addReminder Firestore FAILED", e)
        }
    }

    suspend fun resetCycle(item: CleaningReminder, newNextDueEpochDay: Int) {
        // LOCAL FIRST
        dao.update(item.copy(nextDueEpochDay = newNextDueEpochDay))

        val hid = ensureHousehold() ?: return
        try {
            householdsCol.document(hid)
                .collection("cleaning_reminders")
                .document(item.id)
                .update("nextDueEpochDay", newNextDueEpochDay)
                .await()
        } catch (e: Exception) {
            AppLogger.e(TAG, "resetCycle Firestore FAILED", e)
        }
    }

    suspend fun updateAssignee(
        reminderId: String,
        assignedToUid: String?,
        assignedToName: String?
    ) {
        val hid = ensureHousehold() ?: return
        try {
            householdsCol.document(hid)
                .collection("cleaning_reminders")
                .document(reminderId)
                .update(
                    mapOf(
                        "assignedToUid" to assignedToUid,
                        "assignedToName" to assignedToName
                    )
                )
                .await()
        } catch (e: Exception) {
            AppLogger.e(TAG, "updateAssignee Firestore FAILED", e)
        }
    }

    suspend fun deleteReminder(item: CleaningReminder) {
        // LOCAL FIRST
        dao.delete(item)

        val hid = ensureHousehold() ?: return
        try {
            householdsCol.document(hid)
                .collection("cleaning_reminders")
                .document(item.id)
                .delete()
                .await()
        } catch (e: Exception) {
            AppLogger.e(TAG, "deleteReminder Firestore FAILED", e)
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

