package com.mulaisekarang.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mulaisekarang.app.data.model.Mentor
import com.mulaisekarang.app.ui.components.EmptyState
import com.mulaisekarang.app.viewmodel.NewGroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGroupScreen(
    viewModel: NewGroupViewModel,
    onBack: () -> Unit,
    onGroupCreated: (Int) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.createdEvent.collect { conversationId -> onGroupCreated(conversationId) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grup Baru") },
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Nama Grup") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                "Pilih Peserta",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
            )

            if (uiState.contacts.isEmpty() && !uiState.isLoading) {
                EmptyState(
                    message = "Belum ada kontak yang bisa diajak. Mulai chat dengan mentor atau ikuti kursus untuk menemukan teman sekelas.",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(uiState.contacts, key = { it.id }) { contact ->
                        ContactRow(
                            contact = contact,
                            selected = contact.username in uiState.selectedUsernames,
                            onToggle = { contact.username?.let { viewModel.toggleContact(it) } },
                        )
                    }
                }
            }

            if (uiState.error != null) {
                Text(
                    uiState.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Button(
                onClick = { viewModel.createGroup() },
                enabled = !uiState.isCreating && uiState.title.isNotBlank() && uiState.selectedUsernames.isNotEmpty(),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text("Buat Grup")
            }
        }
    }
}

@Composable
private fun ContactRow(contact: Mentor, selected: Boolean, onToggle: () -> Unit) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onToggle),
        leadingContent = {
            AsyncImage(
                model = contact.profilePhotoUrl,
                contentDescription = contact.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
            )
        },
        headlineContent = { Text(contact.displayName) },
        trailingContent = { Checkbox(checked = selected, onCheckedChange = { onToggle() }) },
    )
}
