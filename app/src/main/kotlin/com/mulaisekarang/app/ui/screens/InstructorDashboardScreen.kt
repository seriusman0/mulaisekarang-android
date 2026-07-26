package com.mulaisekarang.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Grading
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mulaisekarang.app.viewmodel.InstructorDashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructorDashboardScreen(
    viewModel: InstructorDashboardViewModel,
    onBack: () -> Unit,
    onCourses: () -> Unit,
    onWallet: () -> Unit,
    onGrading: () -> Unit,
    onChat: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Portal Instruktur") },
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
            when {
                state.isLoading && state.dashboard == null -> {
                    CircularProgressIndicator()
                }
                state.error != null && state.dashboard == null -> {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                }
                else -> {
                    val d = state.dashboard
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MetricCard("Kelas", (d?.totalCourses ?: 0).toString(), Modifier.weight(1f).testTag("metric_courses"))
                        MetricCard("Siswa", (d?.totalStudents ?: 0).toString(), Modifier.weight(1f).testTag("metric_students"))
                    }
                    Spacer(Modifier.height(12.dp))

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp)) {
                            Text("Saldo Dompet", style = MaterialTheme.typography.labelMedium)
                            Text(
                                "Rp " + formatRupiahId(d?.wallet?.balance ?: 0.0),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("wallet_balance"),
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))

                    Button(onClick = onCourses, modifier = Modifier.fillMaxWidth().testTag("nav_courses")) {
                        Icon(Icons.Filled.School, contentDescription = null)
                        Spacer(Modifier.height(0.dp)); Text("  Kelola Kelas")
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onWallet, modifier = Modifier.fillMaxWidth().testTag("nav_wallet")) {
                        Icon(Icons.Filled.Wallet, contentDescription = null); Text("  Dompet & Penarikan")
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onGrading, modifier = Modifier.fillMaxWidth().testTag("nav_grading")) {
                        Icon(Icons.Filled.Grading, contentDescription = null); Text("  Penilaian Tugas")
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onChat, modifier = Modifier.fillMaxWidth().testTag("nav_chat")) {
                        Icon(Icons.Filled.Chat, contentDescription = null); Text("  Chat Siswa")
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.Start) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Thousands grouping for Rupiah, shared across the instructor portal screens. */
internal fun formatRupiahId(amount: Double): String {
    val whole = amount.toLong().toString()
    return whole.reversed().chunked(3).joinToString(".").reversed()
}
