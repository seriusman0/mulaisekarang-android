package com.mulaisekarang.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mulaisekarang.app.viewmodel.ChatPaywallEvent
import com.mulaisekarang.app.viewmodel.ChatPaywallViewModel
import java.text.NumberFormat
import java.util.Locale

/**
 * Upsell for the mentor-chat add-on. Mirrors the web paywall: a one-off trial,
 * then a paid subscription.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPaywallScreen(
    viewModel: ChatPaywallViewModel,
    onBack: () -> Unit,
    onOpenInvoice: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ChatPaywallEvent.TrialStarted ->
                    snackbarHostState.showSnackbar("Trial aktif! Anda kini bisa chat dengan mentor.")

                is ChatPaywallEvent.OpenInvoice -> onOpenInvoice(event.url)
                is ChatPaywallEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat dengan Mentor") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("paywall_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val status = uiState.status

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .testTag("chat_paywall"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Akses Chat Mentor",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Tanya langsung ke instruktur mana pun, kapan saja. " +
                    "Fitur ini adalah add-on berbayar, sama seperti di web.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (status != null && status.hasAccess) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Langganan aktif",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.testTag("paywall_active"),
                        )
                        val until = status.trialEndsAt ?: status.endsAt
                        until?.let {
                            Text(
                                "Berlaku sampai ${it.take(10)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
                return@Column
            }

            val pricing = status?.pricing
            if (pricing != null) {
                Text(
                    "Harga: ${formatRupiah(pricing.standardPrice)} — " +
                        "promo ${formatRupiah(pricing.promoPrice)} untuk ${pricing.promoDays} hari.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (status?.eligibleForTrial == true) {
                Button(
                    onClick = { viewModel.startTrial() },
                    enabled = !uiState.isWorking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("paywall_trial_button"),
                ) {
                    Text("Mulai Trial ${pricing?.trialDays ?: 14} Hari Gratis")
                }
            }

            OutlinedButton(
                onClick = { viewModel.purchase() },
                enabled = !uiState.isWorking,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("paywall_purchase_button"),
            ) {
                Text(if (uiState.isWorking) "Memproses…" else "Beli Langganan")
            }

            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun formatRupiah(amount: Int): String {
    val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    format.maximumFractionDigits = 0
    return format.format(amount)
}
