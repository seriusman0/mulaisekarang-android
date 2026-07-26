package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.NotificationRepository
import com.mulaisekarang.app.data.model.AppNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val unreadOnly: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.notifications(unreadOnly = _uiState.value.unreadOnly) }
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            notifications = response.data,
                            unreadCount = response.meta.unreadCount,
                            isLoading = false,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Gagal memuat notifikasi.") }
                }
        }
    }

    fun toggleUnreadOnly() {
        _uiState.update { it.copy(unreadOnly = !it.unreadOnly) }
        load()
    }

    fun markRead(id: String) {
        viewModelScope.launch {
            runCatching { repository.markRead(id) }
                .onSuccess { updated ->
                    _uiState.update { state ->
                        state.copy(
                            notifications = state.notifications.map { if (it.id == updated.id) updated else it },
                            unreadCount = (state.unreadCount - 1).coerceAtLeast(0),
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Gagal menandai notifikasi.") }
                }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            runCatching { repository.markAllRead() }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            notifications = state.notifications.map { it.copy(read = true) },
                            unreadCount = 0,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Gagal menandai semua notifikasi.") }
                }
        }
    }
}
