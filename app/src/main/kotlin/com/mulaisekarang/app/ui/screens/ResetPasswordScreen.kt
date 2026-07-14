package com.mulaisekarang.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mulaisekarang.app.viewmodel.ResetPasswordUiState
import com.mulaisekarang.app.viewmodel.ResetPasswordViewModel

@Composable
fun ResetPasswordScreen(
    viewModel: ResetPasswordViewModel,
    email: String,
    onBack: () -> Unit,
    onResetSuccess: () -> Unit,
) {
    var token by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is ResetPasswordUiState.Success) onResetSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 32.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
        }

        Text(
            "Atur Ulang Kata Sandi",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            "Masukkan kode yang kami kirim ke $email beserta kata sandi baru Anda.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Kode Reset") },
            leadingIcon = { Icon(Icons.Filled.Pin, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Kata Sandi Baru") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Konfirmasi Kata Sandi") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )

        if (password.isNotBlank() && confirmPassword.isNotBlank() && password != confirmPassword) {
            Text(
                "Konfirmasi kata sandi tidak cocok.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        if (uiState is ResetPasswordUiState.Error) {
            Text(
                (uiState as ResetPasswordUiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Button(
            onClick = { viewModel.submit(email, token, password) },
            enabled = uiState !is ResetPasswordUiState.Submitting &&
                token.isNotBlank() && password.length >= 8 && password == confirmPassword,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(top = 24.dp),
        ) {
            if (uiState is ResetPasswordUiState.Submitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Reset Kata Sandi", fontWeight = FontWeight.Bold)
            }
        }
    }
}
