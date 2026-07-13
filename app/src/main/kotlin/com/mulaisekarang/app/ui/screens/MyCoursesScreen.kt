package com.mulaisekarang.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mulaisekarang.app.ui.components.CourseCard
import com.mulaisekarang.app.ui.components.EmptyState
import com.mulaisekarang.app.ui.components.ErrorState
import com.mulaisekarang.app.viewmodel.MyCoursesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCoursesScreen(
    viewModel: MyCoursesViewModel,
    onCourseClick: (Int) -> Unit,
    onBrowseMarketplace: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Kursus Saya") }) },
    ) { padding ->
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

                uiState.enrollments.isEmpty() && !uiState.isLoading -> EmptyState(
                    message = "Anda belum terdaftar di course apa pun.",
                    action = {
                        Button(onClick = onBrowseMarketplace) {
                            Text("Jelajahi Marketplace")
                        }
                    },
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(uiState.enrollments, key = { it.id }) { enrollment ->
                        CourseCard(
                            title = enrollment.course.title,
                            coverImageUrl = enrollment.course.coverImageUrl,
                            mentorName = enrollment.course.mentor?.displayName,
                            onClick = { onCourseClick(enrollment.course.id) },
                        ) {
                            LinearProgressIndicator(
                                progress = { enrollment.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                            )
                            Text(
                                "${enrollment.progressPercent}% selesai",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
