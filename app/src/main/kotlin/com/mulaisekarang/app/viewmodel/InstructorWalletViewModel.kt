package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.InstructorPortalRepository
import com.mulaisekarang.app.data.model.CreateWithdrawalRequest
import com.mulaisekarang.app.data.model.WalletSummary
import com.mulaisekarang.app.data.model.Withdrawal
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InstructorWalletUiState(
    val wallet: WalletSummary? = null,
    val withdrawals: List<Withdrawal> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
)

@HiltViewModel
class InstructorWalletViewModel @Inject constructor(
    private val repository: InstructorPortalRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InstructorWalletUiState())
    val uiState: StateFlow<InstructorWalletUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.wallet() }
                .onSuccess { res -> _uiState.update { it.copy(wallet = res.data, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Gagal memuat dompet.") } }

            runCatching { repository.withdrawals() }
                .onSuccess { res -> _uiState.update { it.copy(withdrawals = res.data) } }
                .onFailure { /* non-critical */ }
        }
    }

    fun requestWithdrawal(amount: Double, method: String, accountName: String, accountNumber: String, bankName: String) {
        _uiState.update { it.copy(isSubmitting = true, error = null, successMessage = null) }
        viewModelScope.launch {
            val body = CreateWithdrawalRequest(
                amount = amount,
                method = method,
                accountDetail = buildMap {
                    put("account_name", accountName)
                    put("account_number", accountNumber)
                    if (bankName.isNotBlank()) put("bank_name", bankName)
                },
            )
            runCatching { repository.requestWithdrawal(body) }
                .onSuccess {
                    _uiState.update { it.copy(isSubmitting = false, successMessage = "Permintaan penarikan diajukan.") }
                    load()
                }
                .onFailure { e -> _uiState.update { it.copy(isSubmitting = false, error = e.message ?: "Gagal mengajukan penarikan.") } }
        }
    }

    fun consumeMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
