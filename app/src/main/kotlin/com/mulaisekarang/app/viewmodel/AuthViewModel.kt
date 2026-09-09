package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.AuthRepository
import com.mulaisekarang.app.data.model.User
import com.mulaisekarang.app.data.network.userMessage
import com.mulaisekarang.app.util.CrashReporter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
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

sealed interface ChangePasswordEvent {
    data object Loading : ChangePasswordEvent
    data object Success : ChangePasswordEvent
    data class Error(val message: String) : ChangePasswordEvent
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val crashReporter: CrashReporter,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Checking)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private fun setLoggedIn(user: User) {
        _uiState.value = AuthUiState.LoggedIn(user)
        crashReporter.setUser(user)
    }

    private val _profileUpdateEvent = MutableSharedFlow<ProfileUpdateEvent>(extraBufferCapacity = 1)
    val profileUpdateEvent: SharedFlow<ProfileUpdateEvent> = _profileUpdateEvent

    private val _changePasswordEvent = MutableSharedFlow<ChangePasswordEvent>(extraBufferCapacity = 1)
    val changePasswordEvent: SharedFlow<ChangePasswordEvent> = _changePasswordEvent

    fun checkExistingSession() {
        viewModelScope.launch {
            if (repository.hasToken()) {
                runCatching { repository.me() }
                    .onSuccess { setLoggedIn(it) }
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
                .onSuccess { setLoggedIn(it) }
                .onFailure {
                    crashReporter.logNonFatal("login_failed", it)
                    _uiState.value = AuthUiState.Error(it.userMessage("Login gagal."))
                }
        }
    }

    fun register(firstName: String, lastName: String, username: String, email: String, password: String) {
        _uiState.value = AuthUiState.Submitting
        viewModelScope.launch {
            runCatching { repository.register(firstName, lastName, username, email, password) }
                .onSuccess { setLoggedIn(it) }
                .onFailure { _uiState.value = AuthUiState.Error(it.userMessage("Registrasi gagal.")) }
        }
    }

    fun loginWithGoogle(idToken: String) {
        _uiState.value = AuthUiState.Submitting
        viewModelScope.launch {
            runCatching { repository.loginWithGoogle(idToken) }
                .onSuccess { setLoggedIn(it) }
                .onFailure { _uiState.value = AuthUiState.Error(it.userMessage("Google sign-in gagal.")) }
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
                    setLoggedIn(it)
                    _profileUpdateEvent.emit(ProfileUpdateEvent.Success)
                }
                .onFailure {
                    _profileUpdateEvent.emit(ProfileUpdateEvent.Error(it.userMessage("Gagal memperbarui profil.")))
                }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _changePasswordEvent.emit(ChangePasswordEvent.Loading)
            runCatching { repository.changePassword(currentPassword, newPassword) }
                .onSuccess { _changePasswordEvent.emit(ChangePasswordEvent.Success) }
                .onFailure {
                    _changePasswordEvent.emit(ChangePasswordEvent.Error(it.userMessage("Gagal mengubah password.")))
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            crashReporter.clearUser()
            _uiState.value = AuthUiState.LoggedOut
        }
    }
}
