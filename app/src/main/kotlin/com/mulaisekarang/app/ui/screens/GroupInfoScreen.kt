package com.mulaisekarang.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mulaisekarang.app.data.model.Participant
import com.mulaisekarang.app.viewmodel.GroupInfoUiState
import com.mulaisekarang.app.viewmodel.GroupInfoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    viewModel: GroupInfoViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()
    val inviteLink by viewModel.inviteLink.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(actionResult) {
        actionResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionResult()
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var usernameToAdd by remember { mutableStateOf("") }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tambah Anggota") },
            text = {
                OutlinedTextField(
                    value = usernameToAdd,
                    onValueChange = { usernameToAdd = it },
                    label = { Text("Username") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (usernameToAdd.isNotBlank()) {
                        viewModel.addMember(usernameToAdd.trim())
                        showAddDialog = false
                        usernameToAdd = ""
                    }
                }) {
                    Text("Tambah")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Info Grup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is GroupInfoUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is GroupInfoUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is GroupInfoUiState.Loaded -> {
                val conversation = state.conversation
                val currentUserId = state.currentUserId
                val currentUserParticipant = conversation.participants?.find { it.userId == currentUserId }
                val isOwner = currentUserParticipant?.role == "owner"
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Groups,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(50.dp)
                                )
                            }
                            Text(
                                text = conversation.title ?: "Grup",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                            Text(
                                text = "${conversation.participants?.size ?: 0} Anggota",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isOwner) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Button(
                                    onClick = { showAddDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                    Text("Tambah Anggota")
                                }

                                if (inviteLink == null) {
                                    Button(
                                        onClick = { viewModel.generateInviteLink() },
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                        Text("Buat Link Undangan")
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = inviteLink!!,
                                            onValueChange = {},
                                            readOnly = true,
                                            modifier = Modifier.weight(1f),
                                            label = { Text("Link Undangan") }
                                        )
                                        IconButton(onClick = {
                                            clipboardManager.setText(AnnotatedString(inviteLink!!))
                                        }) {
                                            Icon(Icons.Filled.Share, contentDescription = "Salin Link")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Anggota",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    items(conversation.participants ?: emptyList()) { participant ->
                        ListItem(
                            headlineContent = { Text(participant.user?.displayName ?: "User ${participant.userId}") },
                            supportingContent = { Text(participant.role.replaceFirstChar { it.uppercase() }) },
                            leadingContent = {
                                AsyncImage(
                                    model = participant.user?.profilePhotoUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.LightGray)
                                )
                            },
                            trailingContent = {
                                if (isOwner && participant.userId != currentUserId) {
                                    IconButton(onClick = { viewModel.removeMember(participant.userId) }) {
                                        Icon(Icons.Filled.PersonRemove, contentDescription = "Keluarkan", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        )
                    }

                    item {
                        Button(
                            onClick = { viewModel.removeMember(currentUserId) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text("Keluar Grup")
                        }
                    }
                }
            }
        }
    }
}
