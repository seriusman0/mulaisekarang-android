package com.mulaisekarang.app.data

import android.content.Context
import androidx.work.WorkManager
import com.mulaisekarang.app.data.local.AppDatabase
import com.mulaisekarang.app.data.local.entity.CachedCourseEntity
import com.mulaisekarang.app.data.local.entity.OfflineSyncQueueEntity
import com.mulaisekarang.app.data.model.OfflineManifestResponse
import com.mulaisekarang.app.data.network.OfflineApiService
import com.mulaisekarang.app.worker.DownloadWorker
import com.mulaisekarang.app.worker.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val offlineApiService: OfflineApiService,
    private val json: Json
) {
    private val workManager = WorkManager.getInstance(context)

    fun isDownloadedFlow(lessonId: Int): Flow<Boolean> {
        return db.downloadedLessonDao().isDownloadedFlow(lessonId)
    }

    suspend fun getDownloadedLesson(lessonId: Int) = db.downloadedLessonDao().getByLessonId(lessonId)

    suspend fun getCachedCourseManifest(courseId: Int): OfflineManifestResponse? {
        val cached = db.cachedCourseDao().getById(courseId) ?: return null
        return try {
            json.decodeFromString<OfflineManifestResponse>(cached.manifestJson)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun cacheCourseManifest(courseId: Int) {
        try {
            val response = offlineApiService.getOfflineManifest(courseId)
            val jsonString = json.encodeToString(response)
            val now = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            db.cachedCourseDao().insert(
                CachedCourseEntity(courseId, jsonString, now)
            )
        } catch (e: Exception) {
            // Log error
        }
    }

    fun startDownload(courseId: Int, lessonId: Int) {
        val request = DownloadWorker.buildRequest(lessonId, courseId)
        workManager.enqueue(request)
    }

    suspend fun recordProgress(courseId: Int, lessonId: Int, progressSeconds: Int, isCompleted: Boolean) {
        val now = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        val entity = OfflineSyncQueueEntity(
            syncId = UUID.randomUUID().toString(),
            courseId = courseId,
            lessonId = lessonId,
            progressSeconds = progressSeconds,
            isCompleted = isCompleted,
            recordedAt = now
        )
        db.offlineSyncQueueDao().upsert(entity)
        enqueueSync()
    }

    fun enqueueSync() {
        val request = SyncWorker.buildRequest()
        workManager.enqueue(request)
    }
}
