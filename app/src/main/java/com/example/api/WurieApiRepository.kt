package com.example.api

import com.example.BuildConfig

class WurieApiRepository(
    private val apiService: WurieApiService
) {
    suspend fun sendChatMessage(request: ChatRequest, token: String): ChatResponse {
        return apiService.sendChatMessage(token, request)
    }

    suspend fun registerProvider(request: ProviderRegistrationRequest, token: String): ProviderRegistrationResponse {
        return apiService.registerProvider(token, request)
    }

    suspend fun getMarketPrice(request: MarketPriceRequest, token: String): ChatResponse {
        return apiService.getMarketPrice(token, request)
    }

    suspend fun createBooking(request: BookingRequest, token: String): BookingResponse {
        return apiService.createBooking(token, request)
    }

    suspend fun getWalletBalance(token: String): WalletBalanceResponse {
        return apiService.getWalletBalance(token)
    }

    suspend fun getProfile(token: String): UserProfile {
        return apiService.getProfile(token)
    }

    suspend fun updateProfile(request: ProfileUpdateRequest, token: String): UserProfile {
        return apiService.updateProfile(token, request)
    }

    suspend fun getProfileSettings(token: String): ProfileSettings {
        return apiService.getProfileSettings(token)
    }

    suspend fun updateProfileSettings(request: ProfileSettingsUpdateRequest, token: String): ProfileSettings {
        return apiService.updateProfileSettings(token, request)
    }
}
