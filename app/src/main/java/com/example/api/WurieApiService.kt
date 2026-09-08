package com.example.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

data class ChatRequest(
    val message: String,
    val location: String? = null
)

data class ChatResponse(
    val text: String,
    val action: String? = null,
    val target: String? = null
)

data class ProviderRegistrationRequest(
    val name: String,
    val trade: String,
    val location: String,
    val experience: String
)

data class ProviderRegistrationResponse(
    val status: String,
    val message: String,
    val providerId: String? = null
)

data class MarketPriceRequest(
    val commodity: String,
    val location: String? = null
)

data class BookingRequest(
    val userId: String,
    val providerId: String,
    val serviceType: String,
    val city: String,
    val location: String? = null,
    val scheduledTime: String? = null,
    val notes: String? = null
)

data class BookingResponse(
    val bookingId: String,
    val status: String,
    val message: String
)

data class WalletBalanceResponse(
    val userId: String,
    val balance: Double,
    val currency: String,
    val transactions: List<Map<String, Any>> = emptyList()
)

interface WurieApiService {
    @POST("/api/v1/chat")
    suspend fun sendChatMessage(
        @Header("Authorization") token: String,
        @Body request: ChatRequest
    ): ChatResponse

    @POST("/api/v1/providers/register")
    suspend fun registerProvider(
        @Header("Authorization") token: String,
        @Body request: ProviderRegistrationRequest
    ): ProviderRegistrationResponse

    @POST("/api/v1/market/price")
    suspend fun getMarketPrice(
        @Header("Authorization") token: String,
        @Body request: MarketPriceRequest
    ): ChatResponse

    @POST("/api/v1/bookings")
    suspend fun createBooking(
        @Header("Authorization") token: String,
        @Body request: BookingRequest
    ): BookingResponse

    @GET("/api/v1/wallet/balance")
    suspend fun getWalletBalance(
        @Header("Authorization") token: String
    ): WalletBalanceResponse

    @GET("/health")
    suspend fun healthCheck(): Map<String, String>
}
