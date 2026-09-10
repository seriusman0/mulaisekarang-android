package com.mulaisekarang.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Metadata video yang sudah didownload ke internal storage.
 * Local path menyimpan path absolut file .dat yang ter-obfuscate.
 */
@Entity(tableName = "downloaded_lessons")
data class DownloadedLessonEntity(
    @PrimaryKey
    @ColumnInfo(name = "lesson_id")
    val lessonId: Int,

    @ColumnInfo(name = "course_id")
    val courseId: Int,

    @ColumnInfo(name = "local_path")
    val localPath: String,               // Absolute path: /data/user/0/.../files/vid_a1b2c3d4.dat

    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long,

    @ColumnInfo(name = "downloaded_at")
    val downloadedAt: String,            // ISO 8601

    @ColumnInfo(name = "expires_at")
    val expiresAt: String? = null        // Optional: 30 hari dari downloaded_at
)
