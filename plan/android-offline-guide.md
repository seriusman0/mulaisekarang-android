# Android Implementation Guide — Fitur Offline Mode
## mulaisekarang.com APK

**Versi:** 1.0  
**Tanggal:** 9 September 2026  
**Berlaku untuk:** Android (Kotlin/Jetpack Compose atau XML) — bukan Flutter  
**Status backend:** API v2 sudah tersedia di production (`/api/v2/mobile/`)  
**Dokumen terkait:** `fdd/offline.md` (PRD), `fdd/api/student-api.md` (API v1), `fdd/api/android-dev-reference.md`

---

## Daftar Isi

1. [Gambaran Arsitektur](#1-gambaran-arsitektur)
2. [Dependencies yang Diperlukan](#2-dependencies-yang-diperlukan)
3. [Database Lokal — Room](#3-database-lokal--room)
4. [Networking — Retrofit + OkHttp](#4-networking--retrofit--okhttp)
5. [Manajemen Unduhan — WorkManager](#5-manajemen-unduhan--workmanager)
6. [Sinkronisasi Progres — SyncManager](#6-sinkronisasi-progres--syncmanager)
7. [Video Player — ExoPlayer Offline-Aware](#7-video-player--exoplayer-offline-aware)
8. [UI — Download Button & Status](#8-ui--download-button--status)
9. [Permissions & Manifest](#9-permissions--manifest)
10. [Alur Lengkap End-to-End](#10-alur-lengkap-end-to-end)
11. [Checklist QA Sebelum Rilis](#11-checklist-qa-sebelum-rilis)
12. [Error Handling & Edge Cases](#12-error-handling--edge-cases)
13. [API Contracts (Ringkasan)](#13-api-contracts-ringkasan)

---

## 1. Gambaran Arsitektur

```
┌─────────────────────────────────────────────────────────────────┐
│  Android App                                                    │
│                                                                 │
│  ┌──────────┐   ┌──────────────┐   ┌─────────────────────────┐ │
│  │   UI     │   │  ViewModel   │   │    Repository Layer      │ │
│  │(Compose/ │◄──│  (StateFlow) │◄──│  OfflineRepository      │ │
│  │  XML)    │   │              │   │  CourseRepository        │ │
│  └──────────┘   └──────────────┘   └────────┬────────────────┘ │
│                                             │                   │
│              ┌──────────────────────────────┼──────────────────┐│
│              │                              │                  ││
│  ┌───────────▼────┐  ┌─────────────────┐  ┌▼──────────────┐  ││
│  │  Room Database │  │  Retrofit API   │  │ Download Mgr  │  ││
│  │                │  │  (v1 + v2)      │  │ (WorkManager) │  ││
│  │ offline_sync_  │  │                 │  │               │  ││
│  │   queue        │  │ /api/v1/*       │  │ DownloadWorker│  ││
│  │ downloaded_    │  │ /api/v2/mobile/ │  │               │  ││
│  │   lessons      │  │                 │  └───────────────┘  ││
│  │ cached_courses │  └─────────────────┘                     ││
│  └────────────────┘                                           ││
│                                                               ││
│  ┌────────────────────────────────────────────────────────┐  ││
│  │  SyncManager (auto-sync on connectivity change)        │  ││
│  │  ConnectivityObserver → WorkManager one-shot job       │  ││
│  └────────────────────────────────────────────────────────┘  ││
└──────────────────────────────────────────────────────────────────┘
```

**Prinsip utama:**
- **Offline-first:** semua data dibaca dari Room terlebih dahulu, network hanya untuk refresh & sync
- **Non-destructive:** tidak ada perubahan pada flow v1 yang sudah ada, fitur offline adalah *additive*
- **File aman:** video disimpan di `filesDir` (internal storage, terisolasi per-app), nama file di-obfuscate

---

## 2. Dependencies yang Diperlukan

Tambahkan ke `build.gradle.kts` (module `:app`):

```kotlin
// =====================================================
// Room (Database Lokal)
// =====================================================
val roomVersion = "2.6.1"
implementation("androidx.room:room-runtime:$roomVersion")
implementation("androidx.room:room-ktx:$roomVersion")           // coroutines support
kapt("androidx.room:room-compiler:$roomVersion")                // atau ksp jika pakai KSP

// =====================================================
// WorkManager (Background Download & Sync)
// =====================================================
implementation("androidx.work:work-runtime-ktx:2.9.1")

// =====================================================
// OkHttp + Retrofit (sudah ada, pastikan versinya)
// =====================================================
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.retrofit2:converter-gson:2.11.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// =====================================================
// ExoPlayer / Media3 (Video Player)
// =====================================================
val media3Version = "1.4.1"
implementation("androidx.media3:media3-exoplayer:$media3Version")
implementation("androidx.media3:media3-ui:$media3Version")
implementation("androidx.media3:media3-datasource-okhttp:$media3Version")

// =====================================================
// Connectivity (detect online/offline)
// =====================================================
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

// =====================================================
// Coroutines
// =====================================================
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

// =====================================================
// UUID
// =====================================================
// Sudah ada di Java stdlib — java.util.UUID
```

Di `build.gradle.kts` level root, pastikan plugin kapt (atau KSP) aktif:
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")  // untuk Room annotation processor
}
```

---

## 3. Database Lokal — Room

### 3.1 Entity: `OfflineSyncQueueEntity`

```kotlin
// data/local/entity/OfflineSyncQueueEntity.kt

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
```

### 3.2 Entity: `DownloadedLessonEntity`

```kotlin
// data/local/entity/DownloadedLessonEntity.kt

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
```

### 3.3 Entity: `CachedCourseEntity` (untuk UI offline)

```kotlin
// data/local/entity/CachedCourseEntity.kt

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
```

### 3.4 DAO

```kotlin
// data/local/dao/OfflineSyncQueueDao.kt

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
```

```kotlin
// data/local/dao/DownloadedLessonDao.kt

package com.mulaisekarang.app.data.local.dao

import androidx.room.*
import com.mulaisekarang.app.data.local.entity.DownloadedLessonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedLessonDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadedLessonEntity)

    @Query("SELECT * FROM downloaded_lessons WHERE lesson_id = :lessonId")
    suspend fun getByLessonId(lessonId: Int): DownloadedLessonEntity?

    @Query("SELECT * FROM downloaded_lessons WHERE course_id = :courseId")
    suspend fun getByCourseId(courseId: Int): List<DownloadedLessonEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_lessons WHERE lesson_id = :lessonId)")
    suspend fun isDownloaded(lessonId: Int): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_lessons WHERE lesson_id = :lessonId)")
    fun isDownloadedFlow(lessonId: Int): Flow<Boolean>

    @Query("DELETE FROM downloaded_lessons WHERE lesson_id = :lessonId")
    suspend fun deleteByLessonId(lessonId: Int)

    @Query("SELECT SUM(file_size_bytes) FROM downloaded_lessons")
    suspend fun totalDownloadedBytes(): Long?

    /** Semua lesson yang didownload — untuk halaman "Unduhan Saya" */
    @Query("SELECT * FROM downloaded_lessons ORDER BY downloaded_at DESC")
    fun getAllFlow(): Flow<List<DownloadedLessonEntity>>
}
```

```kotlin
// data/local/dao/CachedCourseDao.kt

package com.mulaisekarang.app.data.local.dao

import androidx.room.*
import com.mulaisekarang.app.data.local.entity.CachedCourseEntity

@Dao
interface CachedCourseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CachedCourseEntity)

    @Query("SELECT * FROM cached_courses WHERE course_id = :courseId")
    suspend fun getById(courseId: Int): CachedCourseEntity?

    @Query("DELETE FROM cached_courses WHERE course_id = :courseId")
    suspend fun deleteById(courseId: Int)
}
```

### 3.5 AppDatabase

```kotlin
// data/local/AppDatabase.kt

package com.mulaisekarang.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mulaisekarang.app.data.local.dao.*
import com.mulaisekarang.app.data.local.entity.*

@Database(
    entities = [
        OfflineSyncQueueEntity::class,
        DownloadedLessonEntity::class,
        CachedCourseEntity::class,
    ],
    version = 1,
    exportSchema = true                  // Wajib true untuk migrasi production-safe
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun offlineSyncQueueDao(): OfflineSyncQueueDao
    abstract fun downloadedLessonDao(): DownloadedLessonDao
    abstract fun cachedCourseDao(): CachedCourseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mulaisekarang_offline.db"
                )
                .fallbackToDestructiveMigrationOnDowngrade() // dev only; produksi: tulis Migration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
```

**⚠️ PENTING:** Gunakan Hilt/Koin untuk inject `AppDatabase` — jangan buat instance manual di setiap class.

---

## 4. Networking — Retrofit + OkHttp

### 4.1 API Service v2 (BARU — jangan modifikasi interface v1)

```kotlin
// data/remote/api/OfflineApiService.kt

package com.mulaisekarang.app.data.remote.api

import com.mulaisekarang.app.data.remote.dto.*
import retrofit2.http.*

/**
 * API v2 khusus fitur offline.
 * Base URL tetap: https://mulaisekarang.com/api/v2/mobile/
 * Auth: Bearer token (sama seperti v1, header Authorization)
 */
interface OfflineApiService {

    /**
     * Ambil manifest course untuk disimpan offline.
     * Panggil ini saat user pertama kali tap "Unduh Course" untuk cache metadata.
     */
    @GET("courses/{courseId}/offline-manifest")
    suspend fun getOfflineManifest(
        @Path("courseId") courseId: Int
    ): OfflineManifestResponse

    /**
     * Minta signed URL untuk download video lesson.
     * URL valid 1 jam. Gunakan segera setelah dapat.
     */
    @GET("lessons/{lessonId}/download-url")
    suspend fun getDownloadUrl(
        @Path("lessonId") lessonId: Int
    ): DownloadUrlResponse

    /**
     * Kirim batch progress yang tersimpan di queue lokal.
     * Panggil saat koneksi internet tersedia.
     */
    @POST("sync-progress")
    suspend fun syncProgress(
        @Body request: SyncProgressRequest
    ): SyncProgressResponse
}
```

**Catatan:** Endpoint `GET /api/v2/mobile/lessons/{lesson}/file?token=xxx` **tidak** perlu Retrofit — gunakan `OkHttpClient` atau `DownloadManager` langsung (karena download file besar, bukan JSON).

### 4.2 DTOs (Data Transfer Objects)

```kotlin
// data/remote/dto/OfflineManifestResponse.kt

package com.mulaisekarang.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OfflineManifestResponse(
    @SerializedName("course") val course: CourseManifest,
    @SerializedName("generated_at") val generatedAt: String
)

data class CourseManifest(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("cover_image_url") val coverImageUrl: String?,
    @SerializedName("topics") val topics: List<TopicManifest>
)

data class TopicManifest(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("order") val order: Int,
    @SerializedName("lessons") val lessons: List<LessonManifest>
)

data class LessonManifest(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("order") val order: Int,
    @SerializedName("duration_hours") val durationHours: Int,
    @SerializedName("duration_minutes") val durationMinutes: Int,
    @SerializedName("duration_seconds") val durationSeconds: Int,
    @SerializedName("is_completed") val isCompleted: Boolean
)
```

```kotlin
// data/remote/dto/DownloadUrlResponse.kt

package com.mulaisekarang.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DownloadUrlResponse(
    @SerializedName("download_url") val downloadUrl: String,
    @SerializedName("expires_at") val expiresAt: String,
    @SerializedName("file_size_bytes") val fileSizeBytes: Long
)
```

```kotlin
// data/remote/dto/SyncProgressRequest.kt + SyncProgressResponse.kt

package com.mulaisekarang.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SyncProgressRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("sync_data") val syncData: List<SyncItem>
)

data class SyncItem(
    @SerializedName("sync_id") val syncId: String,
    @SerializedName("lesson_id") val lessonId: Int,
    @SerializedName("course_id") val courseId: Int,
    @SerializedName("progress_seconds") val progressSeconds: Int,
    @SerializedName("is_completed") val isCompleted: Boolean,
    @SerializedName("recorded_at") val recordedAt: String       // ISO 8601 UTC
)

data class SyncProgressResponse(
    @SerializedName("status") val status: String,
    @SerializedName("synced_ids") val syncedIds: List<String>,
    @SerializedName("rejected_ids") val rejectedIds: List<String>
)
```

### 4.3 Retrofit Instance untuk v2

```kotlin
// data/remote/RetrofitProvider.kt (tambahkan instance v2)

// Base URL v2 berbeda dari v1 karena path prefix berbeda
val retrofitV2 = Retrofit.Builder()
    .baseUrl("https://mulaisekarang.com/api/v2/mobile/")
    .client(okHttpClient)               // OkHttpClient yang sama (sudah ada AuthInterceptor)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val offlineApiService: OfflineApiService = retrofitV2.create(OfflineApiService::class.java)
```

**Penting:** `AuthInterceptor` yang menambahkan `Authorization: Bearer {token}` harus sudah diapply ke `okHttpClient` ini — sama persis dengan yang dipakai v1.

---

## 5. Manajemen Unduhan — WorkManager

### 5.1 DownloadWorker

```kotlin
// worker/DownloadWorker.kt

package com.mulaisekarang.app.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.mulaisekarang.app.data.local.AppDatabase
import com.mulaisekarang.app.data.local.entity.DownloadedLessonEntity
import com.mulaisekarang.app.data.remote.api.OfflineApiService
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * WorkManager Worker untuk download video lesson ke internal storage.
 *
 * Input data (WorkData):
 *   - KEY_LESSON_ID: Int
 *   - KEY_COURSE_ID: Int
 *
 * Output data:
 *   - KEY_LOCAL_PATH: String (path file yang tersimpan)
 */
class DownloadWorker(
    context: Context,
    workerParams: WorkerParameters,
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

        /** Buat WorkRequest untuk 1 lesson */
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

        // 1. Cek apakah sudah pernah didownload
        if (db.downloadedLessonDao().isDownloaded(lessonId)) {
            Log.d(TAG, "Lesson $lessonId already downloaded, skip")
            return Result.success()
        }

        // 2. Cek storage space
        val filesDir = applicationContext.filesDir
        val freeSpace = filesDir.freeSpace
        if (freeSpace < MIN_FREE_SPACE_BYTES) {
            Log.w(TAG, "Not enough space: ${freeSpace}B free, need ${MIN_FREE_SPACE_BYTES}B")
            return Result.failure(
                workDataOf("error" to "Penyimpanan hampir penuh. Hapus beberapa file untuk melanjutkan.")
            )
        }

        return try {
            // 3. Minta Signed URL dari server
            Log.d(TAG, "Requesting download URL for lesson $lessonId")
            val downloadUrlResponse = offlineApiService.getDownloadUrl(lessonId)
            val downloadUrl = downloadUrlResponse.downloadUrl
            val expectedSize = downloadUrlResponse.fileSizeBytes

            // 4. Download file
            val obfuscatedName = obfuscateFilename(lessonId)
            val outputFile = File(filesDir, obfuscatedName)

            Log.d(TAG, "Downloading lesson $lessonId to ${outputFile.absolutePath}")
            downloadFile(downloadUrl, outputFile, expectedSize)

            // 5. Simpan metadata ke Room
            val now = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            db.downloadedLessonDao().insert(
                DownloadedLessonEntity(
                    lessonId = lessonId,
                    courseId = courseId,
                    localPath = outputFile.absolutePath,
                    fileSizeBytes = outputFile.length(),
                    downloadedAt = now,
                    expiresAt = null  // Atau hitung 30 hari dari sekarang jika diperlukan
                )
            )

            Log.d(TAG, "Lesson $lessonId downloaded successfully: ${outputFile.absolutePath}")
            Result.success(workDataOf(KEY_LOCAL_PATH to outputFile.absolutePath))

        } catch (e: Exception) {
            Log.e(TAG, "Download failed for lesson $lessonId", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure(
                workDataOf("error" to (e.message ?: "Download gagal"))
            )
        }
    }

    /**
     * Download file dengan OkHttp, support resume via Range header.
     * Signed URL hanya valid 1 jam — TIDAK perlu auth Bearer (token IS the auth).
     */
    private fun downloadFile(url: String, outputFile: File, expectedSize: Long) {
        // Cek apakah sudah partial download (resume)
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
            val mode = if (existingBytes > 0) "ab" else "wb"  // append atau write
            
            FileOutputStream(outputFile, existingBytes > 0).use { fos ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8 * 1024) // 8KB chunks
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        fos.write(buffer, 0, bytesRead)
                    }
                }
            }
        }

        // Verifikasi ukuran (opsional tapi disarankan)
        if (expectedSize > 0 && outputFile.length() != expectedSize) {
            outputFile.delete()
            throw Exception("File size mismatch: got ${outputFile.length()}, expected $expectedSize")
        }
    }

    /**
     * Nama file ter-obfuscate: vid_<8-char SHA-256 dari lessonId>.dat
     * Contoh: lesson 96 → vid_2c624232.dat
     * TIDAK bisa ditebak atau di-enumerate dari filesystem.
     */
    private fun obfuscateFilename(lessonId: Int): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(lessonId.toString().toByteArray())
            .joinToString("") { "%02x".format(it) }
        return "vid_${hash.take(8)}.dat"
    }
}
```

### 5.2 WorkerFactory (untuk Hilt injection ke WorkManager)

```kotlin
// di/WorkerFactory.kt

