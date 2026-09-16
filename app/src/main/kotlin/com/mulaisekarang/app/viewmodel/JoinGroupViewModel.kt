package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class JoinGroupUiState {
    object Joining : JoinGroupUiState()
    data class Success(val conversationId: Int) : JoinGroupUiState()
    data class Error(val message: String) : JoinGroupUiState()
}

@HiltViewModel
class JoinGroupViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val token: String = checkNotNull(savedStateHandle["token"])

    private val _uiState = MutableStateFlow<JoinGroupUiState>(JoinGroupUiState.Joining)
    val uiState: StateFlow<JoinGroupUiState> = _uiState.asStateFlow()

    init {
        joinGroup()
    }

    private fun joinGroup() {
        viewModelScope.launch {
            try {
                val conversation = chatRepository.joinGroup(token)
                _uiState.value = JoinGroupUiState.Success(conversation.id)
            } catch (e: Exception) {
                _uiState.value = JoinGroupUiState.Error(e.message ?: "Failed to join group")
            }
        }
    }
}
