package com.mulaisekarang.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Antrian lokal progress menonton yang belum ter-sync ke server.
 * Setiap kali user menonton video (setiap 5 detik atau saat selesai),
 * satu baris baru / update baris existing di sini.
 *
 * Setelah sync berhasil (server return synced_ids), baris dihapus.
 */
@Entity(tableName = "offline_sync_queue")
data class OfflineSyncQueueEntity(
    @PrimaryKey
    @ColumnInfo(name = "sync_id")
    val syncId: String,                   // UUID.randomUUID().toString()

    @ColumnInfo(name = "course_id")
    val courseId: Int,

    @ColumnInfo(name = "lesson_id")
    val lessonId: Int,

    @ColumnInfo(name = "progress_seconds")
    val progressSeconds: Int,             // Detik terakhir ditonton

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,             // true saat video habis / user tap Selesai

    @ColumnInfo(name = "recorded_at")
    val recordedAt: String,              // ISO 8601 UTC: "2026-09-09T10:30:00Z"

    @ColumnInfo(name = "status")
    val status: String = "pending"       // "pending" | "syncing"
)
