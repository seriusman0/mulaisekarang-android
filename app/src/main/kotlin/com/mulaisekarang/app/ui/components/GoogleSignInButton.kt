package com.mulaisekarang.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        shape = RoundedCornerShape(20.dp),
        modifier = modifier,
    ) {
        if (!enabled) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary
            )
        } else {
            Text(label, fontWeight = FontWeight.Bold)
        }
    }
}
