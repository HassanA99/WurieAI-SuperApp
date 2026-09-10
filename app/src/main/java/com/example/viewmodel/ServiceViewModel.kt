package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.api.AuthTokenProvider
import com.example.api.RetrofitClient
import com.example.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ServiceViewModel(
    private val repository: WurieApiRepository = WurieApiRepository(RetrofitClient.apiService),
    private val authTokenProvider: AuthTokenProvider = AuthTokenProvider()
) : ViewModel() {
    private val _marketState = MutableStateFlow<ChatResponse?>(null)
    val marketState: StateFlow<ChatResponse?> = _marketState.asStateFlow()

    private val _providerState = MutableStateFlow<ProviderRegistrationResponse?>(null)
    val providerState: StateFlow<ProviderRegistrationResponse?> = _providerState.asStateFlow()

    private val _pendingProviders = MutableStateFlow<List<ProviderSummary>>(emptyList())
    val pendingProviders: StateFlow<List<ProviderSummary>> = _pendingProviders.asStateFlow()

    private val _bookingState = MutableStateFlow<BookingResponse?>(null)
    val bookingState: StateFlow<BookingResponse?> = _bookingState.asStateFlow()

    private val _walletState = MutableStateFlow<WalletBalanceResponse?>(null)
    val walletState: StateFlow<WalletBalanceResponse?> = _walletState.asStateFlow()

    fun registerProvider(name: String, trade: String, location: String, experience: String, token: String? = null) {
        viewModelScope.launch {
            try {
                val activeToken = token ?: authTokenProvider.bearerToken()
                val result = repository.registerProvider(
                    ProviderRegistrationRequest(name, trade, location, experience),
                    activeToken
                )
                _providerState.value = result
            } catch (e: Exception) {
                _providerState.value = ProviderRegistrationResponse("error", e.localizedMessage ?: "registration failed")
            }
        }
    }

    fun loadPendingProviders(token: String? = null) {
        viewModelScope.launch {
            try {
                val activeToken = token ?: authTokenProvider.bearerToken()
                _pendingProviders.value = repository.getPendingProviders(activeToken)
            } catch (e: Exception) {
                _pendingProviders.value = emptyList()
            }
        }
    }

    fun approveProvider(providerId: String, token: String? = null) {
        viewModelScope.launch {
            try {
                val activeToken = token ?: authTokenProvider.bearerToken()
                repository.approveProvider(providerId, activeToken)
                loadPendingProviders(activeToken)
            } catch (_: Exception) {
            }
        }
    }

    fun rejectProvider(providerId: String, token: String? = null) {
        viewModelScope.launch {
            try {
                val activeToken = token ?: authTokenProvider.bearerToken()
                repository.rejectProvider(providerId, activeToken)
                loadPendingProviders(activeToken)
            } catch (_: Exception) {
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

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ServiceViewModel()
            }
        }
    }
}