package com.mulaisekarang.app.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.mulaisekarang.app.data.local.AppDatabase
import com.mulaisekarang.app.data.remote.api.OfflineApiService
import com.mulaisekarang.app.worker.DownloadWorker
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppWorkerFactory @Inject constructor(
    private val offlineApiService: OfflineApiService,
    private val db: AppDatabase,
    private val okHttpClient: OkHttpClient
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            DownloadWorker::class.java.name ->
                DownloadWorker(appContext, workerParameters, offlineApiService, db, okHttpClient)
            SyncWorker::class.java.name ->
                SyncWorker(appContext, workerParameters, offlineApiService, db)
            else -> null
        }
    }
}
```

Daftarkan di `Application.onCreate()`:

```kotlin
// MyApplication.kt

import androidx.work.Configuration
import com.mulaisekarang.app.di.AppWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: AppWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
```

**⚠️ PENTING:** Karena custom WorkerFactory, **HAPUS** `androidx.startup` initializer default WorkManager dari `AndroidManifest.xml`:

```xml
<!-- AndroidManifest.xml — tambahkan ini untuk disable auto-init WorkManager -->
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="merge">
    <meta-data
        android:name="androidx.work.WorkManagerInitializer"
        android:value="androidx.startup"
        tools:node="remove" />
