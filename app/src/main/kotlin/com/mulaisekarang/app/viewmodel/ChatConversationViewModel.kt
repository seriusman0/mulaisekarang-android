package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.ChatRepository
import com.mulaisekarang.app.data.model.ChatMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val POLL_INTERVAL_MS = 4000L

sealed interface ChatConversationUiState {
    data object Loading : ChatConversationUiState
    data class Loaded(val messages: List<ChatMessage>) : ChatConversationUiState
    data class Error(val message: String) : ChatConversationUiState
}

class ChatConversationViewModel(
    private val repository: ChatRepository,
    private val conversationId: Int,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatConversationUiState>(ChatConversationUiState.Loading)
    val uiState: StateFlow<ChatConversationUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var isSending = false

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun refresh() {
        runCatching { repository.messages(conversationId) }
            .onSuccess { messages -> _uiState.update { ChatConversationUiState.Loaded(messages) } }
            .onFailure { e ->
                if (_uiState.value !is ChatConversationUiState.Loaded) {
                    _uiState.update { ChatConversationUiState.Error(e.message ?: "Gagal memuat pesan.") }
                }
            }
    }

    fun sendMessage(body: String) {
        if (body.isBlank() || isSending) return
        isSending = true
        viewModelScope.launch {
            runCatching { repository.sendMessage(conversationId, body) }
                .onSuccess { refresh() }
            isSending = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
