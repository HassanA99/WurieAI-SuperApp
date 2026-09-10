package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.api.AuthTokenProvider
import com.example.api.ProfileSettings
import com.example.api.ProfileSettingsUpdateRequest
import com.example.api.ProfileUpdateRequest
import com.example.api.RetrofitClient
import com.example.api.UserProfile
import com.example.api.WurieApiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val profile: UserProfile? = null,
    val settings: ProfileSettings? = null,
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val repository: WurieApiRepository = WurieApiRepository(RetrofitClient.apiService),
    private val authTokenProvider: AuthTokenProvider = AuthTokenProvider()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val token = authTokenProvider.bearerToken()
                val profile = repository.getProfile(token)
                val settings = repository.getProfileSettings(token)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profile = profile,
                    settings = settings,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Unable to load profile right now."
                )
            }
        }
    }

    fun saveProfile(fullName: String, phone: String?, city: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            try {
                val token = authTokenProvider.bearerToken()
                val request = ProfileUpdateRequest(
                    fullName = fullName.takeIf { it.isNotBlank() },
                    phone = phone?.takeIf { it.isNotBlank() },
                    city = city?.takeIf { it.isNotBlank() }
                )
                val updatedProfile = repository.updateProfile(request, token)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    profile = updatedProfile,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = e.localizedMessage ?: "Unable to save profile right now."
                )
            }
        }
    }

    fun saveSettings(
        notificationsEnabled: Boolean? = null,
        biometricEnabled: Boolean? = null,
        pushEnabled: Boolean? = null,
        offlineCacheEnabled: Boolean? = null,
        language: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            try {
                val token = authTokenProvider.bearerToken()
                val request = ProfileSettingsUpdateRequest(
                    notificationsEnabled = notificationsEnabled,
                    biometricEnabled = biometricEnabled,
                    pushEnabled = pushEnabled,
                    offlineCacheEnabled = offlineCacheEnabled,
                    language = language
                )
                val updatedSettings = repository.updateProfileSettings(request, token)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    settings = updatedSettings,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = e.localizedMessage ?: "Unable to save settings right now."
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ProfileViewModel()
            }
        }
    }
}