</provider>
```

### 5.3 DownloadManager (helper untuk trigger dari ViewModel)

```kotlin
// domain/DownloadManager.kt

package com.mulaisekarang.app.domain

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.*
import com.mulaisekarang.app.worker.DownloadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    /** Mulai download lesson di background. Idempotent — tidak duplikat jika sudah berjalan. */
    fun enqueueDownload(lessonId: Int, courseId: Int): Operation {
        val request = DownloadWorker.buildRequest(lessonId, courseId)
        return workManager.enqueueUniqueWork(
            "download_$lessonId",
            ExistingWorkPolicy.KEEP,  // Jangan restart jika sudah ada
            request
        )
    }

    /** Batalkan download yang sedang berjalan */
    fun cancelDownload(lessonId: Int) {
        workManager.cancelUniqueWork("download_$lessonId")
    }

    /** Observe status download lesson tertentu */
    fun getDownloadStatusLiveData(lessonId: Int): LiveData<List<WorkInfo>> {
        return workManager.getWorkInfosByTagLiveData("download_lesson_$lessonId")
    }

    /** Status download sebagai enum sederhana */
    fun getSimpleStatus(workInfos: List<WorkInfo>?): DownloadStatus {
        return when {
            workInfos.isNullOrEmpty() -> DownloadStatus.NOT_QUEUED
            workInfos.any { it.state == WorkInfo.State.RUNNING } -> DownloadStatus.DOWNLOADING
            workInfos.any { it.state == WorkInfo.State.ENQUEUED } -> DownloadStatus.QUEUED
            workInfos.all { it.state == WorkInfo.State.SUCCEEDED } -> DownloadStatus.DONE
            workInfos.any { it.state == WorkInfo.State.FAILED } -> DownloadStatus.FAILED
            else -> DownloadStatus.NOT_QUEUED
        }
    }

    enum class DownloadStatus {
        NOT_QUEUED,    // Belum pernah didownload
        QUEUED,        // Ada di antrian WorkManager
        DOWNLOADING,   // Sedang download aktif
        DONE,          // Selesai (cek Room DB untuk path)
        FAILED         // Gagal permanen (sudah retry 3x)
    }
}
```

---

## 6. Sinkronisasi Progres — SyncManager

### 6.1 SyncWorker

```kotlin
// worker/SyncWorker.kt

