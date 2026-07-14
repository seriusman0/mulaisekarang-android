package com.mulaisekarang.app.data.local.course

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {

    @Query("SELECT * FROM courses ORDER BY sortOrder ASC")
    fun pagingSource(): PagingSource<Int, CourseEntity>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getById(id: Int): CourseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(courses: List<CourseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCourse(course: CourseEntity)

    @Query("DELETE FROM courses")
    suspend fun clearCourses()

    @Query("SELECT * FROM course_details WHERE id = :id")
    fun observeDetail(id: Int): Flow<CourseDetailEntity?>

    @Query("SELECT * FROM course_details WHERE id = :id")
    suspend fun getDetailById(id: Int): CourseDetailEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDetail(detail: CourseDetailEntity)

    @Query("SELECT * FROM course_remote_keys WHERE id = :id")
    suspend fun remoteKey(id: Int = RemoteKeyEntity.SINGLETON_ID): RemoteKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRemoteKey(key: RemoteKeyEntity)

    @Query("DELETE FROM course_remote_keys")
    suspend fun clearRemoteKeys()
}
