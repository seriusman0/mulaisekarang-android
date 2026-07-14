package com.mulaisekarang.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mulaisekarang.app.data.model.ChatMessage
import com.mulaisekarang.app.ui.components.ErrorState
import com.mulaisekarang.app.ui.components.LoadingState
import com.mulaisekarang.app.viewmodel.ChatConversationUiState
import com.mulaisekarang.app.viewmodel.ChatConversationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatConversationScreen(
    viewModel: ChatConversationViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val conversation by viewModel.conversation.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.startPolling()
                Lifecycle.Event.ON_PAUSE -> viewModel.stopPolling()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopPolling()
        }
    }

    var draft by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val current = conversation
                    val isGroup = current?.type == "group"
                    val title = if (isGroup) current?.title ?: "Grup" else current?.otherParty?.displayName ?: "Chat"
                    Column {
                        Text(title)
                        if (isGroup) {
                            Text(
                                "${current?.participants?.size ?: 0} peserta",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                },
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
                .imePadding(),
        ) {
            when (val state = uiState) {
                is ChatConversationUiState.Loading -> LoadingState(modifier = Modifier.weight(1f))

                is ChatConversationUiState.Error -> ErrorState(
                    message = state.message,
                    modifier = Modifier.weight(1f),
                )

                is ChatConversationUiState.Loaded -> {
                    val listState = rememberLazyListState()
                    LaunchedEffect(state.messages.size) {
                        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
                    }
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        items(state.messages, key = { it.id }) { message ->
                            MessageBubble(
                                message,
                                isAiTutor = conversation?.type == "ai" && !message.isMine,
                                showSenderName = conversation?.type == "group" && !message.isMine,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Tulis pesan...") },
                )
                IconButton(onClick = {
                    if (draft.isNotBlank()) {
                        viewModel.sendMessage(draft)
                        draft = ""
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim")
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, isAiTutor: Boolean = false, showSenderName: Boolean = false) {
    val bubbleColor = when {
        message.isMine -> MaterialTheme.colorScheme.primary
        isAiTutor -> MaterialTheme.colorScheme.inverseSurface
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        message.isMine -> MaterialTheme.colorScheme.onPrimary
        isAiTutor -> MaterialTheme.colorScheme.inverseOnSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val alignment = if (message.isMine) Alignment.CenterEnd else Alignment.CenterStart

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (isAiTutor) {
                Text(
                    "AI TUTOR RESPONSE",
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            } else if (showSenderName && message.senderName != null) {
                Text(
                    message.senderName,
                    color = textColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            Text(message.body ?: "", color = textColor, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
