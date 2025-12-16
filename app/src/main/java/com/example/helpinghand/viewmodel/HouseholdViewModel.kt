package com.example.helpinghand.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helpinghand.AppLogger
import com.example.helpinghand.data.household.HouseholdRepository
import com.example.helpinghand.data.model.HouseholdMember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class HouseholdUiState(
    val householdId: String? = null,
    val members: List<HouseholdMember> = emptyList(),
    val errorMessage: String? = null,
    val isBusy: Boolean = false
)

class HouseholdViewModel(
    private val repo: HouseholdRepository = HouseholdRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HouseholdUiState())
    val uiState: StateFlow<HouseholdUiState> = _uiState

    init {
        viewModelScope.launch {
            try {
                repo.ensureUserDocument()
                val hid = repo.getOrCreateHouseholdId()
                _uiState.value = _uiState.value.copy(householdId = hid)
                if (hid != null) {
                    observeMembers(hid)
                }
            } catch (e: Exception) {
                AppLogger.e(AppLogger.TAG_VM, "HouseholdViewModel init FAILED: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load household"
                )
            }
        }
    }

    private fun observeMembers(householdId: String) {
        viewModelScope.launch {
            repo.observeHouseholdMembers(householdId).collectLatest { members ->
                _uiState.value = _uiState.value.copy(members = members)
            }
        }
    }

    fun addMemberByEmail(email: String) {
        val hid = _uiState.value.householdId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, errorMessage = null)
            try {
                val success = repo.addMemberByEmail(hid, email)
                if (!success) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "No user found with that email",
                        isBusy = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isBusy = false)
                }
            } catch (e: Exception) {
                AppLogger.e(AppLogger.TAG_VM, "addMemberByEmail FAILED: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to add household member",
                    isBusy = false
                )
            }
        }
    }
}

class HouseholdViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HouseholdViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HouseholdViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
