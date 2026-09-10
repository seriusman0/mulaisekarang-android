package com.mulaisekarang.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.mulaisekarang.app.data.local.AppDatabase
import com.mulaisekarang.app.data.model.SyncItem
import com.mulaisekarang.app.data.model.SyncProgressRequest
import com.mulaisekarang.app.data.network.OfflineApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.UUID

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val offlineApiService: OfflineApiService,
    private val db: AppDatabase
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"

        fun buildRequest(): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
        }
    }

    override suspend fun doWork(): Result {
        val pendingItems = db.offlineSyncQueueDao().getPendingItems()
        if (pendingItems.isEmpty()) {
            return Result.success()
        }

        val syncIds = pendingItems.map { it.syncId }
        db.offlineSyncQueueDao().markAsSyncing(syncIds)

        return try {
            val deviceId = UUID.randomUUID().toString() // In a real app, this might be persistent
            val syncData = pendingItems.map {
                SyncItem(
                    syncId = it.syncId,
                    lessonId = it.lessonId,
                    courseId = it.courseId,
                    progressSeconds = it.progressSeconds,
                    isCompleted = it.isCompleted,
                    recordedAt = it.recordedAt
                )
            }

            val request = SyncProgressRequest(deviceId, syncData)
            val response = offlineApiService.syncProgress(request)

            if (response.syncedIds.isNotEmpty()) {
                db.offlineSyncQueueDao().deleteBySyncIds(response.syncedIds)
            }
            if (response.rejectedIds.isNotEmpty()) {
                db.offlineSyncQueueDao().deleteBySyncIds(response.rejectedIds)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            db.offlineSyncQueueDao().resetSyncingToPending()
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
