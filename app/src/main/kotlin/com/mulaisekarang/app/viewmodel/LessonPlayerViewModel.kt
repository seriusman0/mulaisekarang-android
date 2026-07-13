package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.LessonRepository
import com.mulaisekarang.app.data.TokenStore
import com.mulaisekarang.app.data.model.LessonDetail
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface LessonPlayerUiState {
    data object Loading : LessonPlayerUiState
    data class Loaded(val lesson: LessonDetail, val authToken: String?) : LessonPlayerUiState
    data class Error(val message: String) : LessonPlayerUiState
}

sealed interface LessonPlayerEvent {
    data class NavigateToLesson(val lessonId: Int) : LessonPlayerEvent
    data object CourseCompleted : LessonPlayerEvent
}

class LessonPlayerViewModel(
    private val lessonRepository: LessonRepository,
    private val tokenStore: TokenStore,
    private val courseId: Int,
    private val lessonId: Int,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LessonPlayerUiState>(LessonPlayerUiState.Loading)
    val uiState: StateFlow<LessonPlayerUiState> = _uiState.asStateFlow()

    private val _completing = MutableStateFlow(false)
    val completing: StateFlow<Boolean> = _completing.asStateFlow()

    private val _events = MutableSharedFlow<LessonPlayerEvent>()
    val events: SharedFlow<LessonPlayerEvent> = _events.asSharedFlow()

    fun load() {
        _uiState.value = LessonPlayerUiState.Loading
        viewModelScope.launch {
            runCatching {
                val token = tokenStore.currentToken()
                val lesson = lessonRepository.lessonDetail(courseId, lessonId)
                lesson to token
            }.onSuccess { (lesson, token) ->
                _uiState.value = LessonPlayerUiState.Loaded(lesson, token)
            }.onFailure { e ->
                _uiState.value = LessonPlayerUiState.Error(e.message ?: "Gagal memuat pelajaran.")
            }
        }
    }

    fun markComplete() {
        if (_completing.value) return
        _completing.update { true }
        viewModelScope.launch {
            runCatching { lessonRepository.completeLesson(courseId, lessonId) }
                .onSuccess { result ->
                    if (result.nextLessonId != null) {
                        _events.emit(LessonPlayerEvent.NavigateToLesson(result.nextLessonId))
                    } else {
                        _events.emit(LessonPlayerEvent.CourseCompleted)
                    }
                }
            _completing.update { false }
        }
    }
}