package com.mulaisekarang.app.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.mulaisekarang.app.data.local.AppDatabase
import com.mulaisekarang.app.data.remote.api.OfflineApiService
import com.mulaisekarang.app.data.remote.dto.SyncItem
import com.mulaisekarang.app.data.remote.dto.SyncProgressRequest

/**
 * WorkManager Worker untuk sync offline_sync_queue ke server.
 * Dijalankan saat:
 * 1. Koneksi internet tersedia (dipantau ConnectivityObserver)
 * 2. App masuk foreground
 * 3. Periodik (opsional, setiap 15 menit)
 */
class SyncWorker(
    context: Context,
    workerParams: WorkerParameters,
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
                .addTag("sync_progress")
                .build()
        }
    }

    override suspend fun doWork(): Result {
        val pending = db.offlineSyncQueueDao().getPendingItems()

        if (pending.isEmpty()) {
            Log.d(TAG, "No pending items to sync")
            return Result.success()
        }

        Log.d(TAG, "Syncing ${pending.size} items")

        // Mark sebagai syncing
        db.offlineSyncQueueDao().markAsSyncing(pending.map { it.syncId })

        return try {
            val deviceId = getDeviceId()
            val syncData = pending.map { item ->
                SyncItem(
                    syncId = item.syncId,
                    lessonId = item.lessonId,
                    courseId = item.courseId,
                    progressSeconds = item.progressSeconds,
                    isCompleted = item.isCompleted,
                    recordedAt = item.recordedAt
                )
            }

            val response = offlineApiService.syncProgress(
                SyncProgressRequest(
                    deviceId = deviceId,
                    syncData = syncData
                )
            )

            // Hapus yang berhasil di-sync
            if (response.syncedIds.isNotEmpty()) {
                db.offlineSyncQueueDao().deleteBySyncIds(response.syncedIds)
                Log.d(TAG, "Synced ${response.syncedIds.size} items, rejected: ${response.rejectedIds.size}")
            }

            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            // Reset syncing -> pending agar bisa dicoba lagi
            db.offlineSyncQueueDao().resetSyncingToPending()
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun getDeviceId(): String {
        // Ambil dari SharedPreferences yang sudah ada di app
        // Atau gunakan android.provider.Settings.Secure.ANDROID_ID
        val prefs = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getString("device_id", null)
            ?: java.util.UUID.randomUUID().toString().also { id ->
                prefs.edit().putString("device_id", id).apply()
            }
    }
}
```

### 6.2 ConnectivityObserver

```kotlin
// util/ConnectivityObserver.kt

package com.mulaisekarang.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /** Flow yang emit true saat online, false saat offline */
    val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(false) }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Emit status awal
        trySend(isCurrentlyOnline())

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    fun isCurrentlyOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
```

### 6.3 SyncManager (trigger sync dari mana saja)

```kotlin
// domain/SyncManager.kt

package com.mulaisekarang.app.domain

import android.content.Context
import androidx.work.*
import com.mulaisekarang.app.util.ConnectivityObserver
import com.mulaisekarang.app.worker.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val connectivityObserver: ConnectivityObserver
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val workManager = WorkManager.getInstance(context)

    /**
     * Mulai listen perubahan konektivitas dan trigger sync otomatis.
     * Panggil sekali di Application.onCreate() atau di MainActivity.onCreate().
     */
    fun startAutoSync() {
        scope.launch {
            connectivityObserver.isOnline.collect { isOnline ->
                if (isOnline) {
                    triggerSync()
                }
            }
        }
    }

    /** Trigger sync manual (misal: saat app kembali ke foreground) */
    fun triggerSync() {
        workManager.enqueueUniqueWork(
            "sync_progress",
            ExistingWorkPolicy.KEEP,  // Jangan tumpuk jika sedang berjalan
            SyncWorker.buildRequest()
        )
    }
}
```

### 6.4 Progress Recording (saat menonton video)

```kotlin
// domain/ProgressRecorder.kt

