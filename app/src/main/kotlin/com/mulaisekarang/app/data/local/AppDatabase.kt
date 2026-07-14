package com.mulaisekarang.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mulaisekarang.app.data.local.course.CourseDao
import com.mulaisekarang.app.data.local.course.CourseDetailEntity
import com.mulaisekarang.app.data.local.course.CourseEntity
import com.mulaisekarang.app.data.local.course.RemoteKeyEntity

@Database(
    entities = [CourseEntity::class, CourseDetailEntity::class, RemoteKeyEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
}
