package com.example.api

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Supplies the Authorization header for backend calls.
 *
 * There is intentionally no placeholder credential: signed-out callers send no usable
 * token, and the backend rejects them with 401 instead of granting a shared "guest"
 * identity. The previous `guest-token` fallback was accepted by the API without any
 * verification, which made every anonymous caller look like the same authenticated user.
 *
 * An empty string is returned rather than null so the Retrofit service signatures stay
 * unchanged; the API treats it as "Missing bearer token".
 */
class AuthTokenProvider(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun bearerToken(): String {
        val user = auth.currentUser ?: return ""
        return try {
            val token = user.getIdToken(false).await().token
            if (token.isNullOrEmpty()) "" else "Bearer $token"
        } catch (e: Exception) {
            ""
        }
    }

    /** The signed-in uid, or an empty string when there is no session. */
    fun currentUserId(): String = auth.currentUser?.uid ?: ""
}
