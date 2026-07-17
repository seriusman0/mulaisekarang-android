package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.CourseRepository
import com.mulaisekarang.app.data.EnrollmentRepository
import com.mulaisekarang.app.data.model.Course
import com.mulaisekarang.app.data.model.Enrollment
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyCoursesUiState(
    val enrollments: List<Enrollment> = emptyList(),
    val recommended: List<Course> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class MyCoursesViewModel @Inject constructor(
    private val repository: EnrollmentRepository,
    private val courseRepository: CourseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyCoursesUiState())
    val uiState: StateFlow<MyCoursesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.myCourses() }
                .onSuccess { response -> _uiState.update { it.copy(enrollments = response.data, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Gagal memuat kursus saya.") } }

            runCatching { courseRepository.courses(featured = true) }
                .onSuccess { response -> _uiState.update { it.copy(recommended = response.data) } }
                .onFailure { /* recommended courses are non-critical; keep list empty, don't disturb the main error state */ }
        }
    }
}
