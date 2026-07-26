package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.ChatSubscriptionRepository
import com.mulaisekarang.app.data.model.ChatSubscriptionStatus
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

data class ChatPaywallUiState(
    val status: ChatSubscriptionStatus? = null,
    val isLoading: Boolean = false,
    val isWorking: Boolean = false,
    val error: String? = null,
)

sealed interface ChatPaywallEvent {
    data object TrialStarted : ChatPaywallEvent
    data class OpenInvoice(val url: String) : ChatPaywallEvent
    data class Error(val message: String) : ChatPaywallEvent
}

@HiltViewModel
class ChatPaywallViewModel @Inject constructor(
    private val repository: ChatSubscriptionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatPaywallUiState())
    val uiState: StateFlow<ChatPaywallUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ChatPaywallEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ChatPaywallEvent> = _events

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.status() }
                .onSuccess { status -> _uiState.update { it.copy(status = status, isLoading = false) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.userMessage("Gagal memuat status langganan chat.")) }
                }
        }
    }

    fun startTrial() {
        if (_uiState.value.isWorking) return

        _uiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            runCatching { repository.startTrial() }
                .onSuccess {
                    _uiState.update { it.copy(isWorking = false) }
                    _events.emit(ChatPaywallEvent.TrialStarted)
                    load()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isWorking = false) }
                    _events.emit(ChatPaywallEvent.Error(e.userMessage("Gagal memulai trial chat.")))
                }
        }
    }

    fun purchase() {
        if (_uiState.value.isWorking) return

        _uiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            runCatching { repository.purchase() }
                .onSuccess { result ->
                    _uiState.update { it.copy(isWorking = false) }
                    result.invoiceUrl?.let { _events.emit(ChatPaywallEvent.OpenInvoice(it)) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isWorking = false) }
                    _events.emit(ChatPaywallEvent.Error(e.userMessage("Gagal memproses pembelian.")))
                }
        }
    }
}
