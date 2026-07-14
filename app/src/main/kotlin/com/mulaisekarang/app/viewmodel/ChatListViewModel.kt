package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.ChatRepository
import com.mulaisekarang.app.data.model.Conversation
import com.mulaisekarang.app.data.network.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatListUiState(
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ChatListViewModel @Inject constructor(private val repository: ChatRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    private val _openConversationEvent = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val openConversationEvent: SharedFlow<Int> = _openConversationEvent

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.conversations() }
                .onSuccess { conversations -> _uiState.update { it.copy(conversations = conversations, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.userMessage("Gagal memuat percakapan.")) } }
        }
    }

    fun openAiTutor() {
        viewModelScope.launch {
            runCatching { repository.startAiTutorConversation() }
                .onSuccess { conversation -> _openConversationEvent.emit(conversation.id) }
                .onFailure { e -> _uiState.update { it.copy(error = e.userMessage("Gagal membuka AI Tutor.")) } }
        }
    }
}
