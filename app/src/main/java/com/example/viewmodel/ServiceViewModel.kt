package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ServiceViewModel(
    private val repository: WurieApiRepository
) : ViewModel() {
    private val _marketState = MutableStateFlow<ChatResponse?>(null)
    val marketState: StateFlow<ChatResponse?> = _marketState.asStateFlow()

    private val _providerState = MutableStateFlow<ProviderRegistrationResponse?>(null)
    val providerState: StateFlow<ProviderRegistrationResponse?> = _providerState.asStateFlow()

    private val _bookingState = MutableStateFlow<BookingResponse?>(null)
    val bookingState: StateFlow<BookingResponse?> = _bookingState.asStateFlow()

    private val _walletState = MutableStateFlow<WalletBalanceResponse?>(null)
    val walletState: StateFlow<WalletBalanceResponse?> = _walletState.asStateFlow()

    fun registerProvider(name: String, trade: String, location: String, experience: String, token: String) {
        viewModelScope.launch {
            try {
                val result = repository.registerProvider(
                    ProviderRegistrationRequest(name, trade, location, experience),
                    token
                )
                _providerState.value = result
            } catch (e: Exception) {
                _providerState.value = ProviderRegistrationResponse("error", e.localizedMessage ?: "registration failed")
            }
        }
    }

    fun requestMarketPrice(commodity: String, location: String? = null, token: String) {
        viewModelScope.launch {
            try {
                val result = repository.getMarketPrice(MarketPriceRequest(commodity, location), token)
                _marketState.value = result
            } catch (e: Exception) {
                _marketState.value = ChatResponse("Network error: ${e.localizedMessage}", "ERROR", "Market Prices")
            }
        }
    }

    fun createBooking(request: BookingRequest, token: String) {
        viewModelScope.launch {
            try {
                val result = repository.createBooking(request, token)
                _bookingState.value = result
            } catch (e: Exception) {
                _bookingState.value = BookingResponse("", "error", e.localizedMessage ?: "booking failed")
            }
        }
    }

    fun getWalletBalance(token: String) {
        viewModelScope.launch {
            try {
                val result = repository.getWalletBalance(token)
                _walletState.value = result
            } catch (e: Exception) {
                _walletState.value = WalletBalanceResponse("unknown", 0.0, "USD", emptyList())
            }
        }
    }
}
