package com.mulaisekarang.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mulaisekarang.app.ui.components.ErrorState
import com.mulaisekarang.app.ui.components.HtmlText
import com.mulaisekarang.app.ui.components.LessonPlayerSkeleton
import com.mulaisekarang.app.ui.components.VideoPlayer
import com.mulaisekarang.app.viewmodel.LessonPlayerEvent
import com.mulaisekarang.app.viewmodel.LessonPlayerUiState
import com.mulaisekarang.app.viewmodel.LessonPlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonPlayerScreen(
    viewModel: LessonPlayerViewModel,
    onNavigateToLesson: (lessonId: Int) -> Unit,
    onCourseCompleted: () -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val completing by viewModel.completing.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LessonPlayerEvent.NavigateToLesson -> onNavigateToLesson(event.lessonId)
                LessonPlayerEvent.CourseCompleted -> onCourseCompleted()
                is LessonPlayerEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pelajaran") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (uiState is LessonPlayerUiState.Loaded) {
                        val state = uiState as LessonPlayerUiState.Loaded
                        if (state.isDownloaded) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Tersedia Offline",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        } else {
                            Button(onClick = { viewModel.downloadLesson() }, modifier = Modifier.padding(end = 8.dp)) {
                                Text("Unduh")
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (val state = uiState) {
            is LessonPlayerUiState.Loading -> LessonPlayerSkeleton(modifier = Modifier.padding(padding))

            is LessonPlayerUiState.Error -> ErrorState(
                message = state.message,
                modifier = Modifier.padding(padding),
                onRetry = { viewModel.load() },
            )

            is LessonPlayerUiState.Loaded -> {
                val lesson = state.lesson

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    lesson.videoStreamUrl?.let {
                        VideoPlayer(
                            streamUrl = it,
                            authToken = state.authToken,
                            videoCache = viewModel.videoCache,
                            lessonId = lesson.id,
                            startPositionMs = state.startPositionMs,
                            onPositionChanged = { positionMs -> viewModel.savePlaybackPosition(positionMs) },
                            onPlaybackError = { error -> viewModel.reportPlaybackError(error) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (state.startPositionMs > 0L) {
                            Text(
                                "Melanjutkan dari ${formatResumeTimestamp(state.startPositionMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                        Text(
                            lesson.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )

                        val duration = listOfNotNull(
                            lesson.durationHours.takeIf { it > 0 }?.let { "${it}j" },
                            lesson.durationMinutes.takeIf { it > 0 }?.let { "${it}m" },
                            lesson.durationSeconds.takeIf { it > 0 }?.let { "${it}d" },
                        ).joinToString(" ")
                        if (duration.isNotBlank()) {
                            Text(
                                duration,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }

                        if (lesson.isCompleted) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp),
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 6.dp),
                                )
                                Text(
                                    "Pelajaran ini sudah selesai",
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }

                        lesson.content?.takeIf { it.isNotBlank() }?.let {
                            HtmlText(
                                html = it,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 20.dp),
                            )
                        }

                        Button(
                            onClick = { viewModel.markComplete() },
                            enabled = !completing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp),
                        ) {
                            if (completing) {
                                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                            } else {
                                Text("Tandai Selesai & Lanjut")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatResumeTimestamp(positionMs: Long): String {
    val totalSeconds = positionMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
