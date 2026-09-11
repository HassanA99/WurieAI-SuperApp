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

    suspend fun getPendingProviders(token: String): List<ProviderSummary> {
        return apiService.getPendingProviders(token)
    }

    suspend fun searchProviders(city: String?, profession: String?, token: String): List<ProviderSummary> {
        return apiService.searchProviders(token, city, profession)
    }

    suspend fun approveProvider(providerId: String, token: String): ProviderSummary {
        return apiService.approveProvider(token, providerId)
    }

    suspend fun rejectProvider(providerId: String, token: String): ProviderSummary {
        return apiService.rejectProvider(token, providerId)
    }

    suspend fun getMarketPrice(request: MarketPriceRequest, token: String): ChatResponse {
        return apiService.getMarketPrice(token, request)
    }

    suspend fun createBooking(request: BookingRequest, token: String): BookingResponse {
        return apiService.createBooking(token, request)
    }

    suspend fun getBookings(token: String, userId: String? = null): List<BookingHistoryItem> {
        return apiService.getBookings(token, userId)
    }

    suspend fun updateBookingStatus(bookingId: String, status: String, token: String): Map<String, Any> {
        return apiService.updateBookingStatus(token, bookingId, BookingStatusRequest(status))
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

    suspend fun getComments(token: String): List<CommentItem> {
        return apiService.getComments(token)
    }

    suspend fun addComment(request: CommentCreateRequest, token: String): CommentItem {
        return apiService.addComment(token, request)
    }

    suspend fun getNotifications(token: String): List<NotificationItem> {
        return apiService.getNotifications(token)
    }

    suspend fun addNotification(request: NotificationCreateRequest, token: String): NotificationItem {
        return apiService.addNotification(token, request)
    }

    suspend fun markNotificationRead(notificationId: String, token: String): NotificationItem {
        return apiService.markNotificationRead(token, notificationId)
    }
}
