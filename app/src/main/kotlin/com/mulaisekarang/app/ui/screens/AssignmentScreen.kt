package com.mulaisekarang.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mulaisekarang.app.ui.components.ErrorState
import com.mulaisekarang.app.ui.components.HtmlText
import com.mulaisekarang.app.ui.components.LoadingState
import com.mulaisekarang.app.viewmodel.AssignmentViewModel
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentScreen(
    viewModel: AssignmentViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var repositoryUrl by remember { mutableStateOf("") }
    var pickedFileName by remember { mutableStateOf<String?>(null) }
    var pickedFile by remember { mutableStateOf<File?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            val name = "submission_${System.currentTimeMillis()}"
            val file = File(context.cacheDir, name)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            pickedFile = file
            pickedFileName = uri.lastPathSegment ?: name
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.assignment?.title ?: "Tugas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.error != null && uiState.assignment == null -> ErrorState(
                message = uiState.error ?: "Terjadi kesalahan.",
                modifier = Modifier.padding(padding),
                onRetry = { viewModel.load() },
            )

            uiState.assignment == null -> LoadingState(modifier = Modifier.padding(padding))

            else -> {
                val assignment = uiState.assignment!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                ) {
                    Text(
                        "${assignment.totalPoints} Poin",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    HtmlText(
                        html = assignment.instructions,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )

                    if (assignment.mySubmission != null) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column(modifier = Modifier.padding(start = 12.dp)) {
                                    Text("Sudah dikumpulkan", fontWeight = FontWeight.Bold)
                                    assignment.mySubmission.grade?.let {
                                        Text(
                                            "Nilai: $it/${assignment.totalPoints}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (assignment.mySubmission == null || assignment.allowResubmission) {
                        Text(
                            if (assignment.mySubmission == null) "Kumpulkan Tugas" else "Kumpulkan Ulang",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 24.dp),
                        )

                        OutlinedButton(
                            onClick = { filePicker.launch("*/*") },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                        ) {
                            Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text(pickedFileName ?: "Pilih File")
                        }

                        OutlinedTextField(
                            value = repositoryUrl,
                            onValueChange = { repositoryUrl = it },
                            label = { Text("Tautan Repositori (opsional)") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                        )

                        if (uiState.error != null) {
                            Text(
                                uiState.error ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }

                        Button(
                            onClick = { viewModel.submit(repositoryUrl, pickedFile) },
                            enabled = !uiState.isSubmitting && (pickedFile != null || repositoryUrl.isNotBlank()),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp),
                        ) {
                            if (uiState.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(2.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("Kumpulkan")
                            }
                        }
                    }
                }
            }
        }
    }
}
