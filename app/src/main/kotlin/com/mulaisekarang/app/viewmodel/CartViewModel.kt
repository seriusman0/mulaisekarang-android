package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.CartRepository
import com.mulaisekarang.app.data.model.CartPreview
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

data class CartUiState(
    val preview: CartPreview? = null,
    val isLoading: Boolean = false,
    val isCheckingOut: Boolean = false,
    val error: String? = null,
    val voucherInput: String = "",
    val isApplyingVoucher: Boolean = false,
    val voucherError: String? = null,
)

sealed interface CartEvent {
    data class OpenInvoice(val url: String) : CartEvent
    data object EnrolledFree : CartEvent
    data class Error(val message: String) : CartEvent
}

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CartEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CartEvent> = _events

    val courseIds: StateFlow<List<Int>> = cartRepository.courseIds

    init {
        refresh()
    }

    fun refresh() {
        if (cartRepository.courseIds.value.isEmpty()) {
            _uiState.update { it.copy(preview = null, isLoading = false, error = null) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { cartRepository.preview() }
                .onSuccess { preview -> _uiState.update { it.copy(preview = preview, isLoading = false) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.userMessage("Gagal memuat keranjang.")) }
                }
        }
    }

    fun remove(courseId: Int) {
        cartRepository.remove(courseId)
        refresh()
    }

    fun clear() {
        cartRepository.clear()
        refresh()
    }

    fun onVoucherInputChange(text: String) {
        _uiState.update { it.copy(voucherInput = text, voucherError = null) }
    }

    fun applyVoucher() {
        val code = _uiState.value.voucherInput.trim()
        if (code.isBlank()) {
            _uiState.update { it.copy(voucherError = "Masukkan kode voucher terlebih dahulu.") }
            return
        }

        _uiState.update { it.copy(isApplyingVoucher = true, voucherError = null) }
        viewModelScope.launch {
            cartRepository.setVoucherCode(code)
            runCatching { cartRepository.preview() }
                .onSuccess { preview ->
                    _uiState.update {
                        it.copy(
                            preview = preview,
                            isApplyingVoucher = false,
                            voucherError = null,
                        )
                    }
                }
                .onFailure { e ->
                    // Voucher is invalid — roll back the code so future calls don't carry it
                    cartRepository.setVoucherCode(null)
                    _uiState.update {
                        it.copy(
                            isApplyingVoucher = false,
                            voucherError = e.userMessage("Kode voucher tidak valid."),
                        )
                    }
                }
        }
    }

    fun removeVoucher() {
        cartRepository.setVoucherCode(null)
        _uiState.update { it.copy(voucherInput = "", voucherError = null) }
        refresh()
    }

    fun checkout() {
        if (_uiState.value.isCheckingOut) return

        _uiState.update { it.copy(isCheckingOut = true) }
        viewModelScope.launch {
            runCatching { cartRepository.checkout() }
                .onSuccess { result ->
                    _uiState.update { it.copy(isCheckingOut = false) }
                    if (result.status == "completed") {
                        // Everything was free: access is already granted.
                        cartRepository.clear()
                        _events.emit(CartEvent.EnrolledFree)
                    } else {
                        result.invoiceUrl?.let { _events.emit(CartEvent.OpenInvoice(it)) }
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isCheckingOut = false) }
                    _events.emit(CartEvent.Error(e.userMessage("Gagal memproses pembayaran keranjang.")))
                }
        }
    }
}
