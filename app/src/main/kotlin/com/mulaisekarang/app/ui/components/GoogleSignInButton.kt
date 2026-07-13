package com.mulaisekarang.app.ui.components

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.mulaisekarang.app.auth.GoogleAuthClient
import com.mulaisekarang.app.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun GoogleSignInButton(
    authViewModel: AuthViewModel,
    enabled: Boolean,
    label: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val googleAuthClient = remember { GoogleAuthClient(context) }

    OutlinedButton(
        onClick = {
            coroutineScope.launch {
                googleAuthClient.signIn()
                    .onSuccess { idToken -> authViewModel.loginWithGoogle(idToken) }
                    .onFailure { error ->
                        if (error !is GetCredentialCancellationException) {
                            authViewModel.reportError(error.message ?: "Google sign-in gagal.")
                        }
                    }
            }
        },
        enabled = enabled,
        modifier = modifier,
    ) {
        Text(label)
    }
}
