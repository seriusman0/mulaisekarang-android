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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.AttachFile
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onChatPaywall: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val conversation by viewModel.conversation.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
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
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            viewModel.sendAttachment(uri, context, draft)
            draft = ""
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.sendError.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    LaunchedEffect(viewModel) {
        // The add-on lapsed mid-thread: send the student to the upsell.
        viewModel.paywallRequired.collect { onChatPaywall() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                IconButton(
                    onClick = { launcher.launch("*/*") },
                    modifier = Modifier.padding(end = 4.dp),
                    enabled = !isSending
                ) {
                    Icon(androidx.compose.material.icons.Icons.Filled.AttachFile, contentDescription = "Upload")
                }
                
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(if (isSending) "Mengirim..." else "Tulis pesan...") },
                    enabled = !isSending,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
                
                if (isSending) {
                    if (uploadProgress != null) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(start = 8.dp, end = 8.dp)) {
                            CircularProgressIndicator(
                                progress = { uploadProgress!! / 100f },
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF3498DB),
                                trackColor = Color(0xFFEAEAEA)
                            )
                            Text(
                                text = "${uploadProgress}%",
                                style = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = Color(0xFF3498DB)
                            )
                        }
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(start = 12.dp, end = 12.dp)
                                .size(24.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF3498DB)
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (draft.isNotBlank()) {
                                viewModel.sendMessage(draft)
                                draft = ""
                            }
                        },
                        modifier = Modifier.padding(start = 4.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim", tint = Color(0xFF3498DB))
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, isAiTutor: Boolean = false, showSenderName: Boolean = false) {
    val bubbleColor = when {
        message.isMine -> Color(0xFF3498DB)
        isAiTutor -> MaterialTheme.colorScheme.inverseSurface
        else -> Color(0xFFEAEAEA)
    }
    val textColor = when {
        message.isMine -> Color.White
        isAiTutor -> MaterialTheme.colorScheme.inverseOnSurface
        else -> Color(0xFF1A1A1A)
    }
    val alignment = if (message.isMine) Alignment.CenterEnd else Alignment.CenterStart

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(6.dp))
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
            
            if (message.attachment != null) {
                if (message.attachment.isImage) {
                    AsyncImage(
                        model = message.attachment.url,
                        contentDescription = "Gambar terlampir",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (message.body.isNullOrBlank()) 0.dp else 4.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                } else {
                    Text(
                        "📎 File terlampir",
                        color = textColor,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = if (message.body.isNullOrBlank()) 0.dp else 4.dp)
                    )
                }
            }
            
            if (!message.body.isNullOrBlank()) {
                Text(message.body, color = textColor, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
