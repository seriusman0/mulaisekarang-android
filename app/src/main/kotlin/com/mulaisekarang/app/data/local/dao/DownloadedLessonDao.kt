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
