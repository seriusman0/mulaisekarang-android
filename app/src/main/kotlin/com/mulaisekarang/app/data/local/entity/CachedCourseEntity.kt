package com.mulaisekarang.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cache manifest course yang didownload dari /api/v2/mobile/courses/{id}/offline-manifest
 * Disimpan sebagai JSON string agar fleksibel tanpa perlu normalisasi relasional.
 */
@Entity(tableName = "cached_courses")
data class CachedCourseEntity(
    @PrimaryKey
    @ColumnInfo(name = "course_id")
    val courseId: Int,

    @ColumnInfo(name = "manifest_json")
    val manifestJson: String,            // Raw JSON dari server

    @ColumnInfo(name = "cached_at")
    val cachedAt: String                 // ISO 8601 — untuk invalidasi cache
)
