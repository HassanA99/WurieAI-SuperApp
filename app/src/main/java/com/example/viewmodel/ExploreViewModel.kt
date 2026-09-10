package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.api.AuthTokenProvider
import com.example.api.CommentCreateRequest
import com.example.api.CommentItem
import com.example.api.NotificationCreateRequest
import com.example.api.NotificationItem
import com.example.api.RetrofitClient
import com.example.api.WurieApiRepository
import com.example.ui.FeedComment
import com.example.ui.NotificationEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExploreUiState(
    val comments: List<FeedComment> = emptyList(),
    val notifications: List<NotificationEntry> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ExploreViewModel(
    private val repository: WurieApiRepository = WurieApiRepository(RetrofitClient.apiService),
    private val authTokenProvider: AuthTokenProvider = AuthTokenProvider()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    fun loadSocialFeed() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val token = authTokenProvider.bearerToken()
                val comments = repository.getComments(token)
                val notifications = repository.getNotifications(token)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    comments = comments.map { item ->
                        FeedComment(
                            id = item.id,
                            username = item.username,
                            comment = item.body,
                            timeAgo = item.createdAt ?: "just now"
                        )
                    },
                    notifications = notifications.map { item ->
                        NotificationEntry(
                            id = item.id,
                            title = item.title,
                            body = item.body,
                            timeAgo = item.createdAt ?: "just now",
                            unread = !item.read
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Unable to load social feed."
                )
            }
        }
    }

    fun addComment(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            try {
                val token = authTokenProvider.bearerToken()
                val created = repository.addComment(CommentCreateRequest(body = text, username = "You"), token)
                val current = _uiState.value.comments.toMutableList()
                current.add(
                    FeedComment(
                        id = created.id,
                        username = created.username,
                        comment = created.body,
                        timeAgo = created.createdAt ?: "just now"
                    )
                )
                _uiState.value = _uiState.value.copy(comments = current)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.localizedMessage ?: "Unable to post comment."
                )
            }
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            try {
                val token = authTokenProvider.bearerToken()
                repository.markNotificationRead(id, token)
                val updatedNotifications = _uiState.value.notifications.map {
                    if (it.id == id) it.copy(unread = false) else it
                }
                _uiState.value = _uiState.value.copy(notifications = updatedNotifications)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.localizedMessage ?: "Unable to update notification."
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ExploreViewModel()
            }
        }
    }
}
