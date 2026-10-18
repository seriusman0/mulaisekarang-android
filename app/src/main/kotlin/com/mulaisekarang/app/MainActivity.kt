package com.mulaisekarang.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.mulaisekarang.app.util.AppUpdateManager
import com.mulaisekarang.app.data.model.AppVersionResponse

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
            var showUpdateDialog by remember { mutableStateOf(false) }

            var isMaintenanceMode by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                // Play Store owns updates for Play-installed builds.
                // Sideloading APKs from an external URL is prohibited by Google Play policy,
                // so we only redirect to the Play Store listing — never download+install directly.
                val installedFromPlayStore = try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        packageManager.getInstallSourceInfo(packageName).installingPackageName == "com.android.vending"
                    } else {
                        @Suppress("DEPRECATION")
                        packageManager.getInstallerPackageName(packageName) == "com.android.vending"
                    }
                } catch (_: Exception) { false }

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
                        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                            MulaiSekarangNavGraph(
                                sessionEventBus = sessionEventBus,
                                deepLink = pendingDeepLink,
                                onDeepLinkConsumed = { pendingDeepLink = null },
                            )
                            
                            com.mulaisekarang.app.ui.components.FloatingMiniPlayer(
                                modifier = Modifier
                                    .align(androidx.compose.ui.Alignment.BottomEnd)
                                    .padding(bottom = 80.dp) // Offset for bottom nav bar if present
                            )
                        }
                    }

                    if (showUpdateDialog && updateInfo != null) {
                        val required = updateInfo?.updateRequired == true
                        AlertDialog(
                            onDismissRequest = {
                                if (!required) showUpdateDialog = false
                            },
                            title = { Text("Update Tersedia") },
                            text = {
                                Text(updateInfo?.releaseNotes ?: "Versi terbaru tersedia. Silakan perbarui melalui Google Play Store.")
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    appUpdateManager.openPlayStoreListing()
                                    if (!required) showUpdateDialog = false
                                }) {
                                    Text("Perbarui")
                                }
                            },
                            dismissButton = {
                                if (!required) {
                                    TextButton(onClick = { showUpdateDialog = false }) {
                                        Text("Nanti")
                                    }
                                }
                            },
                            properties = DialogProperties(
                                dismissOnBackPress = !required,
                                dismissOnClickOutside = !required
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
