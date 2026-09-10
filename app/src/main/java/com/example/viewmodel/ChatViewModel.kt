package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.api.ChatRequest
import com.example.api.ChatResponse
import com.example.api.AuthTokenProvider
import com.example.api.RetrofitClient
import com.example.api.WurieApiRepository
import com.example.data.local.ActivityLogEntity
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.repository.ActivityRepository
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val repository: ChatRepository,
    private val activityRepository: ActivityRepository,
    private val apiRepository: WurieApiRepository? = null,
    private val authTokenProvider: AuthTokenProvider = AuthTokenProvider()
) : ViewModel() {

    val messages: StateFlow<List<ChatMessageEntity>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        
    private val _navigationEvents = MutableSharedFlow<String>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    fun clearChat() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun sendMessage(text: String, base64Image: String? = null) {
        if (text.isBlank() && base64Image == null) return

        viewModelScope.launch {
            val userMsg = ChatMessageEntity(text = text, isUser = true)
            repository.insertMessage(userMsg)
            val loadingId = UUID.randomUUID().toString()
            val loadingMsg = ChatMessageEntity(id = loadingId, text = "Thinking...", isUser = false, isLoading = true)
            repository.insertMessage(loadingMsg)

            try {
                val backendToken = authTokenProvider.bearerToken()
                val response = apiRepository?.sendChatMessage(
                    ChatRequest(message = text, location = null),
                    backendToken
                )

                if (response != null) {
                    val backendText = response.text.ifBlank { "I’m here to help you with WurieAI." }
                    val actionText = response.action
                    val targetText = response.target
                    val messageText = buildString {
                        append(backendText)
                        if (!actionText.isNullOrBlank() && !targetText.isNullOrBlank()) {
                            append(" [${actionText}:$targetText]")
                        }
                    }

                    updateLoadingMessage(loadingId, messageText)

                    if (!actionText.isNullOrBlank() && !targetText.isNullOrBlank()) {
                        activityRepository.insertLog(
                            ActivityLogEntity(
                                title = "Backend routed action $actionText",
                                description = "Backend suggested target $targetText for user chat.",
                                iconName = "navigateTo",
                                colorHex = 0xFF9C27B0
                            )
                        )
                    }
                    return@launch
                }

                // If the backend client is not configured, we stay in a deterministic demo flow.
                kotlinx.coroutines.delay(500)
                val demoResponse = when {
                    text.contains("price", ignoreCase = true) -> "Opening Market Prices... [NAVIGATE_TO:Market Prices]"
                    text.contains("hire", ignoreCase = true) || text.contains("artisan", ignoreCase = true) || text.contains("provider", ignoreCase = true) -> "Let's find you a professional. [NAVIGATE_TO:Hire Provider]"
                    text.contains("wallet", ignoreCase = true) || text.contains("balance", ignoreCase = true) -> "Opening your digital wallet... [NAVIGATE_TO:Wallet]"
                    else -> "I am WurieAI, your local commerce and service assistant. How can I help you today?"
                }
                updateLoadingMessage(loadingId, demoResponse)
            } catch (e: Exception) {
                e.printStackTrace()
                val demoResponse = when {
                    text.contains("price", ignoreCase = true) -> "Opening Market Prices... [NAVIGATE_TO:Market Prices]"
                    text.contains("hire", ignoreCase = true) || text.contains("artisan", ignoreCase = true) || text.contains("provider", ignoreCase = true) -> "Let's find you a professional. [NAVIGATE_TO:Hire Provider]"
                    text.contains("wallet", ignoreCase = true) || text.contains("balance", ignoreCase = true) -> "Opening your digital wallet... [NAVIGATE_TO:Wallet]"
                    else -> "I am WurieAI, your local commerce and service assistant. How can I help you today?"
                }
                updateLoadingMessage(loadingId, demoResponse)
            }
        }
    }
    
    private suspend fun updateLoadingMessage(id: String, newText: String, isError: Boolean = false) {
        val messageToUpdate = messages.value.find { it.id == id }
        if (messageToUpdate != null) {
            repository.updateMessage(messageToUpdate.copy(text = newText, isLoading = false, isError = isError))
        }
    }
    
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val context = this[APPLICATION_KEY]!!.applicationContext
                val database = AppDatabase.getDatabase(context)
                val repository = ChatRepository(database.chatDao())
                val activityRepository = ActivityRepository(database.activityLogDao())
                val apiRepository = WurieApiRepository(RetrofitClient.apiService)
                ChatViewModel(repository, activityRepository, apiRepository)
            }
        }
    }
}
