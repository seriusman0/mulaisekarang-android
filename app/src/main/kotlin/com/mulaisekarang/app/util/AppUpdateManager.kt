package com.mulaisekarang.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.mulaisekarang.app.BuildConfig
import com.mulaisekarang.app.data.model.AppVersionResponse
import com.mulaisekarang.app.data.network.ApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks if a newer version of the app is available via the backend API.
 *
 * For Play Store-distributed builds this class is intentionally limited to
 * version-check only: sideloading APKs from an external URL is prohibited by
 * Google Play policy (Device and Network Abuse policy).  The download-and-install
 * path has been removed entirely so the class can safely remain compiled into
 * release builds without triggering policy violations.
 *
 * If you need in-app updates for Play builds, use the official
 * Google Play In-App Updates API
 * (https://developer.android.com/guide/playcore/in-app-updates).
 *
 * For direct/sideload distribution the backend can expose a download URL and
 * the host app can open a browser to let the user manually download the update —
 * that flow does not require REQUEST_INSTALL_PACKAGES and does not violate any policy.
 */
@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService,
) {
    /**
     * Returns version info from the backend, or null on error.
     * Callers should check [AppVersionResponse.updateAvailable] and
     * [AppVersionResponse.updateRequired].
     */
    suspend fun checkUpdate(): AppVersionResponse? {
        return try {
            apiService.getAppVersion("stable", BuildConfig.VERSION_CODE)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Opens the Play Store listing so the user can update through official channels.
     * Use this instead of a direct APK download for Play-distributed builds.
     */
    fun openPlayStoreListing() {
        val packageName = context.packageName
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        } catch (_: Exception) {
            // Fallback to browser if Play Store app is not installed
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        }
    }
}
