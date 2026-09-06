package com.example.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class ChatRequest(
    val message: String,
    val location: String? = null
)

data class ChatResponse(
    val text: String,
    val action: String? = null,
    val target: String? = null
)

interface WurieApiService {
    @POST("/api/v1/chat")
    suspend fun sendChatMessage(
        @Header("Authorization") token: String,
        @Body request: ChatRequest
    ): ChatResponse
}
