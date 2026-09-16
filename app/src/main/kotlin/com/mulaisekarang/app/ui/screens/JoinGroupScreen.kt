package com.mulaisekarang.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mulaisekarang.app.viewmodel.JoinGroupUiState
import com.mulaisekarang.app.viewmodel.JoinGroupViewModel

@Composable
fun JoinGroupScreen(
    viewModel: JoinGroupViewModel,
    onSuccess: (Int) -> Unit,
    onError: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is JoinGroupUiState.Success -> onSuccess(state.conversationId)
            is JoinGroupUiState.Error -> {
                // In a real app we might show a dialog before navigating back
                onError()
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (uiState) {
            is JoinGroupUiState.Joining -> {
                CircularProgressIndicator()
            }
            is JoinGroupUiState.Error -> {
                Text(
                    text = (uiState as JoinGroupUiState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {}
        }
    }
}
