package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.ChatRepository
import com.mulaisekarang.app.data.CourseRepository
import com.mulaisekarang.app.data.model.Conversation
import com.mulaisekarang.app.data.model.CourseDetail
import com.mulaisekarang.app.data.network.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CourseDetailUiState {
    data object Loading : CourseDetailUiState
    data class Loaded(val course: CourseDetail, val isSyncing: Boolean = false) : CourseDetailUiState
    data class Error(val message: String) : CourseDetailUiState
}

sealed interface CheckoutEvent {
    data object Loading : CheckoutEvent
    data class OpenInvoice(val url: String) : CheckoutEvent
    data object EnrolledFree : CheckoutEvent
    data class Error(val message: String) : CheckoutEvent
}

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    private val repository: CourseRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val cachedCourse = MutableStateFlow<CourseDetail?>(null)
    private val isSyncing = MutableStateFlow(false)
    private val syncError = MutableStateFlow<String?>(null)
    private var observeJob: Job? = null

    /**
     * Instant paint from Room (cachedCourse), background-sync progress and errors layered
     * on top — a sync failure only surfaces as an Error state when there's no cached data
     * to fall back on, so a stale screen never gets yanked to an error view (PRD 3.2).
     */
    val uiState: StateFlow<CourseDetailUiState> = combine(cachedCourse, isSyncing, syncError) { course, syncing, error ->
        when {
            course != null -> CourseDetailUiState.Loaded(course, isSyncing = syncing)
            error != null -> CourseDetailUiState.Error(error)
            else -> CourseDetailUiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CourseDetailUiState.Loading)

    private val _checkoutEvent = MutableSharedFlow<CheckoutEvent>(extraBufferCapacity = 1)
    val checkoutEvent: SharedFlow<CheckoutEvent> = _checkoutEvent

    private var courseId: Int = 0
    private var pendingReferenceId: String? = null

    fun load(courseId: Int) {
        this.courseId = courseId
        syncError.value = null
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repository.courseDetail(courseId).collect { cachedCourse.value = it }
        }
        refresh()
    }

    fun refresh() {
        val id = courseId
        viewModelScope.launch {
            isSyncing.value = true
            runCatching { repository.refreshCourseDetail(id) }
                .onFailure { e -> syncError.value = e.userMessage("Gagal memuat detail course.") }
            isSyncing.value = false
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
                        refresh()
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
                        refresh()
                    }
                }
                .onFailure { e ->
                    _checkoutEvent.emit(CheckoutEvent.Error(e.userMessage("Gagal memeriksa status pembayaran.")))
                }
        }
    }

    fun startConversationWithMentor(username: String, onResult: (Conversation?) -> Unit) {
        viewModelScope.launch {
            runCatching { chatRepository.startConversation(username) }
                .onSuccess { onResult(it) }
                .onFailure { e ->
                    onResult(null)
                    _checkoutEvent.emit(CheckoutEvent.Error(e.userMessage("Gagal memulai percakapan dengan mentor.")))
                }
        }
    }
}
