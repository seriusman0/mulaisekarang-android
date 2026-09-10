package com.mulaisekarang.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mulaisekarang.app.data.local.course.CourseDao
import com.mulaisekarang.app.data.local.course.CourseDetailEntity
import com.mulaisekarang.app.data.local.course.CourseEntity
import com.mulaisekarang.app.data.local.course.RemoteKeyEntity

import com.mulaisekarang.app.data.local.dao.CachedCourseDao
import com.mulaisekarang.app.data.local.dao.DownloadedLessonDao
import com.mulaisekarang.app.data.local.dao.OfflineSyncQueueDao
import com.mulaisekarang.app.data.local.entity.CachedCourseEntity
import com.mulaisekarang.app.data.local.entity.DownloadedLessonEntity
import com.mulaisekarang.app.data.local.entity.OfflineSyncQueueEntity

@Database(
    entities = [
        CourseEntity::class, 
        CourseDetailEntity::class, 
        RemoteKeyEntity::class,
        OfflineSyncQueueEntity::class,
        DownloadedLessonEntity::class,
        CachedCourseEntity::class
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun offlineSyncQueueDao(): OfflineSyncQueueDao
    abstract fun downloadedLessonDao(): DownloadedLessonDao
    abstract fun cachedCourseDao(): CachedCourseDao
}
