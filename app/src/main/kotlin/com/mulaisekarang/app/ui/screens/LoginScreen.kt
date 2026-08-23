package com.mulaisekarang.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mulaisekarang.app.R
import com.mulaisekarang.app.ui.components.GoogleSignInButton
import com.mulaisekarang.app.viewmodel.AuthUiState
import com.mulaisekarang.app.viewmodel.AuthViewModel

private val LoginBackground = Color(0xFF101426)
private val LoginInputFill = Color(0xFF1A1F36)
private val LoginPrimaryBlue = Color(0xFF3B82F6)
private val LoginTextGrey = Color(0xFFA0AABF)
private val LoginHintGrey = Color(0xFF6B7280)
private val LoginDividerGrey = Color(0xFF272E4A)

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoggedIn: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val uiState by authViewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.LoggedIn) onLoggedIn()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = LoginBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 64.dp, bottom = 16.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "Mulai Sekarang",
                modifier = Modifier
                    .size(85.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(LoginInputFill)
                    .padding(20.dp),
            )
            Text(
                "Selamat Datang",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                "Masuk untuk melanjutkan proses belajar dan capai\ntargetmu.",
                color = LoginTextGrey,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("Alamat Email", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("contoh@email.com", color = LoginHintGrey, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = LoginHintGrey) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = LoginInputFill,
                focusedContainerColor = LoginInputFill,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("email_input"),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Kata Sandi", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("••••••••", color = LoginHintGrey, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = LoginHintGrey) },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (showPassword) "Sembunyikan kata sandi" else "Tampilkan kata sandi",
                        tint = LoginHintGrey,
                    )
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = LoginInputFill,
                focusedContainerColor = LoginInputFill,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("password_input"),
        )

        TextButton(
            onClick = onNavigateToForgotPassword,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            Text("Lupa Kata Sandi?", color = LoginPrimaryBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        if (uiState is AuthUiState.Error) {
            Text(
                (uiState as AuthUiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Button(
            onClick = { authViewModel.login(email, password) },
            enabled = uiState !is AuthUiState.Submitting && email.isNotBlank() && password.isNotBlank(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LoginPrimaryBlue,
                contentColor = Color.White,
                disabledContainerColor = LoginPrimaryBlue.copy(alpha = 0.4f),
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(52.dp)
                .testTag("login_button"),
        ) {
            if (uiState is AuthUiState.Submitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Text("MASUK", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = LoginDividerGrey)
            Text(
                "  ATAU  ",
                color = LoginTextGrey,
                fontSize = 11.sp,
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = LoginDividerGrey)
        }

        GoogleSignInButton(
            authViewModel = authViewModel,
            enabled = uiState !is AuthUiState.Submitting,
            label = "Masuk dengan Google",
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        )

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            Text("Belum punya akun? ", color = LoginTextGrey, fontSize = 13.sp)
            TextButton(
                onClick = onNavigateToRegister,
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("Daftar", color = LoginPrimaryBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
