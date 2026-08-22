package com.mulaisekarang.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mulaisekarang.app.data.model.Conversation
import com.mulaisekarang.app.ui.components.EmptyState
import com.mulaisekarang.app.ui.components.ErrorState
import com.mulaisekarang.app.viewmodel.ChatListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel,
    onConversationClick: (Int) -> Unit,
    onNewGroupClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val mentorConversations = uiState.conversations.filter { it.type != "ai" }

    LaunchedEffect(Unit) {
        viewModel.openConversationEvent.collect { conversationId -> onConversationClick(conversationId) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat") },
                actions = {
                    IconButton(onClick = onNewGroupClick) {
                        Icon(Icons.Filled.GroupAdd, contentDescription = "Buat Grup Baru")
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.load() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    uiState.error != null -> item {
                        ErrorState(
                            message = uiState.error ?: "Terjadi kesalahan.",
                            onRetry = { viewModel.load() },
                        )
                    }

                    mentorConversations.isEmpty() && !uiState.isLoading -> item {
                        EmptyState(
                            message = "Belum ada percakapan dengan mentor. Mulai chat dari halaman detail course yang sudah Anda ikuti.",
                        )
                    }

                    else -> items(mentorConversations, key = { it.id }) { conversation ->
                        ConversationRow(conversation = conversation, onClick = { onConversationClick(conversation.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(conversation: Conversation, onClick: () -> Unit) {
    val isGroup = conversation.type == "group"

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        leadingContent = {
            if (isGroup) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
                }
            } else {
                AsyncImage(
                    model = conversation.otherParty?.profilePhotoUrl,
                    contentDescription = conversation.otherParty?.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                )
            }
        },
        headlineContent = { Text(conversation.title ?: conversation.otherParty?.displayName ?: "") },
        supportingContent = if (isGroup) {
            { Text("${conversation.participants?.size ?: 0} peserta") }
        } else {
            null
        },
        trailingContent = {
            if (conversation.unreadCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge { Text(conversation.unreadCount.toString()) }
                }
            }
        },
    )
}
