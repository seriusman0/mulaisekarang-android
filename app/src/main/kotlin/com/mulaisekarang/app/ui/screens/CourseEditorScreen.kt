package com.mulaisekarang.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.mulaisekarang.app.viewmodel.CourseEditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEditorScreen(
    viewModel: CourseEditorViewModel,
    courseId: Int?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(courseId) { viewModel.start(courseId) }
    LaunchedEffect(state.savedCourseId) {
        if (state.savedCourseId != null) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (courseId == null) "Buat Kelas" else "Ubah Kelas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Judul Kelas") },
                modifier = Modifier.fillMaxWidth().testTag("course_title_input"),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Deskripsi") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().testTag("course_description_input"),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.regularPrice,
                onValueChange = viewModel::onPriceChange,
                label = { Text("Harga (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("course_price_input"),
            )
            Spacer(Modifier.height(14.dp))

            Text("Level", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Row {
                listOf("beginner", "intermediate", "advanced").forEach { lvl ->
                    FilterChip(
                        selected = state.level == lvl,
                        onClick = { viewModel.onLevelChange(lvl) },
                        label = { Text(lvl) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            Text("Status", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Row {
                listOf("draft", "published").forEach { st ->
                    FilterChip(
                        selected = state.status == st,
                        onClick = { viewModel.onStatusChange(st) },
                        label = { Text(st) },
                        modifier = Modifier.padding(end = 8.dp).testTag("status_$st"),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().testTag("course_save_button"),
            ) {
                Text(if (state.isSaving) "Menyimpan..." else "Simpan Kelas")
            }
        }
    }
}
