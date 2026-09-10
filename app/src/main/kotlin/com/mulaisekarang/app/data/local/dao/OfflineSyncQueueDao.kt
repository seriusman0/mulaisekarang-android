package com.mulaisekarang.app.data.local.dao

import androidx.room.*
import com.mulaisekarang.app.data.local.entity.OfflineSyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineSyncQueueDao {

    /** Upsert — kalau lesson_id+course_id sudah ada, timpa (ambil yang terbaru) */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: OfflineSyncQueueEntity)

    /** Semua item pending yang belum di-sync */
    @Query("SELECT * FROM offline_sync_queue WHERE status = 'pending' ORDER BY recorded_at ASC")
    suspend fun getPendingItems(): List<OfflineSyncQueueEntity>

    /** Update status ke 'syncing' saat sedang dikirim */
    @Query("UPDATE offline_sync_queue SET status = 'syncing' WHERE sync_id IN (:syncIds)")
    suspend fun markAsSyncing(syncIds: List<String>)

    /** Hapus setelah server konfirmasi berhasil */
    @Query("DELETE FROM offline_sync_queue WHERE sync_id IN (:syncIds)")
    suspend fun deleteBySyncIds(syncIds: List<String>)

    /** Reset 'syncing' -> 'pending' jika sync gagal (untuk retry) */
    @Query("UPDATE offline_sync_queue SET status = 'pending' WHERE status = 'syncing'")
    suspend fun resetSyncingToPending()

    /** Count untuk badge / indicator di UI */
    @Query("SELECT COUNT(*) FROM offline_sync_queue WHERE status = 'pending'")
    fun pendingCountFlow(): Flow<Int>
}
