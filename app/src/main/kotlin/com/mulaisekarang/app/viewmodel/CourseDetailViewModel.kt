package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.CartRepository
import com.mulaisekarang.app.data.ChatRepository
import com.mulaisekarang.app.data.CourseRepository
import com.mulaisekarang.app.data.ReviewRepository
import com.mulaisekarang.app.data.model.Conversation
import com.mulaisekarang.app.data.model.CourseDetail
import com.mulaisekarang.app.data.network.isChatPaywall
import com.mulaisekarang.app.data.network.userMessage
import com.mulaisekarang.app.util.CrashReporter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
    data class Message(val text: String) : CheckoutEvent

    /** The mentor chat add-on is not active; the caller should show the paywall. */
    data object ChatPaywall : CheckoutEvent
}

/** Review the signed-in student has left on this course, plus form state. */
data class MyCourseReviewState(
    val rating: Int = 0,
    val body: String = "",
    val isSubmitting: Boolean = false,
    val submitted: Boolean = false,
)

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    private val repository: CourseRepository,
    private val chatRepository: ChatRepository,
    private val reviewRepository: ReviewRepository,
    private val cartRepository: CartRepository,
    private val crashReporter: CrashReporter,
) : ViewModel() {

    private val _reviewState = MutableStateFlow(MyCourseReviewState())
    val reviewState: StateFlow<MyCourseReviewState> = _reviewState.asStateFlow()

    val cartCourseIds: StateFlow<List<Int>> = cartRepository.courseIds

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

    fun checkout(courseBatchId: Int? = null) {
        viewModelScope.launch {
            _checkoutEvent.emit(CheckoutEvent.Loading)
            runCatching { repository.checkout(courseId, courseBatchId) }
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
                .onFailure {
                    crashReporter.logNonFatal("checkout_failed_course_$courseId", it)
                    _checkoutEvent.emit(CheckoutEvent.Error(it.userMessage("Gagal memproses pembayaran.")))
                }
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
                    // A paywall is not an error the student can fix by retrying:
                    // route it to the upsell screen instead of a red snackbar.
                    if (e.isChatPaywall()) {
                        _checkoutEvent.emit(CheckoutEvent.ChatPaywall)
                    } else {
                        _checkoutEvent.emit(CheckoutEvent.Error(e.userMessage("Gagal memulai percakapan dengan mentor.")))
                    }
                }
        }
    }

    // ---- Reviews -----------------------------------------------------------

    fun setReviewRating(rating: Int) = _reviewState.update { it.copy(rating = rating) }

    fun setReviewBody(body: String) = _reviewState.update { it.copy(body = body) }

    fun submitReview() {
        val state = _reviewState.value
        if (state.rating !in 1..5 || state.isSubmitting) return

        _reviewState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            runCatching { reviewRepository.submitReview(courseId, state.rating, state.body) }
                .onSuccess {
                    _reviewState.update { it.copy(isSubmitting = false, submitted = true) }
                    _checkoutEvent.emit(CheckoutEvent.Message("Ulasan Anda tersimpan."))
                    refresh()
                }
                .onFailure { e ->
                    _reviewState.update { it.copy(isSubmitting = false) }
                    _checkoutEvent.emit(CheckoutEvent.Error(e.userMessage("Gagal mengirim ulasan.")))
                }
        }
    }

    // ---- Cart --------------------------------------------------------------

    fun addToCart() {
        viewModelScope.launch {
            val added = cartRepository.add(courseId)
            _checkoutEvent.emit(
                if (added) {
                    CheckoutEvent.Message("Ditambahkan ke keranjang.")
                } else {
                    CheckoutEvent.Error("Keranjang penuh (maksimal ${CartRepository.MAX_ITEMS} kelas).")
                }
            )
        }
    }
}
