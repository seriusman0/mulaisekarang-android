package com.mulaisekarang.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

private val Context.videoProgressDataStore by preferencesDataStore(name = "video_progress")

/**
 * Persists last-watched playback position per lesson so the player resumes
 * where the student left off instead of always starting at 00:00 — the UI
 * doesn't need to wait on a server round-trip to know this.
 */
@Singleton
class VideoProgressStore @Inject constructor(@ApplicationContext private val context: Context) {

    private fun key(lessonId: Int) = longPreferencesKey("lesson_${lessonId}_position_ms")

    suspend fun lastPositionMs(lessonId: Int): Long =
        context.videoProgressDataStore.data.first()[key(lessonId)] ?: 0L

    suspend fun savePositionMs(lessonId: Int, positionMs: Long) {
        context.videoProgressDataStore.edit { it[key(lessonId)] = positionMs }
    }

    suspend fun clearPosition(lessonId: Int) {
        context.videoProgressDataStore.edit { it.remove(key(lessonId)) }
    }
}
