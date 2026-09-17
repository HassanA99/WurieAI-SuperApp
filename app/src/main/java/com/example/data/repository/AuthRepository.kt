package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

sealed class AuthRepositoryResult<out T> {
    data class Success<out T>(val data: T) : AuthRepositoryResult<T>()
    data class Error(val message: String, val isPermissionDenied: Boolean = false) : AuthRepositoryResult<Nothing>()
    data class Notice(val title: String, val message: String) : AuthRepositoryResult<Nothing>()
    object Cancelled : AuthRepositoryResult<Nothing>()
}

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun signUpWithEmail(
        fullName: String,
        email: String,
        password: String
    ): AuthRepositoryResult<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return AuthRepositoryResult.Error("User creation returned null.")

            if (fullName.isNotBlank()) {
                try {
                    val profileUpdates = userProfileChangeRequest {
                        displayName = fullName
                    }
                    user.updateProfile(profileUpdates).await()
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Updating user display name failed", e)
                }
            }

            try {
                firestore.collection("users").document(user.uid).set(
                    mapOf(
                        "displayName" to fullName,
                        "email" to email,
                        "profileComplete" to false,
                        "createdAt" to System.currentTimeMillis()
                    )
                ).await()
            } catch (e: FirebaseFirestoreException) {
                Log.w("AuthRepository", "Firestore write blocked by Security Rules (PERMISSION_DENIED): ${e.message}")
            } catch (e: Exception) {
                Log.w("AuthRepository", "Failed to create basic Firestore profile", e)
            }

            AuthRepositoryResult.Success(user)
        } catch (e: FirebaseAuthUserCollisionException) {
            AuthRepositoryResult.Error("An account with this email already exists. Please sign in instead.")
        } catch (e: FirebaseAuthWeakPasswordException) {
            AuthRepositoryResult.Error("Password is too weak. Please use at least 6 characters.")
        } catch (e: Exception) {
            AuthRepositoryResult.Error(e.localizedMessage ?: "Sign up failed. Please try again.")
        }
    }

    suspend fun signInWithEmail(
        email: String,
        password: String
    ): AuthRepositoryResult<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return AuthRepositoryResult.Error("Sign in returned null user.")
            AuthRepositoryResult.Success(user)
        } catch (e: FirebaseAuthInvalidUserException) {
            AuthRepositoryResult.Error("No account found with this email. Tap 'Create Account' below.")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AuthRepositoryResult.Error("Incorrect password or email. Please check and try again.")
        } catch (e: Exception) {
            AuthRepositoryResult.Error(e.localizedMessage ?: "Login failed. Check your connection and try again.")
        }
    }

    suspend fun sendPasswordResetEmail(email: String): AuthRepositoryResult<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            AuthRepositoryResult.Success(Unit)
        } catch (e: Exception) {
            AuthRepositoryResult.Error(e.localizedMessage ?: "Could not send reset email. Check email and try again.")
        }
    }

    suspend fun signInWithGoogle(context: Context): AuthRepositoryResult<FirebaseUser> {
        val serverClientId = BuildConfig.WEB_CLIENT_ID

        if (serverClientId.isEmpty() || serverClientId == "MY_WEB_CLIENT_ID") {
            return AuthRepositoryResult.Notice(
                title = "Google Sign-In Credentials",
                message = "Google Cloud Web Client ID is not configured yet. Please sign in with your email instead."
            )
        }

        return try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val activityContext = context as? Activity
                ?: return AuthRepositoryResult.Error("Context must be an Activity to launch Google Sign-In.")

            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            handleGoogleSignInResult(result)
        } catch (e: GetCredentialCancellationException) {
            AuthRepositoryResult.Cancelled
        } catch (e: NoCredentialException) {
            AuthRepositoryResult.Notice(
                title = "No Google Account",
                message = "No Google account was found on this device or emulator. Please sign in with email instead."
            )
        } catch (e: GetCredentialException) {
            val msg = e.localizedMessage ?: "Unknown error"
            Log.w("AuthRepository", "Google CredentialManager exception: $msg", e)
            if (msg.contains("10:") || msg.contains("16:") || msg.contains("Developer Error", ignoreCase = true) || msg.contains("Cannot find", ignoreCase = true)) {
                AuthRepositoryResult.Notice(
                    title = "Google Credentials Setup",
                    message = "Google Cloud Client ID and SHA-1 certificate configuration are pending in the Firebase Console. Please use Email Sign-In instead."
                )
            } else {
                AuthRepositoryResult.Notice(
                    title = "Google Sign-In",
                    message = "Google Sign-In could not complete ($msg). Please use Email Sign-In instead."
                )
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Google sign-in error", e)
            AuthRepositoryResult.Notice(
                title = "Google Sign-In Notice",
                message = "Google Sign-In encountered an issue (${e.localizedMessage ?: "configuration mismatch"}). Please sign in with your email instead."
            )
        }
    }

    private suspend fun handleGoogleSignInResult(result: GetCredentialResponse): AuthRepositoryResult<FirebaseUser> {
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

            return try {
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user ?: return AuthRepositoryResult.Error("Google Auth returned null user.")
                AuthRepositoryResult.Success(user)
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Firebase auth failed"
                if (msg.contains("DEVELOPER_ERROR", ignoreCase = true) || msg.contains("fingerprint", ignoreCase = true)) {
                    AuthRepositoryResult.Notice(
                        title = "Firebase SHA-1 Notice",
                        message = "The build SHA-1 fingerprint needs to be added in the Firebase Console. Please use Email Sign-In instead."
                    )
                } else {
                    AuthRepositoryResult.Error(msg)
                }
            }
        } else {
            return AuthRepositoryResult.Error("Invalid credential type received.")
        }
    }

    suspend fun checkProfileStatus(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            doc.exists() && doc.getBoolean("profileComplete") == true
        } catch (e: FirebaseFirestoreException) {
            Log.w("AuthRepository", "Firestore access denied by Security Rules in checkProfileStatus: ${e.message}")
            true
        } catch (e: Exception) {
            Log.w("AuthRepository", "Error checking profile status", e)
            true
        }
    }

    suspend fun completeProfile(
        firstName: String,
        lastName: String,
        phone: String,
        city: String
    ): AuthRepositoryResult<Unit> {
        val uid = auth.currentUser?.uid ?: return AuthRepositoryResult.Error("No user logged in.")
        return try {
            val profile = mapOf(
                "firstName" to firstName,
                "lastName" to lastName,
                "phone" to phone,
                "city" to city,
                "email" to (auth.currentUser?.email ?: ""),
                "profileComplete" to true,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(uid).set(profile).await()
            AuthRepositoryResult.Success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Log.w("AuthRepository", "Firestore write blocked by Security Rules: ${e.message}")
            AuthRepositoryResult.Success(Unit)
        } catch (e: Exception) {
            AuthRepositoryResult.Error(e.localizedMessage ?: "Could not save your profile. Please try again.")
        }
    }

    fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.w("AuthRepository", "Sign out error", e)
        }
    }
}
