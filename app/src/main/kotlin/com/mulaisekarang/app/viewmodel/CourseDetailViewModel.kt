package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.CourseRepository
import com.mulaisekarang.app.data.model.CourseDetail
import com.mulaisekarang.app.data.network.userMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CourseDetailUiState {
    data object Loading : CourseDetailUiState
    data class Loaded(val course: CourseDetail) : CourseDetailUiState
    data class Error(val message: String) : CourseDetailUiState
}

sealed interface CheckoutEvent {
    data object Loading : CheckoutEvent
    data class OpenInvoice(val url: String) : CheckoutEvent
    data object EnrolledFree : CheckoutEvent
    data class Error(val message: String) : CheckoutEvent
}

class CourseDetailViewModel(private val repository: CourseRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<CourseDetailUiState>(CourseDetailUiState.Loading)
    val uiState: StateFlow<CourseDetailUiState> = _uiState.asStateFlow()

    private val _checkoutEvent = MutableSharedFlow<CheckoutEvent>(extraBufferCapacity = 1)
    val checkoutEvent: SharedFlow<CheckoutEvent> = _checkoutEvent

    private var courseId: Int = 0
    private var pendingReferenceId: String? = null

    fun load(courseId: Int) {
        this.courseId = courseId
        _uiState.value = CourseDetailUiState.Loading
        viewModelScope.launch {
            runCatching { repository.courseDetail(courseId) }
                .onSuccess { _uiState.value = CourseDetailUiState.Loaded(it) }
                .onFailure { _uiState.value = CourseDetailUiState.Error(it.userMessage("Gagal memuat detail course.")) }
        }
    }

    fun checkout() {
        viewModelScope.launch {
            _checkoutEvent.emit(CheckoutEvent.Loading)
            runCatching { repository.checkout(courseId) }
                .onSuccess { response ->
                    if (response.status == "completed") {
                        pendingReferenceId = null
                        _checkoutEvent.emit(CheckoutEvent.EnrolledFree)
                        load(courseId)
                    } else {
                        pendingReferenceId = response.referenceId
                        response.invoiceUrl?.let { _checkoutEvent.emit(CheckoutEvent.OpenInvoice(it)) }
                    }
                }
                .onFailure { _checkoutEvent.emit(CheckoutEvent.Error(it.userMessage("Gagal memproses pembayaran."))) }
        }
    }

    fun checkPendingPayment() {
        val referenceId = pendingReferenceId ?: return
        viewModelScope.launch {
            runCatching { repository.paymentStatus(referenceId) }
                .onSuccess { status ->
                    if (status == "completed") {
                        pendingReferenceId = null
                        load(courseId)
                    }
                }
        }
    }
}