package com.mulaisekarang.app.domain

import com.mulaisekarang.app.data.local.AppDatabase
import com.mulaisekarang.app.data.local.entity.OfflineSyncQueueEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rekam progress menonton ke offline_sync_queue setiap 5 detik.
 * Panggil dari VideoPlayerViewModel atau VideoPlayerFragment.
 */
@Singleton
class ProgressRecorder @Inject constructor(
    private val db: AppDatabase
) {
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Mulai rekam progress secara periodik.
     *
     * @param courseId ID course
     * @param lessonId ID lesson
     * @param getPositionSeconds Lambda yang mengembalikan posisi player saat ini (detik)
     * @param getDuration Lambda total durasi video (detik)
     */
    fun startRecording(
        courseId: Int,
        lessonId: Int,
        getPositionSeconds: () -> Int,
        getDuration: () -> Int
    ) {
        stopRecording() // Stop sebelumnya jika ada
        recordingJob = scope.launch {
            while (isActive) {
                delay(5_000L) // Setiap 5 detik
                recordProgress(courseId, lessonId, getPositionSeconds(), getDuration())
            }
        }
    }

    /** Hentikan recording (saat video pause, stop, atau keluar screen) */
    fun stopRecording() {
        recordingJob?.cancel()
        recordingJob = null
    }

    /** Rekam progres satu kali — panggil juga saat video selesai */
    suspend fun recordProgress(
        courseId: Int,
        lessonId: Int,
        positionSeconds: Int,
        durationSeconds: Int,
        forceComplete: Boolean = false
    ) {
        val isCompleted = forceComplete || (durationSeconds > 0 && positionSeconds >= durationSeconds * 0.9)
        val now = DateTimeFormatter.ISO_INSTANT.format(Instant.now())

        db.offlineSyncQueueDao().upsert(
            OfflineSyncQueueEntity(
                syncId = UUID.randomUUID().toString(),
                courseId = courseId,
                lessonId = lessonId,
                progressSeconds = positionSeconds,
                isCompleted = isCompleted,
                recordedAt = now,
                status = "pending"
            )
        )
    }
}
```

---

## 7. Video Player — ExoPlayer Offline-Aware

### 7.1 VideoPlayerViewModel

```kotlin
// ui/lesson/VideoPlayerViewModel.kt

package com.mulaisekarang.app.ui.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.local.AppDatabase
import com.mulaisekarang.app.domain.ProgressRecorder
import com.mulaisekarang.app.util.ConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class VideoSourceState(
    val isLoading: Boolean = true,
    val useLocalFile: Boolean = false,
    val localFilePath: String? = null,     // Jika useLocalFile = true
    val streamUrl: String? = null,         // Jika useLocalFile = false
    val bearerToken: String? = null,       // Untuk streaming online
    val error: String? = null
)

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    private val db: AppDatabase,
    private val progressRecorder: ProgressRecorder,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private val _videoSource = MutableStateFlow(VideoSourceState())
    val videoSource: StateFlow<VideoSourceState> = _videoSource

    /**
     * Tentukan sumber video: lokal (downloaded) atau streaming.
     * Prioritas: lokal > online.
     * Fallback: jika offline dan tidak ada lokal → error.
     */
    fun resolveVideoSource(
        lessonId: Int,
        streamUrl: String,
        bearerToken: String
    ) {
        viewModelScope.launch {
            val downloaded = db.downloadedLessonDao().getByLessonId(lessonId)

            if (downloaded != null && File(downloaded.localPath).exists()) {
                // File tersedia lokal → gunakan file lokal
                _videoSource.value = VideoSourceState(
                    isLoading = false,
                    useLocalFile = true,
                    localFilePath = downloaded.localPath
                )
            } else if (connectivityObserver.isCurrentlyOnline()) {
                // File tidak lokal, tapi online → streaming
                _videoSource.value = VideoSourceState(
                    isLoading = false,
                    useLocalFile = false,
                    streamUrl = streamUrl,
                    bearerToken = bearerToken
                )
            } else {
                // Offline dan tidak ada lokal → error
                _videoSource.value = VideoSourceState(
                    isLoading = false,
                    error = "Video belum diunduh. Sambungkan internet untuk menonton."
                )
            }
        }
    }

    fun startProgressRecording(courseId: Int, lessonId: Int, getPos: () -> Int, getDur: () -> Int) {
        progressRecorder.startRecording(courseId, lessonId, getPos, getDur)
    }

    fun stopProgressRecording() {
        progressRecorder.stopRecording()
    }

    suspend fun markLessonComplete(courseId: Int, lessonId: Int, durationSeconds: Int) {
        progressRecorder.recordProgress(
            courseId = courseId,
            lessonId = lessonId,
            positionSeconds = durationSeconds,
            durationSeconds = durationSeconds,
            forceComplete = true
        )
    }

    override fun onCleared() {
        super.onCleared()
        progressRecorder.stopRecording()
    }
}
```

### 7.2 VideoPlayerFragment / Composable

```kotlin
// ui/lesson/VideoPlayerFragment.kt
// (Contoh untuk Fragment + ExoPlayer, adaptasi ke Compose jika diperlukan)

