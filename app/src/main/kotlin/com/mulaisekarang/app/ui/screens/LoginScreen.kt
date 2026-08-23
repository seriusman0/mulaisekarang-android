package com.mulaisekarang.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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

private val LoginBackground = Color(0xFF0F1322)
private val LoginLogoFill = Color(0xFF1E2336)
private val LoginPrimaryBlue = Color(0xFF2563EB)
private val LoginTextGrey = Color(0xFF94A3B8) // slate-400
private val LoginPlaceholderGrey = Color(0xFFCBD5E1) // slate-300
private val LoginIndicatorGrey = Color(0xFF475569) // slate-600
private val LoginDividerGrey = Color(0xFF334155) // slate-700
private val LoginIconGrey = Color(0xFF64748B) // slate-500

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
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(color = LoginBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(vertical = 24.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "Mulai Sekarang",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(LoginLogoFill)
                    .padding(20.dp),
            )
        }

        Text(
            "Selamat Datang",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        Text(
            "Masuk untuk melanjutkan proses belajar dan capai targetmu.",
            color = LoginTextGrey,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 20.dp),
        )

        LoginUnderlineField(
            value = email,
            onValueChange = { email = it },
            placeholder = "Alamat Email",
            leadingIcon = Icons.Filled.Email,
            keyboardType = KeyboardType.Email,
            testTag = "email_input",
        )

        Spacer(modifier = Modifier.height(14.dp))

        LoginUnderlineField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Kata Sandi",
            leadingIcon = Icons.Filled.Lock,
            keyboardType = KeyboardType.Password,
            isPassword = true,
            showPassword = showPassword,
            onTogglePasswordVisibility = { showPassword = !showPassword },
            testTag = "password_input",
        )

        TextButton(
            onClick = onNavigateToForgotPassword,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.padding(top = 12.dp),
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
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LoginPrimaryBlue,
                contentColor = Color.White,
                disabledContainerColor = LoginPrimaryBlue.copy(alpha = 0.4f),
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
                .height(52.dp)
                .testTag("login_button"),
        ) {
            if (uiState is AuthUiState.Submitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Text("MASUK", fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = LoginDividerGrey)
            Text(
                "  ATAU  ",
                color = LoginIconGrey,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = LoginDividerGrey)
        }

        GoogleSignInButton(
            authViewModel = authViewModel,
            enabled = uiState !is AuthUiState.Submitting,
            label = "Lanjutkan dengan Google",
            contentColor = Color.White,
            borderColor = LoginIndicatorGrey,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        )

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
        ) {
            Text("Belum punya akun? ", color = LoginTextGrey, fontSize = 13.sp)
            TextButton(
                onClick = onNavigateToRegister,
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("Daftar", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LoginUnderlineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType,
    testTag: String,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = LoginPlaceholderGrey, fontSize = 15.sp) },
        leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = LoginIconGrey, modifier = Modifier.size(20.dp)) },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { onTogglePasswordVisibility?.invoke() }) {
                    Icon(
                        if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (showPassword) "Sembunyikan kata sandi" else "Tampilkan kata sandi",
                        tint = LoginIconGrey,
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            unfocusedIndicatorColor = LoginIndicatorGrey,
            focusedIndicatorColor = LoginPrimaryBlue,
            cursorColor = LoginPrimaryBlue,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
    )
}
