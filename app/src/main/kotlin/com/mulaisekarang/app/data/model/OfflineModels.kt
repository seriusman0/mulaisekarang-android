package com.mulaisekarang.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OfflineManifestResponse(
    @SerialName("course") val course: CourseManifest,
    @SerialName("generated_at") val generatedAt: String
)

@Serializable
data class CourseManifest(
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("topics") val topics: List<TopicManifest> = emptyList()
)

@Serializable
data class TopicManifest(
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String,
    @SerialName("order") val order: Int,
    @SerialName("lessons") val lessons: List<LessonManifest> = emptyList()
)

@Serializable
data class LessonManifest(
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String,
    @SerialName("order") val order: Int,
    @SerialName("duration_hours") val durationHours: Int = 0,
    @SerialName("duration_minutes") val durationMinutes: Int = 0,
    @SerialName("duration_seconds") val durationSeconds: Int = 0,
    @SerialName("is_completed") val isCompleted: Boolean = false
)

@Serializable
data class DownloadUrlResponse(
    @SerialName("download_url") val downloadUrl: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("file_size_bytes") val fileSizeBytes: Long
)

@Serializable
data class SyncProgressRequest(
    @SerialName("device_id") val deviceId: String,
    @SerialName("sync_data") val syncData: List<SyncItem>
)

@Serializable
data class SyncItem(
    @SerialName("sync_id") val syncId: String,
    @SerialName("lesson_id") val lessonId: Int,
    @SerialName("course_id") val courseId: Int,
    @SerialName("progress_seconds") val progressSeconds: Int,
    @SerialName("is_completed") val isCompleted: Boolean,
    @SerialName("recorded_at") val recordedAt: String
)

@Serializable
data class SyncProgressResponse(
    @SerialName("status") val status: String,
    @SerialName("synced_ids") val syncedIds: List<String> = emptyList(),
    @SerialName("rejected_ids") val rejectedIds: List<String> = emptyList()
)
