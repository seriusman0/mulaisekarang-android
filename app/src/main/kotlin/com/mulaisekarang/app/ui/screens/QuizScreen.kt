package com.mulaisekarang.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mulaisekarang.app.ui.components.ErrorState
import com.mulaisekarang.app.ui.components.LoadingState
import com.mulaisekarang.app.viewmodel.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: QuizViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.quiz?.title ?: "Quiz") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.error != null && uiState.quiz == null -> ErrorState(
                message = uiState.error ?: "Terjadi kesalahan.",
                modifier = Modifier.padding(padding),
                onRetry = { viewModel.load() },
            )

            uiState.quiz == null -> LoadingState(modifier = Modifier.padding(padding))

            uiState.result != null -> QuizResultView(
                score = uiState.result!!.score,
                passed = uiState.result!!.passed,
                correctCount = uiState.result!!.correctCount,
                totalCount = uiState.result!!.totalCount,
                onDone = onBack,
                modifier = Modifier.padding(padding),
            )

            else -> {
                val quiz = uiState.quiz!!
                val question = quiz.questions.getOrNull(uiState.currentIndex)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(20.dp),
                ) {
                    Text(
                        "Pertanyaan ${uiState.currentIndex + 1} dari ${quiz.questions.size}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    LinearProgressIndicator(
                        progress = { (uiState.currentIndex + 1) / quiz.questions.size.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 24.dp),
                    )

                    if (question != null) {
                        Text(
                            question.question,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )

                        Column(modifier = Modifier.padding(top = 20.dp)) {
                            if (question.type == "essay") {
                                OutlinedTextField(
                                    value = uiState.answers[question.id] ?: "",
                                    onValueChange = { viewModel.selectAnswer(question.id, it) },
                                    label = { Text("Jawaban Anda") },
                                    minLines = 3,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                question.options.forEachIndexed { index, option ->
                                    if (option.isBlank()) return@forEachIndexed
                                    val optionValue = index.toString()
                                    val selected = uiState.answers[question.id] == optionValue
                                    Surface(
                                        onClick = { viewModel.selectAnswer(question.id, optionValue) },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                            .selectable(selected = selected, onClick = { viewModel.selectAnswer(question.id, optionValue) }),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                        ) {
                                            RadioButton(selected = selected, onClick = { viewModel.selectAnswer(question.id, optionValue) })
                                            Text(option, modifier = Modifier.padding(start = 4.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.error != null) {
                        Text(
                            uiState.error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        if (uiState.currentIndex > 0) {
                            Button(
                                onClick = { viewModel.previousQuestion() },
                                shape = RoundedCornerShape(20.dp),
                            ) { Text("Sebelumnya") }
                        }

                        if (uiState.currentIndex < quiz.questions.size - 1) {
                            Button(
                                onClick = { viewModel.nextQuestion() },
                                shape = RoundedCornerShape(20.dp),
                            ) { Text("Selanjutnya") }
                        } else {
                            Button(
                                onClick = { viewModel.submit() },
                                enabled = !uiState.isSubmitting,
                                shape = RoundedCornerShape(20.dp),
                            ) {
                                if (uiState.isSubmitting) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Kirim Jawaban")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizResultView(
    score: Double,
    passed: Boolean,
    correctCount: Int,
    totalCount: Int,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = if (passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp),
        )
        Text(
            if (passed) "Selamat, Anda Lulus!" else "Belum Lulus",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            "Skor: $score ($correctCount/$totalCount benar)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(
            onClick = onDone,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
        ) {
            Text("Selesai")
        }
    }
}
