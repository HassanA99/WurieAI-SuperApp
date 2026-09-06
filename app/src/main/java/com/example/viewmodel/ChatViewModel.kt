package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.BuildConfig
import com.example.api.GeminiService
import com.example.api.GeminiResponse

import com.example.api.Candidate
import com.example.api.Content
import com.example.api.FunctionCall
import com.example.api.FunctionDeclaration
import com.example.api.GeminiRequest
import com.example.api.InlineData
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.api.Schema
import com.example.api.Tool
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
    private val activityRepository: ActivityRepository
) : ViewModel() {

    val messages: StateFlow<List<ChatMessageEntity>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        
    private val _navigationEvents = MutableSharedFlow<String>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    private val geminiService = RetrofitClient.geminiService

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
                val apiKey = BuildConfig.GEMINI_API_KEY2
                
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    kotlinx.coroutines.delay(1000)
                    val demoResponse = when {
                        text.contains("price", ignoreCase = true) -> "Opening Market Prices... [NAVIGATE_TO:Market Prices]"
                        text.contains("hire", ignoreCase = true) || text.contains("artisan", ignoreCase = true) || text.contains("provider", ignoreCase = true) -> "Let's find you a professional. [NAVIGATE_TO:Hire Provider]"
                        text.contains("wallet", ignoreCase = true) || text.contains("balance", ignoreCase = true) -> "Opening your digital wallet... [NAVIGATE_TO:Wallet]"
                        else -> "I am operating in Demo Mode because no API key was found.\n\nThe Gemini API is completely free! Just grab a key from Google AI Studio, add it to the Secrets panel (using the GEMINI_API_KEY2 key), and I will come alive.\n\nYou said: \"$text\""
                    }
                    updateLoadingMessage(loadingId, demoResponse)
                    return@launch
                }
                
                val contents = mutableListOf<Content>()
                val messagesList = messages.value
                
                // Add conversation history
                contents.addAll(messagesList
                    .filter { !it.isLoading && !it.isError && it.id != loadingId && it.id != userMsg.id }
                    .map { 
                        Content(
                            parts = listOf(Part(text = it.text)),
                            role = if (it.isUser) "user" else "model"
                        )
                    })
                
                // Add current message
                val currentParts = mutableListOf<Part>()
                if (text.isNotBlank()) currentParts.add(Part(text = text))
                if (base64Image != null) {
                    currentParts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image)))
                }
                contents.add(Content(parts = currentParts, role = "user"))
                
                val systemInstructionText = "You are WurieAI, an intelligent Agentic Super App focused on the Mano River Union (MRU) region. You help users with transportation, finding service providers, market prices, and more. When a user asks for a service (like a plumber, electrician, or driver), use the 'recommendProvider' tool to suggest a verified professional and offer to chat with them. When a user wants to navigate to a specific feature like 'Wallet', 'Explore', or 'Hire Provider', use the 'navigateTo' tool. For 'Hire Provider', you can pass the trade as a parameter like 'Hire Provider:Plumber' to pre-filter the results. If a user uploads an image, analyze it and suggest the appropriate service provider they might need (e.g., 'It looks like a broken pipe, I recommend a plumber'). Speak professionally, clearly, and concisely."
                
                val systemInstruction = Content(
                    parts = listOf(Part(text = systemInstructionText)),
                    role = "user"
                )
                
                val tools = listOf(
                    Tool(
                        functionDeclarations = listOf(
                            FunctionDeclaration(
                                name = "navigateTo",
                                description = "Navigate the user to a specific screen in the app.",
                                parameters = Schema(
                                    type = "OBJECT",
                                    properties = mapOf(
                                        "screenName" to Schema(
                                            type = "STRING",
                                            description = "The name of the screen to navigate to. Allowed values: 'Market Prices', 'Book Artisan', 'Wallet', 'Activity', 'Profile'."
                                        )
                                    ),
                                    required = listOf("screenName")
                                )
                            ),
                            FunctionDeclaration(
                                name = "recommendProvider",
                                description = "Recommend a professional provider or artisan to the user and offer to chat with them.",
                                parameters = Schema(
                                    type = "OBJECT",
                                    properties = mapOf(
                                        "profession" to Schema(
                                            type = "STRING",
                                            description = "The profession of the provider, e.g. Plumber, Electrician, Driver."
                                        ),
                                        "name" to Schema(
                                            type = "STRING",
                                            description = "A simulated name for the provider, e.g. Abu, Fatu, Momodu."
                                        )
                                    ),
                                    required = listOf("profession", "name")
                                )
                            )
                        )
                    )
                )
                
                val request = GeminiRequest(
                    contents = contents,
                    systemInstruction = systemInstruction,
                    tools = tools
                )
                
                // Use gemini-1.5-flash for vision support
                val response = geminiService.generateContent(
                    model = "gemini-1.5-flash",
                    apiKey = apiKey,
                    request = request
                )
                
                val firstPart = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()
                
                if (firstPart?.functionCall != null) {
                    val functionName = firstPart.functionCall.name
                    val args = firstPart.functionCall.args
                    if (functionName == "navigateTo" && args != null) {
                        val screenName = args["screenName"] as? String
                        if (screenName != null) {
                            val msgText = "I'm opening the $screenName screen for you. [NAVIGATE_TO:$screenName]"
                            updateLoadingMessage(loadingId, msgText)
                            
                            // Log the action to the activity ledger
                            activityRepository.insertLog(
                                ActivityLogEntity(
                                    title = "Agent navigated to $screenName",
                                    description = "Automatically suggested opening the $screenName screen.",
                                    iconName = "navigateTo",
                                    colorHex = 0xFF9C27B0 // Purple
                                )
                            )
                        } else {
                            updateLoadingMessage(loadingId, "I encountered an error trying to navigate.")
                        }
                    } else if (functionName == "recommendProvider" && args != null) {
                        val profession = args["profession"] as? String ?: "Professional"
                        val name = args["name"] as? String ?: "Provider"
                        val msgText = "I found a great $profession for you named $name. Would you like to chat with them to discuss your needs? [CHAT_PROVIDER:$name:$profession]"
                        updateLoadingMessage(loadingId, msgText)
                        
                        activityRepository.insertLog(
                            ActivityLogEntity(
                                title = "Recommended $profession",
                                description = "Found a verified $profession ($name) in your area.",
                                iconName = "person",
                                colorHex = 0xFF4CAF50
                            )
                        )
                    } else {
                        updateLoadingMessage(loadingId, "I attempted to use a tool, but something went wrong.")
                    }
                } else {
                    val responseText = firstPart?.text ?: "I'm sorry, I couldn't generate a response."
                    updateLoadingMessage(loadingId, responseText)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                updateLoadingMessage(loadingId, "Network Error: ${e.localizedMessage}", isError = true)
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
                ChatViewModel(repository, activityRepository)
            }
        }
    }
}
