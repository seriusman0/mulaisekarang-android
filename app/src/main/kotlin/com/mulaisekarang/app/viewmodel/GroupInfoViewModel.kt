package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.ChatRepository
import com.mulaisekarang.app.data.model.Conversation
import com.mulaisekarang.app.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class GroupInfoUiState {
    object Loading : GroupInfoUiState()
    data class Loaded(val conversation: Conversation, val currentUserId: Int) : GroupInfoUiState()
    data class Error(val message: String) : GroupInfoUiState()
}

@HiltViewModel
class GroupInfoViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val conversationId: Int = checkNotNull(savedStateHandle["conversationId"])

    private val _uiState = MutableStateFlow<GroupInfoUiState>(GroupInfoUiState.Loading)
    val uiState: StateFlow<GroupInfoUiState> = _uiState.asStateFlow()

    private val _actionResult = MutableStateFlow<String?>(null)
    val actionResult: StateFlow<String?> = _actionResult.asStateFlow()
    
    private val _inviteLink = MutableStateFlow<String?>(null)
    val inviteLink: StateFlow<String?> = _inviteLink.asStateFlow()

    init {
        loadGroupInfo()
    }

    fun loadGroupInfo() {
        viewModelScope.launch {
            _uiState.value = GroupInfoUiState.Loading
            try {
                // Since there is no single conversation endpoint we might need to get it from conversations()
                // In a real app we would have a specific endpoint. Here we filter the list.
                val convs = chatRepository.conversations()
                val conv = convs.find { it.id == conversationId }
                if (conv != null) {
                    val user = authRepository.me()
                    _uiState.value = GroupInfoUiState.Loaded(conv, user.id)
                } else {
                    _uiState.value = GroupInfoUiState.Error("Grup tidak ditemukan")
                }
            } catch (e: Exception) {
                _uiState.value = GroupInfoUiState.Error(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun addMember(username: String) {
        viewModelScope.launch {
            try {
                chatRepository.addParticipant(conversationId, username)
                _actionResult.value = "Anggota berhasil ditambahkan"
                loadGroupInfo()
            } catch (e: Exception) {
                _actionResult.value = "Gagal menambahkan anggota: ${e.message}"
            }
        }
    }

    fun removeMember(userId: Int) {
        viewModelScope.launch {
            try {
                chatRepository.removeParticipant(conversationId, userId)
                _actionResult.value = "Anggota berhasil dikeluarkan"
                loadGroupInfo()
            } catch (e: Exception) {
                _actionResult.value = "Gagal mengeluarkan anggota: ${e.message}"
            }
        }
    }

    fun generateInviteLink() {
        viewModelScope.launch {
            try {
                val response = chatRepository.generateInviteLink(conversationId)
                _inviteLink.value = response.inviteUrl
                _actionResult.value = "Link undangan berhasil dibuat"
            } catch (e: Exception) {
                _actionResult.value = "Gagal membuat link undangan: ${e.message}"
            }
        }
    }

    fun clearActionResult() {
        _actionResult.value = null
    }
}
