package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.AssignmentRepository
import com.mulaisekarang.app.data.model.AssignmentDetail
import com.mulaisekarang.app.data.network.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssignmentUiState(
    val assignment: AssignmentDetail? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AssignmentViewModel @Inject constructor(
    private val repository: AssignmentRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val assignmentId: Int = checkNotNull(savedStateHandle["assignmentId"])

    private val _uiState = MutableStateFlow(AssignmentUiState())
    val uiState: StateFlow<AssignmentUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.assignmentDetail(assignmentId) }
                .onSuccess { assignment -> _uiState.update { it.copy(assignment = assignment, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Gagal memuat tugas.") } }
        }
    }

    fun submit(repositoryUrl: String?, file: File?) {
        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.submit(assignmentId, repositoryUrl?.takeIf { it.isNotBlank() }, file) }
                .onSuccess { submission ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            submitSuccess = true,
                            assignment = it.assignment?.copy(mySubmission = submission),
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSubmitting = false, error = e.userMessage("Gagal mengirim tugas.")) }
                }
        }
    }
}
