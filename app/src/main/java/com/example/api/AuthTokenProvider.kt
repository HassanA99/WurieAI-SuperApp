package com.example.api

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthTokenProvider(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun bearerToken(): String {
        val user = auth.currentUser ?: error("User is not authenticated")
        val token = user.getIdToken(false).await().token
            ?: error("Firebase did not return an ID token")
        return "Bearer $token"
    }

    fun currentUserId(): String = auth.currentUser?.uid ?: error("User is not authenticated")
}
