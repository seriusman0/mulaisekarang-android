package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.CourseRepository
import com.mulaisekarang.app.data.model.Category
import com.mulaisekarang.app.data.model.Course
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MarketplaceUiState(
    val courses: List<Course> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategorySlug: String? = null,
    val search: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

class MarketplaceViewModel(private val repository: CourseRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketplaceUiState())
    val uiState: StateFlow<MarketplaceUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        loadCourses()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            runCatching { repository.categories() }
                .onSuccess { categories -> _uiState.update { it.copy(categories = categories) } }
        }
    }

    fun loadCourses() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val state = _uiState.value
            runCatching { repository.courses(search = state.search, category = state.selectedCategorySlug) }
                .onSuccess { response -> _uiState.update { it.copy(courses = response.data, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Gagal memuat course.") } }
        }
    }

    fun onSearchChange(value: String) {
        _uiState.update { it.copy(search = value) }
    }

    fun onCategorySelect(slug: String?) {
        _uiState.update { it.copy(selectedCategorySlug = if (it.selectedCategorySlug == slug) null else slug) }
        loadCourses()
    }
}
