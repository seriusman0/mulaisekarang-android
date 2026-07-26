package com.mulaisekarang.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mulaisekarang.app.data.model.CartIssue
import com.mulaisekarang.app.data.model.CartLine
import com.mulaisekarang.app.ui.components.EmptyState
import com.mulaisekarang.app.ui.components.ErrorState
import com.mulaisekarang.app.viewmodel.CartEvent
import com.mulaisekarang.app.viewmodel.CartViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    viewModel: CartViewModel,
    onBack: () -> Unit,
    onOpenInvoice: (String) -> Unit,
    onEnrolled: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CartEvent.OpenInvoice -> onOpenInvoice(event.url)
                is CartEvent.EnrolledFree -> onEnrolled()
                is CartEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keranjang") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("cart_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val preview = uiState.preview

        when {
            uiState.error != null -> ErrorState(
                message = uiState.error ?: "Terjadi kesalahan.",
                onRetry = { viewModel.refresh() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )

            preview == null || (preview.items.isEmpty() && preview.issues.isEmpty()) -> EmptyState(
                message = "Keranjang Anda kosong.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cart_list"),
                ) {
                    items(preview.items, key = { it.courseId }) { line ->
                        CartRow(line, onRemove = { viewModel.remove(line.courseId) })
                    }

                    // Server-side problems are shown inline rather than blocking
                    // checkout: the rest of the cart is still purchasable.
                    items(preview.issues, key = { "issue-${it.courseId}" }) { issue -> CartIssueRow(issue) }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Total", style = MaterialTheme.typography.titleMedium)
                        Text(
                            formatRupiah(preview.total),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("cart_total"),
                        )
                    }
                    Button(
                        onClick = { viewModel.checkout() },
                        enabled = preview.items.isNotEmpty() && !uiState.isCheckingOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .testTag("cart_checkout_button"),
                    ) {
                        Text(if (uiState.isCheckingOut) "Memproses…" else "Bayar Sekarang")
                    }
                }
            }
        }
    }
}

@Composable
private fun CartRow(line: CartLine, onRemove: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cart_row"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(line.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    formatRupiah(line.price),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.testTag("cart_remove_${line.courseId}")) {
                Icon(Icons.Filled.Delete, contentDescription = "Hapus dari keranjang")
            }
        }
    }
}

@Composable
private fun CartIssueRow(issue: CartIssue) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cart_issue"),
    ) {
        Text(
            when (issue.reason) {
                "already_owned" -> "Kelas #${issue.courseId} sudah Anda miliki."
                else -> "Kelas #${issue.courseId} tidak tersedia."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp),
        )
    }
}

private fun formatRupiah(amount: Int): String {
    val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    format.maximumFractionDigits = 0
    return format.format(amount)
}
