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
