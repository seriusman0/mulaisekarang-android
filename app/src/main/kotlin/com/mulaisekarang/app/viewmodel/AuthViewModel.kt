package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.AuthRepository
import com.mulaisekarang.app.data.model.User
import com.mulaisekarang.app.data.network.userMessage
import java.io.File
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Checking : AuthUiState
    data object LoggedOut : AuthUiState
    data object Submitting : AuthUiState
    data class LoggedIn(val user: User) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

sealed interface ProfileUpdateEvent {
    data object Loading : ProfileUpdateEvent
    data object Success : ProfileUpdateEvent
    data class Error(val message: String) : ProfileUpdateEvent
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Checking)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _profileUpdateEvent = MutableSharedFlow<ProfileUpdateEvent>(extraBufferCapacity = 1)
    val profileUpdateEvent: SharedFlow<ProfileUpdateEvent> = _profileUpdateEvent

    fun checkExistingSession() {
        viewModelScope.launch {
            if (repository.hasToken()) {
                runCatching { repository.me() }
                    .onSuccess { _uiState.value = AuthUiState.LoggedIn(it) }
                    .onFailure { _uiState.value = AuthUiState.LoggedOut }
            } else {
                _uiState.value = AuthUiState.LoggedOut
            }
        }
    }

    fun login(email: String, password: String) {
        _uiState.value = AuthUiState.Submitting
        viewModelScope.launch {
            runCatching { repository.login(email, password) }
                .onSuccess { _uiState.value = AuthUiState.LoggedIn(it) }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Login gagal.") }
        }
    }

    fun register(firstName: String, lastName: String, username: String, email: String, password: String) {
        _uiState.value = AuthUiState.Submitting
        viewModelScope.launch {
            runCatching { repository.register(firstName, lastName, username, email, password) }
                .onSuccess { _uiState.value = AuthUiState.LoggedIn(it) }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Registrasi gagal.") }
        }
    }

    fun loginWithGoogle(idToken: String) {
        _uiState.value = AuthUiState.Submitting
        viewModelScope.launch {
            runCatching { repository.loginWithGoogle(idToken) }
                .onSuccess { _uiState.value = AuthUiState.LoggedIn(it) }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Google sign-in gagal.") }
        }
    }

    fun reportError(message: String) {
        _uiState.value = AuthUiState.Error(message)
    }

    fun updateProfile(firstName: String, lastName: String?, username: String?, bio: String?, photoFile: File?) {
        viewModelScope.launch {
            _profileUpdateEvent.emit(ProfileUpdateEvent.Loading)
            runCatching { repository.updateProfile(firstName, lastName, username, bio, photoFile) }
                .onSuccess {
                    _uiState.value = AuthUiState.LoggedIn(it)
                    _profileUpdateEvent.emit(ProfileUpdateEvent.Success)
                }
                .onFailure {
                    _profileUpdateEvent.emit(ProfileUpdateEvent.Error(it.userMessage("Gagal memperbarui profil.")))
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = AuthUiState.LoggedOut
        }
    }
}
