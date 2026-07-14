package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.InstructorRepository
import com.mulaisekarang.app.data.model.Course
import com.mulaisekarang.app.data.model.InstructorDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InstructorProfileUiState(
    val instructor: InstructorDetail? = null,
    val courses: List<Course> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class InstructorProfileViewModel(
    private val repository: InstructorRepository,
    private val username: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InstructorProfileUiState())
    val uiState: StateFlow<InstructorProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.instructorDetail(username) }
                .onSuccess { response ->
                    _uiState.update { it.copy(instructor = response.data, courses = response.courses, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Gagal memuat profil instruktur.") }
                }
        }
    }
}
