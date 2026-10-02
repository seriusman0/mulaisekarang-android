package com.mulaisekarang.app.ui.components

import com.mulaisekarang.app.util.GlobalVideoManager
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

private const val PositionSaveIntervalMs = 5_000L


@OptIn(markerClass = [UnstableApi::class])
@Composable
fun VideoPlayer(
    streamUrl: String,
    authToken: String?,
    videoCache: Cache,
    lessonId: Int = 0,
    modifier: Modifier = Modifier,
    startPositionMs: Long = 0L,
    onPositionChanged: (Long) -> Unit = {},
    onPlaybackError: (PlaybackException) -> Unit = {},
) {
    val context = LocalContext.current
    val onPositionChangedState = rememberUpdatedState(onPositionChanged)
    val onPlaybackErrorState = rememberUpdatedState(onPlaybackError)

    val exoPlayer = remember(streamUrl, authToken) {
        if (GlobalVideoManager.streamUrl.value == streamUrl && GlobalVideoManager.exoPlayer != null) {
            GlobalVideoManager.isPlayingLesson.value = true
            GlobalVideoManager.isMiniPlayerVisible.value = false
            GlobalVideoManager.exoPlayer!!
        } else {
            GlobalVideoManager.close() // Close any existing player for a different video
            val httpDataSourceFactory = DefaultHttpDataSource.Factory().apply {
                if (authToken != null) {
                    setDefaultRequestProperties(mapOf("Authorization" to "Bearer $authToken"))
                }
            }
            
            val customDataSourceFactory = androidx.media3.datasource.DataSource.Factory {
                if (streamUrl.startsWith("file://")) {
                    com.mulaisekarang.app.util.EncryptedFileDataSource(lessonId)
                } else {
                    androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory).createDataSource()
                }
            }

            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(videoCache)
                .setUpstreamDataSourceFactory(customDataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            val newPlayer = ExoPlayer.Builder(context.applicationContext)
                .setMediaSourceFactory(DefaultMediaSourceFactory(context.applicationContext).setDataSourceFactory(cacheDataSourceFactory))
                .build()
                .apply {
                    addListener(object : Player.Listener {
                        override fun onPlayerError(error: PlaybackException) {
                            onPlaybackErrorState.value(error)
                        }
                    })
                    setMediaItem(MediaItem.fromUri(streamUrl))
                    if (startPositionMs > 0L) seekTo(startPositionMs)
                    prepare()
                }
            GlobalVideoManager.registerPlayer(newPlayer, streamUrl)
            newPlayer
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(PositionSaveIntervalMs)
            onPositionChangedState.value(exoPlayer.currentPosition)
        }
    }

    DisposableEffect(exoPlayer) {
        GlobalVideoManager.isPlayingLesson.value = true
        onDispose {
            onPositionChangedState.value(exoPlayer.currentPosition)
            if (GlobalVideoManager.exoPlayer == exoPlayer) {
                GlobalVideoManager.enterMiniPlayer()
            }
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        factory = { ctx -> PlayerView(ctx).apply { useController = true } },
        update = { it.player = exoPlayer },
    )
}
