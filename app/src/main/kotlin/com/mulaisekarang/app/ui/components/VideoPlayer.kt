package com.mulaisekarang.app.ui.components

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
    modifier: Modifier = Modifier,
    startPositionMs: Long = 0L,
    onPositionChanged: (Long) -> Unit = {},
    onPlaybackError: (PlaybackException) -> Unit = {},
) {
    val context = LocalContext.current
    val onPositionChangedState = rememberUpdatedState(onPositionChanged)
    val onPlaybackErrorState = rememberUpdatedState(onPlaybackError)

    val exoPlayer = remember(streamUrl, authToken) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory().apply {
            if (authToken != null) {
                setDefaultRequestProperties(mapOf("Authorization" to "Bearer $authToken"))
            }
        }
        // Cache-backed data source: chunks already played (or pre-buffered
        // while the student reads the lesson description) persist to disk,
        // so replays and revisits don't re-download the same bytes.
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(videoCache)
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context, httpDataSourceFactory))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(cacheDataSourceFactory))
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
    }

    // Periodically persist playback position while the player exists, so a
    // student who backgrounds the app mid-lesson resumes near where they
    // left off rather than losing progress entirely.
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(PositionSaveIntervalMs)
            onPositionChangedState.value(exoPlayer.currentPosition)
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            onPositionChangedState.value(exoPlayer.currentPosition)
            exoPlayer.release()
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
