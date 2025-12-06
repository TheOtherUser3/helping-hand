package com.example.helpinghand.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helpinghand.AppLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun login(email: String, password: String) {
        val trimmedEmail = email.trim()
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            auth.signInWithEmailAndPassword(trimmedEmail, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        AppLogger.d(AppLogger.TAG_VM, "login success for $trimmedEmail")
                        _currentUser.value = auth.currentUser
                        _uiState.value = AuthUiState()
                    } else {
                        val msg = task.exception?.localizedMessage ?: "Login failed"
                        AppLogger.e(AppLogger.TAG_VM, "login FAILED: $msg", task.exception)
                        _uiState.value = AuthUiState(isLoading = false, errorMessage = msg)
                    }
                }
        }
    }

    fun register(name: String, email: String, password: String) {
        val trimmedEmail = email.trim()
        val trimmedName = name.trim()
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            auth.createUserWithEmailAndPassword(trimmedEmail, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null && trimmedName.isNotBlank()) {
                            val profileUpdates =
                                com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                    .setDisplayName(trimmedName)
                                    .build()
                            user.updateProfile(profileUpdates)
                        }
                        AppLogger.d(AppLogger.TAG_VM, "register success for $trimmedEmail")
                        _currentUser.value = auth.currentUser
                        _uiState.value = AuthUiState()
                    } else {
                        val msg = task.exception?.localizedMessage ?: "Registration failed"
                        AppLogger.e(AppLogger.TAG_VM, "register FAILED: $msg", task.exception)
                        _uiState.value = AuthUiState(isLoading = false, errorMessage = msg)
                    }
                }
        }
    }

    fun logout() {
        auth.signOut()
        _currentUser.value = null
        _uiState.value = AuthUiState()
        AppLogger.d(AppLogger.TAG_VM, "logout: user signed out")
    }
}

class AuthViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
