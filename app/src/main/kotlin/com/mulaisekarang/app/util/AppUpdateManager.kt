package com.mulaisekarang.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.mulaisekarang.app.BuildConfig
import com.mulaisekarang.app.data.model.AppVersionResponse
import com.mulaisekarang.app.data.network.ApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService,
    private val okHttpClient: OkHttpClient
) {
    suspend fun checkUpdate(): AppVersionResponse? {
        return try {
            apiService.getAppVersion("stable", BuildConfig.VERSION_CODE)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadAndInstall(
        updateInfo: AppVersionResponse,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val url = updateInfo.downloadUrl ?: return@withContext false
        val expectedChecksum = updateInfo.checksumSha256
        val expectedSize = updateInfo.fileSize

        val request = Request.Builder().url(url).build()
        val response = try {
            okHttpClient.newCall(request).execute()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }

        if (!response.isSuccessful) return@withContext false
        val body = response.body ?: return@withContext false

        val apkFile = File(context.cacheDir, "update.apk")
        if (apkFile.exists()) {
            apkFile.delete()
        }

        try {
            val totalBytes = body.contentLength()
            var bytesCopied = 0L
            val buffer = ByteArray(8 * 1024)
            var bytes = body.source().read(buffer)
            val output = FileOutputStream(apkFile)

            output.use { out ->
                while (bytes >= 0) {
                    out.write(buffer, 0, bytes)
                    bytesCopied += bytes
                    if (totalBytes > 0) {
                        onProgress(bytesCopied.toFloat() / totalBytes.toFloat())
                    }
                    bytes = body.source().read(buffer)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            apkFile.delete()
            return@withContext false
        }

        // Verify size
        if (expectedSize != null && apkFile.length() != expectedSize) {
            apkFile.delete()
            return@withContext false
        }

        // Verify checksum
        if (expectedChecksum != null) {
            val checksum = computeSha256(apkFile)
            if (checksum != expectedChecksum) {
                apkFile.delete()
                return@withContext false
            }
        }

        installApk(apkFile)
        return@withContext true
    }

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { fis ->
            val buffer = ByteArray(8 * 1024)
            var bytesRead = fis.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = fis.read(buffer)
            }
        }
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun installApk(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
