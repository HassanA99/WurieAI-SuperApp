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

    fun loadProfile(context: android.content.Context? = null) {
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
                val localUser = if (context != null) {
                    val prefs = context.getSharedPreferences("wurie_user_session", android.content.Context.MODE_PRIVATE)
                    val name = prefs.getString("user_name", "") ?: ""
                    val email = prefs.getString("user_email", "") ?: ""
                    val phone = prefs.getString("user_phone", null)
                    val city = prefs.getString("user_city", null)
                    if (name.isNotBlank() || email.isNotBlank()) {
                        UserProfile(
                            uid = "local",
                            fullName = name,
                            email = email,
                            phone = phone,
                            city = city,
                            role = "customer",
                            profileCompleted = true
                        )
                    } else null
                } else null

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profile = localUser ?: _uiState.value.profile,
                    errorMessage = null
                )
            }
        }
    }

    fun saveProfile(fullName: String, phone: String?, city: String?, context: android.content.Context? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            if (context != null) {
                try {
                    val prefs = context.getSharedPreferences("wurie_user_session", android.content.Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString("user_name", fullName)
                        .putString("user_phone", phone)
                        .putString("user_city", city)
                        .apply()
                } catch (_: Exception) {}
            }
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
                val current = _uiState.value.profile
                val updatedLocal = (current ?: UserProfile(uid = "local", fullName = fullName, email = "")).copy(
                    fullName = fullName,
                    phone = phone,
                    city = city
                )
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    profile = updatedLocal,
                    errorMessage = null
                )
            }
        }
    }

    fun saveSettings(
        notificationsEnabled: Boolean? = null,
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
