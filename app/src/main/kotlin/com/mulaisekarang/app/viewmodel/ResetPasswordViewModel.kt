package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.AuthRepository
import com.mulaisekarang.app.data.network.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ResetPasswordUiState {
    data object Idle : ResetPasswordUiState
    data object Submitting : ResetPasswordUiState
    data object Success : ResetPasswordUiState
    data class Error(val message: String) : ResetPasswordUiState
}

class ResetPasswordViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ResetPasswordUiState>(ResetPasswordUiState.Idle)
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    fun submit(email: String, token: String, password: String) {
        _uiState.value = ResetPasswordUiState.Submitting
        viewModelScope.launch {
            runCatching { repository.resetPassword(email, token, password) }
                .onSuccess { _uiState.value = ResetPasswordUiState.Success }
                .onFailure { _uiState.value = ResetPasswordUiState.Error(it.userMessage("Kode reset tidak valid atau sudah kedaluwarsa.")) }
        }
    }
}
