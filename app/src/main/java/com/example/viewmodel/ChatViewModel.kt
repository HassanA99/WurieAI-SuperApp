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

    private suspend fun updateLoadingMessage(id: String, newText: String, isError: Boolean = false) {
        val updatedMsg = ChatMessageEntity(
            id = id,
            text = newText,
            isUser = false,
            isLoading = false,
            isError = isError,
            timestamp = System.currentTimeMillis()
        )
        repository.insertMessage(updatedMsg)
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

                val responseText = response?.getDisplayText()
                if (response != null && !responseText.isNullOrBlank()) {
                    val actionText = response.action
                    val targetText = response.target
                    val messageText = buildString {
                        append(responseText)
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
                } else {
                    updateLoadingMessage(
                        loadingId,
                        "Backend returned an empty response.",
                        isError = true
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val errMessage = e.localizedMessage ?: "Network/Server Connection Error"
                updateLoadingMessage(
                    loadingId,
                    "Error connecting to backend: $errMessage",
                    isError = true
                )
            }
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
