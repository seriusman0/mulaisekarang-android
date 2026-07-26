package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.InstructorPortalRepository
import com.mulaisekarang.app.data.model.Course
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InstructorCoursesUiState(
    val courses: List<Course> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class InstructorCoursesViewModel @Inject constructor(
    private val repository: InstructorPortalRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InstructorCoursesUiState())
    val uiState: StateFlow<InstructorCoursesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.courses() }
                .onSuccess { res -> _uiState.update { it.copy(courses = res.data, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Gagal memuat kelas.") } }
        }
    }

    fun deleteCourse(id: Int) {
        viewModelScope.launch {
            runCatching { repository.deleteCourse(id) }
                .onSuccess { load() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message ?: "Gagal menghapus kelas.") } }
        }
    }
}
