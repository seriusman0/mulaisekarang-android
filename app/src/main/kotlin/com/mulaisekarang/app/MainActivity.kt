package com.mulaisekarang.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mulaisekarang.app.data.network.SessionEventBus
import com.mulaisekarang.app.ui.navigation.MulaiSekarangNavGraph
import com.mulaisekarang.app.ui.theme.MulaiSekarangTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionEventBus: SessionEventBus

    private var pendingDeepLink by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingDeepLink = intent?.data

        setContent {
            MulaiSekarangTheme(darkTheme = isSystemInDarkTheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MulaiSekarangNavGraph(
                        sessionEventBus = sessionEventBus,
                        deepLink = pendingDeepLink,
                        onDeepLinkConsumed = { pendingDeepLink = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink = intent.data
    }
}
