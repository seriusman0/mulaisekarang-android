package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.CourseRepository
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

class PaymentSuccessViewModel(
    private val repository: CourseRepository,
    private val referenceId: String,
) : ViewModel() {

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
