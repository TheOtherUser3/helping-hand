package com.example.helpinghand.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helpinghand.AppLogger
import com.example.helpinghand.data.dao.DoctorAppointmentDao
import com.example.helpinghand.data.model.DoctorAppointment
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class DoctorAppointmentsViewModel(
    private val dao: DoctorAppointmentDao
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

        viewModelScope.launch {
            try {
                val today = LocalDate.now()
                val nextEpoch = today.plusMonths(interval.toLong()).toEpochDay().toInt()

                val appointment = DoctorAppointment(
                    doctorName = name,
                    type = type,
                    lastVisitEpochDay = null,
                    nextVisitEpochDay = nextEpoch,
                    phoneRaw = phoneNormalized,
                    officeName = office,
                    intervalMonths = interval
                )

                AppLogger.d(AppLogger.TAG_DB, "addAppointment: inserting $appointment")
                dao.insert(appointment)
            } catch (e: Exception) {
                AppLogger.e(
                    AppLogger.TAG_DB,
                    "addAppointment FAILED: ${e.message}",
                    e
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateNextVisit(appointment: DoctorAppointment, newDate: LocalDate) {
        viewModelScope.launch {
            try {
                val updated = appointment.copy(
                    nextVisitEpochDay = newDate.toEpochDay().toInt()
                )
                AppLogger.d(
                    AppLogger.TAG_DB,
                    "updateNextVisit: updating $updated"
                )
                dao.update(updated)
            } catch (e: Exception) {
                AppLogger.e(
                    AppLogger.TAG_DB,
                    "updateNextVisit FAILED: ${e.message}",
                    e
                )
            }
        }
    }

    fun deleteAppointment(appointment: DoctorAppointment) {
        viewModelScope.launch {
            try {
                AppLogger.d(AppLogger.TAG_DB, "deleteAppointment: $appointment")
                dao.delete(appointment)
            } catch (e: Exception) {
                AppLogger.e(
                    AppLogger.TAG_DB,
                    "deleteAppointment FAILED: ${e.message}",
                    e
                )
            }
        }
    }

    private fun normalizePhone(input: String): String {
        // Keep only digits, max length
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
            @Suppress("UNCHECKED_CAST")
            return DoctorAppointmentsViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
