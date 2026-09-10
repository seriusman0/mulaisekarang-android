package com.mulaisekarang.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.mulaisekarang.app.data.local.AppDatabase
import com.mulaisekarang.app.data.local.entity.DownloadedLessonEntity
import com.mulaisekarang.app.data.network.OfflineApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.time.Instant
import java.time.format.DateTimeFormatter

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val offlineApiService: OfflineApiService,
    private val db: AppDatabase,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_LESSON_ID  = "lesson_id"
        const val KEY_COURSE_ID  = "course_id"
        const val KEY_LOCAL_PATH = "local_path"

        private const val TAG = "DownloadWorker"
        private const val MIN_FREE_SPACE_BYTES = 500L * 1024 * 1024  // 500 MB

        fun buildRequest(lessonId: Int, courseId: Int): OneTimeWorkRequest {
            val inputData = workDataOf(
                KEY_LESSON_ID to lessonId,
                KEY_COURSE_ID to courseId
            )
            return OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(inputData)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .addTag("download_lesson_$lessonId")
                .build()
        }
    }

    override suspend fun doWork(): Result {
        val lessonId = inputData.getInt(KEY_LESSON_ID, -1)
        val courseId = inputData.getInt(KEY_COURSE_ID, -1)

        if (lessonId == -1 || courseId == -1) {
            Log.e(TAG, "Invalid input: lessonId=$lessonId courseId=$courseId")
            return Result.failure()
        }

        if (db.downloadedLessonDao().isDownloaded(lessonId)) {
            Log.d(TAG, "Lesson $lessonId already downloaded, skip")
            return Result.success()
        }

        val filesDir = applicationContext.filesDir
        val freeSpace = filesDir.freeSpace
        if (freeSpace < MIN_FREE_SPACE_BYTES) {
            Log.w(TAG, "Not enough space: ${freeSpace}B free")
            return Result.failure(
                workDataOf("error" to "Penyimpanan hampir penuh. Hapus beberapa file untuk melanjutkan.")
            )
        }

        return try {
            val downloadUrlResponse = offlineApiService.getDownloadUrl(lessonId)
            val downloadUrl = downloadUrlResponse.downloadUrl
            val expectedSize = downloadUrlResponse.fileSizeBytes

            val obfuscatedName = obfuscateFilename(lessonId)
            val outputFile = File(filesDir, obfuscatedName)

            downloadFile(downloadUrl, outputFile, expectedSize, lessonId)

            val now = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            db.downloadedLessonDao().insert(
                DownloadedLessonEntity(
                    lessonId = lessonId,
                    courseId = courseId,
                    localPath = outputFile.absolutePath,
                    fileSizeBytes = outputFile.length(),
                    downloadedAt = now,
                    expiresAt = null
                )
            )

            Result.success(workDataOf(KEY_LOCAL_PATH to outputFile.absolutePath))
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for lesson $lessonId", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure(
                workDataOf("error" to (e.message ?: "Download gagal"))
            )
        }
    }

    private fun downloadFile(url: String, outputFile: File, expectedSize: Long, lessonId: Int) {
        val existingBytes = if (outputFile.exists()) outputFile.length() else 0L

        val requestBuilder = Request.Builder().url(url)
        if (existingBytes > 0) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }

        okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful && response.code != 206) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }

            val body = response.body ?: throw Exception("Empty response body")
            
            val fos = FileOutputStream(outputFile, existingBytes > 0)
            com.mulaisekarang.app.util.CryptoUtil.getEncryptingOutputStream(fos, lessonId, existingBytes).use { cos ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        cos.write(buffer, 0, bytesRead)
                    }
                }
            }
        }

        if (expectedSize > 0 && outputFile.length() != expectedSize) {
            outputFile.delete()
            throw Exception("File size mismatch")
        }
    }

    private fun obfuscateFilename(lessonId: Int): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(lessonId.toString().toByteArray())
            .joinToString("") { "%02x".format(it) }
        return "vid_${hash.take(8)}.dat"
    }
}