package com.mulaisekarang.app.ui.lesson

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.mulaisekarang.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class VideoPlayerFragment : Fragment(R.layout.fragment_video_player) {

    private val viewModel: VideoPlayerViewModel by viewModels()
    private var player: ExoPlayer? = null

    // Args (dari Navigation atau intent)
    private val lessonId by lazy { requireArguments().getInt("lesson_id") }
    private val courseId by lazy { requireArguments().getInt("course_id") }
    private val streamUrl by lazy { requireArguments().getString("stream_url", "") }
    private val bearerToken by lazy { requireArguments().getString("bearer_token", "") }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val playerView = view.findViewById<PlayerView>(R.id.player_view)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.resolveVideoSource(lessonId, streamUrl, bearerToken)

            viewModel.videoSource.collect { state ->
                when {
                    state.isLoading -> { /* tampilkan loading indicator */ }

                    state.error != null -> {
                        // Tampilkan error — video tidak bisa diputar
                        showError(state.error)
                    }

                    state.useLocalFile && state.localFilePath != null -> {
                        // ✅ Putar dari file lokal
                        setupPlayerFromFile(playerView, File(state.localFilePath))
                    }

                    state.streamUrl != null -> {
                        // ✅ Putar dari streaming (online)
                        setupPlayerFromStream(playerView, state.streamUrl, state.bearerToken ?: "")
                    }
                }
            }
        }
    }

    /**
     * Setup ExoPlayer untuk file lokal.
     * Tidak butuh auth header — baca langsung dari filesystem.
     */
    private fun setupPlayerFromFile(playerView: PlayerView, file: File) {
        releasePlayer()

        player = ExoPlayer.Builder(requireContext()).build().also { exo ->
            playerView.player = exo

            val mediaItem = MediaItem.fromUri(file.toURI().toString())
            val mediaSource = ProgressiveMediaSource.Factory(
                androidx.media3.datasource.FileDataSource.Factory()
            ).createMediaSource(mediaItem)

            exo.setMediaSource(mediaSource)
            exo.prepare()
            exo.playWhenReady = true

            // Mulai recording progress
            viewModel.startProgressRecording(
                courseId = courseId,
                lessonId = lessonId,
                getPos = { (exo.currentPosition / 1000).toInt() },
                getDur = { (exo.duration / 1000).toInt() }
            )
        }
    }

    /**
     * Setup ExoPlayer untuk streaming online.
     * WAJIB: Auth Bearer token di header request video.
     * Menggunakan DefaultHttpDataSource.Factory — PERSIS seperti yang ada di codebase saat ini.
     */
    private fun setupPlayerFromStream(playerView: PlayerView, url: String, token: String) {
        releasePlayer()

        player = ExoPlayer.Builder(requireContext()).build().also { exo ->
            playerView.player = exo

            // Auth header wajib untuk /api/v1/lessons/{id}/stream
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setDefaultRequestProperties(mapOf("Authorization" to "Bearer $token"))

            val mediaItem = MediaItem.fromUri(url)
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)

            exo.setMediaSource(mediaSource)
            exo.prepare()
            exo.playWhenReady = true

            // Recording progress — sama seperti file lokal
            viewModel.startProgressRecording(
                courseId = courseId,
                lessonId = lessonId,
                getPos = { (exo.currentPosition / 1000).toInt() },
                getDur = { (exo.duration / 1000).toInt() }
            )
        }
    }

    private fun releasePlayer() {
        player?.release()
        player = null
        viewModel.stopProgressRecording()
    }

    override fun onDestroyView() {
        releasePlayer()
        super.onDestroyView()
    }

    private fun showError(message: String) {
        // Tampilkan Snackbar atau dialog
    }
}
```

---

## 8. UI — Download Button & Status

### 8.1 DownloadButtonState + ViewModel integration

```kotlin
// ui/lesson/LessonDetailViewModel.kt (snippet tambahan)

// State untuk tombol download
sealed class DownloadUiState {
    object NotDownloaded : DownloadUiState()
    data class Queued(val progress: Int = 0) : DownloadUiState()
    object Downloading : DownloadUiState()
    object Downloaded : DownloadUiState()
    data class Failed(val message: String) : DownloadUiState()
}

// Di ViewModel, observe Room + WorkManager:
fun observeDownloadState(lessonId: Int): Flow<DownloadUiState> = combine(
    db.downloadedLessonDao().isDownloadedFlow(lessonId),
    downloadManager.getDownloadStatusLiveData(lessonId).asFlow()
) { isDownloaded, workInfos ->
    when {
        isDownloaded -> DownloadUiState.Downloaded
        else -> when (downloadManager.getSimpleStatus(workInfos)) {
            OfflineDownloadManager.DownloadStatus.DOWNLOADING -> DownloadUiState.Downloading
            OfflineDownloadManager.DownloadStatus.QUEUED -> DownloadUiState.Queued()
            OfflineDownloadManager.DownloadStatus.FAILED -> DownloadUiState.Failed("Download gagal. Coba lagi.")
            else -> DownloadUiState.NotDownloaded
        }
    }
}

// Action saat tombol download di-tap:
fun onDownloadTapped(lessonId: Int, courseId: Int) {
    viewModelScope.launch {
        val isDownloaded = db.downloadedLessonDao().isDownloaded(lessonId)
        if (isDownloaded) {
            // Hapus file lokal
            val entity = db.downloadedLessonDao().getByLessonId(lessonId)
            entity?.let {
                File(it.localPath).delete()
                db.downloadedLessonDao().deleteByLessonId(lessonId)
            }
        } else {
            // Mulai download
            downloadManager.enqueueDownload(lessonId, courseId)
        }
    }
}
```

### 8.2 Tampilan tombol download (XML)

```xml
<!-- res/layout/item_lesson.xml — snippet tombol download -->

<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_download"
    style="@style/Widget.Material3.Button.IconButton"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:icon="@drawable/ic_download_24"
    android:contentDescription="Unduh untuk offline" />

<!-- State: sudah didownload → icon check hijau -->
<!-- State: sedang download → CircularProgressIndicator di samping -->
<!-- State: gagal → icon warning merah, retry on tap -->
```

### 8.3 UI Halaman "Unduhan Saya" (ringkasan)

Tampilkan semua lesson yang sudah didownload:

```kotlin
// Dari DAO:
db.downloadedLessonDao().getAllFlow()

// Tampilkan: nama course, nama lesson, ukuran file, tombol hapus
// Total penyimpanan terpakai: db.downloadedLessonDao().totalDownloadedBytes()
```

---

## 9. Permissions & Manifest

### 9.1 Permissions yang diperlukan

```xml
<!-- AndroidManifest.xml -->

<!-- Network -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Storage: TIDAK perlu WRITE_EXTERNAL_STORAGE karena pakai filesDir (internal) -->
<!-- Untuk Android 9+, filesDir tidak butuh permission apapun -->

<!-- WorkManager di background -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

