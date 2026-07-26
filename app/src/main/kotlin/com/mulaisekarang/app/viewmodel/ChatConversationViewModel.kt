package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.ChatRepository
import com.mulaisekarang.app.data.model.ChatMessage
import com.mulaisekarang.app.data.model.Conversation
import com.mulaisekarang.app.data.network.isChatPaywall
import com.mulaisekarang.app.data.network.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

@HiltViewModel
class ChatConversationViewModel @Inject constructor(
    private val repository: ChatRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val conversationId: Int = checkNotNull(savedStateHandle["conversationId"])

    private val _uiState = MutableStateFlow<ChatConversationUiState>(ChatConversationUiState.Loading)
    val uiState: StateFlow<ChatConversationUiState> = _uiState.asStateFlow()

    private val _conversation = MutableStateFlow<Conversation?>(null)
    val conversation: StateFlow<Conversation?> = _conversation.asStateFlow()

    private val _sendError = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val sendError: SharedFlow<String> = _sendError.asSharedFlow()

    private val _paywallRequired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val paywallRequired: SharedFlow<Unit> = _paywallRequired.asSharedFlow()

    private var pollingJob: Job? = null
    private var isSending = false

    init {
        viewModelScope.launch {
            runCatching { repository.conversations() }
                .onSuccess { list -> _conversation.value = list.find { it.id == conversationId } }
        }
    }

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
                .onFailure { e ->
                    // An expired add-on is not a transient send failure — send
                    // the student to the paywall rather than a retry snackbar.
                    if (e.isChatPaywall()) {
                        _paywallRequired.emit(Unit)
                    } else {
                        _sendError.emit(e.userMessage("Pesan gagal terkirim."))
                    }
                }
            isSending = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
