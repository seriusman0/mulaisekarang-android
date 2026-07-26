package com.mulaisekarang.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.mulaisekarang.app.viewmodel.InstructorGradingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructorGradingScreen(
    viewModel: InstructorGradingViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Penilaian Tugas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading && state.submissions.isEmpty() -> {
                Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(Modifier.padding(40.dp))
                }
            }
            state.submissions.isEmpty() -> {
                Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                    Text(state.error ?: "Belum ada pengumpulan tugas.")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.submissions, key = { it.id }) { sub ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.startGrading(sub.id) }
                                .testTag("submission_row"),
                        ) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(sub.student?.displayName ?: "Siswa", fontWeight = FontWeight.Bold)
                                        Text(sub.assignmentTitle ?: "Tugas", style = MaterialTheme.typography.labelMedium)
                                    }
                                    Text(
                                        if (sub.grade != null) "Nilai: ${sub.grade}" else sub.status,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }

                                if (state.gradingId == sub.id) {
                                    Spacer(Modifier.height(10.dp))
                                    GradeEditor(
                                        initialGrade = sub.grade?.toString() ?: "",
                                        initialFeedback = sub.feedback ?: "",
                                        onCancel = viewModel::cancelGrading,
                                        onSave = { grade, feedback -> viewModel.grade(sub.id, grade, feedback) },
                                    )
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
private fun GradeEditor(
    initialGrade: String,
    initialFeedback: String,
    onCancel: () -> Unit,
    onSave: (Int, String) -> Unit,
) {
    var grade by remember { mutableStateOf(initialGrade) }
    var feedback by remember { mutableStateOf(initialFeedback) }

    Column {
        OutlinedTextField(
            value = grade,
            onValueChange = { grade = it.filter(Char::isDigit).take(3) },
            label = { Text("Nilai (0-100)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("grade_input"),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = feedback,
            onValueChange = { feedback = it },
            label = { Text("Umpan Balik") },
            modifier = Modifier.fillMaxWidth().testTag("grade_feedback_input"),
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Batal") }
            Button(
                onClick = { onSave(grade.toIntOrNull()?.coerceIn(0, 100) ?: 0, feedback) },
                modifier = Modifier.weight(1f).testTag("grade_save_button"),
            ) { Text("Simpan") }
        }
    }
}
