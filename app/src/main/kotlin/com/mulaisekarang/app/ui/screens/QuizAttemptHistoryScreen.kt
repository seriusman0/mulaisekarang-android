package com.mulaisekarang.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mulaisekarang.app.data.model.AttemptQuestionReview
import com.mulaisekarang.app.data.model.StudentQuizAttempt
import com.mulaisekarang.app.ui.components.EmptyState
import com.mulaisekarang.app.ui.components.ErrorState
import com.mulaisekarang.app.viewmodel.QuizAttemptHistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizAttemptHistoryScreen(
    viewModel: QuizAttemptHistoryViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val selected = uiState.selected

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selected == null) "Riwayat Kuis" else "Detail Percobaan") },
                navigationIcon = {
                    IconButton(
                        onClick = { if (selected == null) onBack() else viewModel.closeAttempt() },
                        modifier = Modifier.testTag("quiz_history_back"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
    ) { padding ->
        if (selected != null) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag("attempt_detail"),
            ) {
                item {
                    Text(
                        "${selected.quizTitle ?: "Kuis"} — nilai ${selected.score ?: 0}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                items(selected.questions, key = { it.id }) { question -> QuestionReviewCard(question) }
            }
            return@Scaffold
        }

        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.load() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                uiState.error != null -> ErrorState(
                    message = uiState.error ?: "Terjadi kesalahan.",
                    onRetry = { viewModel.load() },
                )

                uiState.attempts.isEmpty() && !uiState.isLoading -> EmptyState(
                    message = "Belum ada percobaan kuis.",
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("quiz_attempt_list"),
                ) {
                    items(uiState.attempts, key = { it.id }) { attempt ->
                        QuizAttemptCard(attempt, onClick = { viewModel.openAttempt(attempt.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizAttemptCard(attempt: StudentQuizAttempt, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("quiz_attempt_row"),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                attempt.quizTitle ?: "Kuis",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Nilai ${attempt.score ?: 0}", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (attempt.passed) "Lulus" else "Belum Lulus",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (attempt.passed) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
            TextButton(onClick = onClick, modifier = Modifier.testTag("attempt_detail_${attempt.id}")) {
                Text("Lihat pembahasan")
            }
        }
    }
}

@Composable
private fun QuestionReviewCard(question: AttemptQuestionReview) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(question.question, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Jawaban Anda: ${question.yourAnswer ?: "-"}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 8.dp),
            )

            // Essays carry no machine verdict: is_correct is null by contract.
            when (question.isCorrect) {
                true -> Text(
                    "Benar",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                false -> Text(
                    "Salah — jawaban benar: ${question.correctAnswer ?: "-"}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )

                null -> Text(
                    "Menunggu penilaian instruktur",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
