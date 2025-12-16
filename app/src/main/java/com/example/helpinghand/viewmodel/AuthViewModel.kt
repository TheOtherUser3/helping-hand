package com.example.helpinghand.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helpinghand.AppLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

        if (trimmedEmail.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState(
                isLoading = false,
                errorMessage = "Email and password are required."
            )
            return
        }

        _uiState.value = AuthUiState(isLoading = true, errorMessage = null)
        AppLogger.d(AppLogger.TAG_VM, "register: starting Firebase registration for $trimmedEmail")

        auth.createUserWithEmailAndPassword(trimmedEmail, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user == null) {
                        _uiState.value = AuthUiState(isLoading = false, errorMessage = "Registration succeeded, but user is null.")
                        return@addOnCompleteListener
                    }

                    val updates = UserProfileChangeRequest.Builder()
                        .setDisplayName(trimmedName)
                        .build()

                    user.updateProfile(updates)
                        .addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                AppLogger.d(AppLogger.TAG_VM, "register: displayName set to \"$trimmedName\"")
                            } else {
                                AppLogger.e(
                                    AppLogger.TAG_VM,
                                    "register: updateProfile FAILED message=${profileTask.exception?.message}",
                                    profileTask.exception
                                )
                            }

                            _currentUser.value = auth.currentUser
                            _uiState.value = AuthUiState(isLoading = false, errorMessage = null)
                        }

                } else {
                    val ex = task.exception
                    AppLogger.e(
                        AppLogger.TAG_VM,
                        "register: FAILED for $trimmedEmail exception=${ex?.javaClass?.simpleName} message=${ex?.message}",
                        ex
                    )

                    val msg = when (ex) {
                        is FirebaseAuthException -> {
                            when (ex.errorCode) {
                                "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Check your internet connection and try again."
                                "ERROR_EMAIL_ALREADY_IN_USE" -> "That email is already in use."
                                "ERROR_INVALID_EMAIL" -> "That email address is invalid."
                                "ERROR_WEAK_PASSWORD" -> "Password is too weak."
                                else -> "Registration failed: ${ex.localizedMessage ?: "Unknown error."}"
                            }
                        }
                        else -> "Registration failed: ${ex?.localizedMessage ?: "Unknown error."}"
                    }

                    _uiState.value = AuthUiState(isLoading = false, errorMessage = msg)
                }
            }
    }


    fun logout() {
        auth.signOut()
        _currentUser.value = null
        _uiState.value = AuthUiState()
        AppLogger.d(AppLogger.TAG_VM, "logout: user signed out")
    }

    fun updateDisplayName(newNameRaw: String): Boolean {
        val newName = newNameRaw.trim()
        if (newName.isBlank()) {
            _uiState.value = AuthUiState(isLoading = false, errorMessage = "Display name can’t be blank.")
            return false
        }

        viewModelScope.launch {
            try {
                _uiState.value = AuthUiState(isLoading = true, errorMessage = null)

                val user = auth.currentUser
                if (user == null) {
                    _uiState.value = AuthUiState(isLoading = false, errorMessage = "Not logged in.")
                    return@launch
                }

                val req = UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build()

                user.updateProfile(req).await()

                // Force recomposition / refresh flows
                _currentUser.value = auth.currentUser
                _uiState.value = AuthUiState(isLoading = false, errorMessage = null)

                AppLogger.d(AppLogger.TAG_VM, "updateDisplayName: success -> \"$newName\"")
            } catch (e: Exception) {
                AppLogger.e(AppLogger.TAG_VM, "updateDisplayName FAILED: ${e.message}", e)
                _uiState.value = AuthUiState(isLoading = false, errorMessage = e.localizedMessage ?: "Update failed")
            }
        }

        return true
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
