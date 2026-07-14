package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.QuizRepository
import com.mulaisekarang.app.data.model.QuizDetail
import com.mulaisekarang.app.data.model.QuizResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuizUiState(
    val quiz: QuizDetail? = null,
    val currentIndex: Int = 0,
    val answers: Map<Int, String> = emptyMap(),
    val result: QuizResult? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
)

class QuizViewModel(
    private val repository: QuizRepository,
    private val quizId: Int,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.quizDetail(quizId) }
                .onSuccess { quiz -> _uiState.update { it.copy(quiz = quiz, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Gagal memuat kuis.") } }
        }
    }

    fun selectAnswer(questionId: Int, answer: String) {
        _uiState.update { it.copy(answers = it.answers + (questionId to answer)) }
    }

    fun nextQuestion() {
        val total = _uiState.value.quiz?.questions?.size ?: 0
        _uiState.update { it.copy(currentIndex = (it.currentIndex + 1).coerceAtMost((total - 1).coerceAtLeast(0))) }
    }

    fun previousQuestion() {
        _uiState.update { it.copy(currentIndex = (it.currentIndex - 1).coerceAtLeast(0)) }
    }

    fun submit() {
        val answers = _uiState.value.answers
        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.submitAttempt(quizId, answers) }
                .onSuccess { result -> _uiState.update { it.copy(result = result, isSubmitting = false) } }
                .onFailure { e -> _uiState.update { it.copy(isSubmitting = false, error = e.message ?: "Gagal mengirim jawaban.") } }
        }
    }
}
