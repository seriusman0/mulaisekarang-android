package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.CourseRepository
import com.mulaisekarang.app.data.DashboardRepository
import com.mulaisekarang.app.data.model.Category
import com.mulaisekarang.app.data.model.Course
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val totalLearningHours: Double = 0.0,
    val categories: List<Category> = emptyList(),
    val featuredCourses: List<Course> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val courseRepository: CourseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val summary = dashboardRepository.summary()
                val categories = courseRepository.categories()
                val featured = courseRepository.courses(featured = true)
                Triple(summary, categories, featured)
            }.onSuccess { (summary, categories, featured) ->
                _uiState.update {
                    it.copy(
                        totalLearningHours = summary.totalLearningHours,
                        categories = categories,
                        featuredCourses = featured.data,
                        isLoading = false,
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Gagal memuat beranda.") }
            }
        }
    }
}
