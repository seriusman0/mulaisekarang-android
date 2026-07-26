package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.StudentHistoryRepository
import com.mulaisekarang.app.data.model.StudentQuizAttempt
import com.mulaisekarang.app.data.model.StudentQuizAttemptDetail
import com.mulaisekarang.app.data.model.StudentSubmission
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuizAttemptHistoryUiState(
    val attempts: List<StudentQuizAttempt> = emptyList(),
    val selected: StudentQuizAttemptDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class QuizAttemptHistoryViewModel @Inject constructor(
    private val repository: StudentHistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizAttemptHistoryUiState())
    val uiState: StateFlow<QuizAttemptHistoryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.quizAttempts() }
                .onSuccess { response -> _uiState.update { it.copy(attempts = response.data, isLoading = false) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Gagal memuat riwayat kuis.") }
                }
        }
    }

    fun openAttempt(attemptId: Int) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.quizAttempt(attemptId) }
                .onSuccess { detail -> _uiState.update { it.copy(selected = detail, isLoading = false) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Gagal memuat detail percobaan.") }
                }
        }
    }

    fun closeAttempt() = _uiState.update { it.copy(selected = null) }
}

data class SubmissionHistoryUiState(
    val submissions: List<StudentSubmission> = emptyList(),
    val statusFilter: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SubmissionHistoryViewModel @Inject constructor(
    private val repository: StudentHistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubmissionHistoryUiState())
    val uiState: StateFlow<SubmissionHistoryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun setStatusFilter(status: String?) {
        _uiState.update { it.copy(statusFilter = status) }
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.submissions(status = _uiState.value.statusFilter) }
                .onSuccess { response -> _uiState.update { it.copy(submissions = response.data, isLoading = false) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Gagal memuat riwayat tugas.") }
                }
        }
    }
}
