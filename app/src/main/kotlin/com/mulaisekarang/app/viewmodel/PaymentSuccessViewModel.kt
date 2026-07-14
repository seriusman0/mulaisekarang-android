package com.mulaisekarang.app.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PaymentVerifyState {
    data object Verifying : PaymentVerifyState
    data object Completed : PaymentVerifyState
    data object Pending : PaymentVerifyState
    data object Failed : PaymentVerifyState
    data class Error(val message: String) : PaymentVerifyState
}

@HiltViewModel
class PaymentSuccessViewModel @Inject constructor(
    private val repository: CourseRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val referenceId: String = Uri.decode(checkNotNull(savedStateHandle["referenceId"]))

    private val _uiState = MutableStateFlow<PaymentVerifyState>(PaymentVerifyState.Verifying)
    val uiState: StateFlow<PaymentVerifyState> = _uiState.asStateFlow()

    init {
        verify()
    }

    fun verify() {
        _uiState.value = PaymentVerifyState.Verifying
        viewModelScope.launch {
            runCatching { repository.paymentStatus(referenceId) }
                .onSuccess { status ->
                    _uiState.value = when (status) {
                        "completed" -> PaymentVerifyState.Completed
                        "failed", "expired" -> PaymentVerifyState.Failed
                        else -> PaymentVerifyState.Pending
                    }
                }
                .onFailure { e -> _uiState.value = PaymentVerifyState.Error(e.message ?: "Gagal memverifikasi status pembayaran.") }
        }
    }
}
