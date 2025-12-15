package com.example.helpinghand.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helpinghand.AppLogger
import com.example.helpinghand.data.dao.DoctorAppointmentDao
import com.example.helpinghand.data.household.HouseholdRepository
import com.example.helpinghand.data.model.DoctorAppointment
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

class DoctorAppointmentsViewModel(
    private val dao: DoctorAppointmentDao,
    private val syncRepo: DoctorAppointmentsSyncRepository
) : ViewModel() {

    val appointments: StateFlow<List<DoctorAppointment>> =
        dao.getAll()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    @RequiresApi(Build.VERSION_CODES.O)
    fun addAppointment(
        nameInput: String,
        type: String,
        phoneInput: String,
        officeInput: String,
        intervalMonthsInput: Int
    ) {
        val name = nameInput.trim().take(MAX_NAME_CHARS)
        val office = officeInput.trim().take(MAX_OFFICE_CHARS)
        val phoneNormalized = normalizePhone(phoneInput)
        val interval = intervalMonthsInput.coerceIn(1, 60)

        AppLogger.d(
            AppLogger.TAG_VM,
            "DoctorAppointmentsViewModel.addAppointment name=\"$name\", type=$type, phone=\"$phoneNormalized\""
        )

        if (name.isBlank()) {
            AppLogger.d(AppLogger.TAG_VM, "addAppointment: name blank, skipping insert")
            return
        }

        val today = LocalDate.now()
        val nextEpoch = today.plusMonths(interval.toLong()).toEpochDay().toInt()

        viewModelScope.launch {
            AppLogger.d(
                AppLogger.TAG_ASYNC,
                "addAppointment: coroutine started for name=\"$name\" (via DoctorAppointmentsSyncRepository)"
            )
            try {
                syncRepo.addAppointment(
                    name = name,
                    type = type,
                    phoneRaw = phoneNormalized,
                    officeName = office,
                    intervalMonths = interval,
                    nextVisitEpochDay = nextEpoch
                )
                AppLogger.d(
                    AppLogger.TAG_DB,
                    "addAppointment: delegated to DoctorAppointmentsSyncRepository for \"$name\""
                )
            } catch (e: Exception) {
                AppLogger.e(
                    AppLogger.TAG_DB,
                    "addAppointment FAILED in syncRepo for \"$name\": ${e.message}",
                    e
                )
            } finally {
                AppLogger.d(
                    AppLogger.TAG_ASYNC,
                    "addAppointment: coroutine finished for name=\"$name\""
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateNextVisit(appointment: DoctorAppointment, newDate: LocalDate) {
        val newEpoch = newDate.toEpochDay().toInt()

        viewModelScope.launch {
            AppLogger.d(
                AppLogger.TAG_ASYNC,
                "updateNextVisit: coroutine started for id=${appointment.id} (via DoctorAppointmentsSyncRepository)"
            )
            try {
                syncRepo.updateNextVisit(
                    appointment = appointment,
                    newNextVisitEpochDay = newEpoch
                )
                AppLogger.d(
                    AppLogger.TAG_DB,
                    "updateNextVisit: delegated to DoctorAppointmentsSyncRepository for id=${appointment.id}, newEpoch=$newEpoch"
                )
            } catch (e: Exception) {
                AppLogger.e(
                    AppLogger.TAG_DB,
                    "updateNextVisit FAILED in syncRepo for id=${appointment.id}: ${e.message}",
                    e
                )
            } finally {
                AppLogger.d(
                    AppLogger.TAG_ASYNC,
                    "updateNextVisit: coroutine finished for id=${appointment.id}"
                )
            }
        }
    }

    fun deleteAppointment(appointment: DoctorAppointment) {
        viewModelScope.launch {
            AppLogger.d(
                AppLogger.TAG_ASYNC,
                "deleteAppointment: coroutine started for id=${appointment.id} (via DoctorAppointmentsSyncRepository)"
            )
            try {
                syncRepo.deleteAppointment(appointment)
                AppLogger.d(
                    AppLogger.TAG_DB,
                    "deleteAppointment: delegated to DoctorAppointmentsSyncRepository for id=${appointment.id}"
                )
            } catch (e: Exception) {
                AppLogger.e(
                    AppLogger.TAG_DB,
                    "deleteAppointment FAILED in syncRepo for id=${appointment.id}: ${e.message}",
                    e
                )
            } finally {
                AppLogger.d(
                    AppLogger.TAG_ASYNC,
                    "deleteAppointment: coroutine finished for id=${appointment.id}"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            AppLogger.d(AppLogger.TAG_VM, "DoctorAppointmentsViewModel.onCleared: clearing sync listener")
            syncRepo.clear()
        } catch (e: Exception) {
            AppLogger.e(
                AppLogger.TAG_VM,
                "DoctorAppointmentsViewModel.onCleared: FAILED to clear sync listener message=${e.message}",
                e
            )
        }
    }

    private fun normalizePhone(input: String): String {
        return input.filter { it.isDigit() }.take(MAX_PHONE_DIGITS)
    }

    fun onHouseholdIdChanged(newHouseholdId: String?) {
        AppLogger.d(AppLogger.TAG_VM, "DoctorAppointmentsViewModel: onHouseholdIdChanged -> $newHouseholdId")
        syncRepo.setHouseholdId(newHouseholdId)
    }


    companion object {
        private const val MAX_NAME_CHARS = 60
        private const val MAX_OFFICE_CHARS = 60
        private const val MAX_PHONE_DIGITS = 15
    }
}

class DoctorAppointmentsViewModelFactory(
    private val dao: DoctorAppointmentDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DoctorAppointmentsViewModel::class.java)) {
            val syncRepo = DoctorAppointmentsSyncRepository(dao)
            @Suppress("UNCHECKED_CAST")
            return DoctorAppointmentsViewModel(dao, syncRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

// Firebase sync for Doctor Appointments

class DoctorAppointmentsSyncRepository(
    private val dao: DoctorAppointmentDao,
    private val householdRepo: HouseholdRepository = HouseholdRepository(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "DoctorAppointmentsSyncRepo"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val householdsCol = db.collection("households")

    private var listener: ListenerRegistration? = null
    @Volatile private var cachedHouseholdId: String? = null

    init {
        AppLogger.d(TAG, "init: attempting to start doctor appointments sync listener")
        scope.launch {
            try {
                val hid = ensureHouseholdAndListener()
                if (hid == null) {
                    AppLogger.e(TAG, "init: householdId is null, doctor listener not started (will retry on next operation)", null)
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

        AppLogger.d(TAG, "ensureHouseholdAndListener: attaching listener for householdId=$hid (doctor appointments)")
        startListening(hid)
        return hid
    }

    private fun startListening(householdId: String) {
        AppLogger.d(TAG, "startListening: attaching snapshot listener for householdId=$householdId (doctor appointments)")
        listener?.remove()
        listener = householdsCol.document(householdId)
            .collection("doctor_appointments")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    AppLogger.e(
                        TAG,
                        "startListening: snapshot error for doctor appointments householdId=$householdId message=${error.message}",
                        error
                    )
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    AppLogger.e(TAG, "startListening: snapshot is null for doctor appointments householdId=$householdId", null)
                    return@addSnapshotListener
                }

                AppLogger.d(TAG, "startListening: received doctor appointments snapshot with ${snapshot.size()} docs for householdId=$householdId")

                val appointments = snapshot.documents.mapNotNull { doc ->
                    val doctorName = doc.getString("doctorName") ?: run {
                        AppLogger.e(TAG, "startListening: skipping doctor doc ${doc.id} missing 'doctorName' field", null)
                        return@mapNotNull null
                    }
                    val type = doc.getString("type") ?: ""
                    val lastVisitEpochDay = doc.getLong("lastVisitEpochDay")?.toInt()
                    val nextVisitEpochDay = doc.getLong("nextVisitEpochDay")?.toInt()
                    val phoneRaw = doc.getString("phoneRaw") ?: ""
                    val officeName = doc.getString("officeName") ?: ""
                    val intervalMonths = (doc.getLong("intervalMonths") ?: 0L).toInt()

                    DoctorAppointment(
                        id = doc.id,
                        doctorName = doctorName,
                        type = type,
                        lastVisitEpochDay = lastVisitEpochDay,
                        nextVisitEpochDay = nextVisitEpochDay,
                        phoneRaw = phoneRaw,
                        officeName = officeName,
                        intervalMonths = intervalMonths
                    )
                }

                scope.launch {
                    try {
                        AppLogger.d(TAG, "startListening: replacing local doctor appointments with ${appointments.size} items")
                        dao.deleteAll()
                        if (appointments.isNotEmpty()) dao.insertAll(appointments)
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "startListening: FAILED to sync doctor appointments snapshot to Room message=${e.message}", e)
                    }
                }
            }
    }

    suspend fun addAppointment(
        name: String,
        type: String,
        phoneRaw: String,
        officeName: String,
        intervalMonths: Int,
        nextVisitEpochDay: Int
    ) {
        val trimmedName = name.trim()
        val trimmedOffice = officeName.trim()

        if (trimmedName.isBlank()) {
            AppLogger.d(TAG, "addAppointment: name blank inside syncRepo, aborting")
            return
        }

        AppLogger.d(
            TAG,
            "addAppointment: preparing to add doctor appointment \"$trimmedName\" type=$type interval=$intervalMonths nextVisit=$nextVisitEpochDay"
        )

        try {
            val hid = ensureHouseholdAndListener()
            if (hid == null) {
                AppLogger.e(TAG, "addAppointment: householdId is null, aborting", null)
                return
            }

            val col = householdsCol.document(hid).collection("doctor_appointments")
            val docRef = col.document()
            val data = mapOf(
                "doctorName" to trimmedName,
                "type" to type,
                "lastVisitEpochDay" to null,
                "nextVisitEpochDay" to nextVisitEpochDay,
                "phoneRaw" to phoneRaw,
                "officeName" to trimmedOffice,
                "intervalMonths" to intervalMonths
            )

            AppLogger.d(TAG, "addAppointment: setting Firestore doctor doc id=${docRef.id}")
            docRef.set(data).await()
            AppLogger.d(TAG, "addAppointment: Firestore write success for doctor id=${docRef.id}")
        } catch (e: Exception) {
            AppLogger.e(TAG, "addAppointment: FAILED Firestore write for \"$trimmedName\" message=${e.message}", e)
            throw e
        }
    }

    suspend fun updateNextVisit(
        appointment: DoctorAppointment,
        newNextVisitEpochDay: Int
    ) {
        AppLogger.d(TAG, "updateNextVisit: preparing to update doctor id=${appointment.id} newNextVisit=$newNextVisitEpochDay")

        try {
            val hid = ensureHouseholdAndListener()
            if (hid == null) {
                AppLogger.e(TAG, "updateNextVisit: householdId is null, aborting", null)
                return
            }

            val docRef = householdsCol.document(hid)
                .collection("doctor_appointments")
                .document(appointment.id)

            docRef.update("nextVisitEpochDay", newNextVisitEpochDay).await()
            AppLogger.d(TAG, "updateNextVisit: Firestore update success for doctor id=${appointment.id}")
        } catch (e: Exception) {
            AppLogger.e(TAG, "updateNextVisit: FAILED Firestore update for doctor id=${appointment.id} message=${e.message}", e)
            throw e
        }
    }

    suspend fun deleteAppointment(appointment: DoctorAppointment) {
        AppLogger.d(TAG, "deleteAppointment: preparing to delete doctor id=${appointment.id} from Firestore")

        try {
            val hid = ensureHouseholdAndListener()
            if (hid == null) {
                AppLogger.e(TAG, "deleteAppointment: householdId is null, aborting", null)
                return
            }

            val docRef = householdsCol.document(hid)
                .collection("doctor_appointments")
                .document(appointment.id)

            docRef.delete().await()
            AppLogger.d(TAG, "deleteAppointment: Firestore delete success for doctor id=${appointment.id}")
        } catch (e: Exception) {
            AppLogger.e(TAG, "deleteAppointment: FAILED Firestore delete for doctor id=${appointment.id} message=${e.message}", e)
            throw e
        }
    }

    fun clear() {
        AppLogger.d(TAG, "clear: removing doctor appointments snapshot listener and resetting household cache")
        listener?.remove()
        listener = null
        cachedHouseholdId = null
    }

    fun setHouseholdId(newHouseholdId: String?) {
        val normalized = newHouseholdId?.trim()?.takeIf { it.isNotBlank() }

        // No change, do nothing
        if (normalized == cachedHouseholdId) return

        AppLogger.d(TAG, "setHouseholdId: switching doctor appointments sync from $cachedHouseholdId -> $normalized")

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
