package com.mulaisekarang.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.mulaisekarang.app.BuildConfig
import com.mulaisekarang.app.viewmodel.AuthViewModel

@Composable
fun GoogleSignInButton(
    authViewModel: AuthViewModel,
    enabled: Boolean,
    label: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val googleSignInClient = remember {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, options)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                authViewModel.loginWithGoogle(idToken)
            } else {
                authViewModel.reportError("Google sign-in gagal: ID token kosong.")
            }
        } catch (e: ApiException) {
            if (e.statusCode != com.google.android.gms.common.api.CommonStatusCodes.CANCELED) {
                authViewModel.reportError("[GoogleSignIn ${e.statusCode}] ${e.message ?: "Google sign-in gagal."}")
            }
        }
    }

    OutlinedButton(
        onClick = {
            googleSignInClient.signOut().addOnCompleteListener {
                launcher.launch(googleSignInClient.signInIntent)
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