<!-- Notifikasi download progress (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### 9.2 Tidak diperlukan (dan jangan diminta)

- `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` — tidak perlu karena simpan di `filesDir`
- `MANAGE_EXTERNAL_STORAGE` — tidak perlu

---

## 10. Alur Lengkap End-to-End

### Alur A: Download Video untuk Pertama Kali

```
User tap tombol "Unduh" di halaman lesson
    │
    ▼
LessonDetailViewModel.onDownloadTapped(lessonId, courseId)
    │
    ▼
OfflineDownloadManager.enqueueDownload(lessonId, courseId)
    │── WorkManager enqueue OneTimeWorkRequest
    │
    ▼
[WorkManager menunggu internet tersedia]
    │
    ▼
DownloadWorker.doWork()
    │── Cek isDownloaded() → false, lanjut
    │── Cek storage space → ok, lanjut
    │── offlineApiService.getDownloadUrl(lessonId)
    │       ↑ GET /api/v2/mobile/lessons/{id}/download-url
    │       ↓ { download_url, expires_at, file_size_bytes }
    │── downloadFile(downloadUrl, outputFile)
    │       ↑ GET /api/v2/mobile/lessons/{id}/file?token=xxx  (NO auth header)
    │       ↓ Byte stream video
    │── Simpan ke filesDir/vid_a1b2c3d4.dat
    │── db.downloadedLessonDao().insert(entity)
    │
    ▼
WorkInfo.State.SUCCEEDED
    │
    ▼
UI update: tombol download → icon ✓ (Downloaded)
```

### Alur B: Menonton Video (Offline/Online aware)

```
User buka lesson
    │
    ▼
GET /api/v1/courses/{course}/lessons/{lesson}
    { video_stream_url, is_accessible, ... }
    │
    ▼
VideoPlayerViewModel.resolveVideoSource(lessonId, streamUrl, token)
    │
    ├── db.downloadedLessonDao().getByLessonId(lessonId) → ada?
    │   └── YES + file exists → VideoSourceState(useLocalFile=true, localFilePath=...)
    │
    └── NO → isCurrentlyOnline()?
        ├── YES → VideoSourceState(useLocalFile=false, streamUrl=..., bearerToken=...)
        └── NO  → VideoSourceState(error="Video belum diunduh...")
    │
    ▼
ExoPlayer setup berdasarkan VideoSourceState
    │
    ▼
ProgressRecorder.startRecording() — setiap 5 detik tulis ke offline_sync_queue
```

### Alur C: Auto-Sync saat Kembali Online

```
Perangkat kembali terhubung internet
    │
    ▼
ConnectivityObserver.isOnline.collect { true }
    │
    ▼
SyncManager.triggerSync()
    │
    ▼
WorkManager enqueue SyncWorker (KEEP — tidak duplikat)
    │
    ▼
SyncWorker.doWork()
    │── db.offlineSyncQueueDao().getPendingItems()
    │── markAsSyncing(syncIds)
    │── offlineApiService.syncProgress(SyncProgressRequest(...))
    │       ↑ POST /api/v2/mobile/sync-progress
    │       ↓ { status, synced_ids, rejected_ids }
    │── db.offlineSyncQueueDao().deleteBySyncIds(response.syncedIds)
    │
    ▼
Queue lokal bersih ✓
```

---

## 11. Checklist QA Sebelum Rilis

### Download
| # | Skenario | Expected |
|---|---|---|
| 1 | Tap unduh saat online | Download berjalan di background, tombol berubah jadi progress indicator |
| 2 | Matikan internet saat download berjalan | Download pause otomatis, lanjut saat internet kembali (WorkManager retry) |
| 3 | Tap unduh saat storage < 500MB | Muncul pesan "Penyimpanan hampir penuh" |
| 4 | Download selesai | Tombol berubah icon ✓ hijau |
| 5 | Tap icon ✓ (sudah downloaded) | File dihapus, tombol kembali ke icon download |
| 6 | Download lesson yang sama 2x | WorkManager KEEP — tidak duplikat |
| 7 | File video ada di filesDir bukan sdcard | `ls /data/data/<package>/files/vid_*.dat` → ada |
| 8 | Nama file tidak bisa ditebak | Buka file manager, nama file adalah `vid_xxxxxxxx.dat` bukan `videoname.mp4` |

### Pemutaran Offline
| # | Skenario | Expected |
|---|---|---|
| 9 | Aktifkan airplane mode → buka lesson yang sudah didownload | Video memutar dari file lokal, tidak ada buffering |
| 10 | Aktifkan airplane mode → buka lesson yang belum didownload | Muncul pesan error "Video belum diunduh. Sambungkan internet..." |
| 11 | Online → buka lesson yang didownload | Tetap putar dari file lokal (offline-first) |
| 12 | Online → buka lesson yang belum didownload | Streaming normal (tidak terpengaruh) |

### Progress Sync
| # | Skenario | Expected |
|---|---|---|
| 13 | Tonton 2 menit offline | Setelah 5 detik pertama, baris muncul di offline_sync_queue |
| 14 | Kembali online setelah offline | SyncWorker jalan otomatis, queue kosong setelahnya |
| 15 | Selesaikan lesson di offline | `is_completed=true` tersync ke server, muncul di web progress |
| 16 | Server lebih baru dari client (LWW) | Server tidak menimpa data yang lebih baru |
| 17 | Sync gagal (timeout) | Status kembali ke `pending`, retry di koneksi berikutnya |

### Edge Cases
| # | Skenario | Expected |
|---|---|---|
| 18 | Uninstall & reinstall app | File video hilang (filesDir dihapus), queue hilang — normal |
| 19 | Lesson dihapus dari course di web | `rejected_ids` muncul di sync response, baris dihapus dari queue |
| 20 | Token kadaluarsa saat download file | `DownloadWorker` retry, request `download-url` baru di retry berikutnya |
| 21 | Signed URL dipakai 2x | Server mengembalikan 403 pada percobaan kedua (token already used) — worker retry & minta URL baru |

---

## 12. Error Handling & Edge Cases

### 12.1 Token Kedaluwarsa saat Download

Signed URL dari `/api/v2/mobile/lessons/{id}/download-url` valid **1 jam**. Jika download belum selesai dalam 1 jam (file besar + koneksi lambat), server akan return `403`.

**Solusi:** Di `DownloadWorker`, tangkap `HTTP 403` dan throw exception untuk memicu WorkManager retry. Saat retry, worker akan memanggil `getDownloadUrl()` lagi untuk mendapat URL baru.

```kotlin
// Di downloadFile():
if (response.code == 403) {
    throw Exception("Token expired, retry will get new URL")
}
```

### 12.2 File Corrupt saat Download Terputus

Jika download terputus di tengah jalan, file di `filesDir` bisa partial. Solusi:

```kotlin
// Di DownloadWorker.doWork():
// Jika file sudah ada tapi ukurannya < expectedSize → resume via Range header
// Jika ukuran tidak bisa diverifikasi (expectedSize = 0) → hapus dan download ulang
if (outputFile.exists() && outputFile.length() == expectedSize) {
    // File sudah lengkap, skip download
    return Result.success()
}
```

### 12.3 Offline Manifest Stale

Manifest yang dicache di Room bisa stale jika instructor mengubah kurikulum. Strategi:

- Invalidasi cache saat user buka course online: fetch manifest baru dari server, update `cached_courses`
- Tampilkan indikator "offline mode — konten mungkin berbeda" saat menampilkan dari cache

### 12.4 Multiple Device Sync

Jika user menonton di 2 device berbeda tanpa koneksi, dan keduanya online bersamaan:

- Device A mengirim progress pukul 10:30
- Device B mengirim progress pukul 10:35 untuk lesson yang sama

Server menggunakan **Last Write Wins (LWW)**: yang `recorded_at`-nya lebih baru (Device B) yang menang. Data Device A diabaikan tapi tetap di-`synced_ids` agar app bisa hapus dari queue.

**Konsekuensi yang harus dikomunikasikan ke user:** "Progres belajar dari semua perangkat disinkronkan secara otomatis. Progres terbaru akan digunakan."

---

## 13. API Contracts (Ringkasan)

Semua endpoint baru ada di base URL: `https://mulaisekarang.com/api/v2/mobile/`

### 13.1 GET `/courses/{courseId}/offline-manifest`
- **Auth:** `Authorization: Bearer {sanctum_token}`
- **Error 403:** User tidak enrollment di course ini

**Response:**
```json
{
  "course": {
    "id": 1,
    "title": "Dasar-Dasar Pemrograman Web",
    "cover_image_url": "https://...",
    "topics": [
      {
        "id": 1,
        "title": "Pengenalan HTML",
        "order": 1,
        "lessons": [
          {
            "id": 96,
            "title": "Video Pembuka",
            "order": 1,
            "duration_hours": 0,
            "duration_minutes": 10,
            "duration_seconds": 0,
            "is_completed": false
          }
        ]
      }
    ]
  },
  "generated_at": "2026-09-09T19:00:00+00:00"
}
```

### 13.2 GET `/lessons/{lessonId}/download-url`
- **Auth:** `Authorization: Bearer {sanctum_token}`
- **Error 403:** Lesson tidak accessible (belum enrollment)
- **Error 404:** File video belum ada di server (lesson belum ada videonya)

**Response:**
```json
{
  "download_url": "https://mulaisekarang.com/api/v2/mobile/lessons/96/file?token=AbCd...xYz",
  "expires_at": "2026-09-09T20:00:00+00:00",
  "file_size_bytes": 52428800
}
```

### 13.3 GET `/lessons/{lessonId}/file?token={token}`
- **Auth:** TIDAK perlu Bearer — token query string menggantikan auth
- **Error 400:** Token tidak disertakan
- **Error 403:** Token invalid / expired / sudah dipakai
- **Error 404:** File tidak ditemukan di server

**Response:** Video stream (HTTP 200 atau 206 untuk Range request)
- `Content-Type: video/mp4`
- `Accept-Ranges: bytes`
- Support partial content (HTTP 206) untuk resume download

**⚠️ Catatan penting:** Token **single-use** — setelah digunakan satu kali, server menandai `used_at` dan request berikutnya dengan token yang sama akan mendapat `403`. Minta URL baru jika download gagal.

### 13.4 POST `/sync-progress`
- **Auth:** `Authorization: Bearer {sanctum_token}`
- **Content-Type:** `application/json`

**Request:**
```json
{
  "device_id": "550e8400-e29b-41d4-a716-446655440000",
  "sync_data": [
    {
      "sync_id": "123e4567-e89b-12d3-a456-426614174000",
      "lesson_id": 96,
      "course_id": 1,
      "progress_seconds": 450,
      "is_completed": true,
      "recorded_at": "2026-09-09T19:30:00Z"
    }
  ]
}
```

**Validasi:**
- `device_id`: required, string, max 128 karakter
- `sync_data`: required, array, max 100 item per request
- `sync_data[].sync_id`: required, valid UUID
- `sync_data[].lesson_id`: required, integer
- `sync_data[].course_id`: required, integer
- `sync_data[].is_completed`: required, boolean
- `sync_data[].recorded_at`: required, date string ISO 8601
- `sync_data[].progress_seconds`: required, integer, min 0

**Response:**
```json
{
  "status": "success",
  "synced_ids": ["123e4567-e89b-12d3-a456-426614174000"],
  "rejected_ids": []
}
```

**Logika server (LWW):**
- Item di `synced_ids`: berhasil diproses (termasuk yang diabaikan karena server lebih baru — tetap di-return di synced_ids agar app bisa hapus dari queue)
- Item di `rejected_ids`: lesson tidak ditemukan atau user tidak punya akses — hapus dari queue juga (jangan retry selamanya)

**Setelah menerima response:** Hapus semua `sync_id` yang ada di `synced_ids` DAN `rejected_ids` dari tabel `offline_sync_queue` lokal.

---

## Catatan Akhir untuk Tim Android

1. **Tidak ada perubahan ke kode v1 yang sudah ada** — seluruh fitur offline adalah additive
2. **Simpan token Sanctum di EncryptedSharedPreferences** (sudah seharusnya di codebase lama)
3. **Tidak simpan video di External Storage** — selalu `context.filesDir` untuk keamanan
4. **Test wajib di airplane mode** sebelum rilis — ini skenario utama fitur ini
5. **WorkManager tidak butuh internet di semua tahap** — download butuh internet, tapi insert ke Room bisa kapan saja
6. **Hilt/Koin wajib** untuk inject dependency ke WorkManager Worker via custom WorkerFactory
7. **signed URL adalah single-use** — jangan cache URL, minta baru setiap kali butuh download
8. **`device_id`** harus konsisten antar sesi — simpan di SharedPreferences saat pertama generate, jangan buat baru setiap sync
