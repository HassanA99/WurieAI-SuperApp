package com.example.api

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthTokenProvider(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun bearerToken(): String {
        return try {
            val user = auth.currentUser
            if (user != null) {
                val token = user.getIdToken(false).await().token
                if (!token.isNullOrEmpty()) "Bearer $token" else "Bearer guest-token"
            } else {
                "Bearer guest-token"
            }
        } catch (e: Exception) {
            "Bearer guest-token"
        }
    }

    fun currentUserId(): String = auth.currentUser?.uid ?: "guest_user"
}
