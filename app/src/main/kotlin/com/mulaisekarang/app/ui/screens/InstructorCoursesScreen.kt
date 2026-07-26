package com.mulaisekarang.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mulaisekarang.app.viewmodel.InstructorCoursesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructorCoursesScreen(
    viewModel: InstructorCoursesViewModel,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (Int) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelas Saya") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate, modifier = Modifier.testTag("create_course_button")) {
                Icon(Icons.Filled.Add, contentDescription = "Buat Kelas Baru")
            }
        },
    ) { padding ->
        when {
            state.isLoading && state.courses.isEmpty() -> {
                Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(Modifier.padding(40.dp))
                }
            }
            state.courses.isEmpty() -> {
                Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                    Text(state.error ?: "Belum ada kelas. Ketuk + untuk membuat.")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.courses, key = { it.id }) { course ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEdit(course.id) }
                                .testTag("course_row"),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(course.title, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${course.enrollmentsCount} siswa",
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteCourse(course.id) },
                                    modifier = Modifier.testTag("delete_course_${course.id}"),
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
