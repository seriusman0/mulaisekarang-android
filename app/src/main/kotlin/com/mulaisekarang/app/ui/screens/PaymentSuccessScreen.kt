package com.mulaisekarang.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mulaisekarang.app.viewmodel.PaymentSuccessViewModel
import com.mulaisekarang.app.viewmodel.PaymentVerifyState

@Composable
fun PaymentSuccessScreen(
    viewModel: PaymentSuccessViewModel,
    onStartLearning: () -> Unit,
    onGoHome: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (val state = uiState) {
                is PaymentVerifyState.Verifying -> {
                    CircularProgressIndicator()
                    Text(
                        "Memverifikasi pembayaran...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }

                is PaymentVerifyState.Completed -> {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp),
                    )
                    Text(
                        "Pembayaran Berhasil!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Text(
                        "Kursus sudah bisa diakses. Selamat belajar!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Button(
                        onClick = onStartLearning,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                    ) { Text("Mulai Belajar", fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = onGoHome,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    ) { Text("Ke Beranda") }
                }

                is PaymentVerifyState.Pending -> {
                    Icon(
                        Icons.Filled.HourglassEmpty,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(72.dp),
                    )
                    Text(
                        "Menunggu Konfirmasi",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Text(
                        "Pembayaran Anda sedang diproses. Ini biasanya hanya perlu beberapa saat.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Button(
                        onClick = { viewModel.verify() },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                    ) { Text("Cek Lagi") }
                }

                is PaymentVerifyState.Failed -> {
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(72.dp),
                    )
                    Text(
                        "Pembayaran Gagal",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Text(
                        "Transaksi tidak berhasil diselesaikan. Silakan coba lagi dari halaman detail course.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    OutlinedButton(
                        onClick = onGoHome,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                    ) { Text("Ke Beranda") }
                }

                is PaymentVerifyState.Error -> {
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(72.dp),
                    )
                    Text(
                        state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Button(
                        onClick = { viewModel.verify() },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                    ) { Text("Coba Lagi") }
                }
            }
        }
    }
}
