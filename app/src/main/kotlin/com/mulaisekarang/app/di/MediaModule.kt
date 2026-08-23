package com.mulaisekarang.app.di

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

/** Chunks are cached to disk so a lesson's first ~10s replay instantly and a
 * lesson the student re-opens doesn't re-download bytes already on device.
 */
private const val VIDEO_CACHE_MAX_BYTES = 300L * 1024 * 1024 // 300 MB

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Provides
    @Singleton
    @UnstableApi
    fun provideVideoCache(@ApplicationContext context: Context): Cache {
        val cacheDir = File(context.cacheDir, "video_cache")
        val evictor = LeastRecentlyUsedCacheEvictor(VIDEO_CACHE_MAX_BYTES)
        val databaseProvider = StandaloneDatabaseProvider(context)
        return SimpleCache(cacheDir, evictor, databaseProvider)
    }
}
