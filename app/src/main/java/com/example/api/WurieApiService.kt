package com.example.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import com.squareup.moshi.Json

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

data class UserProfile(
    val uid: String,
    @Json(name = "full_name") val fullName: String,
    val email: String,
    val phone: String? = null,
    val city: String? = null,
    val role: String = "customer",
    @Json(name = "profile_completed") val profileCompleted: Boolean = false
)

data class ProfileUpdateRequest(
    @Json(name = "full_name") val fullName: String? = null,
    val phone: String? = null,
    val city: String? = null
)

data class ProfileSettings(
    @Json(name = "notifications_enabled") val notificationsEnabled: Boolean = true,
    @Json(name = "biometric_enabled") val biometricEnabled: Boolean = false,
    @Json(name = "push_enabled") val pushEnabled: Boolean = true,
    @Json(name = "offline_cache_enabled") val offlineCacheEnabled: Boolean = true,
    val language: String = "en"
)

data class ProfileSettingsUpdateRequest(
    @Json(name = "notifications_enabled") val notificationsEnabled: Boolean? = null,
    @Json(name = "biometric_enabled") val biometricEnabled: Boolean? = null,
    @Json(name = "push_enabled") val pushEnabled: Boolean? = null,
    @Json(name = "offline_cache_enabled") val offlineCacheEnabled: Boolean? = null,
    val language: String? = null
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

    @GET("/api/v1/profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): UserProfile

    @retrofit2.http.PATCH("/api/v1/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: ProfileUpdateRequest
    ): UserProfile

    @GET("/api/v1/profile/settings")
    suspend fun getProfileSettings(
        @Header("Authorization") token: String
    ): ProfileSettings

    @retrofit2.http.PATCH("/api/v1/profile/settings")
    suspend fun updateProfileSettings(
        @Header("Authorization") token: String,
        @Body request: ProfileSettingsUpdateRequest
    ): ProfileSettings

    @GET("/health")
    suspend fun healthCheck(): Map<String, String>
}
