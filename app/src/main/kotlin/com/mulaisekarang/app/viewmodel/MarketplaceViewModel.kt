package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.mulaisekarang.app.data.CourseRepository
import com.mulaisekarang.app.data.model.Category
import com.mulaisekarang.app.data.model.Course
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MarketplaceFilterState(
    val categories: List<Category> = emptyList(),
    val selectedCategorySlug: String? = null,
    val search: String = "",
)

@HiltViewModel
class MarketplaceViewModel @Inject constructor(private val repository: CourseRepository) : ViewModel() {

    private val _filterState = MutableStateFlow(MarketplaceFilterState())
    val filterState: StateFlow<MarketplaceFilterState> = _filterState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedCourses: Flow<PagingData<Course>> = _filterState
        .map { it.search to it.selectedCategorySlug }
        .distinctUntilChanged()
        .flatMapLatest { (search, category) ->
            if (search.isBlank() && category == null) {
                repository.pagedCourses()
            } else {
                repository.pagedCoursesFiltered(search = search, category = category, sort = null)
            }
        }
        .cachedIn(viewModelScope)

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            runCatching { repository.categories() }
                .onSuccess { categories -> _filterState.update { it.copy(categories = categories) } }
        }
    }

    fun onSearchChange(value: String) {
        _filterState.update { it.copy(search = value) }
    }

    fun onCategorySelect(slug: String?) {
        _filterState.update {
            it.copy(selectedCategorySlug = if (it.selectedCategorySlug == slug) null else slug)
        }
    }
}
