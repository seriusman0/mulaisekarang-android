package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.InstructorPortalRepository
import com.mulaisekarang.app.data.model.InstructorDashboard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InstructorDashboardUiState(
    val dashboard: InstructorDashboard? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class InstructorDashboardViewModel @Inject constructor(
    private val repository: InstructorPortalRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InstructorDashboardUiState())
    val uiState: StateFlow<InstructorDashboardUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.dashboard() }
                .onSuccess { res -> _uiState.update { it.copy(dashboard = res.data, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Gagal memuat dasbor.") } }
        }
    }
}
