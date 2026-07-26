package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.InstructorPortalRepository
import com.mulaisekarang.app.data.model.SaveCourseRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CourseEditorUiState(
    val courseId: Int? = null,
    val title: String = "",
    val description: String = "",
    val regularPrice: String = "0",
    val salePrice: String = "",
    val level: String = "beginner",
    val status: String = "draft",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val savedCourseId: Int? = null,
)

@HiltViewModel
class CourseEditorViewModel @Inject constructor(
    private val repository: InstructorPortalRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CourseEditorUiState())
    val uiState: StateFlow<CourseEditorUiState> = _uiState.asStateFlow()

    /** Load an existing course into the form; pass null/0 to create a new one. */
    fun start(courseId: Int?) {
        if (courseId == null || courseId <= 0 || _uiState.value.courseId == courseId) return
        _uiState.update { it.copy(isLoading = true, courseId = courseId) }
        viewModelScope.launch {
            runCatching { repository.course(courseId) }
                .onSuccess { res ->
                    val c = res.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            title = c.title,
                            description = c.description ?: "",
                            regularPrice = c.regularPrice.toLong().toString(),
                            salePrice = c.salePrice?.toLong()?.toString() ?: "",
                            level = c.level ?: "beginner",
                            status = if (c.type == "bundle") it.status else "draft",
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Gagal memuat kelas.") } }
        }
    }

    fun onTitleChange(v: String) = _uiState.update { it.copy(title = v) }
    fun onDescriptionChange(v: String) = _uiState.update { it.copy(description = v) }
    fun onPriceChange(v: String) = _uiState.update { it.copy(regularPrice = v.filter(Char::isDigit)) }
    fun onSalePriceChange(v: String) = _uiState.update { it.copy(salePrice = v.filter(Char::isDigit)) }
    fun onLevelChange(v: String) = _uiState.update { it.copy(level = v) }
    fun onStatusChange(v: String) = _uiState.update { it.copy(status = v) }

    fun save() {
        val s = _uiState.value
        if (s.title.isBlank()) {
            _uiState.update { it.copy(error = "Judul wajib diisi.") }
            return
        }
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val body = SaveCourseRequest(
                title = s.title.trim(),
                description = s.description.ifBlank { null },
                level = s.level,
                type = "course",
                regularPrice = s.regularPrice.toDoubleOrNull() ?: 0.0,
                salePrice = s.salePrice.toDoubleOrNull(),
                status = s.status,
            )
            val result = if (s.courseId == null) {
                runCatching { repository.createCourse(body) }
            } else {
                runCatching { repository.updateCourse(s.courseId, body) }
            }
            result
                .onSuccess { res -> _uiState.update { it.copy(isSaving = false, savedCourseId = res.data.id) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.message ?: "Gagal menyimpan kelas.") } }
        }
    }
}
