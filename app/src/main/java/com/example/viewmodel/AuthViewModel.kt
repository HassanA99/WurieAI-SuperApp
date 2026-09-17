package com.example.viewmodel

import android.content.Context
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthRepositoryResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    object NeedProfileSetup : AuthState()
    data class Error(val message: String) : AuthState()
    data class CredentialNotice(val title: String, val message: String) : AuthState()
}

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(
        if (authRepository.currentUser != null) AuthState.Success else AuthState.Idle
    )
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun clearError() {
        _authState.value = AuthState.Idle
    }

    fun signUpWithEmail(fullName: String, email: String, password: String, confirmPassword: String, context: Context) {
        val trimmedName = fullName.trim()
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        val trimmedConfirm = confirmPassword.trim()

        if (trimmedName.isEmpty()) {
            _authState.value = AuthState.Error("Please enter your full name.")
            return
        }
        if (trimmedEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _authState.value = AuthState.Error("Please enter a valid email address.")
            return
        }
        if (trimmedPassword.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters.")
            return
        }
        if (trimmedPassword != trimmedConfirm) {
            _authState.value = AuthState.Error("Passwords do not match. Please verify.")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            when (val result = authRepository.signUpWithEmail(trimmedName, trimmedEmail, trimmedPassword)) {
                is AuthRepositoryResult.Success -> {
                    saveLocalUser(context, trimmedName, trimmedEmail)
                    _authState.value = AuthState.NeedProfileSetup
                }
                is AuthRepositoryResult.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }
                is AuthRepositoryResult.Notice -> {
                    _authState.value = AuthState.CredentialNotice(result.title, result.message)
                }
                is AuthRepositoryResult.Cancelled -> {
                    _authState.value = AuthState.Idle
                }
            }
        }
    }

    fun signInWithEmail(email: String, password: String, context: Context) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()

        if (trimmedEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _authState.value = AuthState.Error("Please enter a valid email address.")
            return
        }
        if (trimmedPassword.isEmpty()) {
            _authState.value = AuthState.Error("Please enter your password.")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            when (val result = authRepository.signInWithEmail(trimmedEmail, trimmedPassword)) {
                is AuthRepositoryResult.Success -> {
                    val user = result.data
                    saveLocalUser(context, user.displayName ?: "Wurie Explorer", trimmedEmail)
                    checkIfProfileExists()
                }
                is AuthRepositoryResult.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }
                is AuthRepositoryResult.Notice -> {
                    _authState.value = AuthState.CredentialNotice(result.title, result.message)
                }
                is AuthRepositoryResult.Cancelled -> {
                    _authState.value = AuthState.Idle
                }
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            when (val result = authRepository.signInWithGoogle(context)) {
                is AuthRepositoryResult.Success -> {
                    val user = result.data
                    saveLocalUser(context, user.displayName ?: "Wurie Explorer", user.email ?: "")
                    checkIfProfileExists()
                }
                is AuthRepositoryResult.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }
                is AuthRepositoryResult.Notice -> {
                    _authState.value = AuthState.CredentialNotice(result.title, result.message)
                }
                is AuthRepositoryResult.Cancelled -> {
                    _authState.value = AuthState.Idle
                }
            }
        }
    }

    fun sendPasswordReset(email: String, onComplete: (Boolean, String) -> Unit) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            onComplete(false, "Please enter a valid email address.")
            return
        }

        viewModelScope.launch {
            when (val result = authRepository.sendPasswordResetEmail(trimmedEmail)) {
                is AuthRepositoryResult.Success -> {
                    onComplete(true, "Password reset instructions sent to $trimmedEmail")
                }
                is AuthRepositoryResult.Error -> {
                    onComplete(false, result.message)
                }
                else -> {
                    onComplete(false, "Password reset request failed. Please try again.")
                }
            }
        }
    }

    fun completeProfile(context: Context? = null, firstName: String, lastName: String, phone: String, city: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            if (context != null) {
                try {
                    val fullName = "$firstName $lastName".trim()
                    val prefs = context.getSharedPreferences("wurie_user_session", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString("user_name", fullName)
                        .putString("user_phone", phone)
                        .putString("user_city", city)
                        .apply()
                } catch (e: Exception) {
                    Log.w("AuthViewModel", "Could not save local user profile details", e)
                }
            }
            when (val result = authRepository.completeProfile(firstName, lastName, phone, city)) {
                is AuthRepositoryResult.Success -> {
                    _authState.value = AuthState.Success
                }
                is AuthRepositoryResult.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }
                else -> {
                    _authState.value = AuthState.Success
                }
            }
        }
    }

    fun skipProfileSetup() {
        _authState.value = AuthState.Success
    }

    private suspend fun checkIfProfileExists() {
        val isComplete = authRepository.checkProfileStatus()
        if (isComplete) {
            _authState.value = AuthState.Success
        } else {
            _authState.value = AuthState.NeedProfileSetup
        }
    }

    private fun saveLocalUser(context: Context, name: String, email: String) {
        try {
            val prefs = context.getSharedPreferences("wurie_user_session", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("user_name", name)
                .putString("user_email", email)
                .putBoolean("is_logged_in", true)
                .apply()
        } catch (e: Exception) {
            Log.w("AuthViewModel", "Could not save local user session", e)
        }
    }

    fun signOut(context: Context? = null) {
        authRepository.signOut()
        if (context != null) {
            try {
                val prefs = context.getSharedPreferences("wurie_user_session", Context.MODE_PRIVATE)
                prefs.edit().clear().apply()
            } catch (e: Exception) {
                Log.w("AuthViewModel", "Could not clear local session", e)
            }
        }
        _authState.value = AuthState.Idle
    }
}
