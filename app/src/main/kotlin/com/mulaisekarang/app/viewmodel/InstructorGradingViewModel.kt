package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.InstructorPortalRepository
import com.mulaisekarang.app.data.model.GradeAssignmentRequest
import com.mulaisekarang.app.data.model.InstructorSubmission
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InstructorGradingUiState(
    val submissions: List<InstructorSubmission> = emptyList(),
    val isLoading: Boolean = false,
    val gradingId: Int? = null,
    val error: String? = null,
)

@HiltViewModel
class InstructorGradingViewModel @Inject constructor(
    private val repository: InstructorPortalRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InstructorGradingUiState())
    val uiState: StateFlow<InstructorGradingUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.assignmentSubmissions() }
                .onSuccess { res -> _uiState.update { it.copy(submissions = res.data, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Gagal memuat pengumpulan.") } }
        }
    }

    fun startGrading(id: Int) = _uiState.update { it.copy(gradingId = id) }
    fun cancelGrading() = _uiState.update { it.copy(gradingId = null) }

    fun grade(submissionId: Int, grade: Int, feedback: String) {
        viewModelScope.launch {
            runCatching { repository.gradeAssignment(submissionId, GradeAssignmentRequest(grade, feedback.ifBlank { null })) }
                .onSuccess {
                    _uiState.update { it.copy(gradingId = null) }
                    load()
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message ?: "Gagal menilai.") } }
        }
    }
}
