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
import java.util.UUID

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

        if (name.isBlank()) {
            AppLogger.d(AppLogger.TAG_VM, "addAppointment: name blank, skipping insert")
            return
        }

        val today = LocalDate.now()
        val nextEpoch = today.plusMonths(interval.toLong()).toEpochDay().toInt()

        viewModelScope.launch {
            try {
                syncRepo.addAppointment(
                    name = name,
                    type = type,
                    phoneRaw = phoneNormalized,
                    officeName = office,
                    intervalMonths = interval,
                    nextVisitEpochDay = nextEpoch
                )
            } catch (e: Exception) {
                AppLogger.e(AppLogger.TAG_DB, "addAppointment FAILED: ${e.message}", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateNextVisit(appointment: DoctorAppointment, newDate: LocalDate) {
        val newEpoch = newDate.toEpochDay().toInt()
        viewModelScope.launch {
            try {
                syncRepo.updateNextVisit(appointment, newEpoch)
            } catch (e: Exception) {
                AppLogger.e(AppLogger.TAG_DB, "updateNextVisit FAILED: ${e.message}", e)
            }
        }
    }

    fun updateAppointment(
        appointment: DoctorAppointment,
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

        if (name.isBlank()) {
            AppLogger.d(AppLogger.TAG_VM, "updateAppointment: name blank, skipping update")
            return
        }

        viewModelScope.launch {
            try {
                syncRepo.updateAppointment(
                    appointment = appointment,
                    newName = name,
                    newType = type,
                    newPhoneRaw = phoneNormalized,
                    newOfficeName = office,
                    newIntervalMonths = interval
                )
            } catch (e: Exception) {
                AppLogger.e(AppLogger.TAG_DB, "updateAppointment FAILED: ${e.message}", e)
            }
        }
    }

    fun deleteAppointment(appointment: DoctorAppointment) {
        viewModelScope.launch {
            try {
                syncRepo.deleteAppointment(appointment)
            } catch (e: Exception) {
                AppLogger.e(AppLogger.TAG_DB, "deleteAppointment FAILED: ${e.message}", e)
            }
        }
    }

    fun onHouseholdIdChanged(newHouseholdId: String?) {
        AppLogger.d(AppLogger.TAG_VM, "DoctorAppointmentsViewModel: onHouseholdIdChanged -> $newHouseholdId")
        syncRepo.setHouseholdId(newHouseholdId)
    }

    override fun onCleared() {
        super.onCleared()
        syncRepo.clear()
    }

    private fun normalizePhone(input: String): String {
        return input.filter { it.isDigit() }.take(MAX_PHONE_DIGITS)
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
        scope.launch {
            try {
                ensureHouseholdAndListener()
            } catch (e: Exception) {
                AppLogger.e(TAG, "init ensureHouseholdAndListener FAILED: ${e.message}", e)
            }
        }
    }

    private suspend fun resolveHouseholdId(): String? {
        cachedHouseholdId?.let { return it }
        return try {
            val hid = householdRepo.getOrCreateHouseholdId()
            if (hid.isNullOrBlank()) null else hid.also { cachedHouseholdId = it }
        } catch (e: Exception) {
            AppLogger.e(TAG, "resolveHouseholdId FAILED: ${e.message}", e)
            null
        }
    }

    private suspend fun ensureHouseholdAndListener(): String? {
        val hid = resolveHouseholdId() ?: return null
        if (listener != null) return hid
        startListening(hid)
        return hid
    }

    private fun startListening(householdId: String) {
        listener?.remove()
        listener = householdsCol.document(householdId)
            .collection("doctor_appointments")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    AppLogger.e(TAG, "snapshot error: ${error?.message}", error)
                    return@addSnapshotListener
                }

                val remote = snapshot.documents.mapNotNull { doc ->
                    val doctorName = doc.getString("doctorName") ?: return@mapNotNull null
                    DoctorAppointment(
                        id = doc.id,
                        doctorName = doctorName,
                        type = doc.getString("type") ?: "",
                        lastVisitEpochDay = doc.getLong("lastVisitEpochDay")?.toInt(),
                        nextVisitEpochDay = doc.getLong("nextVisitEpochDay")?.toInt(),
                        phoneRaw = doc.getString("phoneRaw") ?: "",
                        officeName = doc.getString("officeName") ?: "",
                        intervalMonths = (doc.getLong("intervalMonths") ?: 0L).toInt()
                    )
                }

                // IMPORTANT FIX:
                // Do NOT deleteAll() on every snapshot. That wipes local-only items when Firestore is empty/slow.
                // Instead: upsert remote into Room (REPLACE by id).
                scope.launch {
                    try {
                        dao.insertAll(remote)
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "snapshot -> Room upsert FAILED: ${e.message}", e)
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
        if (trimmedName.isBlank()) return

        val id = UUID.randomUUID().toString()
        val local = DoctorAppointment(
            id = id,
            doctorName = trimmedName,
            type = type,
            lastVisitEpochDay = null,
            nextVisitEpochDay = nextVisitEpochDay,
            phoneRaw = phoneRaw,
            officeName = officeName.trim(),
            intervalMonths = intervalMonths
        )

        // Local-first (UI/test friendly)
        dao.insert(local)

        // Best-effort Firestore
        val hid = ensureHouseholdAndListener() ?: return
        val docRef = householdsCol.document(hid).collection("doctor_appointments").document(id)
        val data = mapOf(
            "doctorName" to local.doctorName,
            "type" to local.type,
            "lastVisitEpochDay" to local.lastVisitEpochDay,
            "nextVisitEpochDay" to local.nextVisitEpochDay,
            "phoneRaw" to local.phoneRaw,
            "officeName" to local.officeName,
            "intervalMonths" to local.intervalMonths
        )
        try {
            docRef.set(data).await()
        } catch (e: Exception) {
            AppLogger.e(TAG, "addAppointment Firestore FAILED: ${e.message}", e)
        }
    }

    suspend fun updateNextVisit(appointment: DoctorAppointment, newNextVisitEpochDay: Int) {
        val updated = appointment.copy(nextVisitEpochDay = newNextVisitEpochDay)

        // Local-first so UI updates immediately
        dao.update(updated)

        // Best-effort Firestore
        val hid = ensureHouseholdAndListener() ?: return
        try {
            householdsCol.document(hid)
                .collection("doctor_appointments")
                .document(appointment.id)
                .update("nextVisitEpochDay", newNextVisitEpochDay)
                .await()
        } catch (e: Exception) {
            AppLogger.e(TAG, "updateNextVisit Firestore FAILED: ${e.message}", e)
        }
    }

    suspend fun updateAppointment(
        appointment: DoctorAppointment,
        newName: String,
        newType: String,
        newPhoneRaw: String,
        newOfficeName: String,
        newIntervalMonths: Int
    ) {
        val updated = appointment.copy(
            doctorName = newName.trim(),
            type = newType,
            phoneRaw = newPhoneRaw,
            officeName = newOfficeName.trim(),
            intervalMonths = newIntervalMonths
        )

        // Local-first
        dao.update(updated)

        // Best-effort Firestore
        val hid = ensureHouseholdAndListener() ?: return
        val updates = mapOf(
            "doctorName" to updated.doctorName,
            "type" to updated.type,
            "phoneRaw" to updated.phoneRaw,
            "officeName" to updated.officeName,
            "intervalMonths" to updated.intervalMonths
        )
        try {
            householdsCol.document(hid)
                .collection("doctor_appointments")
                .document(updated.id)
                .update(updates)
                .await()
        } catch (e: Exception) {
            AppLogger.e(TAG, "updateAppointment Firestore FAILED: ${e.message}", e)
        }
    }

    suspend fun deleteAppointment(appointment: DoctorAppointment) {
        // Local-first
        dao.delete(appointment)

        // Best-effort Firestore
        val hid = ensureHouseholdAndListener() ?: return
        try {
            householdsCol.document(hid)
                .collection("doctor_appointments")
                .document(appointment.id)
                .delete()
                .await()
        } catch (e: Exception) {
            AppLogger.e(TAG, "deleteAppointment Firestore FAILED: ${e.message}", e)
        }
    }

    fun clear() {
        listener?.remove()
        listener = null
    }

    fun setHouseholdId(newHouseholdId: String?) {
        val normalized = newHouseholdId?.trim()?.takeIf { it.isNotBlank() }
        if (normalized == cachedHouseholdId) return

        listener?.remove()
        listener = null
        cachedHouseholdId = normalized

        if (normalized != null) startListening(normalized)
    }
}
