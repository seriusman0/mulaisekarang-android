package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.datasource.cache.Cache
import com.mulaisekarang.app.data.LessonRepository
import com.mulaisekarang.app.data.TokenStore
import com.mulaisekarang.app.data.VideoProgressStore
import com.mulaisekarang.app.data.model.LessonDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
    data class Loaded(
        val lesson: LessonDetail,
        val authToken: String?,
        val startPositionMs: Long,
    ) : LessonPlayerUiState
    data class Error(val message: String) : LessonPlayerUiState
}

sealed interface LessonPlayerEvent {
    data class NavigateToLesson(val lessonId: Int) : LessonPlayerEvent
    data object CourseCompleted : LessonPlayerEvent
    data class Error(val message: String) : LessonPlayerEvent
}

@HiltViewModel
class LessonPlayerViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val tokenStore: TokenStore,
    private val videoProgressStore: VideoProgressStore,
    val videoCache: Cache,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val courseId: Int = checkNotNull(savedStateHandle["courseId"])
    private val lessonId: Int = checkNotNull(savedStateHandle["lessonId"])

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
                val startPosition = videoProgressStore.lastPositionMs(lessonId)
                Triple(lesson, token, startPosition)
            }.onSuccess { (lesson, token, startPosition) ->
                _uiState.value = LessonPlayerUiState.Loaded(lesson, token, startPosition)
            }.onFailure { e ->
                _uiState.value = LessonPlayerUiState.Error(e.message ?: "Gagal memuat pelajaran.")
            }
        }
    }

    /** Called periodically and on dispose by the player so progress survives backgrounding. */
    fun savePlaybackPosition(positionMs: Long) {
        viewModelScope.launch { videoProgressStore.savePositionMs(lessonId, positionMs) }
    }

    fun markComplete() {
        if (_completing.value) return
        _completing.update { true }
        viewModelScope.launch {
            runCatching { lessonRepository.completeLesson(courseId, lessonId) }
                .onSuccess { result ->
                    videoProgressStore.clearPosition(lessonId)
                    if (result.nextLessonId != null) {
                        _events.emit(LessonPlayerEvent.NavigateToLesson(result.nextLessonId))
                    } else {
                        _events.emit(LessonPlayerEvent.CourseCompleted)
                    }
                }
                .onFailure { e ->
                    _events.emit(LessonPlayerEvent.Error(e.message ?: "Gagal menandai pelajaran selesai."))
                }
            _completing.update { false }
        }
    }
}
