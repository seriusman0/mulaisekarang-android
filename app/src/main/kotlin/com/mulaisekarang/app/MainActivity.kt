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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.mulaisekarang.app.data.network.SessionEventBus
import com.mulaisekarang.app.ui.navigation.MulaiSekarangNavGraph
import com.mulaisekarang.app.ui.theme.MulaiSekarangTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.mulaisekarang.app.util.AppUpdateManager
import com.mulaisekarang.app.data.model.AppVersionResponse
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionEventBus: SessionEventBus

    @Inject
    lateinit var appUpdateManager: AppUpdateManager

    private var pendingDeepLink by mutableStateOf<Uri?>(null)

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingDeepLink = intent?.data

        setContent {
            var updateInfo by remember { mutableStateOf<AppVersionResponse?>(null) }
            var isDownloading by remember { mutableStateOf(false) }
            var downloadProgress by remember { mutableStateOf(0f) }
            var showUpdateDialog by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            var isMaintenanceMode by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                // Play Store owns updates for Play-installed builds; the in-app APK
                // self-updater is only relevant for direct/sideload distribution.
                val installedFromPlayStore = packageManager.getInstallerPackageName(packageName) == "com.android.vending"
                if (!installedFromPlayStore) {
                    val info = appUpdateManager.checkUpdate()
                    if (info?.updateAvailable == true) {
                        updateInfo = info
                        showUpdateDialog = true
                    }
                }
            }

            LaunchedEffect(Unit) {
                sessionEventBus.maintenanceEvents.collect {
                    isMaintenanceMode = true
                }
            }

            MulaiSekarangTheme(darkTheme = isSystemInDarkTheme()) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { testTagsAsResourceId = true },
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (isMaintenanceMode) {
                        com.mulaisekarang.app.ui.screens.MaintenanceScreen(
                            onRetry = {
                                isMaintenanceMode = false
                            }
                        )
                    } else {
                        MulaiSekarangNavGraph(
                            sessionEventBus = sessionEventBus,
                            deepLink = pendingDeepLink,
                            onDeepLinkConsumed = { pendingDeepLink = null },
                        )
                    }

                    if (showUpdateDialog && updateInfo != null) {
                        val required = updateInfo?.updateRequired == true
                        AlertDialog(
                            onDismissRequest = {
                                if (!required && !isDownloading) {
                                    showUpdateDialog = false
                                }
                            },
                            title = { Text("Update Available") },
                            text = {
                                if (isDownloading) {
                                    Column {
                                        Text("Downloading update...")
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LinearProgressIndicator(progress = { downloadProgress })
                                    }
                                } else {
                                    Text(updateInfo?.releaseNotes ?: "A new version of the app is available.")
                                }
                            },
                            confirmButton = {
                                if (!isDownloading) {
                                    TextButton(onClick = {
                                        isDownloading = true
                                        scope.launch {
                                            val success = appUpdateManager.downloadAndInstall(updateInfo!!) { progress ->
                                                downloadProgress = progress
                                            }
                                            isDownloading = false
                                            if (!success && !required) {
                                                showUpdateDialog = false
                                            }
                                        }
                                    }) {
                                        Text("Update")
                                    }
                                }
                            },
                            dismissButton = {
                                if (!required && !isDownloading) {
                                    TextButton(onClick = { showUpdateDialog = false }) {
                                        Text("Later")
                                    }
                                }
                            },
                            properties = DialogProperties(
                                dismissOnBackPress = !required && !isDownloading,
                                dismissOnClickOutside = !required && !isDownloading
                            )
                        )
                    }
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
