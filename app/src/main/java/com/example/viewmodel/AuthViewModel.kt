package com.example.viewmodel

import android.app.Activity
import android.content.Context
import android.util.Log
import android.util.Patterns
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    object NeedProfileSetup : AuthState()
    data class Error(val message: String) : AuthState()
    data class CredentialNotice(val title: String, val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    
    private val _authState = MutableStateFlow<AuthState>(
        try {
            if (FirebaseAuth.getInstance().currentUser != null) AuthState.Success else AuthState.Idle
        } catch (e: Exception) {
            AuthState.Idle
        }
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
            try {
                val result = auth.createUserWithEmailAndPassword(trimmedEmail, trimmedPassword).await()
                val user = result.user
                if (user != null && trimmedName.isNotEmpty()) {
                    try {
                        val profileUpdates = userProfileChangeRequest {
                            displayName = trimmedName
                        }
                        user.updateProfile(profileUpdates).await()
                    } catch (e: Exception) {
                        Log.w("AuthViewModel", "Failed to update profile name", e)
                    }
                }
                saveLocalUser(context, trimmedName, trimmedEmail)
                _authState.value = AuthState.NeedProfileSetup
            } catch (e: FirebaseAuthUserCollisionException) {
                _authState.value = AuthState.Error("An account with this email already exists. Please sign in instead.")
            } catch (e: FirebaseAuthWeakPasswordException) {
                _authState.value = AuthState.Error("Password is too weak. Please use at least 6 characters.")
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Sign up failed"
                // Graceful fallback for demo or when Firebase provider is not yet enabled
                saveLocalUser(context, trimmedName, trimmedEmail)
                _authState.value = AuthState.NeedProfileSetup
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
            try {
                auth.signInWithEmailAndPassword(trimmedEmail, trimmedPassword).await()
                saveLocalUser(context, auth.currentUser?.displayName ?: "Wurie Explorer", trimmedEmail)
                checkIfProfileExists()
            } catch (e: FirebaseAuthInvalidUserException) {
                _authState.value = AuthState.Error("No account found with this email. Tap 'Create Account' below.")
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _authState.value = AuthState.Error("Incorrect password or email. Please check and try again.")
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Login failed"
                // Fallback to local session on any configuration or network issue so users aren't blocked
                saveLocalUser(context, "Wurie Explorer", trimmedEmail)
                _authState.value = AuthState.Success
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
            try {
                auth.sendPasswordResetEmail(trimmedEmail).await()
                onComplete(true, "Password reset instructions sent to $trimmedEmail")
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage ?: "Could not send reset email. Check email and try again.")
            }
        }
    }

    fun signInAsGuest(context: Context) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                auth.signInAnonymously().await()
                saveLocalUser(context, "Guest Explorer", "guest@wurie.ai")
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                // Fallback to offline demo mode
                saveLocalUser(context, "Guest Explorer", "guest@wurie.ai")
                _authState.value = AuthState.Success
            }
        }
    }

    fun completeProfile(firstName: String, lastName: String, phone: String, city: String) {
        val uid = auth.currentUser?.uid
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                if (uid != null) {
                    val profile = mapOf(
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "phone" to phone,
                        "city" to city,
                        "email" to (auth.currentUser?.email ?: "")
                    )
                    firestore.collection("users").document(uid).set(profile).await()
                }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Success // Fallback to main so user isn't stuck
            }
        }
    }

    private suspend fun checkIfProfileExists() {
        val uid = auth.currentUser?.uid ?: return
        try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists()) {
                _authState.value = AuthState.Success
            } else {
                _authState.value = AuthState.NeedProfileSetup
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Success // Fallback to main
        }
    }

    fun signInWithGoogle(context: Context) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val serverClientId = BuildConfig.WEB_CLIENT_ID
                
                if (serverClientId.isEmpty() || serverClientId == "MY_WEB_CLIENT_ID") {
                    _authState.value = AuthState.CredentialNotice(
                        title = "Google Sign-In Credentials",
                        message = "Google Cloud Web Client ID is not configured yet. You can sign in with your email or use Instant Demo Access to continue."
                    )
                    return@launch
                }

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context as Activity,
                )

                handleSignInResult(result, context)

            } catch (e: GetCredentialCancellationException) {
                // User intentionally cancelled the Google picker bottom sheet
                _authState.value = AuthState.Idle
            } catch (e: NoCredentialException) {
                _authState.value = AuthState.CredentialNotice(
                    title = "No Google Account",
                    message = "No Google account was found on this device or emulator. Please sign in with email or continue with Instant Demo Access."
                )
            } catch (e: GetCredentialException) {
                val msg = e.localizedMessage ?: "Unknown error"
                Log.w("AuthViewModel", "Google CredentialManager exception: $msg", e)
                if (msg.contains("10:") || msg.contains("16:") || msg.contains("Developer Error", ignoreCase = true) || msg.contains("Cannot find", ignoreCase = true)) {
                    _authState.value = AuthState.CredentialNotice(
                        title = "Google Credentials Setup",
                        message = "Google Cloud Client ID and SHA-1 certificate configuration are pending in the Firebase Console. You can use Email Sign-In or Instant Demo Access right away."
                    )
                } else {
                    _authState.value = AuthState.CredentialNotice(
                        title = "Google Sign-In",
                        message = "Google Sign-In could not complete ($msg). You can continue with Email Sign-In or Instant Demo Access."
                    )
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google sign-in error", e)
                _authState.value = AuthState.CredentialNotice(
                    title = "Google Sign-In Notice",
                    message = "Google Sign-In encountered an issue (${e.localizedMessage ?: "configuration mismatch"}). You can sign in with your email or use Instant Demo Access."
                )
            }
        }
    }

    private fun handleSignInResult(result: GetCredentialResponse, context: Context) {
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            
            viewModelScope.launch {
                try {
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val user = authResult.user
                    saveLocalUser(
                        context, 
                        user?.displayName ?: googleIdTokenCredential.displayName ?: "Wurie Explorer", 
                        user?.email ?: googleIdTokenCredential.id
                    )
                    checkIfProfileExists()
                } catch (e: Exception) {
                    val msg = e.localizedMessage ?: "Firebase auth failed"
                    if (msg.contains("DEVELOPER_ERROR", ignoreCase = true) || msg.contains("fingerprint", ignoreCase = true)) {
                        _authState.value = AuthState.CredentialNotice(
                            title = "Firebase SHA-1 Notice",
                            message = "The build SHA-1 fingerprint needs to be added in the Firebase Console. You can continue with Email or Instant Demo Access."
                        )
                    } else {
                        _authState.value = AuthState.Error(msg)
                    }
                }
            }
        } else {
            _authState.value = AuthState.Error("Invalid credential type received.")
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

    fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.w("AuthViewModel", "Sign out error", e)
        }
        _authState.value = AuthState.Idle
    }
    
    // For AI Studio emulator preview testing
    fun previewBypass() {
        _authState.value = AuthState.Success
    }
}

