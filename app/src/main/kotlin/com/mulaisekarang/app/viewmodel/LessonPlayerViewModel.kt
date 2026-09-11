package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.datasource.cache.Cache
import com.mulaisekarang.app.data.LessonRepository
import com.mulaisekarang.app.data.TokenStore
import com.mulaisekarang.app.data.VideoProgressStore
import com.mulaisekarang.app.data.model.LessonDetail
import com.mulaisekarang.app.util.CrashReporter
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
        val courseId: Int,
        val lesson: LessonDetail,
        val authToken: String?,
        val startPositionMs: Long,
        val isDownloaded: Boolean,
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
    private val crashReporter: CrashReporter,
    private val offlineRepository: com.mulaisekarang.app.data.OfflineRepository,
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
            val startPosition = videoProgressStore.lastPositionMs(lessonId)
            val downloaded = offlineRepository.getDownloadedLesson(lessonId)
            val token = runCatching { tokenStore.currentToken() }.getOrNull()

            val lessonResult = runCatching {
                lessonRepository.lessonDetail(courseId, lessonId)
            }.recoverCatching { e ->
                if (downloaded != null) {
                    val manifest = offlineRepository.getCachedCourseManifest(courseId)
                    val lessonManifest = manifest?.course?.topics?.flatMap { it.lessons }?.find { it.id == lessonId }
                    if (lessonManifest != null) {
                        return@recoverCatching LessonDetail(
                            id = lessonManifest.id,
                            title = lessonManifest.title,
                            content = null,
                            durationHours = lessonManifest.durationHours,
                            durationMinutes = lessonManifest.durationMinutes,
                            durationSeconds = lessonManifest.durationSeconds,
                            allowPreview = false,
                            isAccessible = true,
                            isCompleted = lessonManifest.isCompleted,
                            videoStreamUrl = "file://" + downloaded.localPath
                        )
                    }
                }
                throw e
            }

            lessonResult.onSuccess { lesson ->
                val effectiveLesson = if (downloaded != null) {
                    lesson.copy(videoStreamUrl = "file://" + downloaded.localPath)
                } else lesson
                val isDownloaded = downloaded != null
                _uiState.value = LessonPlayerUiState.Loaded(courseId, effectiveLesson, token, startPosition, isDownloaded)
            }.onFailure { e ->
                _uiState.value = LessonPlayerUiState.Error(e.message ?: "Gagal memuat pelajaran.")
            }
        }
    }

    /** Called periodically and on dispose by the player so progress survives backgrounding. */
    fun savePlaybackPosition(positionMs: Long) {
        viewModelScope.launch { 
            videoProgressStore.savePositionMs(lessonId, positionMs)
            val lessonDurationMs = (uiState.value as? LessonPlayerUiState.Loaded)?.lesson?.let { 
                (it.durationHours * 3600 + it.durationMinutes * 60 + it.durationSeconds) * 1000L
            } ?: 0L
            val isCompleted = lessonDurationMs > 0 && positionMs >= lessonDurationMs * 0.9
            offlineRepository.recordProgress(courseId, lessonId, (positionMs / 1000).toInt(), isCompleted)
        }
    }

    fun reportPlaybackError(error: Throwable) {
        crashReporter.logNonFatal("video_playback_error_lesson_$lessonId", error)
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

    fun downloadLesson() {
        offlineRepository.startDownload(courseId, lessonId)
    }
}
