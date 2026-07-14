package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.AuthRepository
import com.mulaisekarang.app.data.network.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ForgotPasswordUiState {
    data object Idle : ForgotPasswordUiState
    data object Submitting : ForgotPasswordUiState
    data class Sent(val message: String) : ForgotPasswordUiState
    data class Error(val message: String) : ForgotPasswordUiState
}

class ForgotPasswordViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun submit(email: String) {
        _uiState.value = ForgotPasswordUiState.Submitting
        viewModelScope.launch {
            runCatching { repository.forgotPassword(email) }
                .onSuccess { _uiState.value = ForgotPasswordUiState.Sent(it) }
                .onFailure { _uiState.value = ForgotPasswordUiState.Error(it.userMessage("Gagal mengirim kode reset.")) }
        }
    }
}
