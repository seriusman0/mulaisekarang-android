package com.mulaisekarang.app.di

import android.content.Context
import androidx.room.Room
import com.mulaisekarang.app.data.local.AppDatabase
import com.mulaisekarang.app.data.local.Converters
import com.mulaisekarang.app.data.local.course.CourseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context, converters: Converters): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "mulaisekarang.db")
            .addTypeConverter(converters)
            .build()

    @Provides
    fun provideCourseDao(database: AppDatabase): CourseDao = database.courseDao()
}
