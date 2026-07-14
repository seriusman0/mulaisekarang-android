package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.ChatRepository
import com.mulaisekarang.app.data.model.Mentor
import com.mulaisekarang.app.data.network.userMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewGroupUiState(
    val contacts: List<Mentor> = emptyList(),
    val selectedUsernames: Set<String> = emptySet(),
    val title: String = "",
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val error: String? = null,
)

class NewGroupViewModel(private val repository: ChatRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(NewGroupUiState())
    val uiState: StateFlow<NewGroupUiState> = _uiState.asStateFlow()

    private val _createdEvent = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val createdEvent: SharedFlow<Int> = _createdEvent

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.eligibleContacts() }
                .onSuccess { contacts -> _uiState.update { it.copy(contacts = contacts, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.userMessage("Gagal memuat daftar kontak.")) } }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value) }
    }

    fun toggleContact(username: String) {
        _uiState.update {
            val selected = it.selectedUsernames
            it.copy(selectedUsernames = if (username in selected) selected - username else selected + username)
        }
    }

    fun createGroup() {
        val state = _uiState.value
        if (state.title.isBlank() || state.selectedUsernames.isEmpty()) return
        _uiState.update { it.copy(isCreating = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.createGroup(state.title, state.selectedUsernames.toList()) }
                .onSuccess { conversation ->
                    _uiState.update { it.copy(isCreating = false) }
                    _createdEvent.emit(conversation.id)
                }
                .onFailure { e -> _uiState.update { it.copy(isCreating = false, error = e.userMessage("Gagal membuat grup.")) } }
        }
    }
}
