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
}
