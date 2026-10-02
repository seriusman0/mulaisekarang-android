
package com.mulaisekarang.app.util

import androidx.compose.runtime.mutableStateOf
import androidx.media3.exoplayer.ExoPlayer

object GlobalVideoManager {
    var exoPlayer: ExoPlayer? = null
    val streamUrl = mutableStateOf<String?>(null)
    val isMiniPlayerVisible = mutableStateOf(false)
    val isPlayingLesson = mutableStateOf(false)

    fun registerPlayer(player: ExoPlayer, url: String) {
        exoPlayer = player
        streamUrl.value = url
        isMiniPlayerVisible.value = false
        isPlayingLesson.value = true
    }

    fun enterMiniPlayer() {
        if (exoPlayer != null && exoPlayer!!.isPlaying) {
            isMiniPlayerVisible.value = true
            isPlayingLesson.value = false
        } else {
            close()
        }
    }

    fun close() {
        isMiniPlayerVisible.value = false
        isPlayingLesson.value = false
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
        streamUrl.value = null
    }
}