package com.mulaisekarang.app.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.mulaisekarang.app.viewmodel.InstructorWalletViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructorWalletScreen(
    viewModel: InstructorWalletViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("bank_transfer") }
    var accountName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dompet") },
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
            val wallet = state.wallet
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Saldo Tersedia", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "Rp " + formatRupiahId(wallet?.balance ?: 0.0),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("wallet_balance"),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Ajukan Penarikan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter(Char::isDigit) },
                label = { Text("Jumlah (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("withdrawal_amount_input"),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = accountName,
                onValueChange = { accountName = it },
                label = { Text("Nama Pemilik Rekening") },
                modifier = Modifier.fillMaxWidth().testTag("withdrawal_account_name"),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = accountNumber,
                onValueChange = { accountNumber = it },
                label = { Text("Nomor Rekening") },
                modifier = Modifier.fillMaxWidth().testTag("withdrawal_account_number"),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = bankName,
                onValueChange = { bankName = it },
                label = { Text("Nama Bank (opsional)") },
                modifier = Modifier.fillMaxWidth().testTag("withdrawal_bank_name"),
            )
            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    viewModel.requestWithdrawal(
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        method = method,
                        accountName = accountName,
                        accountNumber = accountNumber,
                        bankName = bankName,
                    )
                },
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth().testTag("withdrawal_submit"),
            ) {
                Text(if (state.isSubmitting) "Memproses..." else "Ajukan Penarikan")
            }

            state.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("withdrawal_error"))
            }
            state.successMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.testTag("withdrawal_success"))
            }

            Spacer(Modifier.height(24.dp))
            Text("Riwayat Penarikan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (state.withdrawals.isEmpty()) {
                Text("Belum ada penarikan.", style = MaterialTheme.typography.bodyMedium)
            } else {
                state.withdrawals.forEach { w ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                        ) {
                            Text("Rp " + formatRupiahId(w.amount), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(w.status)
                        }
                    }
                }
            }
        }
    }
}
